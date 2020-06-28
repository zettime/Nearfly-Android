package de.pbma.nearflyexample.measureTimes;

import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import de.pbma.nearfly.Constants;
import de.pbma.nearfly.NearflyBindingActivity;
import de.pbma.nearfly.NearflyClient;
import de.pbma.nearfly.NearflyListener;
import de.pbma.nearfly.NearflyService;
import de.pbma.nearflyexample.R;

/** component for checking the necessary build times of the network **/
public class TimeNetworkBuild2 extends NearflyBindingActivity {
    /** If true, debug logs are shown on the device. */
    private static final boolean DEBUG = true;

    private final String TAG = "testActivity";

    /** To measure the time */
    private AtomicLong mConnectStartTime = new AtomicLong(0);
    private int mNodeNo = 0;

    /** A running log of debug messages. Only visible when DEBUG=true. */
    private TextView mDebugLogView;
    private Integer cnt = 0;
    private final int DESIRED_NOTES = 5;
    private final int INTERVAL = 2; // in Minutes
    private final AtomicBoolean done = new AtomicBoolean(false);

    private final String NEARFLY_CHANNEL = "measureTest/a";
    Date today = Calendar.getInstance().getTime();
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-hh.mm.ss");
    Logger mLogger = new Logger(DESIRED_NOTES+"Nearby Network build");
    private boolean mDisconnectTimerStarted = false;
    private boolean mFinished = false;

    @Override
    public void onNearflyServiceBound() {
        connect(mDebugLogView);
    }

    @Override
    public void onNearflyServiceUnbound() {
    }

    boolean mRoot = false;

    private void logView(String str){
        runOnUiThread(() -> {
            mDebugLogView.append(str);
        });
    }

    ArrayList<String> strArray = new ArrayList<>();
    NearflyListener nearflyListener = new NearflyListener() {
        @Override
        public void onLogMessage(String output) {
            if (output.equals("State set to ROOT"))
                mRoot = true;
            if (output.equals("State set to NODE"))
                mRoot = false;

            if (mRoot==false){
                logView(output+"\n");
                return;
            }

            if (output.matches("connectedToEndpoint.*")) {
                if (!strArray.contains(output)){
                    strArray.add(output);
                    mNodeNo++;
                    mLogger.log((System.currentTimeMillis()- mConnectStartTime.get())+ ";");
                }

                if (mFinished){
                    nearflyService.pubIt(NEARFLY_CHANNEL, "finished");
                }
                // mNodeNo++;
                mLogger.log((System.currentTimeMillis()- mConnectStartTime.get())+ ";");
                logView("--> Node " + mNodeNo + "connected in "+
                        (System.currentTimeMillis()- mConnectStartTime.get())+ "ms\n");
            }
            /*if (output.matches("disconnectedFromEndpoint.*"))
                mNodeNo--;*/
            if (mRoot==true && mNodeNo>=DESIRED_NOTES && mDisconnectTimerStarted==false) {
                new Thread(() -> {
                    mDisconnectTimerStarted = true;
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                if (mRoot==true && mNodeNo>=DESIRED_NOTES) {
                    mFinished = true;
                    nearflyService.pubIt(NEARFLY_CHANNEL, "finished");
                    nearflyService.pubIt(NEARFLY_CHANNEL, "finished");
                    nearflyService.pubIt(NEARFLY_CHANNEL, "finished");
                    nearflyService.pubIt(NEARFLY_CHANNEL, "finished");
                    mNodeNo = 0;
                    strArray.clear();
                    reconnectAtRightTime();
                    mLogger.log("\n");
                }
                mDisconnectTimerStarted=false;
                }).start();
                }


        }

        long lastConnectOn = 1;
        public void reconnectAtRightTime(){
            new Thread(() -> {
                aa:
                try {
                    while(true){
                        Thread.sleep(500);
                        Calendar rightNow = Calendar.getInstance();
                        int minute = rightNow.get(Calendar.MINUTE);
                        // Prevent to wake up more than one time in same minute
                        if (minute%INTERVAL==0 && lastConnectOn!=minute){
                            lastConnectOn=minute;
                            logView("time to connect...");
                            connect(mDebugLogView);
                            mConnectStartTime.set(System.currentTimeMillis());
                            mFinished=false;
                            break aa;
                        }
                    }
                } catch (InterruptedException e) { e.printStackTrace(); }
            }).start();
        }

        @Override
        public void onMessage(String channel, String message) {
            logView("channel:"+channel+" message: "+message + "\n");
            if (message.equals("finished")){
                disconnect(mDebugLogView); // Go to STANDBY MODE
                reconnectAtRightTime();
            }

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
        nearflyService.askForPermissions(this, true);
        nearflyService.addSubCallback(nearflyListener);
        final long excepted = System.currentTimeMillis();
        mConnectStartTime.set(excepted); // Reset time
        nearflyService.connect("19moa18", NearflyClient.USE_NEARBY);
        nearflyService.subIt(NEARFLY_CHANNEL);
    }

    public void disconnect(View view){
        nearflyService.unsubIt(NEARFLY_CHANNEL);
        // mLogger.log("disconnect()\n");
        nearflyService.disconnect();
        nearflyService.removeSubCallback(nearflyListener);
    }

    public void publish(View view){
        nearflyService.pubIt(NEARFLY_CHANNEL, String.valueOf(++cnt) );
    }

    public void toggleConnectionMode(View view){
        if (nearflyService.getConnectionMode()==NearflyClient.USE_MQTT)
            nearflyService.switchConnectionMode(NearflyClient.USE_NEARBY);
        else
            nearflyService.switchConnectionMode(NearflyClient.USE_MQTT);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDebugLogView = findViewById(R.id.debug_log);
        // mDebugLogView.setVisibility(DEBUG ? View.VISIBLE : View.GONE);
        mDebugLogView.setMovementMethod(new ScrollingMovementMethod());
        String str = "";
        for (int i=0; i<DESIRED_NOTES; i++){
            str+="NODE"+i+"; ";
        }
        mLogger.log(str+"\n");

        /*tvCurrentState = findViewById(R.id.tv_current_state);
        tvRootNode = findViewById(R.id.tv_root_node);*/
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onDestroy() {
        disconnect(mDebugLogView);
        mLogger.close();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        super.onDestroy();
    }

    public void logIt(String str){
        // TODO
        super.logIt(str);
    }
}
