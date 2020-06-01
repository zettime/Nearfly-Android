package de.pbma.nearflyexample.scenarios;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import java.util.Random;

import de.pbma.nearfly.NearflyClient;
import de.pbma.nearfly.NearflyListener;
import de.pbma.nearflyexample.R;

public class StandardWithClient extends AppCompatActivity {
    /** If true, debug logs are shown on the device. */
    private static final boolean DEBUG = true;

    private final String TAG = "testActivity";

    /** A running log of debug messages. Only visible when DEBUG=true. */
    private TextView mDebugLogView;
    private Integer cnt = 0;

    private final String NEARFLY_CHANNEL = "test/a";
    private Button mBtnToggleConMode;
    private NearflyClient mNearflyClient;


    NearflyListener nearflyListener = new NearflyListener() {
        @Override
        public void onLogMessage(String output) {
            runOnUiThread(() -> {
                mDebugLogView.append(output + "\n");
            });

            switch (output){
                case NearflyClient.State.CONNECTED:
                    int color = (mNearflyClient.getConnectionMode()== mNearflyClient.USE_MQTT)? R.color.state_connected: R.color.colorAccent;
                    runOnUiThread(() ->
                            mBtnToggleConMode.setBackgroundColor(ResourcesCompat.getColor(
                                    getResources(), color, null)));

                    break;
                case NearflyClient.State.DISCONNECTED:
                    if (!mNearflyClient.isModeSwitching()){
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

        }
    };

    public void publish(View view){
         // mNearflyClient.pubBigBytes(String.valueOf(++cnt).getBytes());
        // mNearflyClient.pubBigBytes(getRandomData(1_000_000).getBytes());
        mNearflyClient.pubIt(NEARFLY_CHANNEL, String.valueOf(++cnt) );
        // OR
        // mNearflyClient.pubIt(NEARFLY_CHANNEL, String.valueOf(++cnt), 0, false );
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
        mNearflyClient.connect("19moa18", mNearflyClient.getConnectionMode());
    }

    public void disconnect(View view){
        mNearflyClient.disconnect();
    }

    public void toggleConnectionMode(View view){
        if (mNearflyClient.getConnectionMode()== mNearflyClient.USE_MQTT)
            mNearflyClient.switchConnectionMode(mNearflyClient.USE_NEARBY);
        else
            mNearflyClient.switchConnectionMode(mNearflyClient.USE_MQTT);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBtnToggleConMode = findViewById(R.id.btn_toggle_conmode);

        mDebugLogView = findViewById(R.id.debug_log);
        mDebugLogView.setMovementMethod(new ScrollingMovementMethod());

        mNearflyClient = new NearflyClient(getApplicationContext());

        mNearflyClient.addSubCallback(nearflyListener);
        mNearflyClient.connect("19moa18", mNearflyClient.USE_NEARBY);
        mNearflyClient.subIt(NEARFLY_CHANNEL);
    }


    public void logIt(String str){
        // super.logIt(str);
        mDebugLogView.append(str + "\n");
    }
}