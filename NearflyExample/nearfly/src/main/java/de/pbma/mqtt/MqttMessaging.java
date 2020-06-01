package de.pbma.mqtt;

import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;


// Simple MQTT Messaging class using the synchronous client
// in a separate thread. The user API is asynchronous handling
// all callbacks in yet another separate thread.
// ToDo: reconnect and resubscribe
class MqttMessaging {
    final static String TAG = MqttMessage.class.getCanonicalName();

    private volatile String clientId;
    private volatile MqttClient client;
    private AtomicBoolean connectPending = new AtomicBoolean(false);
    private AtomicBoolean ready = new AtomicBoolean(false); // client connected and read to send

    private ConcurrentSkipListSet<String> subscriptions = new ConcurrentSkipListSet<>();
    private ConcurrentHashMap<Long, Pair<String,String>> pendingMessages = new ConcurrentHashMap<>();
    private AtomicLong pendingMessageId = new AtomicLong(0);

    private ExecutorService mqttExecutor;
    private ExecutorService callbackExecutor;

    public static class Pair<S, T> {

        private final S s;
        private final T t;

        public static <S, T> Pair<S, T> createPair(S s, T t) {
            return new Pair<>(s, t);
        }

        public Pair(S s, T t) {
            this.s = s;
            this.t = t;
        }

        public S getFirst() {
            return s;
        }

        public T getSecond() {
            return t;
        }

    }

    public interface MessageListener {
        void onMessage(String topic, byte[] msg);
    }

    public interface ConnectionListener {
        void onConnect();
        void onDisconnect();
    }

    public interface FailureListener {
        void onConnectionError(Throwable throwable);
        void onMessageError(Throwable throwable, String msg);
        void onSubscriptionError(Throwable throwable, String topic);
    }

    private volatile MessageListener messageListener;
    private volatile ConnectionListener connectionListener;
    private volatile FailureListener failureListener;

    public MqttMessaging() {
        this(null, null, null);
    }

    public MqttMessaging(FailureListener failureListener) {
        this(failureListener, null, null);
    }

    public MqttMessaging(FailureListener failureListener, MessageListener messageListener) {
        this(failureListener, messageListener, null);
    }

    private MqttCallback mqttCallback = new MqttCallback() {
        @Override
        public void messageArrived(String topic, MqttMessage message) {
            doMessage(topic, message);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            // ignore
        }

        @Override
        public void connectionLost(Throwable cause) {
            ready.set(false);
            doConnectionFailure(cause); // connection lost is a connection failure
        }
    };

    public MqttMessaging(FailureListener failureListener,
                         MessageListener messageListener,
                         ConnectionListener connectionListener) {
        this.failureListener = failureListener;
        this.messageListener = messageListener;
        this.connectionListener = connectionListener;
        clientId = MqttClient.generateClientId();
        Log.v(TAG, "constructed");
    }

    public void connect(String broker) {
        connect(broker, null);
    }

    public static MqttConnectOptions getMqttConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        return options;
    }

    public boolean connect(String broker, final MqttConnectOptions options) { // do in startCommand
        if (ready.get() || connectPending.get()) { // already connected and ready
            return true;
        }

        if (mqttExecutor != null) {
            mqttExecutor.shutdown();
        }
        mqttExecutor = Executors.newSingleThreadExecutor();
        if (callbackExecutor != null) {
            callbackExecutor.shutdown();
        }
        callbackExecutor = Executors.newSingleThreadExecutor();
        connectPending.set(true);
        mqttExecutor.execute(() -> {
            try {
                // MemoryPersistence is useless if vm may restart
                client = new MqttClient(broker, clientId, null);
                client.setCallback(mqttCallback);
                MqttConnectOptions connectOptions = options;
                if (connectOptions == null) { // default
                    connectOptions = MqttMessaging.getMqttConnectOptions();
                }
                Log.v(TAG, String.format("xxx: username=%s, password=%s, ", connectOptions.getUserName(), connectOptions.getPassword()));
                client.connect(connectOptions);
                connectPending.set(false);
                ready.set(true);
                if (connectionListener != null) {
                    connectionListener.onConnect();
                }
                Log.v(TAG, "connected");
            } catch (MqttException e) {
                connectPending.set(false);
                ready.set(false);
                Log.e(TAG, "connection failure", e);
                callbackExecutor.execute(() -> {
                    doConnectionFailure(e);
                });
            }
        });

        return true;
    }

    private void doConnectionFailure(Throwable e) {
        FailureListener f = failureListener;
        if (f != null) {
            f.onConnectionError(e);
        }
    }

    private void doMessageFailure(Throwable e, String topic, String msg) {
        FailureListener f = failureListener;
        if (f != null) {
            // f.onMessageError(e, Util.fmt("%s: %d", topic, msg));
            // Log.v(Constants.TAGLOG, topic + ": "+ msg);
        }
    }

    private void doSubscriptionFailure(Throwable e, String topicFilter) {
        FailureListener f = failureListener;
        if (f != null) {
            f.onSubscriptionError(e, topicFilter);
        }
    }

    private void doMessage(final String topic, final MqttMessage message) {
        callbackExecutor.execute(() -> {
            MessageListener messageListener = this.messageListener;
            if (messageListener != null) {
                // String msg = message.toString(); // of the payload
                messageListener.onMessage(topic, message.getPayload());
            }
        });
    }

    public void setFailureListener(FailureListener failureListener) {
        this.failureListener = failureListener;
    }

    public void setMessageListener(MessageListener messageListener) {
        this.messageListener = messageListener;
    }

    public void subscribe(String topicFilter) {
        if (!ready.get() && !connectPending.get()) {
            // throw new RuntimeException("connect not yet called");
            Log.e(TAG, "connect not yet called");
        }
        subscriptions.add(topicFilter);
        if (mqttExecutor!=null){
            mqttExecutor.execute(() -> {
                try {
                    Log.v(TAG, "subscribe: " + topicFilter + ready.get());
                    MqttClient c = client;
                    if (c != null && ready.get()) {
                        c.subscribe(topicFilter);
                    }
                } catch (MqttException e) {
                    Log.e(TAG, String.format("  subscribe failed: topic=%s, cause=%s", topicFilter, e.getMessage()));
                    subscriptions.remove(topicFilter);
                    doSubscriptionFailure(e, topicFilter);
                }
            });
        }
    }

    public boolean isConnected(){
        // return !(!ready.get() && !connectPending.get());
        return ready.get();
    }

    public void unsubscribe(String topicFilter) {
        if (!ready.get() && !connectPending.get()) {
            throw new RuntimeException("connect not yet called");
        }

        if (mqttExecutor!=null)
            mqttExecutor.execute(() -> {
                try {
                    boolean contained = subscriptions.remove(topicFilter);
                    if (contained) {
                        MqttClient c = client;
                        if (c != null && ready.get()) {
                            client.unsubscribe(topicFilter);
                        }
                    }
                } catch (MqttException e) {
                    // even failed unsubscribe is a subscription failure
                    doSubscriptionFailure(e, topicFilter);
                }
            });
    }

    /*public void send(final String topic, final String msg) {
        if (!ready.get() && !connectPending.get()) {
            throw new RuntimeException("connect not yet called");
        }
        final long id = pendingMessageId.incrementAndGet();
        pendingMessages.put(id, Pair.createPair(topic,  msg));
        mqttExecutor.execute(() -> {
            try {
                pendingMessages.remove(id); // we are processing it
                Log.v(TAG, String.format("send: topic=%s, msg=%s",topic, msg));
                MqttMessage message = new MqttMessage();
                message.setPayload(msg.getBytes());
                message.setQos(1); // we always do 1
                MqttClient c = client;
                if (c != null) {
                    pendingMessages.remove(id);
                    c.publish(topic, message);
                }
            } catch (MqttException e) {
                Log.e(TAG, String.format("  sent failed: topic=%s, msg=%s, cause=%s", topic, msg, e.getMessage()));
                doMessageFailure(e, topic, msg);
            }
        });
    }*/

    public void send(final String topic, final byte[] msg) {
        if (!ready.get() && !connectPending.get()) {
            throw new RuntimeException("connect not yet called");
        }
        final long id = pendingMessageId.incrementAndGet();
        pendingMessages.put(id, Pair.createPair(topic,  msg.toString()));
        mqttExecutor.execute(() -> {
            try {
                pendingMessages.remove(id); // we are processing it
                Log.v(TAG, String.format("send: topic=%s, msg=%s",topic, msg));
                MqttMessage message = new MqttMessage();
                message.setPayload(msg);
                message.setQos(1); // we always do 1
                MqttClient c = client;
                if (c != null) {
                    pendingMessages.remove(id);
                    c.publish(topic, message);
                }
            } catch (MqttException e) {
                Log.e(TAG, String.format("  sent failed: topic=%s, msg=%s, cause=%s", topic, msg, e.getMessage()));
                doMessageFailure(e, topic, msg.toString());
            }
        });
    }

    // TODO START: Send Bytes *********************************************************
    /*public void sendBytes(final String topic, byte[] bytes) {
        if (!ready.get() && !connectPending.get()) {
            throw new RuntimeException("connect not yet called");
        }
        final long id = pendingMessageId.incrementAndGet();
        pendingMessages.put(id, Pair.createPair(topic,  bytes.toString()));
        mqttExecutor.execute(() -> {
            try {
                pendingMessages.remove(id); // we are processing it
                Log.v(TAG, String.format("sendBytes: topic=%s, msg=%s",topic, bytes.toString()));
                MqttMessage message = new MqttMessage();
                message.setPayload(bytes);
                message.setQos(1); // we always do 1
                MqttClient c = client;
                if (c != null) {
                    pendingMessages.remove(id);
                    c.publish(topic, message);
                }
            } catch (MqttException e) {
                Log.e(TAG, String.format("  sendBytes failed: topic=%s, msg=%s, cause=%s", topic, bytes.toString(), e.getMessage()));
                doMessageFailure(e, topic, bytes.toString());
            }
        });
    }*/
    // TODO END: Send Bytes *********************************************************

    public String getClientId() {
        return clientId;
    }

    public boolean hasPendingMessages() {
        return !pendingMessages.isEmpty();
    }

    public List<Pair<String, String>> disconnect() { // do in onDestroy()
        final MqttClient c = client;
        client = null; // let all pending messaging perish
        if (callbackExecutor != null) {
            List<Runnable> pending = callbackExecutor.shutdownNow();
            // well, if there are incoming messages pending, then
            // you didn't do the callbacks fast enough -> no mercy, ignore
            if (!pending.isEmpty()) {
                Log.w(TAG, String.format("disconnect: %d incoming lost", pending.size()));
            }
            callbackExecutor = null;
        }
        if (c != null && ready.get()) {
            ready.set(false);
            List<Runnable> pending = mqttExecutor.shutdownNow();
            mqttExecutor = null;
            if (!pending.isEmpty()) {
                Log.w(TAG, String.format("disconnect: %d outgoing lost", pending.size()));
            }
            // force that one to be in background
            new Thread() {
                @Override
                public void run() {
                    try {
                        c.disconnect();
                        if (connectionListener != null) {
                            connectionListener.onDisconnect();
                        }

                    } catch (MqttException e) {
                        // ignore
                    }
                }
            }.start();
        }
        messageListener = null; // do not keep anything
        failureListener = null; // do not keep anything
        return getClearPendingMessages();
    }

    private List<Pair<String, String>> getClearPendingMessages() {
        List<Pair<String, String>> pm = new ArrayList<>(pendingMessages.size());
        pm.addAll(pendingMessages.values());
        pendingMessages.clear();
        return pm;
    }

}
