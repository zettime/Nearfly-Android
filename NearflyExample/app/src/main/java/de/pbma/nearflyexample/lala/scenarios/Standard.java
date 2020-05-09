package de.pbma.nearflyexample.lala.scenarios;


import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import de.pbma.nearfly.NearflyBindingActivity;
import de.pbma.nearfly.NearflyListener;
import de.pbma.nearfly.NearflyService;
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

    private final String NEARFLY_CHANNEL = "test/a";
    private boolean neaflyServiceConnectCalled = false;


    @Override
    public void onNearflyServiceBound() {
        if (!neaflyServiceConnectCalled) {
            nearflyService.addSubCallback(nearflyListener);
            nearflyService.connect("19moa18", NearflyService.USE_MQTT);
            nearflyService.subIt(NEARFLY_CHANNEL);
            neaflyServiceConnectCalled = true;
        }
    }

    @Override
    public void onNearflyServiceUnbound() {
    }

    NearflyListener nearflyListener = new NearflyListener() {
        @Override
        public void onLogMessage(String output) {
                runOnUiThread(() -> {
                    mDebugLogView.append(output + "\n");
                });
        }

        @Override
        public void onMessage(String channel, String message) {
            runOnUiThread(() -> {
                mDebugLogView.append("channel:"+channel+" message: "+message + "\n");
            });
        }

        @Override
        public void onFile(String channel, String path, String textAttachment){
            runOnUiThread(() -> {
                mDebugLogView.append(channel + " " + path + " " + textAttachment + "\n");
            });
        }
    };

    public void publish(View view){
        nearflyService.pubIt(NEARFLY_CHANNEL, String.valueOf(++cnt) );
        // OR
        // nearflyService.pubIt(NEARFLY_CHANNEL, String.valueOf(++cnt), 0, false );
    }

    public void toggleConnectionMode(View view){
        if (nearflyService.getConnectionMode()==nearflyService.USE_MQTT)
            nearflyService.switchConnectionMode(NearflyService.USE_NEARBY);
        else
            nearflyService.switchConnectionMode(NearflyService.USE_MQTT);
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
