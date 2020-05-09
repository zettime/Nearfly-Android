package de.pbma.nearbyconnections;
import com.google.android.gms.nearby.connection.Payload;
import org.json.JSONException;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;

public class NeCon {
    // Payloads
    final static private String CHANNEL = "c";
    final static private String PAYLOAD = "p";

    final static private String FILE_EXTENSION = "e";
    final static private String FILE_ID = "i";
    final static private String TEXT_ATTACHMENT = "t";

    final public static int STRING = 0;
    final public static int FILEINFORMATION = 1;

    public static int getMessageType(Payload payload){
        String str = new String(payload.asBytes());
        String[] parts = str.split(":", 2);
        return Integer.valueOf(parts[0]);
    }

    public abstract class Message{
    }

    public class TextMessage extends Message {
        final private int type = STRING;
        private String payload;
        private String channel;
        private JSONObject message;
        public Long time;

        public String getPayload() { return payload; }
        public String getChannel() { return channel; }

        public TextMessage(String payload, String channel){
            this.payload = payload;
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
                message.put(PAYLOAD, payload);
                message.put(CHANNEL, channel);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public TextMessage createTextMessage(Payload payload){
        try {
            String str= new String(payload.asBytes());
            String[] parts = str.split(":", 2);
            JSONObject jsonObject= new JSONObject(parts[1]);

            return new TextMessage(
                    jsonObject.getString(PAYLOAD),
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
