package de.pbma.nearfly;


import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.nearby.connection.Payload;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import de.pbma.mqtt.MqttAdapter;
import de.pbma.mqtt.MyMqttListener;
import de.pbma.nearbyconnections.NeConAdapter;

import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * A Bound Service that wrap up the feature functions from the Google Nearby API and MQTT Paho.
 *
 * <p>The {@code NeaflyService} is started according to the usual procedure for starting
 * bound service via {@link AppCompatActivity#startService(Intent)}}. After the activity is bound,
 * {@code NearflyService} can be used. In order to facilitate the integration of
 * the {@code NeaflyService} the Activity can inherit from the {@link NearflyBindingActivity}.
 *
 * <h3>Usage Examples</h3>
 * <p>
 * Here is an example of an activity that extends from the {@link NearflyBindingActivity}
 * and publishes message as soon as it is connected to the {@code NeaflyService}.
 *
 * <pre> {@code
 *
 * public class NearflySampleActivity extends NearflyBindingActivity {
 *
 *     private final String NEARFLY_CHANNEL = "sensors/humidity";
 *     private boolean neaflyServiceConnectCalled = false;
 *
 *     \@Override
 *     public void onNearflyServiceUnbound() {
 *         if (!neaflyServiceConnectCalled) {
 *             nearflyService.addSubCallback(nearflyListener);
 *             nearflyService.connect("ThisIsMyUniqueRoomString", NearflyService.USE_NEARBY);
 *             nearflyService.subIt(NEARFLY_CHANNEL);
 *             neaflyServiceConnectCalled = true;
 *         }
 *     }
 *
 *     \@Override
 *     public void onNearflyServiceUnbound() {
 *     }
 *
 *     NearflyListener nearflyListener = new NearflyListener() {
 *         @Override
 *         public void onLogMessage(String state) {
 *             // Log.v("measureTest", output);
 *             switch (state){
 *                 case NearflyService.State.CONNECTED:
 *                     Log.v("measureTest", "Hello World!");
 *                     nearflyService.pubIt(NEARFLY_CHANNEL, "Hello World!");
 *                     // OR
 *                     // nearflyService.pubIt(NEARFLY_CHANNEL, "Hello World!", -10, true);
 *                     break;
 *                 case NearflyService.State.DISCONNECTED:
 *                     Log.v("measureTest", "disconnected");
 *                     break;
 *             }
 *         }
 *
 *         @Override
 *         public void onMessage(String channel, String message) {
 *             Log.v("measureTest",channel + " " + message);
 *         }
 *
 *         @Override
 *         public void onFile(String path, String textAttachment) {
 *         }
 *     };
 *
 *     @Override
 *     protected void onCreate(@Nullable Bundle savedInstanceState) {
 *         super.onCreate(savedInstanceState);
 *         setContentView(R.layout.main);
 *     }
 * }
 * </pre>
 *
 * @author Alexis Danilo Morgado dos Santos
 * @edited 01.05.2020
 * */
@Deprecated
public class NearflyServiceOld extends Service {
    /** Maximal payload size for the  {@link #pubIt(String, String, Integer, boolean)}
     * command(in KBytes)**/
    private static final int MAX_PUBIT_MESSAGE_SIZE = 30_000;

    /* This are the both Modes that can be used for Nearfly */
    @Retention(SOURCE)
    @IntDef({USE_MQTT, USE_NEARBY})
    public @interface ConnectionMode {
    }

    public final static int USE_NEARBY = 1;
    public final static int USE_MQTT = 2;
    private final static int PAUSED = 0;

    /**
     * Technology to use(MQTT or Nearby)
     **/
    @ConnectionMode
    public int mConnectionMode;
    private int mModeSwitcher = PAUSED;

    private String TAG = "NearflyServices";
    private ArrayList<String> subscribedChannels = new ArrayList<>();

    public final static String ACTION_START = "start";
    public final static String ACTION_STOP = "stop";
    public final static String ACTION_BIND = "bind";

    private boolean mConnected = false;
    ReentrantLock connectedLock = new ReentrantLock();
    private ExecutorService callbackExecutor;

    /**
     * Defines the visibility mContext
     **/
    private String room = this.getClass().getCanonicalName();

    /**
     * For the Priority
     **/
    PriorityBlockingQueue<NearflyNice.NearflyMessage> nearflyQueue = new PriorityBlockingQueue<>();
    NearflyNice nearflyNice = new NearflyNice();

    /**
     * Listeners
     **/
    // private NearflyListener nearflyListener;
    final private CopyOnWriteArrayList<NearflyListener> listeners = new CopyOnWriteArrayList<>();
    private NeConAdapter neConAdapter = new NeConAdapter();
    private MqttAdapter mqttAdapter = new MqttAdapter();

    /**
     * Helps to interpret the {@link NearflyListener#onLogMessage(String)} correctly
     * <p></p>
     * <p>
     * {@link State#CONNECTED}: {@Code NearflyService} Was successfully connected.
     * {@see NearflyService#mConnected}
     * </p>
     * <p>
     * {@link State#DISCONNECTED}: {@Code NearflyService} Was disconnected.
     * {@see NearflyService#mConnected}
     * </p>
     **/
    public static class State {
        public final static String CONNECTED = "connected";
        public final static String DISCONNECTED = "disconnected";
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mqttAdapter.registerMqttListener(getApplicationContext(), mMqttListener);
        neConAdapter.registerListener(getApplicationContext(), mNeConListener);

        // ---------------
        new Thread(() -> {

            for (; ; ) {
                try {
                    while(!isConnected()){
                        synchronized (connectedLock) {
                            connectedLock.wait();
                        }
                    }

                    NearflyNice.NearflyMessage msg;
                    msg = nearflyQueue.take();

                    if (msg instanceof NearflyNice.NearflyFileMessage) {
                        NearflyNice.NearflyFileMessage fileMsg = (NearflyNice.NearflyFileMessage)msg;
                        String channel = fileMsg.getChannel();
                        Uri uri = fileMsg.getUri();
                        String textAttachment = fileMsg.getTextAttachment();

                        switch (mConnectionMode) {
                            case USE_NEARBY:
                                neConAdapter.pubFile(room + "/" + channel, uri, textAttachment);
                                break;
                            case USE_MQTT:
                                mqttAdapter.pubFile(room + "/" + channel, uri, textAttachment);
                                break;
                        }
                    }

                    if (msg instanceof NearflyNice.NearflyTextMessage) {
                        NearflyNice.NearflyTextMessage textMsg = (NearflyNice.NearflyTextMessage)msg;
                        byte[] channel = textMsg.getChannel().getBytes();
                        // byte[] payload = textMsg.getPayload().getBytes();
                        byte[] payload = textMsg.getPayload();

                        switch (mConnectionMode) {
                            case USE_NEARBY:
                                neConAdapter.publishIt(room + "/" + channel, payload);
                                break;
                            case USE_MQTT:
                                mqttAdapter.publishIt(room + "/" + channel, payload);
                                break;
                        }
                    }


                } catch (InterruptedException e) {
                    // e.printStackTrace();
                    break;
                }
            }

        }).start();
    }

    /**
     * Returns {@code true} if either in {@link ConnectionMode} {@link #USE_MQTT}, there is
     * a connection to the mqtt server or in {@link ConnectionMode} {@link #USE_NEARBY} the
     * device is connected to at least one other node.
     *
     * @return {@code true} the underlying technology has a connection
     */
    public boolean isConnected() {
        return mConnected;
    }

    public void setConnected(boolean connected) {
        mConnected = connected;

        if (connected){
            synchronized (connectedLock){
                connectedLock.notifyAll();
            }
        }
    }

    private NeConAdapter.NeConListener mNeConListener = new NeConAdapter.NeConListener() {
        @Override
        public void onLogMessage(CharSequence msg) {
            ThisOnLogMessage(String.valueOf(msg));

            if (msg.equals("connected")) {
                setConnected(true);

                if (isConnected() && mModeSwitcher == USE_NEARBY) {
                    // Actually change of Technology
                    mConnectionMode = USE_NEARBY;
                    mModeSwitcher = PAUSED;
                    onLogMessage("ConnectionMode changed to"+mConnectionMode);
                    disconnectToMQTT();
                }
                // Change ConnectionMode before Connect-Event is triggert
                ThisOnLogMessage(State.CONNECTED);
            }
            if (msg.equals("disconnected")) {
                setConnected(false);
                ThisOnLogMessage(State.DISCONNECTED);
            }
        }

        @Override
        public void onStateChanged(String state) {
            onLogMessage("STATE CHANGED: " + state);
        }

        @Override
        public void onMessage(String channel, byte[] message) {
            ThisOnMessage(channel, new String(message));
        }

        @Override
        public void onFile(String channel, String path, String textAttachment) {
            ThisOnFile(channel, path, textAttachment);
        }

        @Override
        public void onBigBytes(String channel, byte[] bigBytes) {

        }
    };

    private MyMqttListener mMqttListener = new MyMqttListener() {
        @Override
        public void onMessage(String topic, String message) {
            ThisOnMessage(topic, message);
        }

        @Override
        public void onStatus(boolean connected) {
            if (connected) {
                setConnected(true);

                if (mModeSwitcher == USE_MQTT) {
                    mConnectionMode = USE_MQTT;
                    mModeSwitcher = PAUSED;
                    onLogMessage("ConnectionMode changed to"+mConnectionMode);
                    disconnectToNearby();
                }
                // Change ConnectionMode before Connect-Event is triggert
                ThisOnLogMessage(State.CONNECTED);

            } else {
                setConnected(false);
                ThisOnLogMessage(State.DISCONNECTED);
            }
        }

        @Override
        public void onLogMessage(String message) {
            ThisOnLogMessage(message);
        }

        @Override
        public void onFile(String channel, String path, String textAttachment) {
            ThisOnFile(channel, path, textAttachment);
        }
    };


    private void ThisOnLogMessage(String output){
        if (callbackExecutor==null)
            return;

        callbackExecutor.execute(() ->{
            for (NearflyListener nearflyListener : listeners)
                nearflyListener.onLogMessage(output);
        });
    }

    private void ThisOnMessage(String topic, String message) {
        if (callbackExecutor==null)
            return;

        callbackExecutor.execute(() -> {
            for (NearflyListener nearflyListener : listeners)
                nearflyListener.onMessage(topic, message);
        });
    }

    private void ThisOnFile(String channel, String path, String textAttachment) {
        if (callbackExecutor==null)
            return;

        callbackExecutor.execute(() -> {
            for (NearflyListener nearflyListener : listeners)
                nearflyListener.onFile(channel, path, textAttachment);
        });
    }

    private static final String[] REQUIRED_PERMISSIONS = {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            };

    private static final String[] STORAGE_PERMISSIONS = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            };

    private static final String[] FULL_PERMISSIONS ={
            REQUIRED_PERMISSIONS[0],
            REQUIRED_PERMISSIONS[1],
            STORAGE_PERMISSIONS[0],
            STORAGE_PERMISSIONS[1]
    };

    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 18504;

    /** Asks for the permission, which are needed by the NearflyService.
     * @param app also asks for permissions READ_EXTERNAL_STORAGE and WRITE_EXTERNAL_STORAGE,
     *           which are required to execute the pubfile method
     * @return {@Code true} if the permissions to be granted have already been granted.
     */
    public boolean askForPermissions(AppCompatActivity app, boolean filePermission){
        if (hasPermissions(app, REQUIRED_PERMISSIONS))
            return true;

        app.requestPermissions(filePermission?FULL_PERMISSIONS:REQUIRED_PERMISSIONS,
                REQUEST_CODE_REQUIRED_PERMISSIONS);
        return false;
    }

    private static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Subscribes to the given channel so that the {@link NearflyListener} will be dissolved
     * if a publish is made to the specified channel in the future.
     *
     * @param channel to subscribed channel
     **/
    public void subIt(String channel) {
        // neConAdapter.addSubCallback("measureTest", neConAdapter);
        /*neConAdapter.subscirbe(channel);
        mqttAdapter.subscribe(channel);*/

        if (!subscribedChannels.contains(channel))
            subscribedChannels.add(channel);

        /*if (!isConnected())
            return;*/

        if (mConnectionMode == USE_NEARBY)
            neConAdapter.subscribe(room + "/" + channel);
        else
            mqttAdapter.subscribe(room + "/" +  channel);
    }


    /**
     * Subscribes to the given channel so that the {@link NearflyListener} will be dissolved
     * if a publish is made to the specified channel in the future.
     *
     * @param channel to subscribed channel
     **/
    public void unsubIt(String channel) {
        if (!subscribedChannels.contains(channel))
            subscribedChannels.remove(channel);

        /*if (!isConnected())
            return;*/

        if (mConnectionMode == USE_NEARBY)
            neConAdapter.unsubscribe(room + "/" + channel);
        else
            mqttAdapter.unsubscribe(room + "/" +  channel);
    }

    /**
     * Publishes the specified {@code message} to the respective {@code channel}
     * in the specified room. Optionally a priority between -20 (Publishes that should arrive
     * first) to 19 (Messages that should arrive last) can be assigned.
     *
     * @param channel the channel to be subscribed to
     * @param message the message to be published
     * @param nice (optional) the priority of the message to be published. (default 0)
     * @param retain (optional) if this is true, the message is kept until it can be published.
     *                (default false)
     *
     * @return {@code false} if publishing was aborted due to a nonexistent connection or
     *                          exceeding the maximum size
     **/
    public boolean pubIt(String channel, String message,
                         @IntRange(from=-20, to=19) Integer nice, boolean retain) {
        if (!isConnected() && !retain)
            return false;

        byte[] byteMessage = message.getBytes();

        if (message.getBytes().length>MAX_PUBIT_MESSAGE_SIZE){
            Log.e(TAG, "the message to be sent is larger than the maximum allowed size");
            return false;
        }

        nearflyQueue.add(nearflyNice.new NearflyTextMessage(channel, byteMessage, nice));
        return isConnected();
    }

    /** @see #pubIt(String, String, Integer, boolean) **/
    public boolean pubIt(String channel, String message, Integer nice) {
        return pubIt(channel, message, 0, false);
    }

    /** @see #pubIt(String, String, Integer, boolean) **/
    public boolean pubIt(String channel, String message) {
        return pubIt(channel, message, 0, false);
    }

    /**
     * Publish the specified file in the specified uri to the  to the respective {@code channel}
     * in the specified room. Optionally a priority between -20 (Publishes that should arrive
     * first) to 19 (Messages that should arrive last) can be assigned.
     *
     * <p></p>
     * <h2>Attention</h2>
     * Calling this method requires the following permission to read and write the external storage:
     *
     * <ul>
     * <li>{@code android.permission.READ_EXTERNAL_STORAGE }</li>
     * <li>{@code android.permission.WRITE_EXTERNAL_STORAGE }</li>
     * </ul>
     *
     * @param channel to unsubscribed channel
     * @param uri uri of the file to be published
     * @param nice (optional) priority of the message to be published.(default 0)
     * @param retain (optional) if this is true, the message is kept until it can be published.
     *               The file is not cached, only the uri. (default false)
     **/
    /*@RequiresPermission(allOf = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE})*/
    public boolean pubFile(String channel, Uri uri, String textAttachment,
                           @IntRange(from=-20, to=19) Integer nice, boolean retain) {
        if (!isConnected() && !retain)
            return false;

        nearflyQueue.add(nearflyNice.new NearflyFileMessage(channel, uri, textAttachment, nice));
        return isConnected();
    }

    /**
     * @see #pubFile(String, Uri, String, Integer, boolean)
     **/
    /*@RequiresPermission(allOf = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE})*/
    public boolean pubFile(String channel, Uri uri, String textAttachment) {
        return pubFile(channel, uri, textAttachment, 0, false);
    }

    /**
     * only for testing
     **/
    private void pubStream(Payload stream) {
        neConAdapter.pubStream(stream);
    }

    /**
     * Initializes the connection to the technology currently in use. If {@link ConnectionMode}
     * {@link #USE_MQTT} is used, an attempt is made to establish a connection to the MQTT broker.
     * If {@link #USE_NEARBY} is used as {@link ConnectionMode} the automatic construction process
     * of a peer-to-peer network is initiated or a network that uses the same room string is
     * joined.
     * <p></p>
     * <h3>ATTENTION</h3>
     * The roomstring should currently be left at 19moa18, since the hs-ma broker limits topics
     * based on the student account and may not let other top-level topics through.
     * limited and they will not be
     *
     * @param room All devices with the same number can connect to one another, while different
     *             devices are ignored.
     *             If the room is not specified, this corresponds to the canonical
     *             name of the user activity (not recommended)
     * @param connectionMode technology to be used
     **/
    @SuppressWarnings(
            "The roomstring should currently be left at 19moa18, since the hs-ma broker limits" +
                    "topics based on the student account and may not let other top-level topics" +
                    "through. limited and they will not be")
    public void connect(String room, @ConnectionMode int connectionMode) {
        this.room = room;
        this.mConnectionMode = connectionMode;
        connect();
    }

    /**
     * @see #connect(String, int)
     **/
    public void connect() {
        if (listeners.isEmpty())
            Log.e(TAG, "you haven't given a NearlyListener yet");

        /*if (subscribedChannels.isEmpty())
            Log.e(TAG, "you haven't subscribed to any channels yet");*/

        if (callbackExecutor != null) {
            callbackExecutor.shutdown();
        }
        callbackExecutor = Executors.newSingleThreadExecutor();

        if (!hasPermissions(this, REQUIRED_PERMISSIONS)){
            Log.e(TAG, "the application does not have the required permissions.");
            return;
        }

        if (mConnectionMode == USE_NEARBY)
            connectToNearby();
        else
            connectToMQTT();
    }

    /**
     * Disconnects the connection to the MQTT broker when in {@link ConnectionMode}
     * {@link #USE_MQTT}. in {@link ConnectionMode} {@link #USE_NEARBY}, the connection to
     * all connected endpoints is disconnected.
     **/
    public void disconnect() {
        if (mConnectionMode == USE_NEARBY)
            disconnectToNearby();
        else
            disconnectToMQTT();

        if (callbackExecutor != null) {
            List<Runnable> pending = callbackExecutor.shutdownNow();
            if (!pending.isEmpty()) {
                Log.w(TAG, String.format("disconnect: %d incoming lost", pending.size()));
            }
            callbackExecutor = null;
        }
    }


    /**
     * Adds a {@link NearflyListener} to the {@code NearflyService}, which is triggered for all future incoming
     * messages on all subscribed channels and to status messages like connect and disconnect.
     * The {@link NearflyListener} does not offer the possibility to react to certain of the
     * subscribed channels. However, this can easily be implemented by the user.
     * <p>
     * @param nearflyListener listener to respond to issues from {@code NearflyService}
     *
     * @return {@code true} if the nearflyListener was added
     *
     **/
    public boolean addSubCallback(NearflyListener nearflyListener) {
        return listeners.addIfAbsent(nearflyListener);
    }
    /*public void addSubCallback(NearflyListener nearflyListener) {
        this.nearflyListener = nearflyListener;
    }*/




    /**
     * OTHERS
     **/
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand");
        String action;
        if (intent != null) {
            action = intent.getAction();
        } else {
            Log.w(TAG, "upps, restart");
            action = ACTION_START;
        }
        if (action == null) {
            Log.w(TAG, "  action=null, nothing further to do");
            return START_STICKY;
        }
        switch (action) {
            case ACTION_START:
                Log.v(TAG, "onStartCommand: starting Nearfly");
                // connect();
                return START_STICKY;
            case ACTION_STOP:
                Log.v(TAG, "onStartCommand: stopping Nearfly");
                // disconnect();
                return START_NOT_STICKY;
            default:
                Log.w(TAG, "onStartCommand: unkown action=" + action);
                return START_NOT_STICKY;
        }
    }

    /**
     * Removes the {@link NearflyListener} previously added by
     * {@link #addSubCallback(NearflyListener)}.
     *
     * @param nearflyListener nearflylistener to be removed from the list, if present
     * @return {@code true} if the nearflyListener was added before.
     **/
    public boolean removeSubCallback(NearflyListener nearflyListener) {
        return listeners.remove(nearflyListener);
    }
    /*public void removeSubCallback(NearflyListener nearflyListener) {
        // listeners.remove(nearflyListener);
        this.nearflyListener = null;
    }*/

    /********************************************************************/
    public class LocalBinder extends Binder {
        public NearflyServiceOld getNearflyService() {
            Log.v(TAG, "onBinding");
            return NearflyServiceOld.this;
        }
    }

    final private IBinder localBinder = new LocalBinder();

    /*********************************************************************/


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "onBind");
        String action = intent.getAction();
        if (action != null && action.equals(NearflyServiceOld.ACTION_BIND)) {
            Log.v(TAG, "onBind success");
            return localBinder;
        } else {
            Log.e(TAG, "onBind only defined for ACTION_BIND");
            Log.e(TAG, "       did you want to call startService? ");
            return null;
        }
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "onStop");

        // TODO
        if (mConnectionMode == USE_NEARBY)
            disconnectToNearby();
        else
            disconnectToMQTT();

        super.onDestroy();
    }

    public boolean IsModeSwitching(){
        return (mModeSwitcher !=PAUSED);
    }

    @ConnectionMode
    public int getConnectionMode(){
        return mConnectionMode;
    }

    /**
     * Allows changing the {@link ConnectionMode} without reconnecting to the
     * {@Code NearflyService}.
     * The current used underlying technology is only disconnected, when the
     * {link #destConnectionMode} technology has been successfully connected. Is there an error,
     * e.g. on the part of MQTT, since there is no internet, the process is canceled. Note that
     * nearby connections sometimes turns on the hotspot and interferes with MQTT, since MQTT may
     * not have internet in this case. if this happens you can repeat the process.
     * {@link #IsModeSwitching()} can be used to query whether the
     * switching process is still running. Note that when you switch to Nearby, the techSwitch
     * is complete when a network with 2 nodes has been set up. Messages that are published
     * during the switch will therefore not reach everyone.
     *
     * @param destConnectionMode {@link ConnectionMode} to which to switch
     *
     * @return false if {@Code NearflyService} is already in the desired {@link ConnectionMode}
     **/
    public boolean switchConnectionMode(@ConnectionMode int destConnectionMode) {
        if (mConnectionMode==destConnectionMode)
            return false;

        mModeSwitcher = destConnectionMode;
        if (destConnectionMode == USE_MQTT) {
            // disconnectToNearby();
            connectToMQTT();
        } else {
            // disconnectToMQTT();
            connectToNearby();
        }

        return true;
    }

    /* TODO: ****************************************************** */
    private void connectToMQTT() {
        WifiManager wifiManager= (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);
        // WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        // Boolean wifiOn = (wifiInfo.getSupplicantState() == SupplicantState.ASSOCIATED);

        // mqttAdapter.registerMqttListener(getApplicationContext(), mMqttListener);
        mqttAdapter.connect();
        for (String channel : subscribedChannels) {
            mqttAdapter.subscribe(room + "/" + channel);
        }
    }

    private void disconnectToMQTT() {
        for (String channel : subscribedChannels) {
            mqttAdapter.unsubscribe(room + "/" + channel);
        }
        mqttAdapter.disconnect();
        // mqttAdapter.deregisterMqttListener();
    }

    private void connectToNearby() {
        // neConAdapter.initClient(getApplicationContext(), mNeConListener, room);
        neConAdapter.startConnection(room);

        for (String channel : subscribedChannels) {
            neConAdapter.subscribe(room + "/" + channel);
        }
    }

    private void disconnectToNearby() {
        /*if (neConAdapter==null)
            return;*/

        for (String channel : subscribedChannels) {
            neConAdapter.unsubscribe(room + "/" + channel);
        }

        neConAdapter.stopConnection();
        // neConAdapter = null;
        // neConAdapter.deregisterNearbyListener(mNeConListener);
    }
}
