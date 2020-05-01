package de.pbma.notused.nearbymessanger;

// Unpublisht die Nearby-Nachrichten nach einer gewissen Zeit, um ein ähnlicheres Verhalten zu
// MQTT zu kreieren d.h. publishen und dann Weg

import android.content.Context;

import com.google.android.gms.nearby.Nearby;

import java.util.LinkedList;
import java.util.Queue;

class NearbyUnpublisher{
    private Queue<ExtMessage> extMessageToUnpub;
    private Thread threadUnpub;
    private Object lock = new Object();
    private long millisToLive = 5000; // Time to live in Millis

    public NearbyUnpublisher(Context context){
        extMessageToUnpub = new LinkedList<ExtMessage>();

        threadUnpub = new Thread(() -> {
            while(true){
                waitForWork();

                Long millisUntilExpired = extMessageToUnpub.peek().time + millisToLive - System.currentTimeMillis();

                // Schaue ob Elemente älter als die Konstante(z.B. 3 Sek ist)
                if (millisUntilExpired < 0){
                    Nearby.getMessagesClient(context).unpublish(extMessageToUnpub.remove().getMessage());
                }

                // Unpulishen, ansonsten warten, solange bis Zeit um ist
                else{
                    try {
                        Thread.sleep(millisUntilExpired);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        threadUnpub.setName("NearbyUnpulisher");
        threadUnpub.start();
    }

    public void unpubAfterExpired(ExtMessage extMessage){
        extMessageToUnpub.add(extMessage);

        // Aufwachen, wenn Thread eingeschlafen
        synchronized(lock) {
            lock.notify();
        }
    }

    public void waitForWork(){
        synchronized(lock) {
            while (extMessageToUnpub.isEmpty()) {
                try {
                    lock.wait(); //wait for the queue to become empty
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
