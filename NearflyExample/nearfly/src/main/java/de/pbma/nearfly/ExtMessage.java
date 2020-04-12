package de.pbma.nearfly;

import androidx.annotation.StringDef;

import com.google.android.gms.nearby.connection.Payload;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.nio.charset.StandardCharsets;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public class ExtMessage {
    /*public static String SEPERATOR;
    static {
        try {
            SEPERATOR = new String("DELIMIT".getBytes(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }*/
    final static private String PAYLOAD = "p";
    final static String CHANNEL = "c";
    final static String TYPE = "t";

    @Retention(SOURCE)
    @StringDef({STRING , FILEINFORMATION})
    public @interface ConnectionMode {}
    public final static String STRING = "string";
    public final static String FILEINFORMATION = "fileinf";


    private String payload;
    private String channel;
    private String type;
    private JSONObject message;
    public Long time;

    public String getPayload() { return payload; }
    // public void setPayload(String payload) { this.payload = payload; buildMessage(); }
    public String getChannel() { return channel; }
    // public void setChannel(String channel) {this.channel=channel; buildMessage(); }
    // public Message getMessage() { return message; }

    public ExtMessage(String payload, String channel, String type){
        this.payload = payload;
        this.channel = channel;
        this.type = type;
        this.time = System.currentTimeMillis();

        buildMessage();
    }

    /*public static ExtMessage createExtMessage(Message message){
        // Extrahiert aus dem Nearby-String Kanal und Payload
        String[] channelAndPayload = new String(message.getContent()).split(ExtMessage.SEPERATOR);
        String a = new String(message.getContent());

        return new ExtMessage(channelAndPayload[0], channelAndPayload[1]);
    }*/

    public static ExtMessage createExtMessage(byte[] message){
        try {
            JSONObject jsonObject= new JSONObject(new String(message));

            return new ExtMessage(
                    jsonObject.getString(ExtMessage.PAYLOAD),
                    jsonObject.getString(ExtMessage.CHANNEL),
                    jsonObject.getString(ExtMessage.TYPE)
            );
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ExtMessage createExtMessage(Payload payload){
        try {
            JSONObject jsonObject= new JSONObject(new String(payload.asBytes()));

            return new ExtMessage(
                    jsonObject.getString(ExtMessage.PAYLOAD),
                    jsonObject.getString(ExtMessage.CHANNEL),
                    jsonObject.getString(ExtMessage.TYPE)
            );
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] getBytes(){
        // return (channel + ExtMessage.SEPERATOR + payload).getBytes();
        return message.toString().getBytes(StandardCharsets.UTF_8);
    }

    public String getType(){
        return type;
    }

    private void buildMessage(){
        // Nachrichten werden durch Separatorcode getrennt
        // message = ((channel + ExtMessage.SEPERATOR + payload).getBytes());

        message = new JSONObject();
        try {
            message.put(ExtMessage.PAYLOAD, payload);
            message.put(ExtMessage.CHANNEL, channel);
            message.put(ExtMessage.TYPE, type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
