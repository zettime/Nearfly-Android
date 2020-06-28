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

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;

import de.pbma.nearfly.NearflyBindingActivity;
import de.pbma.nearfly.NearflyClient;
import de.pbma.nearfly.NearflyListener;
import de.pbma.nearfly.NearflyService;
import de.pbma.nearflyexample.R;

public class BouncingBallActivity extends NearflyBindingActivity {

    private static final String TAG = "dada";
    private final String NEARFLY_CHANNEL = "ball/";
    private boolean neaflyServiceConnectCalled = false;

    private final String ALIVE = "alive";
    private final String PLAY_DATA = "playData";
    private final String GAME_STATE = "gameState";
    private final String[] SUBCHANNELS = {ALIVE, PLAY_DATA, GAME_STATE};

    private Handler mHandler = new Handler();
    private GameView mGameView;
    private final int FPS = 25;
    private final int QOS_BUFFER = FPS;


    private TextView mSurviveTime;
    private LinearLayout mGameOverScreen;
    private TextView mTextViewScore;
    private Button mBtnToggleConMode;
    private String mPlayerId;
    private TextView mPlayerBoard;
    private volatile int mSyncCnt = 0;

    /** TeamMates with keepAlive time as value **/
    // ConcurrentHashMap<String, Integer> mTeamMates = new ConcurrentHashMap<>();
    class Playdata{
        double xAbs, yAbs, hOri, vOri;

        public Playdata(double xAbs, double yAbs, double hOri, double vOri) {
            this.xAbs = xAbs;
            this.yAbs = yAbs;
            this.hOri = hOri;
            this.vOri = vOri;
        }
    }


    class TeamMate{
        public String id;
        public int cnt;
        /*public BlockingQueue<Double> xAbs = new LinkedBlockingDeque<>();
        public BlockingQueue<Double> yAbs = new LinkedBlockingDeque<>();
        public BlockingQueue<Double> hOri = new LinkedBlockingDeque<>();
        public BlockingQueue<Double> vOri = new LinkedBlockingDeque<>();*/
        public BlockingQueue<Playdata> playdata = new LinkedBlockingDeque<>();

        public TeamMate(String id, int cnt){
            this.id = id;
            this.cnt = cnt;
        }

        @Override
        public int hashCode() {
            return Integer.parseInt(id);
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

    // List<TeamMate> mTeamMates = Collections.synchronizedList(new ArrayList<>());
    ConcurrentHashMap<String, TeamMate> mTeamMates = new ConcurrentHashMap<>();

    @Override
    public void onNearflyServiceBound() {
        if (!neaflyServiceConnectCalled) {
            NearflyClient.askForPermissions(this, false);
            nearflyService.addSubCallback(nearflyListener);
            nearflyService.connect("19moa18", NearflyClient.USE_MQTT);
            for (String subChannel : SUBCHANNELS)
                nearflyService.subIt(NEARFLY_CHANNEL+subChannel);
            neaflyServiceConnectCalled = true;
        }
    }

    @Override
    public void onNearflyServiceUnbound() {
    }

    public void toggleConnectionMode(View view){
        if (nearflyService.getConnectionMode()==NearflyClient.USE_MQTT)
            nearflyService.switchConnectionMode(NearflyClient.USE_NEARBY);
        else
            nearflyService.switchConnectionMode(NearflyClient.USE_MQTT);
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
            logIt("message received");
            try {
                switch (channel) {
                    case NEARFLY_CHANNEL + PLAY_DATA:
                        JSONObject json = new JSONObject(message);
                        String teammateId = json.getString("teammateId");
                        Double xAbsolute = json.getDouble("xAbsolute");
                        Double yAbsolute = json.getDouble("yAbsolute");
                        Double hOrientation = json.getDouble("hOrientation");
                        Double vOrientation = json.getDouble("vOrientation");

                        TeamMate teamMate = mTeamMates.get(teammateId);
                        if (teamMate == null) {
                            Log.v(TAG, "unknown object for v and h Orientation");
                            // teamMate = new TeamMate(message, 4);
                            // mTeamMates.put(teammateId, teamMate);
                        }else{
                            /*teamMate.xAbs.add(xAbsolute);
                            teamMate.yAbs.add(yAbsolute);
                            teamMate.hOri.add(hOrientation);
                            teamMate.vOri.add(vOrientation);*/

                            Playdata playdata = new Playdata(xAbsolute, yAbsolute, hOrientation, vOrientation);
                            teamMate.playdata.add(playdata);
                        }

                        // mGameView.addValToPosition(vOrientation, hOrientation);
                        break;
                    case NEARFLY_CHANNEL+ALIVE:
                        if (!mTeamMates.containsKey(message)){
                            TeamMate teamMate2 = new TeamMate(message, 4);
                            mTeamMates.put(message, teamMate2);
                        }else{
                            mTeamMates.get(message).cnt = 4;
                        }
                        /*int index = mTeamMates.indexOf(teamMate);
                        if (index==-1)
                            mTeamMates.add(teamMate);
                        else{
                            mTeamMates.get(index).cnt = 4;
                        }*/
                        break;
                    case NEARFLY_CHANNEL+GAME_STATE:
                        if (message.equals("gameOver")){
                            runOnUiThread(() -> mGameView.changeState(GameView.STATE_GAMEOVER, true));
                            mTeamMates.forEach((v, tmpTeamMate) -> {
                                /*tmpTeamMate.xAbs.clear();
                                tmpTeamMate.yAbs.clear();
                                tmpTeamMate.hOri.clear();
                                tmpTeamMate.vOri.clear();*/
                                tmpTeamMate.playdata.clear();
                            });

                        }else if (message.equals("startGame")){
                            runOnUiThread(() -> mGameView.changeState(GameView.STATE_PLAYING, true));
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
        mTeamMates.put(mPlayerId, myself);
        // Log.v("dada", mTeamMates.toString());

        mGameView = findViewById(R.id.game_view);
        mPlayerBoard = findViewById(R.id.playerboard);

        mGameView.onCreate(getApplicationContext());
        mGameView.registerListener(new GameView.GameViewListener() {
            @Override
            public void onStateChanged(int state, boolean onMessage) {
                switch (state){
                    case GameView.STATE_GAMEOVER:
                        mGameOverScreen.setVisibility(View.VISIBLE);
                        mSurviveTime.setText(""+mGameView.getScore());

                        // Delete all values
                        mTeamMates.forEach((k, teamMate) -> {
                            /*teamMate.xAbs.clear();
                            teamMate.yAbs.clear();
                            teamMate.hOri.clear();
                            teamMate.vOri.clear();*/
                            teamMate.playdata.clear();
                        });


                        if (onMessage==false)
                            nearflyService.pubIt(NEARFLY_CHANNEL+GAME_STATE,"gameOver");
                        break;
                    case GameView.STATE_PLAYING:
                        mGameOverScreen.setVisibility(View.GONE);
                        if (onMessage==false)
                            nearflyService.pubIt(NEARFLY_CHANNEL+GAME_STATE,"startGame");
                        break;
                }
            }

            @Override
            public void onStep(double xAbsolute, double yAbsolute, double hOrientation, double vOrientation) {
                if (nearflyService==null)
                    return;

                // Log.v(TAG, mTeamMates.toString());
                /*mTeamMates.forEach((k, v) ->{
                    Log.v(TAG, k+ " " + v );
                });*/


                TeamMate myself = mTeamMates.get(mPlayerId);
                // if (myself.xAbs.size()<QOS_BUFFER){
                if (myself.playdata.size()<QOS_BUFFER){
                    /*myself.xAbs.add(xAbsolute);
                    myself.yAbs.add(yAbsolute);
                    myself.hOri.add(hOrientation);
                    myself.vOri.add(vOrientation);*/
                    Playdata playdata = new Playdata(xAbsolute, yAbsolute, hOrientation, vOrientation);
                    myself.playdata.add(playdata);

                    try {
                        JSONObject json = new JSONObject();
                        // json.put("syncCnt", ++mSyncCnt);
                        json.put("teammateId", mPlayerId);
                        json.put("xAbsolute", xAbsolute);
                        json.put("yAbsolute", yAbsolute);
                        json.put("hOrientation", hOrientation);
                        json.put("vOrientation", vOrientation);
                        nearflyService.pubIt(NEARFLY_CHANNEL+PLAY_DATA, json.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
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
                String[] str = new String[2]; // Seconds place just fun
                str[0] = "";
                // AtomicReference<String> fStr = new AtomicReference<>(str);

                /*for (int i=0; i<mTeamMates.size(); i++){
                    TeamMate teamMate = mTeamMates.get(i);

                    if (!teamMate.id.equals(mPlayerId)) // If not myself
                        teamMate.cnt-=1;

                    if (teamMate.cnt==0) {
                        mTeamMates.remove(teamMate); // Kick player out
                    }else{
                        str+=teamMate.id + "\n";
                    }
                }*/
                mTeamMates.forEach((k, teamMate) ->{
                    if (!k.equals(mPlayerId) && --teamMate.cnt<0){
                        try{
                            /*teamMate.xAbs.add(-100d);  // set Marker
                            teamMate.yAbs.add(-100d);  // set Marker
                            teamMate.vOri.add(-100d);  // set Marker
                            teamMate.hOri.add(-100d);  // set Marker*/
                            Playdata playdata = new Playdata(-100, -100, -100, -100);
                            teamMate.playdata.add(playdata);
                        }catch (NullPointerException e){
                        }
                    }else{
                        str[0] += k + '\n';
                        // fStr.updateAndGet((a) -> a+k+'\n');
                    }
                });

                runOnUiThread(() -> mPlayerBoard.setText(str[0]));
            }
        }).start();

        class Sum{
            Double sumX = 0d;
            Double sumY = 0d;
            Double sumHOri = 0d;
            Double sumVOri = 0d;
        }

        // Queue Draining
        new Thread(() -> {
            Sum sum= new Sum();

            while(!Thread.interrupted()){
                mTeamMates.forEach((k, teamMate) ->{
                    try {
                        /*Double sumX = teamMate.xAbs.take();
                        Double sumY = teamMate.yAbs.take();
                        Double sumHOri = teamMate.hOri.take();
                        Double sumVOri = teamMate.vOri.take();*/
                        Playdata playdata = teamMate.playdata.take();
                        if (playdata.xAbs==-100d){
                            /*teamMate.xAbs = null;
                            teamMate.yAbs = null;
                            teamMate.hOri = null;
                            teamMate.vOri = null;*/
                            teamMate.playdata = null;
                            mTeamMates.remove(k);
                            runOnUiThread(() -> mGameView.changeState(GameView.STATE_GAMEOVER, false));
                        }else{
                            //if (!teamMate.id.equals(mPlayerId)){
                            /*sum.sumX += sumX;
                            sum.sumY += sumY;
                            sum.sumHOri += sumHOri;
                            sum.sumVOri += sumVOri;*/
                            sum.sumX += playdata.xAbs;
                            sum.sumY += playdata.yAbs;
                            sum.sumHOri += playdata.hOri;
                            sum.sumVOri += playdata.vOri;
                            //}
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (NullPointerException e){
                        e.printStackTrace();
                    }
                });

                Log.v(TAG, sum+"");
                /*for (int i=0; i<mTeamMates.size(); i++) {
                    TeamMate teamMate = mTeamMates.get(i);

                    sumV += teamMate.xAbs.take();
                    sumH += teamMate.yAbs.take();
                }*/

                final int size = mTeamMates.size();
                double xMean = sum.sumX/size;
                double yMean = sum.sumY/size;
                double hOriMean = sum.sumHOri/size;
                double vOriMean = sum.sumVOri/size;

                double xAbs = xMean+hOriMean;
                double yAbs = yMean-vOriMean;

                // mGameView.addValToPosition(sum.sumV, sum.sumH);
                mGameView.setBallPosition(xAbs, yAbs, size);
                sum.sumX = 0d;
                sum.sumY = 0d;
                sum.sumHOri = 0d;
                sum.sumVOri = 0d;
            }
        }).start();
    }
}