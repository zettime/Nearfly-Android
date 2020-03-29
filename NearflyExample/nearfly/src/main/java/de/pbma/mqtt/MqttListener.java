package de.pbma.mqtt;

/**
 * Application interface
 */
public interface MqttListener {
    void onMqttMessage(String id, String message);
    void onMQTTStatus(boolean connected);
    void onLogMessage(String message);
}