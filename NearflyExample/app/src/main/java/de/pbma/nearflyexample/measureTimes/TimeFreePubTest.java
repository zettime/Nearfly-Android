package de.pbma.nearflyexample.measureTimes;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import org.w3c.dom.Text;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import de.pbma.nearfly.NearflyBindingActivity;
import de.pbma.nearfly.NearflyListener;
import de.pbma.nearfly.NearflyService;
import de.pbma.nearflyexample.R;

/** Test Activity **/
@Deprecated
public class TimeFreePubTest extends NearflyBindingActivity{
    private final String NEARFLY_CHANNEL = "sensors/humidity";
    private boolean neaflyServiceConnectCalled = false;
    CountDownLatch startSignal = new CountDownLatch(1);
    private TextView mDebugLogView;
    private final int MEDIA_REQUEST_CODE=15255;
    public final String TAG = "freePubTest";

    private int useTech = NearflyService.USE_NEARBY;
    private Logger mLogger = new Logger((
            ((useTech==NearflyService.USE_NEARBY)?"nearby":"mqtt"))+"_free_pubtest_");

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


    private String strPadding(String toPad, int width) {
        return new String(new char[width - toPad.length()]).replace('\0', '0') + toPad;
    }

    public void pubIt(View view){
        Log.v(TAG, "pubit: ");
        String msg = ""+System.currentTimeMillis()+":dkjfdsafhasfd";

        nearflyService.pubIt(NEARFLY_CHANNEL, strPadding(msg, 100));
    }

    public void openMediaTaker(View view) {
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

        nearflyService.pubFile(NEARFLY_CHANNEL, uri,
                System.currentTimeMillis()+":"+fileSize);
    }

    public long getSize(FileDescriptor fd) throws IOException {
        try (FileInputStream fis = new FileInputStream(fd)) {
            return fis.getChannel().size();
        }
    }

    public void toggleConnectionMode(View view){
        if (nearflyService.getConnectionMode()==nearflyService.USE_MQTT)
            nearflyService.switchConnectionMode(NearflyService.USE_NEARBY);
        else
            nearflyService.switchConnectionMode(NearflyService.USE_MQTT);
    }

    NearflyListener nearflyListener = new NearflyListener() {
        @Override
        public void onLogMessage(String state) {
            // Log.v("measureTest", output);
            switch (state){
                case NearflyService.State.CONNECTED:
                    Log.v(TAG, "Hello World!");
                    startSignal.countDown();
                    // OR
                    // nearflyService.pubIt(NEARFLY_CHANNEL, "Hello World!", -10, true);
                    break;
                case NearflyService.State.DISCONNECTED:
                    Log.v(TAG, "disconnected");
                    break;
            }
            // Log.v("measureTest", state);
        }

        @Override
        public void onMessage(String channel, String message) {
            // Calculate endtime as early as possible so that the result is as right as possible
            long endTime = System.currentTimeMillis();

            Log.v(TAG,channel + " " + message);
            long startTime = Long.valueOf(message.split(":", 2)[0]);
            long neededTime = endTime - startTime;

            runOnUiThread(() ->{
                mDebugLogView.append("byte-message of size: " + message.length() +"Bytes\n");
                mDebugLogView.append("time needed: " + neededTime +"ms\n\n");
                mLogger.log("time;"+neededTime+";bytes;"+message.length()+"\n");
            });
        }

        @Override
        public void onFile(String channel, String path, String textAttachment) {
            // Calculate endtime as early as possible so that the result is as right as possible
            long endTime = System.currentTimeMillis();

            Log.v(TAG,channel + " " + path);
            long startTime = Long.valueOf(textAttachment.split(":", 2)[0]);
            long neededTime = endTime - startTime;
            Long fileSize = Long.valueOf(textAttachment.split(":", 2)[1]);

            runOnUiThread(() ->{
                    mDebugLogView.append("file-message of size:" + fileSize+"Bytes\n");
                    mDebugLogView.append("time needed:" + neededTime +"ms\n\n");
                    mLogger.log("time;"+neededTime+";bytes;"+fileSize+"\n");
                });
        }

        @Override
        public void onBigBytes(String channel, byte[] bytes) {

        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.measure_testview);

        mDebugLogView = findViewById(R.id.debug_log);
    }
}
