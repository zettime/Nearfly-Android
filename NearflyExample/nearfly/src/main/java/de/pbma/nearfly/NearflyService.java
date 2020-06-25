package de.pbma.nearfly;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.annotation.Retention;

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
public class NearflyService extends Service {
    private String TAG = "NearflyServices";

    public final static String ACTION_START = "start";
    public final static String ACTION_STOP = "stop";
    public final static String ACTION_BIND = "bind";

    NearflyClient mNearflyClient;

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
        mNearflyClient = new NearflyClient(getApplicationContext());
    }

    /**
     * Returns {@code true} if either in {@link NearflyClient.ConnectionMode} {@link NearflyClient#USE_MQTT}, there is
     * a connection to the mqtt server or in {@link NearflyClient.ConnectionMode} {@link NearflyClient#USE_NEARBY} the
     * device is connected to at least one other node.
     *
     * @return {@code true} the underlying technology has a connection
     */
    public boolean isConnected() {
        return mNearflyClient.isConnected();
    }

    public void setConnected(boolean connected) {
        mNearflyClient.setConnected(connected);
    }

    /**
     * Subscribes to the given channel so that the {@link NearflyListener} will be dissolved
     * if a publish is made to the specified channel in the future.
     *
     * @param channel to subscribed channel
     **/
    public void subIt(String channel) {
        mNearflyClient.subIt(channel);
    }

    /**
     * Subscribes to the given channel so that the {@link NearflyListener} will be dissolved
     * if a publish is made to the specified channel in the future.
     *
     * @param channel to subscribed channel
     **/
    public void unsubIt(String channel) {
        mNearflyClient.unsubIt(channel);
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
        return mNearflyClient.pubIt(channel, message, nice, retain);
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
        return mNearflyClient.pubFile(channel, uri, textAttachment, nice, retain);
    }

    /**
     * @see #pubFile(String, Uri, String, Integer, boolean)
     **/
    /*@RequiresPermission(allOf = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE})*/
    public boolean pubFile(String channel, Uri uri, String textAttachment) {
        return pubFile(channel, uri, textAttachment, 0, false);
    }

    /*public void pubBigBytes(String channel, byte[] bigBytes){
        mNearflyClient.pubBigBytes(channel, bigBytes);
    }*/

    /**
     * Initializes the connection to the technology currently in use. If {@link NearflyClient.ConnectionMode}
     * {@link NearflyClient#USE_MQTT} is used, an attempt is made to establish a connection to the MQTT broker.
     * If {@link NearflyClient#USE_NEARBY} is used as {@link NearflyClient.ConnectionMode} the automatic construction process
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
    public void connect(String room, @NearflyClient.ConnectionMode int connectionMode) {
        mNearflyClient.connect(room, connectionMode);
    }

    /**
     * @see #connect(String, int)
     **/
    public void connect() {
        mNearflyClient.connect();
    }

    /**
     * Disconnects the connection to the MQTT broker when in {@link NearflyClient.ConnectionMode}
     * {@link NearflyClient#USE_MQTT}. in {@link NearflyClient.ConnectionMode} {@link NearflyClient#USE_NEARBY}, the connection to
     * all connected endpoints is disconnected.
     **/
    public void disconnect() {
        mNearflyClient.disconnect();
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
        return mNearflyClient.addSubCallback(nearflyListener);
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
        return mNearflyClient.removeSubCallback(nearflyListener);
    }
    /*public void removeSubCallback(NearflyListener nearflyListener) {
        // listeners.remove(nearflyListener);
        this.nearflyListener = null;
    }*/

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
        mNearflyClient.cleanUp();

        super.onDestroy();
    }

    public boolean isModeSwitching(){
        return mNearflyClient.isModeSwitching();
    }

    @NearflyClient.ConnectionMode
    public int getConnectionMode(){
        return mNearflyClient.getConnectionMode();
    }

    /**
     * Allows changing the {@link NearflyClient.ConnectionMode} without reconnecting to the
     * {@Code NearflyService}.
     * The current used underlying technology is only disconnected, when the
     * {link #destConnectionMode} technology has been successfully connected. Is there an error,
     * e.g. on the part of MQTT, since there is no internet, the process is canceled. Note that
     * nearby connections sometimes turns on the hotspot and interferes with MQTT, since MQTT may
     * not have internet in this case. if this happens you can repeat the process.
     * {@link #isModeSwitching()} can be used to query whether the
     * switching process is still running. Note that when you switch to Nearby, the techSwitch
     * is complete when a network with 2 nodes has been set up. Messages that are published
     * during the switch will therefore not reach everyone.
     *
     * @param destConnectionMode {@link NearflyClient.ConnectionMode} to which to switch
     *
     * @return false if {@Code NearflyService} is already in the desired {@link NearflyClient.ConnectionMode}
     **/
    public boolean switchConnectionMode(@NearflyClient.ConnectionMode int destConnectionMode) {
        return mNearflyClient.switchConnectionMode(destConnectionMode);
    }
}
