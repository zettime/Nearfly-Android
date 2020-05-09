package de.pbma.mqtt;

/**
 * Application interface
 */
public interface MyMqttListener {
    void onMessage(String topic, String message);
    void onStatus(boolean connected);
    void onLogMessage(String message);
    void onFile(String channel, String path, String textAttachment);
}
