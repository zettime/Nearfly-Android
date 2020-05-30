package de.pbma.nearflyexample.measureTimes;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import de.pbma.nearfly.NearflyBindingActivity;
import de.pbma.nearfly.NearflyListener;
import de.pbma.nearfly.NearflyService;
import de.pbma.nearflyexample.R;

/** Calculates the average latency for a publish **/
public class TimePubIt extends NearflyBindingActivity {

    private final String TAG = "timePubIt";
    private final String NEARFLY_CHANNEL = "sensors/humidity";
    private boolean neaflyServiceConnectCalled = false;
    private final int DATA_SIZE = 30_000;
    private String mRandStr = getRandomString(DATA_SIZE);
    private long mStartTime = 0;
    // private long mEndTime = 0;
    private int SENDER= 0;
    private int RECEIVER = 1;
    // private int PAUSE = 2;
    private int mState = RECEIVER;
    private long mAverage = 0;
    private int mCnt = 0;
    private boolean init=false;

    private Button mBtnStartSending;
    private Button mBtnStopSending;
    private ArrayList<Long> nearbyBuff = new ArrayList<>();
    private ArrayList<Long> mqttBuff = new ArrayList<>();
    private int useTech = NearflyService.USE_MQTT;
    private Logger mLogger;
    private int mWantedMsg = 2;
    private int mRealMsg = 2;

    @Override
    public void onNearflyServiceBound() {
        if (!neaflyServiceConnectCalled) {
            nearflyService.addSubCallback(nearflyListener);
            nearflyService.connect("19moa18", useTech);
            nearflyService.subIt(NEARFLY_CHANNEL);
            neaflyServiceConnectCalled = true;
        }
    }

    @Override
    public void onNearflyServiceUnbound() {
    }


    public String getRandomString(int size){
        String str="";
        Random random = new Random();

        for (int i=0; i<size; i++){
            str += random.nextInt(10);
        }

        return str;
    }

    public void startSending(){
        logV("startSending");
        mState=SENDER;

        // Beginn with logging
        mLogger = new Logger((
                ((useTech==NearflyService.USE_NEARBY)?"nearby":"mqtt"))+"_bytes_"+DATA_SIZE);
        mLogger.log(DATA_SIZE+"Byte\n");

        //for (int i=0; i<mWantedMsg; i++)
        mStartTime = System.currentTimeMillis();
        nearflyService.pubIt(NEARFLY_CHANNEL, mRandStr);
    }

    public void stopSending(){
        logV("stopSending");
        mState = RECEIVER;
        nearflyService.disconnect();
        nearflyService.connect();
    }

    /*public void pubIt(View view){
        nearflyService.pubIt(NEARFLY_CHANNEL, mRandStr);
        mState = SENDER;
    }*/


    NearflyListener nearflyListener = new NearflyListener() {
        @Override
        public void onLogMessage(String state) {
            // Log.v("measureTest", output);
            /*switch (state){
                case NearflyService.State.CONNECTED:
                    Log.v(TAG, "connected");
                    // nearflyService.pubIt(NEARFLY_CHANNEL, "Hello World!");
                    break;
                case NearflyService.State.DISCONNECTED:
                    Log.v("measureTest", "disconnected");
                    break;
            }*/
            // Log.v("measureTest", state);
        }

        @Override
        public void onMessage(String channel, String message) {
            long endTime = System.currentTimeMillis();
            if (mState==RECEIVER){ // First to go faster
                nearflyService.pubIt(NEARFLY_CHANNEL, message);
            }

            Log.v("measureTest",channel + " " + message);
            if (mState==SENDER){
                long neededTime = (endTime-mStartTime)/2;
                mLogger.log(neededTime+"\n");
                Log.v(TAG, ""+neededTime);
                nearbyBuff.add(neededTime);
                if (nearbyBuff.size()==100){
                    // nearflyService.disconnect();
                    long sum=0;
                    for (Long val : nearbyBuff){
                        sum+=val;
                    }

                    mLogger.log("AVERAGE:"+(sum/nearbyBuff.size()));
                    nearbyBuff.clear();
                    mLogger.close();

                    finish();

                    /*nearflyService.disconnect();
                    // nearflyService.connect("19moa18", NearflyService.USE_MQTT);
                    mLogger.log("AVERAGE:"+(sum/nearbyBuff.size()));
                    nearflyService.connect("19moa18", useTech);*/
                    // stopSending();
                }


                new Thread(() -> {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    mStartTime = System.currentTimeMillis();
                    nearflyService.pubIt(NEARFLY_CHANNEL, mRandStr);
                }).start();
            }
        }

        @Override
        public void onFile(String channel, String path, String textAttachment) {
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.timemeasure);

        mBtnStartSending = findViewById(R.id.btn_startsending);
        mBtnStopSending = findViewById(R.id.btn_stopsennding);
        // mBtnStartPause = findViewById(R.id.btn_pause);

        mBtnStartSending.setOnClickListener((view) -> startSending());
        mBtnStopSending.setOnClickListener((view) -> stopSending());
        // mBtnStartPause.setOnClickListener((view) -> pause());
    }

    public void logV(String str){
        Log.v(TAG, str);
    }
}