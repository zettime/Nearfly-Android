package de.pbma.nearbyconnections;

// Unpublisht die Nearby-Nachrichten nach einer gewissen Zeit, um ein ähnlicheres Verhalten zu
// MQTT zu kreieren d.h. publishen und dann Weg

import com.google.android.gms.nearby.connection.Payload;

import java.util.LinkedList;
import java.util.Queue;

@Deprecated
class PublishForwarder {
    public static Object Task;
    private Thread threadForwarder;
    private Object lock = new Object();
    public  Queue<Payload> queuePayload;
    public  Queue<String> queueExcludedEntpoint;

    public interface Task{
        void execute();
    }
    Task task;

    public PublishForwarder(Task task/*, Queue<Payload> queuePayload,  Queue<String> queueExcludedEntpoint*/){
        /*this.queuePayload = queuePayload;
        this.queueExcludedEntpoint = queueExcludedEntpoint;*/
        this.queuePayload = new LinkedList<Payload>();
        this.queueExcludedEntpoint = new LinkedList<String>();

        this.task = task;

        threadForwarder = new Thread(() -> {
            while(true){
                waitForWork();
                task.execute();
                }
            });
        threadForwarder.setName("NearbyUnpulisher");
        threadForwarder.start();
    }

    public void newMessage(Payload payload, String endpointId){
        queuePayload.add(payload);
        queueExcludedEntpoint.add(endpointId);

        // Aufwachen, wenn Thread eingeschlafen
        synchronized(lock) {
            lock.notify();
        }
    }

    public void newMessage(Payload payload){
        queuePayload.add(payload);
    }

    public void newMessage(String endpointId){
        queueExcludedEntpoint.add(endpointId);
    }

    public void waitForWork(){
        synchronized(lock) {
            while (queuePayload.isEmpty()) {
                try {
                    lock.wait(); //wait for the queue to become empty
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
