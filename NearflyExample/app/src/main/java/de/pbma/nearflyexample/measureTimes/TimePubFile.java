package de.pbma.nearflyexample.measureTimes;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import java.io.File;
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
public class TimePubFile extends NearflyBindingActivity {

    private final String TAG = "TimePubFile";
    private final String NEARFLY_CHANNEL = "sensors/humidity";
    private boolean neaflyServiceConnectCalled = false;
    private long mStartTime = 0;
    // private long mEndTime = 0;
    private int SENDER= 0;
    private int RECEIVER = 1;
    // private int PAUSE = 2;
    private int mState = RECEIVER;
    private final int MEDIA_REQUEST_CODE=15255;
    private long mAverage = 0;
    private int mCnt = 0;
    private boolean init=false;

    private Button mBtnStartSending;
    private Button mBtnStopSending;
    private ArrayList<Long> nearbyBuff = new ArrayList<>();
    private ArrayList<Long> mqttBuff = new ArrayList<>();
    private int useTech = NearflyService.USE_NEARBY;
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

    // --------------------------------

    public void openMediaTaker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, MEDIA_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == MEDIA_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && resultData != null) {
                pubFileOnResult(resultData);
            } else Toast.makeText(this, "You haven't picked Image", Toast.LENGTH_LONG).show();
        }
    }

    private void pubFileOnResult(Intent resultData) {
        // The URI of the file selected by the user.
        Uri uri = resultData.getData();
        ContentResolver cr = getContentResolver();
        Long fileSize = null;
        try {
            fileSize = getSize(cr.openFileDescriptor(uri, "r").getFileDescriptor());
        } catch (IOException e) {
            e.printStackTrace();
        }

        logV("startSending");
        mState=SENDER;

        // Beginn with logging
        mLogger.log(fileSize+"Byte\n");

        //for (int i=0; i<mWantedMsg; i++)
        mStartTime = System.currentTimeMillis();
        nearflyService.pubFile(NEARFLY_CHANNEL, uri, "1");
    }

    public long getSize(FileDescriptor fd) throws IOException {
        try (FileInputStream fis = new FileInputStream(fd)) {
            return fis.getChannel().size();
        }
    }

    public void stopSending(){
        Log.v(TAG, "nothing happens");
    }

    @Override
    protected void onDestroy() {
        mLogger.close();
        super.onDestroy();
    }

    // --------------------------


    NearflyListener nearflyListener = new NearflyListener() {
        @Override
        public void onLogMessage(String state) {
        }

        @Override
        public void onMessage(String channel, String message) {
        }

        @Override
        public void onFile(String channel, String path, String textAttachment) {
            long endTime = System.currentTimeMillis();
            if (mState==RECEIVER){ // First to go faster
                Context context = getApplicationContext();
                Uri uri = FileProvider.getUriForFile(
                        context,
                        context.getApplicationContext().getPackageName() + ".provider",
                        new File(path));
                nearflyService.pubFile(NEARFLY_CHANNEL, uri, textAttachment);
            }

            // Log.v("measureTest",channel + " " + message);
            if (mState==SENDER){
                long neededTime = (endTime-mStartTime)/2;
                mLogger.log(neededTime+"\n");
                Log.v(TAG, "neddedTime "+neededTime);
                }
            }

        @Override
        public void onBigBytes(String channel, byte[] bytes) {

        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.timemeasure);

        mBtnStartSending = findViewById(R.id.btn_startsending);
        mBtnStopSending = findViewById(R.id.btn_stopsennding);
        // mBtnStartPause = findViewById(R.id.btn_pause);

        mBtnStartSending.setOnClickListener((view) -> openMediaTaker());
        mBtnStopSending.setOnClickListener((view) -> stopSending());
        // mBtnStartPause.setOnClickListener((view) -> pause());

        mLogger = new Logger((
                ((useTech==NearflyService.USE_NEARBY)?"nearby":"mqtt"))+"_file");
    }

    public void logV(String str){
        Log.v(TAG, str);
    }
}