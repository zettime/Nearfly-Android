package de.pbma.nearfly;

import com.google.android.gms.nearby.connection.Payload;

public interface NearflyListener {
    void onLogMessage(CharSequence msg);
    void onStateChanged(String state);
    void onRootNodeChanged(String rootNode);
    void onMessage(String channel, String message);
    void onStream(Payload payload);
    void onBinary(Payload payload);
    void onFile(String path, String textAttachment);
}
