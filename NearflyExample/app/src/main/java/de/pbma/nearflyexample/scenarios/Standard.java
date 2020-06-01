package de.pbma.nearflyexample.scenarios;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import java.util.Random;

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
    private Button mBtnToggleConMode;


    @Override
    public void onNearflyServiceBound() {
        if (!neaflyServiceConnectCalled) {
            nearflyService.addSubCallback(nearflyListener);
            nearflyService.connect("19moa18", NearflyService.USE_NEARBY);
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

            switch (output){
                case NearflyService.State.CONNECTED:
                    int color = (nearflyService.getConnectionMode()==nearflyService.USE_MQTT)? R.color.state_connected: R.color.colorAccent;
                    runOnUiThread(() ->
                            mBtnToggleConMode.setBackgroundColor(ResourcesCompat.getColor(
                                    getResources(), color, null)));

                    break;
                case NearflyService.State.DISCONNECTED:
                    if (!nearflyService.isModeSwitching()){
                        runOnUiThread(() ->
                                mBtnToggleConMode.setBackgroundColor(ResourcesCompat.getColor(
                                        getResources(), R.color.gray, null)));
                    }
                    break;
            }
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

        @Override
        public void onBigBytes(String channel, byte[] bytes) {
            runOnUiThread(() -> {
                mDebugLogView.append("channel:"+channel+" message: "+new String(bytes)
                        + "\n");
            });
        }
    };

    public void publish(View view){
         // nearflyService.pubBigBytes(NEARFLY_CHANNEL, String.valueOf(++cnt).getBytes());
        nearflyService.pubIt(NEARFLY_CHANNEL, getRandomData(50_000));
        // nearflyService.pubIt(NEARFLY_CHANNEL, String.valueOf(++cnt) );
        // OR
        // nearflyService.pubIt(NEARFLY_CHANNEL, String.valueOf(++cnt), 0, false );
    }

    private String getRandomData(long a){
        Random random = new Random();
        String str = "";
        for (int i=0; i<a; i++){
            str += ""+random.nextInt(10);
        }
        return str;
    }

    public void connect(View view){
        nearflyService.connect("19moa18", nearflyService.getConnectionMode());
    }

    public void disconnect(View view){
        nearflyService.disconnect();
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
        mBtnToggleConMode = findViewById(R.id.btn_toggle_conmode);

        mDebugLogView = findViewById(R.id.debug_log);
        mDebugLogView.setMovementMethod(new ScrollingMovementMethod());
    }


    public void logIt(String str){
        super.logIt(str);
        mDebugLogView.append(str + "\n");
    }
}