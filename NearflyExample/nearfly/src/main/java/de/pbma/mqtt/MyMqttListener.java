package de.pbma.mqtt;

/**
 * Application interface
 */
public interface MyMqttListener {
    void onMqttMessage(String topic, String message);
    void onMQTTStatus(boolean connected);
    void onLogMessage(String message);
}
