package de.pbma.nearfly;


import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;

import com.google.android.gms.nearby.connection.Payload;

import java.lang.annotation.Retention;
import java.util.ArrayList;

import de.pbma.mqtt.MyMQTTClient;
import de.pbma.mqtt.MyMqttListener;
import de.pbma.nearbyconnections.MyNearbyConnectionsClient;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * A Bound Service that wrap up the feature functions from the Google Nearby API and MQTT Paho.
 *
 * <p>The {@code NeaflyService} is started according to the usual procedure for starting
 * bound service via {@link #startService(Intent)}. After the activity is bound,
 * {@code NearflyService} can be used. In order to facilitate the integration of
 * the {@code NeaflyService} it is recommended to inherit from the {@link NearflyBindingActivity},
 * also ensures that the necessary permissions are queried by the activity
 * The {@link NearflyBindingActivity} also ensures that the necessary permissions are queried
 * by the activity at the beginning.
 *
 * <h3>Usage Examples</h3>
 *
 * Here is an example of an activity that extends from the {@link NearflyBindingActivity}
 * and publishes message as soon as it is connected to the {@code NeaflyService}.
 *
 * <pre> {@code
 * public class NearflySampleActivity extends NearflyBindingActivity {
 *
 *     private final String NEARFLY_CHANNEL = "sensors/humidity";
 *     private boolean neaflyServiceConnectCalled = false;
 *     private CountDownLatch nearflyServiceStartedSignal = new CountDownLatch(1);
 *
 *     @Override
 *     public void onNearflyServiceBound() {
 *         if (!neaflyServiceConnectCalled) {
 *             nearflyService.addSubCallback(nearflyListener);
 *             nearflyService.connect("ThisIsMyUniqueRoomString", NearflyService.USE_NEARBY);
 *             nearflyService.subIt(NEARFLY_CHANNEL);
 *             neaflyServiceConnectCalled = true;
 *             nearflyServiceStartedSignal.countDown();
 *         }
 *     }
 *
 *     @Override
 *     public void onNearflyServiceUnbound() {
 *     }
 *
 *     NearflyListener nearflyListener = new NearflyListener() {
 *         @Override
 *         public void onLogMessage(String output) {
 *         }
 *
 *         @Override
 *         public void onMessage(String channel, String message) {
 *             logIt(channel + " " + message);
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
 *         new Thread(() -> {
 *             try {
 *                 nearflyServiceStartedSignal.await();
 *             } catch (InterruptedException e) {
 *                 e.printStackTrace();
 *             }
 *             nearflyService.pubIt(NEARFLY_CHANNEL, "Hello World!");
 *         }).start();
 *     }
 * }</pre>
 *
 *
 *  @edited 01.05.2020
 *  @author Alexis Danilo Morgado dos Santos
 */
public class NearflyService extends Service {
    /* This are the both Modes that can be used for Nearfly */
    @Retention(SOURCE)
    @IntDef({USE_MQTT, USE_NEARBY})
    public @interface ConnectionMode {}
    public final static int USE_NEARBY = 1;
    public final static int USE_MQTT = 2;
    public final static int DONT_SWITCH = 0;

    /** Technology to use(MQTT or Nearby) **/
    public int techToBeUsed;
    private int changeTechWhenReady = DONT_SWITCH;

    private String TAG = "NearflyServices";
    private ArrayList<String> subscribedChannels = new ArrayList<>();

    public final static String ACTION_START = "start";
    public final static String ACTION_STOP = "stop";
    public final static String ACTION_BIND = "bind";

    private boolean isConnected = false;

    /** Defines the visibility context **/
    private String room = this.getClass().getCanonicalName();

    /** Listeners **/
    private NearflyListener nearflyListener;
    private MyNearbyConnectionsClient nearbyConnectionsClient = new MyNearbyConnectionsClient();
    private MyMQTTClient mqttClient = new MyMQTTClient();

    /**
     * Returns {@code true} if either in {@link ConnectionMode} {@link #USE_MQTT}, there is
     * a connection to the mqtt server or in {@link ConnectionMode} {@link #USE_NEARBY} the
     * device is connected to at least one other node.
     *
     * @return {@code true} the underlying technology has a connection
     */
    public boolean isConnected(){
        return isConnected;
    }

    private MyNearbyConnectionsClient.MyConnectionsListener nearbyConnectionListener =new MyNearbyConnectionsClient.MyConnectionsListener() {
        @Override
        public void onLogMessage(CharSequence msg) {
            nearflyListener.onLogMessage(String.valueOf(msg));
        }

        @Override
        public void onStateChanged(String state) {
            nearflyListener.onLogMessage("STATE CHANGED: " + state);

            if (techToBeUsed == USE_NEARBY){
                if ((state.equals(MyNearbyConnectionsClient.State.ROOT.toString()) ||state.equals(MyNearbyConnectionsClient.State.CONNODE.toString())))
                    isConnected=true;
                else
                    isConnected=false;
            }


            if (isConnected && changeTechWhenReady==USE_NEARBY){
                /** Actually change of Technology **/
                techToBeUsed = USE_NEARBY;

                changeTechWhenReady=DONT_SWITCH;
                disconnectToMQTT();
            }
        }

        @Override
        public void onRootNodeChanged(String rootNode) {
            nearflyListener.onLogMessage("ROOT NODE CHANGED: " + rootNode);

        }

        @Override
        public void onMessage(String channel, String msg) {
            nearflyListener.onMessage(channel, msg);
        }

        @Override
        public void onStream(Payload payload) { /*nearflyListener.onStream(payload);*/ }

        @Override
        public void onBinary(Payload payload) {
            /*nearflyListener.onBinary(payload);*/
        }

        @Override
        public void onFile(String path, String textAttachment) {
            nearflyListener.onFile(path, textAttachment);
        }
    };

    private MyMqttListener mqttListener = new MyMqttListener() {
        @Override
        public void onMessage(String topic, String message) {
            nearflyListener.onMessage(topic, message);
        }

        @Override
        public void onStatus(boolean connected) {
            nearflyListener.onLogMessage((connected==true)?"connected":"disconnected");

            if (techToBeUsed==USE_MQTT)
                isConnected=connected;

            if (changeTechWhenReady==USE_MQTT){
                techToBeUsed=USE_MQTT;
                changeTechWhenReady=DONT_SWITCH;
                disconnectToNearby();
            }
        }

        @Override
        public void onLogMessage(String message) {
            nearflyListener.onLogMessage(message);
        }

        @Override
        public void onFile(String path, String textAttachment) {
            nearflyListener.onFile(path, textAttachment);
        }
    };

    /**
     * Subscribes to the given channel so that the {@link NearflyListener} will be dissolved
     * if a publish is made to the specified channel in the future.
     *
     * @param channel to subscribed channel
     * **/
    public void subIt(String channel) {
        // nearbyConnectionsClient.addSubCallback("test", nearbyConnectionsClient);
        /*nearbyConnectionsClient.subscirbe(channel);
        mqttClient.subscribe(channel);*/

        if (!subscribedChannels.contains(channel))
            subscribedChannels.add(channel);

        if (techToBeUsed==USE_NEARBY)
            nearbyConnectionsClient.subscribe(room + "/" +  channel);
        else
            // mqttClient.subscribe(room + "/" +  channel);
            mqttClient.subscribe(channel);
    }


    /**
     * Subscribes to the given channel so that the {@link NearflyListener} will be dissolved
     * if a publish is made to the specified channel in the future.
     *
     * @param channel to subscribed channel
     * **/
    public void unsubIt(String channel) {
        // nearbyConnectionsClient.unsubIt("test", nearflyListener);
        subscribedChannels.remove(channel);

        nearbyConnectionsClient.unsubscribe(room + "/" +  channel);
        //mqttClient.unsubscribe(channel);
    }

    /**
     * Publishes the specified {@code message} to the respective {@code channel}
     * in the specified room.
     *
     * @param channel to subscribed channel
     * @param message message to be published
     * **/
    public boolean pubIt(String channel, String message) {
        switch (techToBeUsed){
            case USE_NEARBY:
                if (!nearbyConnectionsClient.isConnected()){
                    nearflyListener.onLogMessage("not Connected");
                    return false;
                }
                nearbyConnectionsClient.publishIt(room + "/" +  channel, message);
                return false;
                // break;
            case USE_MQTT:
                // mqttClient.publishIt(room + "/" +  channel, message);
                mqttClient.publishIt(channel, message);
                break;
        }
        return true;
    }

    /**
     * Publish the specified file in the specified uri to the  to the respective {@code channel}
     * in the specified room.
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
     *
     * **/
    public boolean pubFile(String channel, Uri uri, String textAttachment) {
        switch (techToBeUsed){
            case USE_NEARBY:
                if (!nearbyConnectionsClient.isConnected()){
                    nearflyListener.onLogMessage("not Connected");
                    return false;
                }

                nearbyConnectionsClient.pubFile(room + "/" +  channel, uri, textAttachment);
                return false;
            // break;
            case USE_MQTT:
                // mqttClient.pubFile(channel, uri, textAttachment);
                mqttClient.pubFile(channel, uri, textAttachment);
                break;
        }
        return true;
    }


    /** only for testing **/
    private void pubStream(Payload stream) {
        nearbyConnectionsClient.pubStream(stream);
    }

    /**
     * Initializes the connection to the technology currently in use. If {@link ConnectionMode}
     * {@link #USE_MQTT} is used, an attempt is made to establish a connection to the MQTT broker.
     * If {@link #USE_NEARBY} is used as {@link ConnectionMode} the automatic construction process
     * of a peer-to-peer network is initiated or a network that uses the same room string is
     * joined.
     *
     * @param room All devices with the same number can connect to one another, while different
     *             devices are ignored.
     *             If the room is not specified, this corresponds to the canonical
     *             name of the user activity (not recommended)
     * **/
    public void connect(String room, @ConnectionMode int techToBeUsed){
        this.room = room;
        this.techToBeUsed = techToBeUsed;
        connect();
    }

    /**  @see #connect(String, int) **/
    public void connect(){
        /*if (nearflyListener==null)
            throw new RuntimeException("you haven't given a NearlyListener yet");*/

        if (subscribedChannels.isEmpty())
            Log.e(TAG, "you haven't subscribed to any channels yet");


        if (techToBeUsed==USE_NEARBY)
            connectToNearby();
        else
            connectToMQTT();
    }

    /**
     * Disconnects the connection to the MQTT broker when in {@link ConnectionMode}
     * {@link #USE_MQTT}. in {@link ConnectionMode} {@link #USE_NEARBY}, the connection to
     * all connected endpoints is disconnected.
     * **/
    public void disconnect(){
        if (techToBeUsed==USE_NEARBY)
            disconnectToNearby();
        else
            disconnectToMQTT();
    }


    /**
     * Adds a {@link NearflyListener} to the {@code NearflyService}, which is triggered for all future incoming
     * messages on all subscribed channels and to status messages like connect and disconnect.
     * The {@link NearflyListener} does not offer the possibility to react to certain of the
     * subscribed channels. However, this can easily be implemented by the user.
     *
     * param nearflyListener listener to respond to issues from {@code NearflyService}
     * **/
    public void addSubCallback(NearflyListener nearflyListener) {
        this.nearflyListener = nearflyListener;
    }


    /** OTHERS **/
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
                Log.v(TAG, "onStartCommand: starting Nearby");
                // connect();
                return START_STICKY;
            case ACTION_STOP:
                Log.v(TAG, "onStartCommand: stopping Nearby");
                // disconnect();
                return START_NOT_STICKY;
            default:
                Log.w(TAG, "onStartCommand: unkown action=" + action);
                return START_NOT_STICKY;
        }
    }

    /**
     * removes the {@link NearflyListener} previously added by
     * {@link #addSubCallback(NearflyListener)}
     * **/
    public void removeSubCallback(NearflyListener nearflyListener) {
        // listeners.remove(nearflyListener);
        this.nearflyListener = null;
    }

    /********************************************************************/
    public class LocalBinder extends Binder {
        public NearflyService getNearflyService() {
            Log.v(TAG, "onBinding");
            return NearflyService.this;
        }
    }

    final private IBinder localBinder = new LocalBinder();

    /*********************************************************************/


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "onBind");
        String action = intent.getAction();
        if (action != null && action.equals(NearflyService.ACTION_BIND)) {
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
        if (techToBeUsed==USE_NEARBY)
            disconnectToNearby();
        else
            disconnectToMQTT();

        super.onDestroy();
    }

    /** Change will by Applied, when connection in MQTT or Nearby was successful
    {@link #nearbyConnectionListener} & {@Link #mqttListener} **/
    public void changeTech(@ConnectionMode int mode){
        if (mode==USE_MQTT){
            // disconnectToNearby();
            connectToMQTT();
        }else{
            // disconnectToMQTT();
            connectToNearby();
        }

        changeTechWhenReady = mode;  // Change later
        // USED_TECH = mode;
    }

    /* TODO: ****************************************************** */
    private void connectToMQTT(){
        mqttClient.registerMqttListener(getApplicationContext(), mqttListener);
        mqttClient.connect();
        for (String channel : subscribedChannels){
            mqttClient.subscribe(room + "/" +  channel);
        }

    }

    private void disconnectToMQTT(){
        for (String channel : subscribedChannels){
            mqttClient.unsubscribe(room + "/" +  channel);
        }
        mqttClient.disconnect();
        mqttClient.deregisterMqttListener();
    }

    private void connectToNearby(){
        nearbyConnectionsClient.initClient(getApplicationContext(), nearbyConnectionListener, room);
        nearbyConnectionsClient.startConnection();

        for (String channel : subscribedChannels){
            nearbyConnectionsClient.subscribe(room + "/" +  channel);
        }
    }

    private void disconnectToNearby(){
        if (nearbyConnectionsClient==null)
            return;

        for (String channel : subscribedChannels){
            nearbyConnectionsClient.unsubscribe(room + "/" +  channel);
        }

        nearbyConnectionsClient.stopConnection();
        // nearbyConnectionsClient = null;
        // nearbyConnectionsClient.deregisterNearbyListener(nearbyConnectionListener);
    }
}
