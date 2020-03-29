package de.pbma.mqtt;

/**
 * Application interface
 */
public interface MyMqttListener {
    void onMqttMessage(String id, String text);
    void onMQTTStatus(boolean connected);
    void onLogMessage(String message);
}
