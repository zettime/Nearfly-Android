package de.pbma.nearflyexample.measureTimes;

import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import de.pbma.nearfly.Constants;
import de.pbma.nearfly.NearflyBindingActivity;
import de.pbma.nearfly.NearflyListener;
import de.pbma.nearfly.NearflyService;

public class MQTTConnection extends NearflyBindingActivity {

    private final String NEARFLY_CHANNEL = "sensors/humidity";
    private boolean neaflyServiceConnectCalled = false;
    private long mStartTime = 0;

    Date today = Calendar.getInstance().getTime();
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-hh.mm.ss");
    private File filePath = new File(Constants.fileDirectory, "mqtt_build"+formatter.format(today)+"_statistic.txt");
    private FileWriter writer=null;
    {
        try {
            writer = new FileWriter(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onNearflyServiceBound() {
        if (!neaflyServiceConnectCalled) {
            nearflyService.addSubCallback(nearflyListener);
            mStartTime = System.currentTimeMillis();
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
        public void onLogMessage(String state) {
            // Log.v("measureTest", output);
            switch (state){
                case NearflyService.State.CONNECTED:
                    // Log.v("measureTest", "Hello World!");
                    // nearflyService.pubIt(NEARFLY_CHANNEL, "Hello World!");
                    String time = (System.currentTimeMillis()-mStartTime)+"\n";
                    Log.v("measure", "Needed Time: "+ time);
                    try {
                        writer.append(time);
                        writer.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mStartTime=System.currentTimeMillis();
                    neaflyServiceConnectCalled = false;
                    nearflyService.disconnect();
                    new Thread(() -> {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        onNearflyServiceBound();
                    }).start();
                    // OR
                    // nearflyService.pubIt(NEARFLY_CHANNEL, "Hello World!", -10, true);
                    break;
                case NearflyService.State.DISCONNECTED:
                    Log.v("measureTest", "disconnected");
                    break;
            }
            // Log.v("measureTest", state);
        }

        @Override
        public void onMessage(String channel, String message) {
            Log.v("measureTest",channel + " " + message);
        }

        @Override
        public void onFile(String channel, String path, String textAttachment) {
        }

        @Override
        public void onBigBytes(String channel, byte[] bytes) {

        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // setContentView(R.layout.main);
    }
}