package de.pbma.nearbyconnections;
import android.util.Log;

import com.google.android.gms.nearby.connection.Payload;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/** Component that keeps the two different NeConMessage **/
public class NeCon {
    // Payloads
    private final static String CHANNEL = "1";
    private final static String PAYLOAD = "2";

    private final static String FILE_EXTENSION = "3";
    private final static String FILE_ID = "4";
    private final static String TEXT_ATTACHMENT = "5";
    private final static String MESSAGE_TYPE= "6";

    public final static int BYTES = 0;
    public final static int FILEINFORMATION = 1;
    public final static int CONTROLMESSAGE = 2;

    private static final String CMD = "7";
    public static final int CMD_SUB = 0;
    public static final int CMD_UNSUB = 1;

    public static final String PREHEADER_FORMAT = "000:";

    public static Message createMessage(Payload payload){
        try {
            String str= new String(payload.asBytes());
            String[] segments = str.split(":", 2);
            int payloadBegin =  Integer.valueOf(segments[0]);

            byte[] fullMessage = payload.asBytes();

            JSONObject json = null;
            byte[] byteMessage=null;
            if (payloadBegin!=0){
                byte[] jsonBytes = new byte[payloadBegin-PREHEADER_FORMAT.length()];
                byteMessage = new byte[fullMessage.length-payloadBegin];
                System.arraycopy(fullMessage, PREHEADER_FORMAT.length(), jsonBytes, 0, jsonBytes.length);
                System.arraycopy(fullMessage, payloadBegin, byteMessage, 0, byteMessage.length);

                json = new JSONObject(new String(jsonBytes));
            }else{
                json = new JSONObject(segments[1]);
            }

            int type = json.getInt(MESSAGE_TYPE);

            if (type==BYTES)
                return createTextMessage(json, byteMessage);
            if (type==FILEINFORMATION)
                return createFileInfMessage(json);
            if (type==CONTROLMESSAGE)
                return createControlMessage(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static class Message{
    }

    public static class BytesMessage extends Message {
        final private int type = BYTES;
        private byte[] payload;
        private String channel;
        private JSONObject jsonHeader;
        public Long time;

        public byte[] getPayload() { return payload; }
        public String getChannel() { return channel; }

        public BytesMessage(byte[] payload, String channel){
            this.payload = payload;
            this.channel = channel;
            this.time = System.currentTimeMillis();

            buildMessage();
        }/******* TODO ***********/

        public byte[] getBytes(){
            String jsonStr = jsonHeader.toString();
            int payloadBegin = PREHEADER_FORMAT.length()
                    +jsonStr.getBytes(StandardCharsets.UTF_8).length;

            byte[] header = (strPadding(""+payloadBegin)+":"+jsonStr).getBytes(StandardCharsets.UTF_8);

            byte[] message = new byte[header.length+payload.length];
            System.arraycopy(header, 0, message, 0, header.length);
            System.arraycopy(payload, 0, message, payloadBegin, payload.length);

            return message;
        }

        private String strPadding(String toPad) {
            final int width = 3;
            return new String(new char[width - toPad.length()]).replace('\0', '0') + toPad;
        }

        private void buildMessage(){
            jsonHeader= new JSONObject();
            try {
                // message.put(PAYLOAD, payload);
                jsonHeader.put(CHANNEL, channel);
                jsonHeader.put(MESSAGE_TYPE, type);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public static BytesMessage createTextMessage(JSONObject jsonSegment, byte[] byteMessage){
        try {
            return new BytesMessage(
                    byteMessage,
                    jsonSegment.getString(CHANNEL)
            );
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static class FileInfMessage extends Message {
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
            return (PREHEADER_FORMAT+message.toString()).getBytes(StandardCharsets.UTF_8);
        }

        private void buildMessage(){
            message = new JSONObject();
            try {
                message.put(CHANNEL, channel);
                message.put(FILE_EXTENSION, fileExtension);
                message.put(FILE_ID, fileId);
                message.put(TEXT_ATTACHMENT, textAttachment);
                message.put(MESSAGE_TYPE, type);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public static FileInfMessage createFileInfMessage(JSONObject jsonSegment){
        try {
            return new FileInfMessage(
                    jsonSegment.getString(CHANNEL),
                    jsonSegment.getString(FILE_EXTENSION),
                    jsonSegment.getLong(FILE_ID),
                    jsonSegment.getString(TEXT_ATTACHMENT)
            );
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    // TODO Controlling for Subscriptions
    public static class ControlMessage extends Message {
        final private int type = CONTROLMESSAGE;
        private int cmd;
        private ArrayList<String> subList;
        private JSONObject message;

        public int getCMD() {
            return cmd;
        }
        public ArrayList<String> getSubList() {
            return subList;
        }

        public ControlMessage(int cmd, ArrayList<String> payload) {
            this.cmd = cmd;
            this.subList = payload;

            buildMessage();
        }

        public byte[] getBytes(){
            return (PREHEADER_FORMAT+message.toString()).getBytes(StandardCharsets.UTF_8);
        }

        private void buildMessage(){
            message = new JSONObject();
            try {
                // Make ArrayList
                JSONArray array = new JSONArray(subList.toArray());

                message.put(CMD, cmd);
                message.put(PAYLOAD, array);
                message.put(MESSAGE_TYPE, type);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public static ControlMessage createControlMessage(JSONObject jsonSegment){
        try {
            JSONArray jArray = jsonSegment.getJSONArray(PAYLOAD);

            ArrayList<String> array = new ArrayList<String>();
            if (jArray != null) {
                for (int i=0;i<jArray.length();i++){
                    array.add(jArray.getString(i));
                }
            }

            return new ControlMessage(
                    jsonSegment.getInt(CMD),
                    array
            );
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
