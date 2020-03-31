package de.pbma.nearflyexample.lala.scenarios;


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

import de.pbma.nearbyconnections.ConnectionsActivityWithPermissions;
import de.pbma.nearfly.NearflyListener;
import de.pbma.nearfly.NearflyService;
import de.pbma.nearflyexample.R;

public class Standard extends NearflyBindingAktivity {
    /** If true, debug logs are shown on the device. */
    private static final boolean DEBUG = true;

    private final String TAG = "testActivity";

    /** Displays the current state. */
    private TextView tvCurrentState;

    private TextView tvRootNode;

    /** A running log of debug messages. Only visible when DEBUG=true. */
    private TextView mDebugLogView;
    private Integer cnt = 0;


    @Override
    public void onNearflyServiceConnected() {
        nearflyService.addSubCallback(nearflyListener);
        nearflyService.subIt("19moa18/test");
        nearflyServiceBound = true;
    }

    @Override
    public void onNearflyServiceDisconnected() {
    }

    @Override
    public NearflyListener getNearflyListener() { return nearflyListener; }
    NearflyListener nearflyListener = new NearflyListener() {
        @Override
        public void onLogMessage(CharSequence msg) {
            if (DEBUG==true)
                runOnUiThread(() -> {

                    mDebugLogView.append(msg + "\n");
                });
        }

        @Override
        public void onStateChanged(String state) {
            if (DEBUG==true)
                runOnUiThread(() -> {
                    mDebugLogView.append(state + "\n");
                    tvCurrentState.setText(state);
                });
        }

        @Override
        public void onRootNodeChanged(String rootNode) {
            if (DEBUG==true)
                runOnUiThread(() -> {
                    mDebugLogView.append(rootNode + "\n");
                    tvRootNode.setText(rootNode);
                });
        }

        @Override
        public void onMessage(String message) {
            runOnUiThread(() -> {
                mDebugLogView.append(message + "\n");
            });
        }
    };

    public void publish(View view){
        nearflyService.pubIt("19moa18/test", String.valueOf(++cnt) );
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDebugLogView = findViewById(R.id.debug_log);
        // mDebugLogView.setVisibility(DEBUG ? View.VISIBLE : View.GONE);
        mDebugLogView.setMovementMethod(new ScrollingMovementMethod());

        tvCurrentState = findViewById(R.id.tv_current_state);
        tvRootNode = findViewById(R.id.tv_root_node);
    }


    public void logIt(String str){
        super.logIt(str);
        mDebugLogView.append(str + "\n");
    }
}
