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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import de.pbma.nearfly.Constants;
import de.pbma.nearfly.NearflyClientTarget;

public class MqttAdapter implements NearflyClientTarget {
    final static String TAG = MqttAdapter.class.getCanonicalName();
    // Messages
    private static final String MQTT_PASSWORD = "779747ee";
    private static final String MQTT_USERNAME = "19moa18";
    private static final String MQTT_CONNECTION_URL = "ssl://pma.inftech.hs-mannheim.de:8883";
    final String PREHEADER_FORMAT="000:";

    private static final String TYPE_FILE =  "f";
    private static final String TYPE_MSG =  "m";
    private String uniqueId;
    MqttConnectOptions options;
    Context context;

    private AtomicBoolean mForceConnection = new AtomicBoolean(false);

    Random random = new Random();

    MyMqttListener listener;
    private MqttMessaging mqttMessaging;
    private AtomicBoolean thRunning = new AtomicBoolean(false);
    private volatile Boolean mConnected = false;
    // private ExecutorService mReconnectExecutor = Executors.newSingleThreadExecutor();
    private ArrayList<String> mSubscriptionBuffer = new ArrayList<>();

    public MqttAdapter(){
        mqttMessaging = new MqttMessaging(failureListener, messageListener, connectionListener);

        // Create Random Name Generator
        uniqueId= ""+random.nextInt(99_999);
    }

    final private MqttMessaging.FailureListener failureListener = new MqttMessaging.FailureListener() {
        @Override
        public void onConnectionError(Throwable throwable) {
            mConnected = false;
            listener.onLogMessage("ConnectionError: " + throwable.getMessage());
            listener.onLogMessage("disconnected");

            // Try to reconnect, if disconnect was not explicit called
            if (mForceConnection.get()==true && !thRunning.get()){
                // mReconnectExecutor.execute(() -> loopReconnection());
                new Thread(() -> loopReconnection()).start();
            }
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
            mConnected = true;
            if (listener!=null)
                listener.onStatus(true); // on purpose a little weird, typical to have interface translation
            logD("Connected to Broker");

            for (String topic: mSubscriptionBuffer)
                mqttMessaging.subscribe(topic);
        }

        @Override
        public void onDisconnect() {
            mConnected = false;
            if (listener!=null)
                listener.onStatus(false);
            logD("Disconnected from Broker");

            // Try to reconnect, if disconnect() was not explicit called
            if (mForceConnection.get()==true && !thRunning.get()){
                // mReconnectExecutor.execute(() -> loopReconnection());
                new Thread(() -> loopReconnection()).start();
            }
        }
    };
    /*final private MqttMessaging.MessageListener messageListener = (topic, rawPayload) -> {
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

                listener.onFile(topic, filePath, textAttachment);
                logD(topic + " " + rawPayload);
            }

        } catch (JSONException e) {
            e.printStackTrace(System.err);
        }
    };*/
    HashMap<Integer, ArrayList<byte[]>> incomingBinary = new HashMap<>();

    final private MqttMessaging.MessageListener messageListener = (topic, msgAsBytes) -> {
        JSONObject json;
        try {
            String str = new String(msgAsBytes);
            String[] parts= str.split(":", 2);
            int payloadBegin = Integer.valueOf(parts[0]);

            byte[] jsonBytes = new byte[payloadBegin-PREHEADER_FORMAT.length()];
            byte[] payloadAsBytes = new byte[msgAsBytes.length-payloadBegin];

            System.arraycopy(msgAsBytes, PREHEADER_FORMAT.length(), jsonBytes, 0, jsonBytes.length);
            System.arraycopy(msgAsBytes, payloadBegin, payloadAsBytes,0,payloadAsBytes.length);

            Log.v("testy", new String(jsonBytes));

            json = new JSONObject(new String(jsonBytes));

            // Check if this is my own message -> no Echo
            if (json.getString("uniqueId").equals(uniqueId))
                return;

            if (json.getString("msgType").equals(TYPE_MSG)){
                listener.onMessage(topic, new String(payloadAsBytes));
                logD(topic + " " + json);
            }else {

                String textAttachment = json.getString("textAttachment");
                String fileExtension = json.getString("fileExtension");
                String filePath = Constants.fileDirectory + File.separator
                        + genFileName(fileExtension);
                int maxSqn = json.getInt("maxSqn");
                int sqn = json.getInt("sqn");
                int fileId = json.getInt("fileId");

                if (maxSqn==1){
                    // Save Data
                    try (FileOutputStream fos = new FileOutputStream(filePath)) {
                        fos.write(payloadAsBytes);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    listener.onFile(topic, filePath, textAttachment);
                }else{
                    if (!incomingBinary.containsKey(fileId))
                        incomingBinary.put(fileId, new ArrayList<>());


                    /*for (int i = 0; i < payloadAsBytes.length; i++) {
                        incomingBinary.get(fileId).add(payloadAsBytes[i]);
                    }*/
                    incomingBinary.get(fileId).add(sqn, payloadAsBytes);

                    Log.v("testy", ""+incomingBinary.size());

                    // CHUNKNIG?
                    if (sqn == maxSqn - 1) {

                        // Convert Byte to byte
                        // byte[] filaAsBytes = new byte[incomingBinary.get(fileId).size()];
                        /*for (int i = 0; i < incomingBinary.size(); i++) {
                            filaAsBytes[i] = incomingBinary.get(fileId).get(i);
                        }*/

                        // Save Data
                        try (FileOutputStream fos = new FileOutputStream(filePath)) {
                            // fos.write(filaAsBytes);
                            for (int i = 0; i < incomingBinary.get(fileId).size(); i++) {
                                fos.write(incomingBinary.get(fileId).get(i));
                            }
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }finally {
                            incomingBinary.remove(fileId);
                        }

                        listener.onFile(topic, filePath, textAttachment);

                        incomingBinary.clear();
                    }
                }
                logD(topic + " " + json);
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
        this.listener = myMqttListener;
        this.context = context;
    }

    public void deregisterMqttListener() {
        this.listener = null;
    }

    public void publishIt(String topic, byte[] payload) {
        byte[] payloadAsBytes = payload;

        JSONObject json = new JSONObject();
        try {
            json.put("uniqueId", uniqueId);
            json.put("msgType", TYPE_MSG);
        } catch (JSONException e) {
            e.printStackTrace(System.err);
        }

        String jsonStr = json.toString();
        int payloadBegin = PREHEADER_FORMAT.length() + jsonStr.getBytes(StandardCharsets.UTF_8).length;

        byte[] header = (strPadding(""+payloadBegin)+":"+jsonStr).getBytes();

        byte[] message = new byte[header.length+payloadAsBytes.length];
        System.arraycopy(header, 0, message, 0, header.length);
        System.arraycopy(payloadAsBytes, 0, message, payloadBegin, payloadAsBytes.length);

        mqttMessaging.send(topic, message);

        /*byte[] header = (PREHEADER_FORMAT+json.toString()).getBytes();
        mqttMessaging.send(topic, header);*/
    }

    @Deprecated
    private void pubFileAsBase64(String topic, Uri uri, String textAttachment) {
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

        mqttMessaging.send(topic, json.toString().getBytes());
    }

    public void pubFile(String topic, Uri uri, String textAttachment) {
        ContentResolver cr = context.getContentResolver();
        String fileExtension = cr.getType(uri).split("\\/")[1]; // MimeType e.g. image/jpeg
        final InputStream fileStream;
        byte[] fileAsBytes;

        try {
            fileStream = cr.openInputStream(uri);
            fileAsBytes = readBytes(fileStream);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        final int MAX_CHUNK_SIZE = 2_000_000;

        int maxSqn = (int) Math.ceil(
                Float.valueOf((float) fileAsBytes.length / (float) MAX_CHUNK_SIZE)
        );

        int fileId = random.nextInt(99_9999);

        // Payload Fragment
        for (int sqn = 0; sqn < maxSqn; sqn++) {  // e.g. 0 - 1*16381; 1*16381 - 2*16381; ...
            int from = sqn * MAX_CHUNK_SIZE;
            int to = (fileAsBytes.length < (sqn + 1) * MAX_CHUNK_SIZE ? fileAsBytes.length : (sqn + 1) * MAX_CHUNK_SIZE);
            byte[] chunk = Arrays.copyOfRange(fileAsBytes, from, to);

            JSONObject json = new JSONObject();
            try {
                json.put("uniqueId", uniqueId);
                json.put("msgType", TYPE_FILE);
                json.put("sqn", sqn);
                json.put("maxSqn", maxSqn);
                json.put("textAttachment", textAttachment);
                json.put("fileExtension", fileExtension);
                json.put("fileId", fileId);
            } catch (JSONException e) {
                e.printStackTrace(System.err);
                return;
            }

            String jsonStr = json.toString();
            int payloadBegin = PREHEADER_FORMAT.length() + jsonStr.getBytes(StandardCharsets.UTF_8).length;
            byte[] header = (strPadding(""+payloadBegin)+":"+jsonStr).getBytes();

            /** PREPARE TO SEND**/
            byte[] message = new byte[header.length+chunk.length];
            System.arraycopy(header, 0, message, 0, header.length);
            System.arraycopy(chunk, 0, message, payloadBegin, chunk.length);

            mqttMessaging.send(topic, message);
        }
    }

    private String strPadding(String toPad) {
        final int width = 3;
        return new String(new char[width - toPad.length()]).replace('\0', '0') + toPad;
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
            intDisconnect();
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
        mForceConnection.set(true);
        mqttMessaging.connect(MQTT_CONNECTION_URL, options); // secure via URL

        // Try to reconnect, if disconnect was not explicit called
        if (!thRunning.get()){
            // mReconnectExecutor.execute(() -> loopReconnection());
            new Thread(() -> loopReconnection()).start();
        }
    }

    /** Should only be called with thread **/
    public void loopReconnection(){
        // Reconnection must be called in Loop cause asking for connectionstate needs additional permissions
        thRunning.set(true);
        // Log.v(TAG, "1");
        try {
            while (mForceConnection.get()) {
                // Log.v(TAG, "2");
                Thread.sleep(1000);
                if (mqttMessaging != null && mConnected) {
                    break;
                }else{
                    // connect();
                    mqttMessaging.connect(MQTT_CONNECTION_URL, options); // secure via URL
                    listener.onLogMessage("trying to connect...");
                }
            }
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        thRunning.set(false);
    }

    public void subscribe(String topic) {
        if (isConnected())
            mqttMessaging.subscribe(topic);
        else
            mSubscriptionBuffer.add(topic);
    }

    public void unsubscribe(String topic) {
        if (isConnected())
            mqttMessaging.unsubscribe(topic);
        else
            mSubscriptionBuffer.remove(topic);
            // Log.e(TAG, "unsubscribe failed because not connected");
    }


    /** Same as disconnect but without touching {@link #mForceConnection} **/
    private void intDisconnect() {
        Log.v(TAG, "disconnect");
        if (mqttMessaging != null) {

            List<MqttMessaging.Pair<String, String>> pending = mqttMessaging.disconnect();
            if (!pending.isEmpty()) {
                Log.w(TAG, "pending messages: " + pending.size());
            }
        }
        mqttMessaging = null;
    }

    /**
     * Stoppen
     **/
    public void disconnect() {
        mForceConnection.set(false);

        Log.v(TAG, "disconnect");
        if (mqttMessaging != null) {

            List<MqttMessaging.Pair<String, String>> pending = mqttMessaging.disconnect();
            if (!pending.isEmpty()) {
                Log.w(TAG, "pending messages: " + pending.size());
            }
        }
        // mqttMessaging = null;
    }

    private void logD(String msg) {
        Log.d("MQTTClient", msg);
    }

    public boolean isConnected() {
        return mConnected;
    }
}
