package de.pbma.nearfly;

import android.os.Bundle;

import androidx.annotation.Nullable;

import java.util.concurrent.CountDownLatch;

class NearflySampleActivity extends NearflyBindingActivity {

    private final String NEARFLY_CHANNEL = "sensors/humidity";
    private boolean neaflyServiceConnectCalled = false;
    private CountDownLatch nearflyServiceStartedSignal = new CountDownLatch(1);

    @Override
    public void onNearflyServiceBound() {
        if (!neaflyServiceConnectCalled) {
            nearflyService.addSubCallback(nearflyListener);
            nearflyService.connect("ThisIsMyUniqueRoomString", NearflyService.USE_NEARBY);
            nearflyService.subIt(NEARFLY_CHANNEL);
            neaflyServiceConnectCalled = true;
            nearflyServiceStartedSignal.countDown();
        }
    }

    @Override
    public void onNearflyServiceUnbound() {
    }

    NearflyListener nearflyListener = new NearflyListener() {
        @Override
        public void onLogMessage(String output) {
        }

        @Override
        public void onMessage(String channel, String message) {
            logIt(channel + " " + message);
        }

        @Override
        public void onFile(String path, String textAttachment) {
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.main);
        new Thread(() -> {
            try {
                nearflyServiceStartedSignal.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            nearflyService.pubIt(NEARFLY_CHANNEL, "Hello World!");
        }).start();
    }
}