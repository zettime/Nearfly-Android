package de.pbma.nearbyconnections;

import com.google.android.gms.nearby.connection.Payload;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

class NeConFileMessage extends NeConMessage {
    final static private String PAYLOAD = "p";
    final static String CHANNEL = "c";
    final static String TYPE = "t";

    private String payload;
    private String channel;
    private String type = "sdsada";
    private JSONObject message;
    public Long time;
    private String fileExtension;
    private String fileAttachment;
    private String payloadId;


    public String getType(){
        return FILEINFORMATION;
    }

    public String getPayload() { return payload; }
    public String getChannel() { return channel; }


    public NeConFileMessage(String payload, String channel, String type){
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

    public static NeConFileMessage createExtMessage(byte[] message){
        try {
            JSONObject jsonObject= new JSONObject(new String(message));

            return new NeConFileMessage(
                    jsonObject.getString(NeConFileMessage.PAYLOAD),
                    jsonObject.getString(NeConFileMessage.CHANNEL),
                    jsonObject.getString(NeConFileMessage.TYPE)
            );
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static NeConFileMessage createExtMessage(Payload payload){
        try {
            JSONObject jsonObject= new JSONObject(new String(payload.asBytes()));

            return new NeConFileMessage(
                    jsonObject.getString(NeConFileMessage.PAYLOAD),
                    jsonObject.getString(NeConFileMessage.CHANNEL),
                    jsonObject.getString(NeConFileMessage.TYPE)
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

    private void buildMessage(){
        // Nachrichten werden durch Separatorcode getrennt
        // message = ((channel + ExtMessage.SEPERATOR + payload).getBytes());

        message = new JSONObject();
        try {
            message.put(NeConFileMessage.PAYLOAD, payload);
            message.put(NeConFileMessage.CHANNEL, channel);
            message.put(NeConFileMessage.TYPE, type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
