package de.pbma.nearflyexample.scenarios;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import de.pbma.nearfly.NearflyBindingActivity;
import de.pbma.nearfly.NearflyListener;
import de.pbma.nearfly.NearflyService;

public class NearflySampleActivity extends NearflyBindingActivity {

    private final String NEARFLY_CHANNEL = "sensors/humidity";
    private boolean neaflyServiceConnectCalled = false;

    @Override
    public void onNearflyServiceBound() {
        if (!neaflyServiceConnectCalled) {
            nearflyService.addSubCallback(nearflyListener);
            nearflyService.connect("ThisIsMyUniqueRoomString", NearflyService.USE_NEARBY);
            nearflyService.subIt(NEARFLY_CHANNEL);
            neaflyServiceConnectCalled = true;
        }
    }

    @Override
    public void onNearflyServiceUnbound() {
    }

    NearflyListener nearflyListener = new NearflyListener() {
        @Override
        public void onLogMessage(String state) {
            // Log.v("test", output);
            switch (state){
                case NearflyService.State.CONNECTED:
                    Log.v("test", "Hello World!");
                    nearflyService.pubIt(NEARFLY_CHANNEL, "Hello World!");
                    // OR
                    // nearflyService.pubIt(NEARFLY_CHANNEL, "Hello World!", -10, true);
                    break;
                case NearflyService.State.DISCONNECTED:
                    Log.v("test", "disconnected");
                    break;
            }
            // Log.v("test", state);
        }

        @Override
        public void onMessage(String channel, String message) {
            Log.v("test",channel + " " + message);
        }

        @Override
        public void onFile(String channel, String path, String textAttachment) {
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.main);
    }
}