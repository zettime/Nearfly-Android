package de.pbma.mqtt;

import android.content.ContentResolver;
import android.content.Context;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import de.pbma.nearfly.Constants;

public class MyMQTTClient {
    final static String TAG = MyMQTTClient.class.getCanonicalName();
    // Messages
    private static final String MQTT_PASSWORD = "779747ee";
    private static final String MQTT_USERNAME = "19moa18";
    private static final String MQTT_CONNECTION_URL = "ssl://pma.inftech.hs-mannheim.de:8883";

    private static final String TYPE_FILE =  "f";
    private static final String TYPE_MSG =  "m";
    private String uniqueId;
    MqttConnectOptions options;
    Context context;

    final private CopyOnWriteArrayList<MyMqttListener> listeners = new CopyOnWriteArrayList<>();
    MyMqttListener listener;
    private MqttMessaging mqttMessaging;

    public MyMQTTClient(){
        mqttMessaging = new MqttMessaging(failureListener, messageListener, connectionListener);

        // Create Random Name Generator
        uniqueId= "";
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            uniqueId += random.nextInt(10);
        }
    }

    final private MqttMessaging.FailureListener failureListener = new MqttMessaging.FailureListener() {
        @Override
        public void onConnectionError(Throwable throwable) {
            listener.onLogMessage("ConnectionError: " + throwable.getMessage());

            listener.onLogMessage("disconnected");
        }

        @Override
        public void onMessageError(Throwable throwable, String msg) {
            listener.onLogMessage("MessageError: " + throwable.getMessage());
        }

        @Override
        public void onSubscriptionError(Throwable throwable, String topic) {
            listener.onLogMessage("SubscriptionError:" + throwable.getMessage());
        }
    };
    final private MqttMessaging.ConnectionListener connectionListener = new MqttMessaging.ConnectionListener() {
        @Override
        public void onConnect() {
            if (listener!=null)
                listener.onStatus(true); // on purpose a little weird, typical to have interface translation
            logD("Connected to Broker");
        }

        @Override
        public void onDisconnect() {
            if (listener!=null)
                listener.onStatus(false);
            logD("Disconnected from Broker");
        }
    };
    final private MqttMessaging.MessageListener messageListener = (topic, rawPayload) -> {
        // onMessage
        /*String[] parts =  payload.split("/", 2);
        logD(Arrays.toString(parts));
        if (!parts[0].equals(uniqueId))
            listener.onMessage(topic, parts[1]);*/

        JSONObject json;
        try {
            json = new JSONObject(rawPayload);

            // Check if this is my own message -> no Echo
            if (json.getString("uniqueId").equals(uniqueId))
                return;

            String msgType = json.getString("msgType");

            if (msgType.equals(TYPE_MSG)){
                String payload = json.getString("payload");
                listener.onMessage(topic, payload);
                logD(topic + " " + rawPayload);
            }

            if (msgType.equals(TYPE_FILE))
            {
                String file = json.getString("file");
                String textAttachment = json.getString("textAttachment");
                String fileExtension = json.getString("fileExtension");
                String filePath = Constants.fileDirectory + File.separator
                        + genFileName(fileExtension);

                // Get Message
                byte[] bytes = Base64.decode(file.getBytes(), Base64.DEFAULT);
                try (FileOutputStream fos = new FileOutputStream(filePath)) {
                    fos.write(bytes);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                listener.onFile(filePath, textAttachment);
                logD(topic + " " + rawPayload);
            }

        } catch (JSONException e) {
            e.printStackTrace(System.err);
        }
    };

    public String genFileName(String fileExtension) {
        // Make File  Name
        Date today = Calendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-hh.mm.ss");
        String fileName = "Nearfly " + formatter.format(today);

        return fileName + "." + fileExtension;
    }

    public void registerMqttListener(Context context, MyMqttListener myMqttListener) {
        // return listeners.addIfAbsent(myMqttListener);
        this.listener = myMqttListener;
        this.context = context;
    }

    //public boolean deregisterMqttListener(MyMqttListener myMqttListener) {
    public void deregisterMqttListener() {
        // return listeners.remove(myMqttListener);
        this.listener = null;
    }

    public void publishIt(String topic, String payload) {
        JSONObject json = new JSONObject();
        try {
            json.put("uniqueId", uniqueId);
            json.put("msgType", TYPE_MSG);
            json.put("payload", payload);
        } catch (JSONException e) {
            e.printStackTrace(System.err);
        }

        // String header = TYPE_MSG + "/" ;//+ "f" +
        mqttMessaging.send(topic, json.toString());
    }

    public void pubFile(String topic, Uri uri, String textAttachment) {
        // BODY
        /*try {
            pfd = cr.openFileDescriptor(uri, "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }*/
        /*final InputStream fileStream;
        byte[] body;
        try {
            fileStream = cr.openInputStream(uri);
            body = readBytes(fileStream);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }*/

        // HEADER
        /*if (textAttachment.length() > 30_000)
            throw new RuntimeException("textAttachment must be smaller than 30kByte");

        byte[] header = (intPadding(textAttachment, 5) + "/" + fileExtension
                + "/" + textAttachment).getBytes();*/


        /** PREPARE TO SEND**/
        /*byte[] payload = new byte[header.length + body.length];
        System.arraycopy(header, 0, payload, 0, header.length);
        System.arraycopy(body, 0, payload, header.length, body.length);*/

        ContentResolver cr = context.getContentResolver();
        String fileExtension = cr.getType(uri).split("\\/")[1]; // MimeType e.g. image/jpeg
        final InputStream fileStream;
        String filePayload="";
        byte[] bytes;

        try {
            fileStream = cr.openInputStream(uri);
            bytes = readBytes(fileStream);
            filePayload = Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        JSONObject json = new JSONObject();
        try {
            json.put("uniqueId", uniqueId);
            json.put("msgType", TYPE_FILE);
            json.put("file", filePayload);
            json.put("textAttachment", textAttachment);
            json.put("fileExtension", fileExtension);
        } catch (JSONException e) {
            e.printStackTrace(System.err);
            return;
        }

        mqttMessaging.send(topic, json.toString());
    }

    public void createHeaderForMSG(String uniqueId){

    }
    public void createHeaderForFILE(String uniqueId, String fileExtension, String textAttachment){

    }


    public byte[] readBytes(InputStream inputStream) throws IOException {
        // this dynamically extends to take the bytes you read
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        // this is storage overwritten on each iteration with bytes
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        // we need to know how may bytes were read to write them to the byteBuffer
        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }

        // and then we can return your byte array.
        return byteBuffer.toByteArray();
    }

    private String strPadding(String toPad, int width) {
        // e.g. (30, 5) -> "   30"
        return new String(new char[width - toPad.length()]).replace('\0', ' ') + toPad;
    }

    private String intPadding(String toPad, int width) {
        // e.g. (30, 5) -> "00030"
        return new String(new char[width - toPad.length()]).replace('\0', '0') + toPad;
    }

    /**
     * Starten
     **/
    public void connect() {
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
        Log.v(TAG, String.format("username=%s, password=%s, ", MQTT_USERNAME, MQTT_PASSWORD));

        // connect
        mqttMessaging.connect(MQTT_CONNECTION_URL, options); // secure via URL
    }

    public void subscribe(String topic) {
        mqttMessaging.subscribe(topic);
    }

    public void unsubscribe(String topic) {
        mqttMessaging.unsubscribe(topic);
    }

    /**
     * Stoppen
     **/
    public void disconnect() {
        Log.v(TAG, "disconnect");
        if (mqttMessaging != null) {

            List<MqttMessaging.Pair<String, String>> pending = mqttMessaging.disconnect();
            if (!pending.isEmpty()) {
                Log.w(TAG, "pending messages: " + pending.size());
            }
        }
        mqttMessaging = null;
    }

    private void logD(String msg) {
        Log.d("MQTTClient", msg);
    }
}
