package de.pbma.nearflyexample.scenarios.BouncingBall;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

import de.pbma.nearfly.NearflyBindingActivity;
import de.pbma.nearfly.NearflyListener;
import de.pbma.nearfly.NearflyService;
import de.pbma.nearflyexample.R;

public class BouncingBallActivity extends NearflyBindingActivity {

    private final String NEARFLY_CHANNEL = "ball";
    private boolean neaflyServiceConnectCalled = false;

    private Handler mHandler = new Handler();
    private GameView mGameView;
    private final int FPS = 25;

    private TextView mSurviveTime;
    private LinearLayout mGameOverScreen;
    private TextView mTextViewScore;
    private Button mBtnToggleConMode;

    @Override
    public void onNearflyServiceBound() {
        if (!neaflyServiceConnectCalled) {
            nearflyService.askForPermissions(this, false);
            nearflyService.addSubCallback(nearflyListener);
            nearflyService.connect("19moa18", NearflyService.USE_NEARBY);
            nearflyService.subIt(NEARFLY_CHANNEL);
            neaflyServiceConnectCalled = true;
        }
    }

    @Override
    public void onNearflyServiceUnbound() {
    }

    public void toggleConnectionMode(View view){
        if (nearflyService.getConnectionMode()==nearflyService.USE_MQTT)
            nearflyService.switchConnectionMode(NearflyService.USE_NEARBY);
        else
            nearflyService.switchConnectionMode(NearflyService.USE_MQTT);
    }

    NearflyListener nearflyListener = new NearflyListener() {
        @Override
        public void onLogMessage(String output) {
            switch (output){
                case NearflyService.State.CONNECTED:
                    int color = (nearflyService.getConnectionMode()==nearflyService.USE_MQTT)? R.color.state_connected: R.color.colorAccent;
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
            logIt("message received");
            try {
                JSONObject json = new JSONObject(message);
                float vOrientation = (float) json.getDouble("vOrientation");
                float hOrientation = (float) json.getDouble("hOrientation");

                mGameView.addValToPosition(vOrientation, hOrientation);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFile(String channel, String path, String textAttachment) {
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.bouncingball);
        mSurviveTime = findViewById(R.id.survie_time);
        mGameOverScreen = findViewById(R.id.gameover_screen);
        mTextViewScore = findViewById(R.id.tv_score);
        mBtnToggleConMode = findViewById(R.id.btn_toggle_conmode);

        mGameView = findViewById(R.id.game_view);
        mGameView.onCreate(getApplicationContext());
        mGameView.registerListener(new GameView.GameViewListener() {
            @Override
            public void onStateChanged(int state) {
                switch (state){
                    case GameView.STATE_GAMEOVER:
                        mGameOverScreen.setVisibility(View.VISIBLE);
                        mSurviveTime.setText(""+mGameView.getScore());
                        break;
                    case GameView.STATE_PLAYING:
                        mGameOverScreen.setVisibility(View.GONE);
                        break;
                }
            }

            @Override
            public void onStep(float vOrientation, float hOrientation) {
                if (nearflyService==null)
                    return;

                try {
                    JSONObject json = new JSONObject();
                    json.put("vOrientation", vOrientation);
                    json.put("hOrientation", hOrientation);
                    nearflyService.pubIt(NEARFLY_CHANNEL, json.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        /** Creates the Gameloop that is updated every {@link #FRAME_RATE} seconds **/
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                mHandler.post(() -> {
                        if (mGameOverScreen.getVisibility()==View.GONE)
                            mTextViewScore.setText(""+mGameView.getScore());
                        mGameView.invalidate();
                    });
                }
        }, 0, 1000/FPS);
    }
}
