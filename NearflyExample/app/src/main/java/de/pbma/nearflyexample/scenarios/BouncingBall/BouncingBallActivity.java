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
import org.w3c.dom.Text;

import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;

import de.pbma.nearfly.NearflyBindingActivity;
import de.pbma.nearfly.NearflyListener;
import de.pbma.nearfly.NearflyService;
import de.pbma.nearflyexample.R;

public class BouncingBallActivity extends NearflyBindingActivity {

    private final String NEARFLY_CHANNEL = "ball/";
    private boolean neaflyServiceConnectCalled = false;

    private final String ALIVE = "alive";
    private final String PLAY_DATA = "playData";
    private final String GAME_STATE = "gameState";
    private final String[] SUBCHANNELS = {ALIVE, PLAY_DATA, GAME_STATE};

    private Handler mHandler = new Handler();
    private GameView mGameView;
    private final int FPS = 25;

    private TextView mSurviveTime;
    private LinearLayout mGameOverScreen;
    private TextView mTextViewScore;
    private Button mBtnToggleConMode;
    private String mPlayerId;
    private TextView mPlayerBoard;

    /** TeamMates with keepAlive time as value **/
    // ConcurrentHashMap<String, Integer> mTeamMates = new ConcurrentHashMap<>();
    class TeamMate{
        public String id;
        public int cnt;

        public TeamMate(String id, int cnt){
            this.id = id;
            this.cnt = cnt;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof TeamMate){
                TeamMate other = (TeamMate) obj;
                return id.equals(other.id);
            }
            return false;
        }
    }

    List<TeamMate> mTeamMates = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void onNearflyServiceBound() {
        if (!neaflyServiceConnectCalled) {
            nearflyService.askForPermissions(this, false);
            nearflyService.addSubCallback(nearflyListener);
            nearflyService.connect("19moa18", NearflyService.USE_NEARBY);
            for (String subChannel : SUBCHANNELS)
                nearflyService.subIt(NEARFLY_CHANNEL+subChannel);
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
                switch (channel){
                    case NEARFLY_CHANNEL+PLAY_DATA:
                        JSONObject json = new JSONObject(message);
                        float vOrientation = (float) json.getDouble("vOrientation");
                        float hOrientation = (float) json.getDouble("hOrientation");

                        mGameView.addValToPosition(vOrientation, hOrientation);
                        break;
                    case NEARFLY_CHANNEL+ALIVE:
                        TeamMate teamMate = new TeamMate(message, 4);
                        int index = mTeamMates.indexOf(teamMate);
                        if (index==-1)
                            mTeamMates.add(teamMate);
                        else{
                            mTeamMates.get(index).cnt = 4;
                        }
                        break;
                    case NEARFLY_CHANNEL+GAME_STATE:
                        if (message.equals("gameOver")){
                            runOnUiThread(() -> mGameView.changeState(GameView.STATE_GAMEOVER));
                        }else if (message.equals("startGame")){
                            runOnUiThread(() -> mGameView.changeState(GameView.STATE_PLAYING));
                        }
                        break;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFile(String channel, String path, String textAttachment) {
        }

        @Override
        public void onBigBytes(String channel, byte[] bytes) {

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

        mPlayerId = ""+new Random().nextInt(99_999);
        TeamMate myself = new TeamMate(mPlayerId, 4);
        mTeamMates.add(myself);

        mGameView = findViewById(R.id.game_view);
        mPlayerBoard = findViewById(R.id.playerboard);

        mGameView.onCreate(getApplicationContext());
        mGameView.registerListener(new GameView.GameViewListener() {
            @Override
            public void onStateChanged(int state) {
                switch (state){
                    case GameView.STATE_GAMEOVER:
                        mGameOverScreen.setVisibility(View.VISIBLE);
                        mSurviveTime.setText(""+mGameView.getScore());
                        nearflyService.pubIt(NEARFLY_CHANNEL+GAME_STATE,"gameOver");
                        break;
                    case GameView.STATE_PLAYING:
                        mGameOverScreen.setVisibility(View.GONE);
                        nearflyService.pubIt(NEARFLY_CHANNEL+GAME_STATE,"startGame");
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
                    nearflyService.pubIt(NEARFLY_CHANNEL+PLAY_DATA, json.toString());
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



        /** Keep sending singal, to note  that you're participating **/
        new Thread(() -> {
            while(!Thread.interrupted()){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                nearflyService.pubIt(NEARFLY_CHANNEL+ALIVE, mPlayerId);
                String str = "";
                for (int i=0; i<mTeamMates.size(); i++){
                    TeamMate teamMate = mTeamMates.get(i);

                    if (!teamMate.id.equals(mPlayerId)) // If not myself
                        teamMate.cnt-=1;

                    if (teamMate.cnt==0) {
                        mTeamMates.remove(teamMate); // Kick player out
                    }else{
                        str+=teamMate.id + "\n";
                    }
                }

                final String fStr = str;
                runOnUiThread(() -> mPlayerBoard.setText(fStr));
            }
        }).start();
    }
}
