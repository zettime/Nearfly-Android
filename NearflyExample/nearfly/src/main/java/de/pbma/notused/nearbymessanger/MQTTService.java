package de.pbma.notused.nearbymessanger;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


class MQTTService extends Service {
    final static String TAG = MQTTService.class.getCanonicalName();

    // for LocalService getInstance
    public final static String ACTION_START = "start"; // connect
    public final static String ACTION_STOP = "stop"; // disconnect
    // for LocalService Messaging
    public final static String ACTION_PRESS = "press";
    public final static String ACTION_LOG = "log";
    // Messages
    private static final String MQTT_PASSWORD= "779747ee";
    private static final String MQTT_USERNAME= "19moa18";
    //private static final String MQTT_PUB_TOPIC= "19moa18/measureTest";
    //private static final String MQTT_SUB_TOPIC= "19moa18/#";
    private static final String MQTT_CONNECTION_URL = "ssl://pma.inftech.hs-mannheim.de:8883";

    MqttConnectOptions options;


    final private CopyOnWriteArrayList<MyMqttListener> listeners = new CopyOnWriteArrayList<>();
    private MqttMessaging mqttMessaging;

    private void remoteLog(String line) {
        Log.v(TAG, "remoteLog: " + line);
        for (MyMqttListener listener : listeners) {
            listener.onLogMessage(line);
        }
    }

    private void doMqttStatus(boolean connected) {
        for (MyMqttListener listener : listeners) {
            listener.onStatus(connected);
        }
    }

    private void doOnMqttMessage(String topic, String msg) {
        for (MyMqttListener listener : listeners) {
            listener.onMessage(topic, msg);
        }
    }

    public boolean registerMqttListener(MyMqttListener myMqttListener) {
        return listeners.addIfAbsent(myMqttListener);
    }

    public boolean deregisterMqttListener(MyMqttListener myMqttListener) {
        return listeners.remove(myMqttListener);
    }

    public void publishIt(String topic, String str) {
        mqttMessaging.send(topic, str);
    }

    // end methods to call from a connected activity
    public class LocalBinder extends Binder {
        public MQTTService getMQTTService() {
            return MQTTService.this;
        }
    }
    final private IBinder localBinder = new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "onBind");
        String action = intent.getAction();
        if (action != null && action.equals(MQTTService.ACTION_PRESS)) {
            Log.v(TAG, "onBind for Press");
            return localBinder;
            // } else if (action.equals(MQTTService.ACTION_LOG)) {
            //    Log.v(TAG, "onBind for Log");
            //    return messenger.getBinder();
            // we do not provide messaging in this small example
            // you might want to
        } else {
            Log.e(TAG, "onBind only defined for ACTION_PRESS"); // or ACTION_LOG ");
            Log.e(TAG, "       did you want to call startService? ");
            return null;
        }
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        disconnect();
        super.onDestroy();
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
                Log.v(TAG, "onStartCommand: starting MQTT");
                remoteLog("starting");
                connect();
                // whatever else needs to be done on start may be done  here
                return START_STICKY;
            case ACTION_STOP:
                Log.v(TAG, "onStartCommand: stopping MQTT");
                remoteLog("stopping");
                disconnect();
                // whatever else needs to be done on stop may be done  here
                return START_NOT_STICKY;
            default:
                Log.w(TAG, "onStartCommand: unkown action=" + action);
                return START_NOT_STICKY;
        }
    }

    final private MqttMessaging.FailureListener failureListener = new MqttMessaging.FailureListener() {
        @Override
        public void onConnectionError(Throwable throwable) {
            remoteLog("ConnectionError: " + throwable.getMessage());
        }

        @Override
        public void onMessageError(Throwable throwable, String msg) {
            remoteLog("MessageError: " + throwable.getMessage());
        }

        @Override
        public void onSubscriptionError(Throwable throwable, String topic) {
            remoteLog("SubscriptionError:" + throwable.getMessage());
        }
    };

    final private MqttMessaging.ConnectionListener connectionListener = new MqttMessaging.ConnectionListener() {
        @Override
        public void onConnect() { ;
            doMqttStatus(true); // on purpose a little weird, typical to have interface translation
        }

        @Override
        public void onDisconnect() {
            doMqttStatus(false);
        }
    };

    final private MqttMessaging.MessageListener messageListener = new MqttMessaging.MessageListener() {
        @Override
        public void onMessage(String topic, String stringMsg) {
            // Nachrichten werden hier empfangen
            doOnMqttMessage(topic, stringMsg);
        }
    };

    private void connect() {
        if (mqttMessaging != null) {
            disconnect();
            Log.w(TAG, "reconnect");
        }

        // Set Listener
        mqttMessaging = new MqttMessaging(failureListener, messageListener, connectionListener);

        // Add Options
        options = MqttMessaging.getMqttConnectOptions();

        options.setUserName(MQTT_USERNAME);
        options.setPassword(MQTT_PASSWORD.toCharArray());
        Log.v(TAG, String.format("username=%s, password=%s, ", MQTT_USERNAME , MQTT_PASSWORD));

        // connect
        mqttMessaging.connect(MQTT_CONNECTION_URL, options); // secure via URL
        // mqttMessaging.subscribe("19moa18/test1");
        // mqttMessaging.subscribe("19moa18/test2");
    }

    public void subscribe(String topic){
        mqttMessaging.subscribe(topic);
    }

    public void unsubscribe(String topic){
        mqttMessaging.unsubscribe(topic);
    }

    private void disconnect() {
        Log.v(TAG, "disconnect");
        if (mqttMessaging != null) {
            // mqttMessaging.unsubscribe(MQTT_SUB_TOPIC);
            // mqttMessaging.unsubscribe("19moa18/test1");
            // mqttMessaging.unsubscribe("19moa18/test2");

            List<MqttMessaging.Pair<String, String>> pending = mqttMessaging.disconnect();
            if (!pending.isEmpty()) {
                Log.w(TAG, "pending messages: " + pending.size());
            }
        }
        mqttMessaging = null;
    }
}
