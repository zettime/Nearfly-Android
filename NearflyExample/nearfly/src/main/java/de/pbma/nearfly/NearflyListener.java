package de.pbma.nearfly;

public interface NearflyListener {
    void onLogMessage(CharSequence msg);
    void onStateChanged(String state);
    void onRootNodeChanged(String rootNode);
    void onMessage(String message);
}
