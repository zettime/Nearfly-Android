package de.pbma.nearfly;
import com.google.android.gms.nearby.messages.Message;
import java.io.UnsupportedEncodingException;

public class ExtMessage {
    public static String SEPERATOR;
    static {
        try {
            SEPERATOR = new String("DELIMIT".getBytes(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private String channel;
    private String payload;
    private Message message;
    public Long time;

    public String getChannel() { return channel; }
    // public void setChannel(String channel) {this.channel=channel; buildMessage(); }
    public String getPayload() { return payload; }
    // public void setPayload(String payload) { this.payload = payload; buildMessage(); }
    public Message getMessage() { return message; }

    public ExtMessage(String channel, String payload){
        this.channel = channel;
        this.payload = payload;
        this.time = System.currentTimeMillis();

        buildMessage();
    }

    public static ExtMessage createExtMessage(Message message){
        // Extrahiert aus dem Nearby-String Kanal und Payload
        String[] channelAndPayload = new String(message.getContent()).split(ExtMessage.SEPERATOR);
        String a = new String(message.getContent());

        return new ExtMessage(channelAndPayload[0], channelAndPayload[1]);
    }

    private void buildMessage(){
        // Nachrichten werden durch Separatorcode getrennt
        message = new Message((channel + ExtMessage.SEPERATOR + payload).getBytes());
    }
}
