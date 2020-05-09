package de.pbma.nearflyexample.lala.scenarios.Touchpoint;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.widget.RadioButton;

import androidx.annotation.Nullable;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

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

    @Override
    public void onNearflyServiceBound() {
        nearflyService.addSubCallback(nearflyListener);
        nearflyService.subIt("19moa18/test");
        nearflyService.connect("19moa18", NearflyService.USE_NEARBY);
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

        nearflyService.pubIt("19moa18/test", msg.toString());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.touchpoint);
        btnUColor = findViewById(R.id.btn_ucolor);

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
