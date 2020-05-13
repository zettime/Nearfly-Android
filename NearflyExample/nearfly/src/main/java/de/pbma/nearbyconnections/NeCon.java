package de.pbma.nearbyconnections;
import android.util.Log;

import com.google.android.gms.nearby.connection.Payload;
import org.json.JSONException;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class NeCon {
    // Payloads
    private final static String CHANNEL = "c";
    private final static String PAYLOAD = "p";

    private final static String FILE_EXTENSION = "e";
    private final static String FILE_ID = "i";
    private final static String TEXT_ATTACHMENT = "t";

    public final static int STRING = 0;
    public final static int FILEINFORMATION = 1;

    public final String PREHEADER_FORMAT = "I:000:";


    public static int getMessageType(Payload payload){
        String str = new String(payload.asBytes());
        String[] parts = str.split(":", 2);
        return Integer.valueOf(parts[0]);
    }

    public abstract class Message{
    }

    public class TextMessage extends Message {
        final private int type = STRING;
        private byte[] payload;
        private String channel;
        private JSONObject jsonHeader;
        public Long time;

        public byte[] getPayload() { return payload; }
        public String getChannel() { return channel; }

        public TextMessage(byte[] payload, String channel){
            this.payload = payload;
            this.channel = channel;
            this.time = System.currentTimeMillis();

            buildMessage();
        }/******* TODO ***********/

        public byte[] getBytes(){
            String jsonStr = jsonHeader.toString();
            int payloadBegin = PREHEADER_FORMAT.length()
                    +jsonStr.getBytes(StandardCharsets.UTF_8).length;

            byte[] header = (type+":"+strPadding(""+payloadBegin, 3)+":"+jsonStr).getBytes(StandardCharsets.UTF_8);

            byte[] message = new byte[header.length+payload.length];
            System.arraycopy(header, 0, message, 0, header.length);
            System.arraycopy(payload, 0, message, payloadBegin, payload.length);

            return message;
        }

        private String strPadding(String toPad, int width) {
            return new String(new char[width - toPad.length()]).replace('\0', '0') + toPad;
        }

        private void buildMessage(){
            jsonHeader= new JSONObject();
            try {
                // message.put(PAYLOAD, payload);
                jsonHeader.put(CHANNEL, channel);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public TextMessage createTextMessage(Payload payload){
        try {
            String str= new String(payload.asBytes());
            String[] segments = str.split(":", 3);
            int payloadBegin =  Integer.valueOf(segments[1]);

            byte[] fullMessage = payload.asBytes();

            byte[] jsonHeader = new byte[payloadBegin-PREHEADER_FORMAT.length()];
            byte[] byteMessage = new byte[fullMessage.length-payloadBegin];

            System.arraycopy(fullMessage, PREHEADER_FORMAT.length(), jsonHeader, 0, jsonHeader.length);
            System.arraycopy(fullMessage, payloadBegin, byteMessage, 0, byteMessage.length);

            JSONObject jsonObject= new JSONObject(new String(jsonHeader));

            Log.v("testy", new String(byteMessage));

            return new TextMessage(
                    byteMessage,
                    jsonObject.getString(CHANNEL)
            );
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public class FileInfMessage extends Message {
        final private int type = FILEINFORMATION;
        private String fileExtension;
        private long fileId;
        private String textAttachment;
        private String channel;
        private JSONObject message;
        private Long time;

        public String getFileExtension() {
            return fileExtension;
        }

        public long getFileId() {
            return fileId;
        }

        public String getTextAttachment() {
            return textAttachment;
        }

        public String getChannel() {
            return channel;
        }

        public FileInfMessage(String channel, String fileExtension, long fileId, String textAttachment) {
            this.fileExtension = fileExtension;
            this.fileId = fileId;
            this.textAttachment = textAttachment;
            this.channel = channel;
            this.time = System.currentTimeMillis();

            buildMessage();
        }

        public byte[] getBytes(){
            return (type+":"+message.toString()).getBytes(StandardCharsets.UTF_8);
        }

        private void buildMessage(){
            message = new JSONObject();
            try {
                message.put(CHANNEL, channel);
                message.put(FILE_EXTENSION, fileExtension);
                message.put(FILE_ID, fileId);
                message.put(TEXT_ATTACHMENT, textAttachment);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public FileInfMessage createFileInfMessage(Payload payload){
        try {
            String str= new String(payload.asBytes());
            String[] parts = str.split(":", 2);
            JSONObject jsonObject= new JSONObject(parts[1]);

            return new FileInfMessage(
                    jsonObject.getString(CHANNEL),
                    jsonObject.getString(FILE_EXTENSION),
                    jsonObject.getLong(FILE_ID),
                    jsonObject.getString(TEXT_ATTACHMENT)
            );
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
