package de.pbma.nearfly;


import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.StringDef;

import java.lang.annotation.Retention;

import de.pbma.mqtt.MyMQTTClient;
import de.pbma.mqtt.MyMqttListener;
import de.pbma.nearbyconnections.MyNearbyConnectionsClient;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public class NearflyService extends Service {
    /* This are the both Modes that can be used for Nearfly */
    @Retention(SOURCE)
    @StringDef({USE_MQTT, USE_NEARBY})
    public @interface ConnectionMode {}
    public final static String USE_MQTT = "MQTT";
    public final static String USE_NEARBY = "Nearby";

    public String USED_TECH = NearflyService.USE_NEARBY;
    private String changeTechWhenReady = USED_TECH;

    public String TAG = "NearflyServices";

    public final static String ACTION_START = "start";
    public final static String ACTION_STOP = "stop";
    public final static String ACTION_BIND = "bind";

    // Nearby
    MyNearbyConnectionsClient nearbyConnectionsClient = new MyNearbyConnectionsClient();
    MyNearbyConnectionsClient.MyConnectionsListener narbyConnectionListener = new MyNearbyConnectionsClient.MyConnectionsListener() {
        @Override
        public void onLogMessage(CharSequence msg) {/*Wahrscheinlich nicht relevant für Nutzer*/
            nearflyListener.onLogMessage(msg);
        }

        @Override
        public void onStateChanged(String state) {
            if (state.equals(MyNearbyConnectionsClient.State.FINDROOT) && changeTechWhenReady==USE_NEARBY){
                nearflyListener.onStateChanged(state);

                /** Actually change of Technology **/
                USED_TECH = USE_NEARBY;
                mqttClient.deregisterMqttListener(mqttListener);
                mqttClient.disconnect();
            }
        }

        @Override
        public void onRootNodeChanged(String rootNode) {
            nearflyListener.onRootNodeChanged(rootNode);
        }

        @Override
        public void onMessage(String msg) {
            nearflyListener.onMessage(msg);
        }
    };

    // MQTT
    private MyMQTTClient mqttClient = new MyMQTTClient();
    MyMqttListener mqttListener = new MyMqttListener() {
        @Override
        public void onMqttMessage(String id, String text) {
            nearflyListener.onMessage(text);
        }

        @Override
        public void onMQTTStatus(boolean connected) {
            nearflyListener.onStateChanged((connected==true)?"connected":"disconnected");
        }

        @Override
        public void onLogMessage(String message) {
            nearflyListener.onLogMessage(message);
        }
    };


    // private ArrayList<NearflyListener> listeners = new ArrayList<>();
    NearflyListener nearflyListener;

    @Override
    public void onCreate() {
        super.onCreate();
        if (USED_TECH=="Nearby"){
            nearbyConnectionsClient.onCreate(getApplicationContext(), narbyConnectionListener);
        }
        else
            mqttClient.connect();
    }

    public void subIt(String channel, NearflyListener nearflyListener) {
        // nearbyConnectionsClient.addSubCallback("test", nearbyConnectionsClient);
    }

    public void unsubIt() {
        // nearbyConnectionsClient.unsubIt("test", nearflyListener);
    }

    public void pubIt(String channel, String message) {
        if (USED_TECH=="Nearby")
            nearbyConnectionsClient.pubIt(channel, message);
        else
            mqttClient.publishIt(channel, message);
    }

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
                if (USED_TECH=="Nearby")
                    nearbyConnectionsClient.onStart();
                else
                    mqttClient.registerMqttListener(mqttListener);
                // whatever else needs to be done on start may be done  here
                return START_STICKY;
            case ACTION_STOP:
                Log.v(TAG, "onStartCommand: stopping Nearby");
                if (USED_TECH=="Nearby")
                    nearbyConnectionsClient.onStop();
                else{
                    mqttClient.deregisterMqttListener(mqttListener);
                }
                // whatever else needs to be done on stop may be done  here
                return START_NOT_STICKY;
            default:
                Log.w(TAG, "onStartCommand: unkown action=" + action);
                return START_NOT_STICKY;
        }
    }

    public void addSubCallback(String channel, NearflyListener nearflyListener) {
        // Channel wird derzeitig noch nicht benutzt
        // listeners.add(nearflyListener);
        this.nearflyListener = nearflyListener;
    }

    public void removeSubCallback(NearflyListener nearflyListener) {
        // listeners.remove(nearflyListener);
        this.nearflyListener = null;
    }

    // TODO

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
        if (USED_TECH=="Nearby")
            nearbyConnectionsClient.onStop();
        else
            mqttClient.disconnect();

        super.onDestroy();
    }

    public void changeTech(@ConnectionMode String mode){
        if (mode==USE_MQTT){
            nearbyConnectionsClient.onStop();

            mqttClient.registerMqttListener(mqttListener);
            mqttClient.connect();
            USED_TECH = mode;

        }else{
            nearbyConnectionsClient.onCreate(getApplicationContext(), narbyConnectionListener);
            nearbyConnectionsClient.onStart();

            // Change will by Applied, when Connected to Nearby Entpoint
            changeTechWhenReady = mode; /** {@link #narbyConnectionListener}*/
        }
    }
}
