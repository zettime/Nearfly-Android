package de.pbma.nearflyexample.scenarios.ScoreBoardNotepad;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;

import de.pbma.nearfly.NearflyClient;
import de.pbma.nearfly.NearflyListener;
import de.pbma.nearfly.NearflyService;
import de.pbma.nearflyexample.R;
import de.pbma.nearfly.NearflyBindingActivity;
import de.pbma.nearflyexample.scenarios.BouncingBall.BouncingBallActivity;
import de.pbma.nearflyexample.scenarios.Messenger.MessengerActivity;

public class ScoreboardActivity extends NearflyBindingActivity {
    private final String TAG = getClass().getCanonicalName();

    /** Settings **/
    private static final int NEW_USERNAME_REQUEST_CODE = 18495;
    public final String ACTIVITY_NAME = "ScoreboardActivity";
    private SharedPreferences mSharedPreferences;

    /** Views **/
    // private TextView mScoreboard;
    private Button mBtnStartGame;
    private Button mBtnBackToLobby;
    private Button mBtnPlusOne;
    private Button mBtnPlusTwo;

    private TextView mPlayerNumber;
    private TextView mPlayerName;
    private TextView mPlayerTurns;
    private TextView mPlayerScore;

    /** Responsible for continual updating the Lobby**/
    private Thread mLobbyLooper;

    /** Player States **/
    private int mPlayerOnTurn = 1;
    private Player mMy = new Player("", 0);
    private LinkedList<Player> playerList = new LinkedList<>();

    /** Channels **/
    private final String DEFAULT_CHANNEL = "19moa18/measureTest/";
    private final String SEARCH_PLAYER_NUMBER = "searchPlayerNumber";
    private final String START_GAME = "startGame";
    private final String NEXT_PLAYER = "nextPlayer";
    private final String END_GAME = "endGame";
    private final String NEW_PLAYER_SCORE = "newPlayerScore";
    private final String[] USED_CHANNELS = {SEARCH_PLAYER_NUMBER, START_GAME, NEXT_PLAYER, NEW_PLAYER_SCORE, END_GAME};

    /** GameStates **/
    private String mCurrentGameState;
    private final String STATE_LOBBY = "stateLobby";
    private final String STATE_ON_TURN = "stateOnTurn";
    private final String STATE_SPECTATOR = "stateSpectator";
    private final String STATE_MANAGER = "stateManager";
    private final String STATE_GAME_END = "stateGameEnd";

    /** Avoid Subscirbe nearflyService over and over after connection to Service **/
    private boolean neaflyServiceConnectCalled = false;
    private CountDownLatch nearflyServiceStartedSignal = new CountDownLatch(1);

    @Override
    public void onNearflyServiceBound() {
        if (!neaflyServiceConnectCalled) {
            NearflyClient.askForPermissions(this, true);
            nearflyService.addSubCallback(nearflyListener);
            nearflyService.connect("19moa18/scorebaord", NearflyClient.USE_NEARBY);

            for (String str : USED_CHANNELS)
                nearflyService.subIt(DEFAULT_CHANNEL + str);

            neaflyServiceConnectCalled = true;
            nearflyServiceStartedSignal.countDown();
        }
    }

    @Override
    public void onNearflyServiceUnbound() {
    }

    NearflyListener nearflyListener = new NearflyListener() {
        @Override
        public void onLogMessage(String output) {
            //logIt(output);
        }

        @Override
        public void onMessage(String channel, String message) {
            // logIt("############################" + channel + " " + message);
            String[] channelSegmented = channel.split("/");
            String eventType = channelSegmented[channelSegmented.length - 1];

            switch (eventType) {
                case SEARCH_PLAYER_NUMBER:
                    onSearchPlayerNumber(message);
                    break;
                case START_GAME:
                    onStartGame();
                    break;
                case NEW_PLAYER_SCORE:
                    onNewPlayerScore(message);
                    break;
                case NEXT_PLAYER:
                    onNextPlayer(message);
                    break;
                case END_GAME:
                    onGameEnd();
                    break;
            }
        }

        @Override
        public void onFile(String channel, String path, String textAttachment) {
        }

        @Override
        public void onBigBytes(String channel, byte[] bytes) {

        }
    };

    /** For publishing current Username & entersTime **/
    private void onSearchPlayerNumber(String jsonStr) {
        if (!mCurrentGameState.equals(STATE_LOBBY))
            return;

        JSONObject msg = null;
        try {
            msg = new JSONObject(jsonStr);
        } catch (JSONException e) {
            e.printStackTrace(System.err);
        }

        try {
            long entersList = msg.getLong("entersTime");
            String username = msg.getString("username");

            Player tempPlayer = new Player(username, entersList);
            if (!playerList.contains(tempPlayer)) {
                playerList.add(tempPlayer);
                // If the player had previously pubed to early
                // pubSearchPlayerNumber();
            }
            playerList.get(playerList.indexOf(tempPlayer)).keepAliveCounter=4;

            logIt("" + playerList);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        refreshScoreBoard();
        changeState(STATE_LOBBY);
    }

    private void onStartGame() {
        if (!mCurrentGameState.equals(STATE_LOBBY))
            return;

        mPlayerOnTurn = 1;

        if (mMy.number == playerList.size()) {
            changeState(STATE_MANAGER);
        } else {
            changeState(STATE_SPECTATOR);
        }
    }

    private void onNewPlayerScore(String jsonStr) {
        if (mCurrentGameState.equals(STATE_LOBBY))
            return;

        JSONObject msg = null;
        try {
            msg = new JSONObject(jsonStr);
        } catch (JSONException e) {
            e.printStackTrace(System.err);
        }

        try {
            int playerNumber = msg.getInt("playerNumber");
            int playerTotalTurns = msg.getInt("PlayerTotalTurns");
            int playerScore = msg.getInt("playerScore");

            // Always work with absolute values
            Player playerOnTurn = playerList.get(playerNumber - 1);
            playerOnTurn.totalTurns = playerTotalTurns;
            playerOnTurn.score = playerScore;

        } catch (JSONException e) {
            e.printStackTrace();
        }

        refreshScoreBoard();
    }

    private void onNextPlayer(String playerOnTurn) {
        if (mCurrentGameState.equals(STATE_LOBBY))
            return;

        mPlayerOnTurn = Integer.valueOf(playerOnTurn);
        int calcOnTurn = mPlayerOnTurn-1;
        int playerBeforePlayerOnTurn = ((calcOnTurn +(playerList.size() - 1))%playerList.size())+1;

        logErr("playerBeforePlayerOnTurn: " + playerBeforePlayerOnTurn);

        if (mMy.number == mPlayerOnTurn)
            changeState(STATE_ON_TURN);
        else if (mMy.number == playerBeforePlayerOnTurn)
            changeState(STATE_MANAGER);
        else
            changeState(STATE_SPECTATOR);
    }

    private void onGameEnd() {
        if (mCurrentGameState.equals(STATE_LOBBY))
            return;

        changeState(STATE_GAME_END);
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scoreboard);

        // mScoreboard = findViewById(R.id.tv_scoreboard);

        mBtnStartGame = findViewById(R.id.btn_start_game);
        mBtnStartGame.setOnClickListener((v) -> pubStartGame());

        mBtnBackToLobby = findViewById(R.id.btn_back_to_lobby);
        mBtnBackToLobby.setOnClickListener((v) -> backToLobby());

        mBtnPlusOne = findViewById(R.id.btn_plus_one);
        mBtnPlusOne.setOnClickListener((v) -> pubPlayerOnTurnPlus(1));

        mBtnPlusTwo = findViewById(R.id.btn_plus_two);
        mBtnPlusTwo.setOnClickListener((v) -> pubPlayerOnTurnPlus(2));

        mPlayerNumber = findViewById(R.id.tv_playernumber);
        mPlayerName = findViewById(R.id.tv_playername);
        mPlayerTurns = findViewById(R.id.tv_player_totalturns);
        mPlayerScore = findViewById(R.id.tv_playerscore);

        mMy.entersTime = System.currentTimeMillis();
        mMy.username = getSavedUsername();
        if (mMy.username == null)
            openSettings();

        changeState(STATE_LOBBY);

        // Help to Fill list
        playerList.add(mMy);
        pubSearchPlayerNumber_afterConnected();
    }

    /** After the Game ends every Player can go to the lobby and clear Scoreboard**/
    private void backToLobby(){
        playerList.clear();
        mMy.resetPlayerStates();
        playerList.add(mMy);
        changeState(STATE_LOBBY);
        pubSearchPlayerNumber_afterConnected();
        refreshScoreBoard();
    }

    private void pubStartGame() {
        mLobbyLooper.interrupt();
        changeState(STATE_ON_TURN);
        nearflyService.pubIt(DEFAULT_CHANNEL + START_GAME, "", 0, true);
    }

    private void pubSearchPlayerNumber_afterConnected() {
        // Pub Player, if nearflyService connected
        mLobbyLooper = new Thread(() -> {
            try {
                nearflyServiceStartedSignal.await();
                while (!nearflyService.isConnected()) {
                    Thread.sleep(100);
                    // logErr("waiting for connection");
                }

                // logErr("Connections successfull");

                while (mCurrentGameState.equals(STATE_LOBBY)) {
                    Thread.sleep(2000);
                    pubSearchPlayerNumber();
                    for (int i=0; i<playerList.size(); i++){
                        if (playerList.get(i)==mMy)
                            continue;
                        if (playerList.get(i).keepAliveCounter--==0)
                            playerList.remove(i);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        mLobbyLooper.start();
    }

    private void pubSearchPlayerNumber() {
        JSONObject msg = new JSONObject();
        try {
            msg.put("entersTime", mMy.entersTime);
            msg.put("username", mMy.username);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        nearflyService.pubIt(DEFAULT_CHANNEL + SEARCH_PLAYER_NUMBER, msg.toString(), 0, true);

        refreshScoreBoard();
    }

    private void refreshScoreBoard() {
        String playerNumber = "\n";
        String playerName = "\n";
        String playerTurns = "\n";
        String playerScore = "\n";

        // Findout the right playerOrder
        playerList.sort((o1, o2) -> Long.valueOf(o1.entersTime).compareTo(o2.entersTime));
        int i = 0;
        for (Player player : playerList) {
            player.number = ++i;
            // scoreboard += "P" + player.number + " Score" + player.toString();
            playerNumber += "P"+player.number + "\n";
            playerName += player.username + "\n";
            playerTurns += player.totalTurns + "\n";
            playerScore += player.score + "\n";
        }

        // Change Scoreboard
        // final String fScorreboard = scoreboard;
        final String fPlayerNumber = playerNumber;
        final String fPlayerName = playerName;
        final String fPlayerTurns = playerTurns;
        final String fPlayerScore = playerScore;
        runOnUiThread(() -> {
            // mScoreboard.setText(fScorreboard);
            mPlayerNumber.setText(fPlayerNumber);
            mPlayerName.setText(fPlayerName);
            mPlayerTurns.setText(fPlayerTurns);
            mPlayerScore.setText(fPlayerScore);
        });
    }

    /** The Manager can give the main player 1 or 2 points per round**/
    private void pubPlayerOnTurnPlus(int val) {
        int calcOnTurn = mPlayerOnTurn-1;
        int playerBeforePlayerOnTurn = ((calcOnTurn +(playerList.size() - 1))%playerList.size())+1;
        if (mMy.number != playerBeforePlayerOnTurn) {
            logErr("That's not the turn of Player " + playerBeforePlayerOnTurn
                    + "rather " + mPlayerOnTurn);
            return;
        }

        Player playerOnTurn = playerList.get(mPlayerOnTurn - 1);
        playerOnTurn.totalTurns++;
        playerOnTurn.score += val;

        JSONObject msg = new JSONObject();
        try {
            msg.put("playerNumber", playerOnTurn.number);
            msg.put("PlayerTotalTurns", playerOnTurn.totalTurns);
            msg.put("playerScore", playerOnTurn.score);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        nearflyService.pubIt(DEFAULT_CHANNEL + NEW_PLAYER_SCORE, msg.toString(), 0, true);

        // Next Players Turn
        if (playerOnTurn.totalTurns % 10 == 0) {
            mPlayerOnTurn = ((mMy.number + 1) % (playerList.size()) + 1);

            if (mPlayerOnTurn != 1) {
                // changeState(STATE_SPECTATOR);
                onNextPlayer("" + mPlayerOnTurn);
                nearflyService.pubIt(DEFAULT_CHANNEL + NEXT_PLAYER, "" + mPlayerOnTurn, 0, true);
            } else {
                changeState(STATE_GAME_END);
                nearflyService.pubIt(DEFAULT_CHANNEL + END_GAME, "", 0, true);
            }
        }

        refreshScoreBoard();
    }

    public void logIt(String str) {
        Log.v(TAG, str);
    }

    public void logErr(String str) {
        Log.e(TAG, str);
    }


    // SETTINGS ----------------------
    public String getSavedUsername() {
        mSharedPreferences = getSharedPreferences(ACTIVITY_NAME, Context.MODE_PRIVATE);
        return mSharedPreferences.getString("username", null);
    }

    public void openSettings() {
        Intent i = new Intent(this, ScoreboardLoginActivity.class);
        startActivityForResult(i, NEW_USERNAME_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == NEW_USERNAME_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && resultData != null) {
                mMy.username = getSavedUsername();

            } else
                Toast.makeText(this, "You haven't entered a username jet", Toast.LENGTH_LONG).show();
        }
    }

    // STATE UI CHANGE ----------------------
    public void changeState(String gameState) {
        mCurrentGameState = gameState;

        switch (gameState) {
            case STATE_LOBBY:
                runOnUiThread(() -> {
                    mBtnStartGame.setVisibility((mMy.number == 1) ? View.VISIBLE : View.GONE);
                    mBtnPlusOne.setVisibility(View.GONE);
                    mBtnPlusTwo.setVisibility(View.GONE);
                    mBtnBackToLobby.setVisibility(View.GONE);
                });
                break;
            case STATE_MANAGER:
                runOnUiThread(() -> {
                    mBtnStartGame.setVisibility(View.GONE);
                    mBtnPlusOne.setVisibility(View.VISIBLE);
                    mBtnPlusTwo.setVisibility(View.VISIBLE);
                    mBtnBackToLobby.setVisibility(View.GONE);
                });
                break;
            case STATE_ON_TURN:
            case STATE_SPECTATOR:
                runOnUiThread(() -> {
                    mBtnStartGame.setVisibility(View.GONE);
                    mBtnPlusOne.setVisibility(View.GONE);
                    mBtnPlusTwo.setVisibility(View.GONE);
                    mBtnBackToLobby.setVisibility(View.GONE);
                });
                break;
            case STATE_GAME_END:
                runOnUiThread(() -> {
                    mBtnStartGame.setVisibility(View.GONE);
                    mBtnPlusOne.setVisibility(View.GONE);
                    mBtnPlusTwo.setVisibility(View.GONE);
                    mBtnBackToLobby.setVisibility(View.VISIBLE);
                });
                break;
        }
    }

    @Override
    public void onBackPressed() {
    }
}
