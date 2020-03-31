package de.pbma.nearflyexample.lala.scenarios;


import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

import de.pbma.nearfly.NearflyListener;
import de.pbma.nearflyexample.R;

public class TouchpointActivity extends NearflyBindingAktivity {
    /** If true, debug logs are shown on the device. */
    private static final boolean DEBUG = true;

    private final String TAG = "TouchpointActivity";

    /** Displays the current state. */
    private TextView tvCurrentState;

    private TextView tvRootNode;

    /** A running log of debug messages. Only visible when DEBUG=true. */
    private TextView mDebugLogView;
    private Integer cnt = 0;

    CustomView gameView;
    private Handler handler = new Handler();
    private final long FRAME_RATE = 30;


    @Override
    public void onNearflyServiceConnected() {
        nearflyService.addSubCallback(nearflyListener);
        nearflyService.subIt("19moa18/test");
    }

    @Override
    public void onNearflyServiceDisconnected() {
    }

    @Override
    public NearflyListener getNearflyListener() { return nearflyListener; }
    NearflyListener nearflyListener = new NearflyListener() {
        @Override
        public void onLogMessage(CharSequence msg) {
        }

        @Override
        public void onStateChanged(String state) {
        }

        @Override
        public void onRootNodeChanged(String rootNode) {
        }

        @Override
        public void onMessage(String message) {
            logIt(message);

            JSONObject msg = null;
            float tpX = gameView.RESET_X;
            float tpY = gameView.RESET_Y;
            int tpColor = Color.BLACK;

            try {
                msg = new JSONObject(new String(message));

                tpX = (float) msg.getDouble("xPos");
                tpY = (float) msg.getDouble("yPos");
                tpColor = msg.getInt("tpColor");
            } catch (JSONException e) {
                e.printStackTrace();
            }


            gameView.createTouchpoint(tpX, tpY, tpColor);
        }
    };

    public void publish(float tpX, float tpY, int tpColor){
        JSONObject msg = new JSONObject();
        try {

            msg.put("xPos", tpX);
            msg.put("yPos", tpY);
            msg.put("tpColor", tpColor);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        nearflyService.pubIt("19moa18/test", msg.toString());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        gameView = new CustomView(this, new CustomView.CustomViewListener() {
            @Override
            public void onAction(float tpX, float tpY, int tpColor) {
                publish(tpX, tpY, tpColor);
            }
        });
        setContentView(gameView);

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
