package de.pbma.nearflyexample.lala.scenarios;


import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import de.pbma.nearfly.NearflyBindingActivity;
import de.pbma.nearfly.NearflyListener;
import de.pbma.nearflyexample.R;

public class Standard extends NearflyBindingActivity {
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
    public void onNearflyServiceBound() {
        nearflyService.addSubCallback(nearflyListener);
        nearflyService.subIt("19moa18/test");
        // nearflyServiceBound = true;
    }

    @Override
    public void onNearflyServiceUnbound() {
    }

    NearflyListener nearflyListener = new NearflyListener() {
        @Override
        public void onLogMessage(String output) {
            if (DEBUG==true)
                runOnUiThread(() -> {

                    mDebugLogView.append(output + "\n");
                });
        }

        @Override
        public void onMessage(String channel, String message) {
            runOnUiThread(() -> {
                mDebugLogView.append(message + "\n");
            });
        }

        @Override
        public void onFile(String path, String textAttachment){}
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
