package com.google.location.nearby.apps.walkietalkie;

public interface NearflyListener {
    void onLogMessage(CharSequence msg);
    void onStateChanged(String state);
    void onRootNodeChanged(String rootNode);
    void onMessage(String message);
}
