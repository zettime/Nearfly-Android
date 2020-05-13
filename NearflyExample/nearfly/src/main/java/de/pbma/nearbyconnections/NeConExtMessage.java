package de.pbma.nearbyconnections;

import androidx.annotation.StringDef;

import com.google.android.gms.nearby.connection.Payload;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.nio.charset.StandardCharsets;

import static java.lang.annotation.RetentionPolicy.SOURCE;

@Deprecated
class NeConExtMessage {
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

    public NeConExtMessage(String payload, String channel, String type){
        this.payload = payload;
        this.channel = channel;
        this.type = type;
        this.time = System.currentTimeMillis();

        buildMessage();
    }

    /*public static NeConExtMessage createExtMessage(Message message){
        // Extrahiert aus dem Nearby-String Kanal und Payload
        String[] channelAndPayload = new String(message.getContent()).split(NeConExtMessage.SEPERATOR);
        String a = new String(message.getContent());

        return new NeConExtMessage(channelAndPayload[0], channelAndPayload[1]);
    }*/

    public static NeConExtMessage createExtMessage(byte[] message){
        try {
            JSONObject jsonObject= new JSONObject(new String(message));

            return new NeConExtMessage(
                    jsonObject.getString(NeConExtMessage.PAYLOAD),
                    jsonObject.getString(NeConExtMessage.CHANNEL),
                    jsonObject.getString(NeConExtMessage.TYPE)
            );
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static NeConExtMessage createExtMessage(Payload payload){
        try {
            JSONObject jsonObject= new JSONObject(new String(payload.asBytes()));

            return new NeConExtMessage(
                    jsonObject.getString(NeConExtMessage.PAYLOAD),
                    jsonObject.getString(NeConExtMessage.CHANNEL),
                    jsonObject.getString(NeConExtMessage.TYPE)
            );
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] getBytes(){
        // return (channel + NeConExtMessage.SEPERATOR + payload).getBytes();
        return message.toString().getBytes(StandardCharsets.UTF_8);
    }

    public String getType(){
        return type;
    }

    private void buildMessage(){
        // Nachrichten werden durch Separatorcode getrennt
        // message = ((channel + NeConExtMessage.SEPERATOR + payload).getBytes());

        message = new JSONObject();
        try {
            message.put(NeConExtMessage.PAYLOAD, payload);
            message.put(NeConExtMessage.CHANNEL, channel);
            message.put(NeConExtMessage.TYPE, type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
