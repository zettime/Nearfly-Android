package com.google.location.nearby.apps.walkietalkie;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;


import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class NearflyService extends Service {
    public final static String USE_MQTT = "mqtt";
    public final static String USE_NEARBY = "nearby";
    public final String TAG = "NearflyServices";

    public final static String ACTION_START = "start";
    public final static String ACTION_STOP = "stop";
    public final static String ACTION_BIND = "bind";

    MyNearbyConnectionsClient nearbyConnectionsClient = new MyNearbyConnectionsClient();
    MyNearbyConnectionsClient.MyConnectionsListener narbyConnectionListener = new MyNearbyConnectionsClient.MyConnectionsListener() {
        @Override
        public void onLogMessage(CharSequence msg) {/*Wahrscheinlich nicht relevant für Nutzer*/
            nearflyListener.onLogMessage(msg);
        }

        @Override
        public void onStateChanged(String state) {/*Wahrscheinlich nicht relevant für Nutzer*/
            nearflyListener.onStateChanged(state);
        }

        @Override
        public void onRootNodeChanged(String rootNode) {/*Wahrscheinlich nicht relevant für Nutzer*/
            nearflyListener.onRootNodeChanged(rootNode);
        }

        @Override
        public void onMessage(String msg) {
            nearflyListener.onMessage(msg);
        }
    };

    Context context;
    // private ArrayList<NearflyListener> listeners = new ArrayList<>();
    NearflyListener nearflyListener;

    @Override
    public void onCreate() {
        super.onCreate();

        nearbyConnectionsClient.onCreate(getApplicationContext(), narbyConnectionListener);

    }

    public void subIt(String channel, NearflyListener nearflyListener) {
        // nearbyConnectionsClient.addSubCallback("test", nearbyConnectionsClient);
    }

    public void unsubIt() {
        // nearbyConnectionsClient.unsubIt("test", nearflyListener);
    }

    public void pubIt(String channel, String message) {
        nearbyConnectionsClient.pubIt(channel, message);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand");
        String action;
        if (intent != null) {
            action = intent.getAction();
        } else {
            Log.w(TAG, "upps, restart");
            action = ACTION_START;
        }
        if (action == null) {
            Log.w(TAG, "  action=null, nothing further to do");
            return START_STICKY;
        }
        switch (action) {
            case ACTION_START:
                Log.v(TAG, "onStartCommand: starting Nearby");
                nearbyConnectionsClient.onStart();
                // whatever else needs to be done on start may be done  here
                return START_STICKY;
            case ACTION_STOP:
                Log.v(TAG, "onStartCommand: stopping Nearby");
                nearbyConnectionsClient.onStop();
                // whatever else needs to be done on stop may be done  here
                return START_NOT_STICKY;
            default:
                Log.w(TAG, "onStartCommand: unkown action=" + action);
                return START_NOT_STICKY;
        }
    }

    public void addSubCallback(String channel, NearflyListener nearflyListener) {
        // Channel wird derzeitig noch nicht benutzt
        // listeners.add(nearflyListener);
        this.nearflyListener = nearflyListener;
    }

    public void removeSubCallback(NearflyListener nearflyListener) {
        // listeners.remove(nearflyListener);
        this.nearflyListener = null;
    }

    // TODO

    /********************************************************************/
    public class LocalBinder extends Binder {
        public NearflyService getNearflyService() {
            Log.v(TAG, "onBinding");
            return NearflyService.this;
        }
    }

    final private IBinder localBinder = new LocalBinder();

    /*********************************************************************/


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "onBind");
        String action = intent.getAction();
        if (action != null && action.equals(NearflyService.ACTION_BIND)) {
            Log.v(TAG, "onBind success");
            return localBinder;
        } else {
            Log.e(TAG, "onBind only defined for ACTION_BIND");
            Log.e(TAG, "       did you want to call startService? ");
            return null;
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onStop");
        nearbyConnectionsClient.onStop();

        super.onDestroy();
    }

    /** MQTT /*************************************************************************/

}
