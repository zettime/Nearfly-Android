package de.pbma.notused.nearbymessanger;

/**
 * Application interface
 */
interface MyMqttListener {
    void onMessage(String topic, String message);
    void onStatus(boolean connected);
    void onLogMessage(String message);
    void onFile(String path, String textAttachment);
}
