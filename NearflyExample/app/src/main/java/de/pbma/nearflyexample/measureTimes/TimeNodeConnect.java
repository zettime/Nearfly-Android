package de.pbma.nearflyexample.measureTimes;


import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Time;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import de.pbma.nearfly.Constants;
import de.pbma.nearfly.NearflyBindingActivity;
import de.pbma.nearfly.NearflyListener;
import de.pbma.nearfly.NearflyService;
import de.pbma.nearflyexample.R;

public class TimeNodeConnect extends NearflyBindingActivity {
    /** If true, debug logs are shown on the device. */
    private static final boolean DEBUG = true;

    private final String TAG = "testActivity";

    /** To measure the time */
    private AtomicLong mConnectStartTime = new AtomicLong(0);
    private AtomicBoolean mThRunning = new AtomicBoolean(false);

    /** A running log of debug messages. Only visible when DEBUG=true. */
    private TextView mDebugLogView;
    private Integer cnt = 0;

    private final String NEARFLY_CHANNEL = "measureTest/a";
    Date today = Calendar.getInstance().getTime();
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-hh.mm.ss");
    private File filePath = new File(Constants.fileDirectory, formatter.format(today)+"_statistic.txt");
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
        connect(mDebugLogView);
    }

    @Override
    public void onNearflyServiceUnbound() {
    }

    boolean mNode = false;

    private void logFile(String str){
        try {
            writer.append(str);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void logView(String str){
        runOnUiThread(() -> {
            mDebugLogView.append(str);
        });
    }

    NearflyListener nearflyListener = new NearflyListener() {
        @Override
        public void onLogMessage(String output) {
                // logView(output + "\n");


                /** End Connection**/
                switch (output){
                    case NearflyService.State.CONNECTED:
                        logView("---------> Needed time for Connection: "+
                                        (System.currentTimeMillis()- mConnectStartTime.get())+ "ms\n");
                        logFile((System.currentTimeMillis()- mConnectStartTime.get())+ "\n");
                        if (mNode==true){
                            if (mThRunning.get())
                                return;

                            new Thread(() -> {
                                mThRunning.set(true);
                                try {
                                    Thread.sleep(5000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                disconenct(mDebugLogView);
                                disconenct(mDebugLogView);
                                try {
                                    Thread.sleep(10000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                connect(mDebugLogView);
                                mThRunning.set(false);
                            }).start();
                        }
                        break;
                    case NearflyService.State.DISCONNECTED:
                        mConnectStartTime.set(System.currentTimeMillis());
                        logView("disconnected\n");
                        break;
                    case "State set to NODE":
                        mNode = true;
                        logView("-----------> Nedded time for NODE State: "+
                                (System.currentTimeMillis()- mConnectStartTime.get())+ "ms\n");
                        logFile((System.currentTimeMillis()- mConnectStartTime.get())+ ";");
                        break;
                }

            if (output.matches("attemptsInBackoff:.*")){
                logView("----------> Nedded time for attemptsInBackoff: "+
                        (System.currentTimeMillis()- mConnectStartTime.get())+ "ms\n");
                logFile("----------> Nedded time for attemptsInBackoff: "+
                        (System.currentTimeMillis()- mConnectStartTime.get())+ "ms\n");
            }

            if (output.matches("time .*")){
                logView(output+"\n");
                logFile(output.split(":")[1].replace(" ","")+";");
            }
        }

        @Override
        public void onMessage(String channel, String message) {
            logView("channel:"+channel+" message: "+message + "\n");
        }

        @Override
        public void onFile(String channel, String path, String textAttachment){
            logView(channel + " " + path + " " + textAttachment + "\n");
        }

        @Override
        public void onBigBytes(String channel, byte[] bytes) {

        }
    };

    public void connect(View view){
        nearflyService.addSubCallback(nearflyListener);
        mConnectStartTime.set(System.currentTimeMillis());
        nearflyService.connect("19moa18", NearflyService.USE_NEARBY);
        // logFile("connecting()\n");
        nearflyService.subIt(NEARFLY_CHANNEL);
    }

    public void disconenct(View view){
        nearflyService.unsubIt(NEARFLY_CHANNEL);
        // logFile("disconnect()\n");
        nearflyService.disconnect();
        nearflyService.removeSubCallback(nearflyListener);
    }

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
        logFile("NODE, COLAVOID, CONNODE\n");


        /*tvCurrentState = findViewById(R.id.tv_current_state);
        tvRootNode = findViewById(R.id.tv_root_node);*/
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    @Override
    protected void onDestroy() {
        disconenct(mDebugLogView);
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        super.onDestroy();
    }

    public void logIt(String str){
        // TODO
        super.logIt(str);
        logFile(str + "\n");
    }
}
