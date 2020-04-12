package de.pbma.nearfly;


import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.StringDef;

import com.google.android.gms.nearby.connection.Payload;

import java.io.File;
import java.lang.annotation.Retention;
import java.util.ArrayList;

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
    public final static String DONT_SWITCH = "None";

    /** Technology to use(MQTT or Nearby) **/
    public String USED_TECH = NearflyService.USE_NEARBY;
    private String changeTechWhenReady = DONT_SWITCH;

    public String TAG = "NearflyServices";
    private ArrayList<String> subscribedChannels;

    public final static String ACTION_START = "start";
    public final static String ACTION_STOP = "stop";
    public final static String ACTION_BIND = "bind";

    private boolean isConnected = false;

    // Nearby
    // MyNearbyConnectionsClient nearbyConnectionsClient;
    public MyNearbyConnectionsClient nearbyConnectionsClient = new MyNearbyConnectionsClient();
    MyNearbyConnectionsClient.MyConnectionsListener nearbyConnectionListener =new MyNearbyConnectionsClient.MyConnectionsListener() {
        @Override
        public void onLogMessage(CharSequence msg) {/*Wahrscheinlich nicht relevant für Nutzer*/
            nearflyListener.onLogMessage(msg);
        }

        @Override
        public void onStateChanged(String state) {
            nearflyListener.onStateChanged(state);

            if ((state.equals(MyNearbyConnectionsClient.State.ROOT) ||state.equals(MyNearbyConnectionsClient.State.CONNODE))
                    && changeTechWhenReady==USE_NEARBY){
                /** Actually change of Technology **/
                USED_TECH = USE_NEARBY;
                changeTechWhenReady=DONT_SWITCH;
                disconnectToMQTT();
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

        @Override
        public void onStream(Payload payload) { nearflyListener.onStream(payload); }

        @Override
        public void onBinary(Payload payload) {
            nearflyListener.onBinary(payload);
        }

        @Override
        public void onFile(String path) {
            nearflyListener.onFile(path);
        }
    };

    // MQTT
    MyMQTTClient mqttClient = new MyMQTTClient();
    MyMqttListener mqttListener = new MyMqttListener() {
        @Override
        public void onMqttMessage(String id, String text) {
            nearflyListener.onMessage(text);
        }

        @Override
        public void onMQTTStatus(boolean connected) {
            nearflyListener.onStateChanged((connected==true)?"connected":"disconnected");

            if (changeTechWhenReady==USE_MQTT){
                USED_TECH=USE_MQTT;
                changeTechWhenReady=DONT_SWITCH;
                disconnectToNearby();
            }
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
        subscribedChannels = new ArrayList<>();
        /*if (USED_TECH=="Nearby"){
            connectToNearby();
        }
        else
            connectToMQTT();*/
    }

    //public void subIt(String channel, NearflyListener nearflyListener) {
    public void subIt(String channel) {
        // nearbyConnectionsClient.addSubCallback("test", nearbyConnectionsClient);
        /*nearbyConnectionsClient.subscirbe(channel);
        mqttClient.subscribe(channel);*/
        if (!subscribedChannels.contains(channel))
            subscribedChannels.add(channel);
    }

    public void unsubIt(String channel) {
        // nearbyConnectionsClient.unsubIt("test", nearflyListener);
        subscribedChannels.remove(channel);

        /*nearbyConnectionsClient.unsubscribe(channel);
        mqttClient.unsubscribe(channel);*/
    }

    public boolean pubIt(String channel, String message) {
        if (USED_TECH=="Nearby"){
            // NEARBY
            if (!nearbyConnectionsClient.isConnected()){
                nearflyListener.onLogMessage("not Connected");
                return false;
            }
            nearbyConnectionsClient.pubIt(channel, message);
            return false;
        }
        else{
            mqttClient.publishIt(channel, message);
        }
        return true;
    }

    public boolean pubFile(String channel, ParcelFileDescriptor pfd) {
        if (USED_TECH=="Nearby"){
            // NEARBY
            if (!nearbyConnectionsClient.isConnected()){
                nearflyListener.onLogMessage("not Connected");
                return false;
            }
            nearbyConnectionsClient.pubFile(channel, pfd);
            return false;
        }
        /*else{
            mqttClient.publishBytes(channel, bytes);
        }*/
        return true;
    }

    public void pubBinaryTST(byte[] bytes) {
        nearbyConnectionsClient.pubBinaryTST(bytes);
    }

    public void pubStream(Payload stream) {
        nearbyConnectionsClient.pubStream(stream);
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
                    // nearbyConnectionsClient.onStart();
                    connectToNearby();
                else
                    //mqttClient.registerMqttListener(mqttListener);
                    connectToMQTT();
                // whatever else needs to be done on start may be done  here
                return START_STICKY;
            case ACTION_STOP:
                Log.v(TAG, "onStartCommand: stopping Nearby");
                if (USED_TECH=="Nearby")
                    //nearbyConnectionsClient.onStop();
                    disconnectToNearby();
                else{
                    //mqttClient.deregisterMqttListener(mqttListener);
                    disconnectToMQTT();
                }
                // whatever else needs to be done on stop may be done  here
                return START_NOT_STICKY;
            default:
                Log.w(TAG, "onStartCommand: unkown action=" + action);
                return START_NOT_STICKY;
        }
    }

    public void addSubCallback(NearflyListener nearflyListener) {
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
        // TODO
        if (USED_TECH=="Nearby")
            disconnectToNearby();
        else
            disconnectToMQTT();

        super.onDestroy();
    }

    /** Change will by Applied, when connection in MQTT or Nearby was successful
    {@link #nearbyConnectionListener} & {@Link #mqttListener} **/
    public void changeTech(@ConnectionMode String mode){
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


    private void connectToMQTT(){
        mqttClient.registerMqttListener(mqttListener);
        mqttClient.connect();
        for (String channel : subscribedChannels){
            mqttClient.subscribe(channel);
        }

    }

    private void disconnectToMQTT(){
        for (String channel : subscribedChannels){
            mqttClient.unsubscribe(channel);
        }
        mqttClient.disconnect();
        mqttClient.deregisterMqttListener(mqttListener);
    }

    private void connectToNearby(){
        nearbyConnectionsClient = new MyNearbyConnectionsClient();
        nearbyConnectionsClient.initClient(getApplicationContext(), nearbyConnectionListener);
        nearbyConnectionsClient.onStart();
        for (String channel : subscribedChannels){
            nearbyConnectionsClient.subscribe(channel);
        }
    }

    private void disconnectToNearby(){
        if (nearbyConnectionsClient==null)
            return;

        for (String channel : subscribedChannels){
            nearbyConnectionsClient.unsubscribe(channel);
        }

        nearbyConnectionsClient.onStop();
        nearbyConnectionsClient = null;
        // nearbyConnectionsClient.deregisterNearbyListener(nearbyConnectionListener);
    }


}
