package de.pbma.nearbyconnections;

import androidx.annotation.StringDef;

import com.google.android.gms.nearby.connection.Payload;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.nio.charset.StandardCharsets;

import static java.lang.annotation.RetentionPolicy.SOURCE;

abstract public class NeConMessage {
    @NeConMsgType
    private String type;

    private String channel;
    private JSONObject message;
    public Long time;

    @Retention(SOURCE)
    @StringDef({STRING , FILEINFORMATION})
    @interface NeConMsgType {}
    final public static String STRING = "string";
    final public static String FILEINFORMATION = "fileinf";

    @NeConMsgType
    public static String getNeConMsgType(Payload payload){
        try {
            JSONObject jsonObject= new JSONObject(new String(payload.asBytes()));
            return jsonObject.getString(NeConFileMessage.TYPE);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] getBytes(){
        return message.toString().getBytes(StandardCharsets.UTF_8);
    }
}
