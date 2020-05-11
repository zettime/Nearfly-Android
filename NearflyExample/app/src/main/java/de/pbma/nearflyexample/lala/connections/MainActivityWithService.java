package de.pbma.nearflyexample.lala.connections;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import de.pbma.nearfly.NearflyBindingActivity;
import de.pbma.nearfly.NearflyListener;
import de.pbma.nearfly.NearflyService;
import de.pbma.nearflyexample.R;

public class MainActivityWithService extends AppCompatActivity {
    /** If true, debug logs are shown on the device. */
    private static final boolean DEBUG = true;

    private final String TAG = "testActivity";

    /** Displays the current state. */
    private TextView tvCurrentState;

    private TextView tvRootNode;

    /** A running log of debug messages. Only visible when DEBUG=true. */
    private TextView mDebugLogView;
    private Integer cnt = 0;

    private NearflyService nearflyService;
    private boolean nearflyServiceBound;

    NearflyListener nearflyListener = new NearflyListener() {
        @Override
        public void onLogMessage(String output) {
            /*if (DEBUG==true)
                runOnUiThread(() -> {

                    mDebugLogView.append(output + "\n");
                });*/
        }

        @Override
        public void onMessage(String channel, String message) {
                runOnUiThread(() -> {
                    mDebugLogView.append(message + "\n");
                });
            }

        /*@Override
        public void onStream(Payload payload) {}

        @Override
        public void onBinary(Payload payload) {

        }*/

        @Override
        public void onFile(String channel, String path, String textAttachment){}
    };

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            logIt("onServiceConnected");
            nearflyService = ((NearflyService.LocalBinder) service).getNearflyService();
            nearflyService.addSubCallback(nearflyListener);
            nearflyService.subIt("19moa18/test");
            nearflyServiceBound = true;

            startNearflyService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            logIt("onServiceDisconnected");
            nearflyServiceBound = false;
            unbindNearflyService();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDebugLogView = findViewById(R.id.debug_log);
        // mDebugLogView.setVisibility(DEBUG ? View.VISIBLE : View.GONE);
        mDebugLogView.setMovementMethod(new ScrollingMovementMethod());

        tvCurrentState = findViewById(R.id.tv_current_state);
        tvRootNode = findViewById(R.id.tv_root_node);

        nearflyServiceBound = false;
    }

    @Override
    protected void onStop() {
        unbindNearflyService();
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!nearflyServiceBound)
            bindNearflyService();
    }

    public void startNearflyService(View... view){
        logIt("onStartService");
        Intent intent = new Intent(this, NearflyService.class);
        intent.setAction(NearflyService.ACTION_START);
        startService(intent);
    }


    public void stopNearflyService() {
        logIt( "onStopService");
        Intent intent = new Intent(this, NearflyService.class);
        intent.setAction(NearflyService.ACTION_STOP);
        startService(intent); // to stop
    }

    public void publish(View view){
        nearflyService.pubIt("19moa18/test", String.valueOf(++cnt) );
    }

    private void bindNearflyService() {
        logIt("bindMQTTService");
        Intent intent = new Intent(this, NearflyService.class);
        intent.setAction(NearflyService.ACTION_BIND);
        nearflyServiceBound = bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        if (!nearflyServiceBound) {
            logIt("could not try to bind service, will not be bound");
        }
    }

    private void unbindNearflyService() {
        if (nearflyServiceBound){
            unbindService(serviceConnection);
            nearflyServiceBound = false;
        }
    }

    public void logIt(String str){
        Log.v(TAG, str);
        mDebugLogView.append(str + "\n");
    }

    /*public void changeTech(View view){
        if (nearflyService.USED_TECH==NearflyService.USE_MQTT)
            nearflyService.changeTech(NearflyService.USE_NEARBY);
        else
            nearflyService.changeTech(NearflyService.USE_MQTT);
    }*/
}
