package de.pbma.notused.nearbymessanger;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.MessagesClient;
import com.google.android.gms.nearby.messages.PublishCallback;
import com.google.android.gms.nearby.messages.PublishOptions;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeCallback;
import com.google.android.gms.nearby.messages.SubscribeOptions;

class NearbyService extends Service {
    public final static String USE_MQTT = "mqtt";
    public final static String USE_NEARBY = "nearby";

    Message mMessage;
    public MessageListener mMessageListener;
    String TAG = "stdout";
    Integer cnt = 0;
    Context context;

    MessagesClient messagesSubClient;
    MessagesClient messagesPubClient;

    NearbyUnpublisher nearbyUnpublisher;

    public void initService(Context context, MessageListener messageListener) {
        this.context = context;
        this.mMessageListener = messageListener;
        messagesSubClient = Nearby.getMessagesClient(context);
        messagesPubClient = Nearby.getMessagesClient(context);

        // Unpublisher Aktivieren
        nearbyUnpublisher = new NearbyUnpublisher(context);
    }

    public void subIt() {
        SubscribeCallback subscribeCallback = new SubscribeCallback() {
            @Override
            public void onExpired() {
                super.onExpired();
                Log.v(TAG, "sub message expired");
            }
        };

        SubscribeOptions options = new SubscribeOptions.Builder()
                .setStrategy(Strategy.DEFAULT)
                .setCallback(subscribeCallback)
                .build();

        messagesSubClient.subscribe(mMessageListener);

    }

    public void unsubIt() {
        messagesSubClient.unsubscribe(mMessageListener);
    }

    public void pubIt() {

        cnt++;
        final ExtMessage extMessage = new ExtMessage("test/", cnt.toString());

        //final Message mActiveMessage = new Message(cnt.toString().getBytes());
        //pubQueue.add(extMessage);

        // Publish
        PublishCallback publishCallback = new PublishCallback() {
            @Override
            public void onExpired() {
                super.onExpired();
                Log.v(TAG, "pub message expired");
            }
        };

        PublishOptions options = new PublishOptions.Builder()
                .setStrategy(Strategy.DEFAULT)
                .setCallback(publishCallback)
                .build();

        messagesSubClient.publish(extMessage.getMessage(), options);
        nearbyUnpublisher.unpubAfterExpired(extMessage);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    // TODO ?
    /********************************************************************/
    public class LocalBinder extends Binder {
        public NearbyService getNearflyService() {
            return NearbyService.this;
        }
    }

    final private IBinder localBinder = new LocalBinder();
    /*********************************************************************/

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "onBind");
        String action = intent.getAction();// MQTT oder Nearby benutzen
        Log.v(TAG, "Nearfly is using " + action);
        return localBinder;
    }

    @Override
    public void onDestroy() {
        //Nearby.getMessagesClient(this).unpublish(mMessage);

        Log.d(TAG, "onStop");
        Nearby.getMessagesClient(this).unsubscribe(mMessageListener);

        super.onDestroy();
    }

    /** MQTT /*************************************************************************/

}
