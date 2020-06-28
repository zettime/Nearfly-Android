package de.pbma.nearflyexample.scenarios.Touchpoint;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

import de.pbma.nearfly.NearflyClient;
import de.pbma.nearfly.NearflyListener;
import de.pbma.nearfly.NearflyService;
import de.pbma.nearflyexample.R;
import de.pbma.nearfly.NearflyBindingActivity;

public class TouchpointActivity extends NearflyBindingActivity {
    /** If true, debug logs are shown on the device. */
    private static final boolean DEBUG = true;

    private final String TAG = "TouchpointActivity";
    RadioButton btnUColor;

    /** components used for the gameloop **/
    CustomView gameView;
    private Handler handler = new Handler();
    private final long FRAME_RATE = 30;

    private boolean neaflyServiceConnectCalled = false;
    private String NEARFLY_CHANNEL = "/touchpoint";
    private Button mBtnToggleConMode;

    @Override
    public void onNearflyServiceBound() {
        if (!neaflyServiceConnectCalled) {
            nearflyService.askForPermissions(this, false);
            nearflyService.addSubCallback(nearflyListener);
            nearflyService.connect("19moa18", NearflyClient.USE_MQTT);
            nearflyService.subIt(NEARFLY_CHANNEL);
            neaflyServiceConnectCalled = true;
        }
    }

    public void toggleConnectionMode(View view){
        if (nearflyService.getConnectionMode()==NearflyClient.USE_MQTT)
            nearflyService.switchConnectionMode(NearflyClient.USE_NEARBY);
        else
            nearflyService.switchConnectionMode(NearflyClient.USE_MQTT);
    }

    @Override
    public void onNearflyServiceUnbound() {
    }

    NearflyListener nearflyListener = new NearflyListener() {
        @Override
        public void onLogMessage(String output) {
            switch (output){
                case NearflyService.State.CONNECTED:
                    int color = (nearflyService.getConnectionMode()==NearflyClient.USE_MQTT)? R.color.state_connected: R.color.colorAccent;
                    runOnUiThread(() ->
                            mBtnToggleConMode.setBackgroundColor(ResourcesCompat.getColor(
                                    getResources(), color, null)));

                    break;
                case NearflyService.State.DISCONNECTED:
                    runOnUiThread(() ->
                    mBtnToggleConMode.setBackgroundColor(ResourcesCompat.getColor(
                            getResources(), R.color.gray, null)));
                    break;
            }
        }

        @Override
        public void onMessage(String channel, String message) {
            logIt(message);

            JSONObject msg = null;
            float percTpX = gameView.RESET_X;
            float percTpY = gameView.RESET_Y;
            int tpColorIndex = Color.BLACK;

            try {
                msg = new JSONObject(new String(message));

                percTpX = (float) msg.getDouble("xPos");
                percTpY = (float) msg.getDouble("yPos");
                tpColorIndex = msg.getInt("tpColorIndex");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            gameView.createTouchpoint(percTpX, percTpY, tpColorIndex);
        }

        @Override
        public void onFile(String channel, String path, String textAttachment){}

        @Override
        public void onBigBytes(String channel, byte[] bytes) {

        }
    };

    public void publish(float percTpX, float percTpY, int tpColorIndex){
        JSONObject msg = new JSONObject();
        try {
            msg.put("xPos", percTpX);
            msg.put("yPos", percTpY);
            msg.put("tpColorIndex", tpColorIndex);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        nearflyService.pubIt(NEARFLY_CHANNEL, msg.toString());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.touchpoint);
        btnUColor = findViewById(R.id.btn_ucolor);

        mBtnToggleConMode = findViewById(R.id.btn_toggle_conmode);

        /** Listener for the {@linkplain CustomView} **/
        gameView = findViewById(R.id.custom_view);
        gameView.registerListener((percTpX, percTpY, tpColorIndex) -> {
                publish(percTpX, percTpY, tpColorIndex);
            });
        btnUColor.setButtonTintList(ColorStateList.valueOf(gameView.getMyColor()));

        /** Creates the Gameloop that is updated every {@link #FRAME_RATE} seconds **/
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        gameView.invalidate();
                    }
                });
            }
        }, 0, FRAME_RATE);
    }

    public void logIt(String str){
        super.logIt(str);
    }
}
