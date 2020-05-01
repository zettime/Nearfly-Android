package de.pbma.nearfly;


import android.app.Service;
import android.content.Intent;
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
    public String room = this.getClass().getCanonicalName();

    /** Listeners **/
    private NearflyListener nearflyListener;
    private MyNearbyConnectionsClient nearbyConnectionsClient = new MyNearbyConnectionsClient();
    private MyMQTTClient mqttClient = new MyMQTTClient();

    /** **/
    public boolean isConnected(){
        return isConnected;
    }


    /** **/
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

    /** **/
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

    /** **/
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
    // Laadfsdaf


    /** **/
    public void unsubIt(String channel) {
        // nearbyConnectionsClient.unsubIt("test", nearflyListener);
        subscribedChannels.remove(channel);

        nearbyConnectionsClient.unsubscribe(room + "/" +  channel);
        //mqttClient.unsubscribe(channel);
    }

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

    /** **/
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


    /** (testing) **/
    private void pubStream(Payload stream) {
        nearbyConnectionsClient.pubStream(stream);
    }

    /**
     * Initializes the connection with the Nearly API.
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

    /** **/
    public void disconnect(){
        if (techToBeUsed==USE_NEARBY)
            disconnectToNearby();
        else
            disconnectToMQTT();
    }


    /** **/
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

    /** **/
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
