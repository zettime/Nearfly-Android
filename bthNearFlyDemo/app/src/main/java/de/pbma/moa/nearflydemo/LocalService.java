package de.pbma.moa.nearflydemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

import de.pbma.nearfly.NearflyClient;
import de.pbma.nearfly.NearflyListener;
import de.pbma.nearfly.NearflyService;

public class LocalService extends Service {
    final static String TAG = LocalService.class.getCanonicalName();

    public static final String CHANNEL_DEFAULT = "test/a";
    private static int[] connectionModes = {
            // ToDo: warum gibt es das nochmal im NearflyService? ✓
            NearflyClient.USE_NEARBY,
            NearflyClient.USE_MQTT
    };
    private static Map<Integer, String> connectionModesMap;
    static {
        connectionModesMap = new TreeMap<>();
        connectionModesMap.put(NearflyClient.USE_NEARBY, "Nearby");
        connectionModesMap.put(NearflyClient.USE_MQTT, "MQTT");
    }

    public static final String CMD_START = "start";
    public static final String CMD_STOP = "stop";

    private int connectionMode = NearflyClient.USE_NEARBY;
    private boolean permissionsAsked = false;
    private NearflyClient nearflyClient;

    private IBinder binder = new LocalBinder();
    public class LocalBinder extends Binder {
        public LocalService getLocalService() {
            return LocalService.this;
        }
    }

    @Override public IBinder onBind(Intent intent) {
        return binder;
    }

    public interface NFMessageListener {
        void onMessage(String channel, String message);
    }
    private CopyOnWriteArrayList<NFMessageListener> listeners;
    public void registerNFMessageListener(NFMessageListener listener) {
        if (listeners.contains(listener)) {
            Log.w(TAG, "register: listener already contained, ignoring");
            return;
        }
        listeners.add(listener);
    }

    public void deregisterNFMessageListener(NFMessageListener listener) {
        if (!listeners.contains(listener)) {
            Log.w(TAG, "deregister: listener not contained, ignoring");
            return;
        }
        listeners.remove(listener);
    }

    public void clearNDMessageListeners() {
        listeners.clear();
    }

    private NearflyListener nearflyListener = new NearflyListener() {
        @Override
        public void onLogMessage(String msg) {
            Log.v(TAG, "onLogMessage: " + msg);
        }

        @Override
        public void onMessage(String channel, String message) {
            Log.v(TAG, String.format("onMessage: chan=%s, msg=%s", channel, message));
            for (NFMessageListener listener : listeners) {
                listener.onMessage(channel, message);
            }
        }

        @Override
        public void onFile(String channel, String path, String content) {
            Log.v(TAG, String.format("onFile: chan=%s, path=%s, contents=%s",
                    channel, path, content));
        }

        @Override
        public void onBigBytes(String s, byte[] bytes) {
            Log.v(TAG, String.format("onBigBytes: s(what is s?)=%s, bytes.length=%d",
                    s, bytes.length));
        }
    };

    public int getConnectionMode() {
        return connectionMode;
    }

    public String getConnectionModeString() {
        String ret = connectionModesMap.get(connectionMode);
        if (ret == null) {
            Log.e(TAG, "Error: getConnectionModeString: connectionMode=" + connectionMode);
            Log.e(TAG, connectionModesMap.toString());
        }
        return ret;
    }

    public int nextConnectionMode() {
        int idx = Arrays.binarySearch(connectionModes, connectionMode);
        if (idx == -1) {
            idx = 0;
        } else {
            idx = (idx + 1) % connectionModes.length;
        }
        connectionMode = connectionModes[idx];
        return connectionMode;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "onCreate");
        listeners = new CopyOnWriteArrayList<>();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        switch (action) {
            case CMD_START:
                return START_STICKY;
            case CMD_STOP:
                return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    // TODO: Ich will mir das natürlich merken und dann selbst setzen ✓
    // TODO: und nicht eine Konstante tippen müssen ✓
    @SuppressLint("WrongConstant")
    // TODO: Wieso die AppCompatActivity? Echt? Keine Context? ✓
    public void connect(Activity app) {
        Log.v(TAG, "connect: app=" + app.getLocalClassName() + "nearflyClient=" + nearflyClient);
        if (nearflyClient == null) {
            nearflyClient = new NearflyClient(getApplicationContext());
            nearflyClient.addSubCallback(nearflyListener);
        }
        // if (!permissionsAsked) {
        if (!NearflyClient.hasPermissions(this, false)) {
            Log.v(TAG, "do ask for permission");
            // TODO: How to ask for permission if i do not have the nearflyClient yet? ✓
            NearflyClient.askForPermissions(app, false);
            // permissionsAsked = true;
        }
        nearflyClient.connect(CHANNEL_DEFAULT, connectionMode);
        nearflyClient.subIt(CHANNEL_DEFAULT);
    }

    public void publish(String channel, String message) {
        nearflyClient.pubIt(CHANNEL_DEFAULT, message);
    }
}
