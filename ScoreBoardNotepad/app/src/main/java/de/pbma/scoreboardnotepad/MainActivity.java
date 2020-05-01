package de.pbma.scoreboardnotepad;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;

import de.pbma.nearfly.NearflyListener;
import de.pbma.nearfly.NearflyService;


public class MainActivity extends NearflyBindingAktivity {
    private final String TAG = getClass().getCanonicalName();

    // Player Distinction
    private int mPlayerToken;
    private int mManagerToken;
    private int mSpectatorToken;

    private long mEntersTime;
    private String mUsername;
    private CountDownLatch nearflyServiceStartedSignal = new CountDownLatch(1);

    private final String DEFAULT_CHANNEL = "19moa18/test/";

    private LinkedList<Player> playerList = new LinkedList<>();


    // Nearfly Specific
    private boolean neaflyServiceConnectCalled = false;

    @Override
    public void onNearflyServiceConnected() {
        if (!neaflyServiceConnectCalled) {
            nearflyService.addSubCallback(nearflyListener);
            nearflyService.connect("ScoreBoardNotepad", NearflyService.USE_NEARBY);
            nearflyService.subIt(DEFAULT_CHANNEL);
            neaflyServiceConnectCalled = true;

            nearflyServiceStartedSignal.countDown();
        }
    }

    @Override
    public void onNearflyServiceDisconnected() {
    }

    NearflyListener nearflyListener = new NearflyListener() {
        @Override
        public void onLogMessage(String output) {
            logIt(output);
        }

        @Override
        public void onMessage(String channel, String message) {
            String[] channelSegmented = channel.split("/");
            String eventType = channelSegmented[channelSegmented.length-1];

            switch (eventType){
                case "searchPlayerNumber": onSearchPlayerNumber(message); break;
                // case "searchPlayerNumber": onSearchPlayerNumber(); break;
            }
        }

        @Override
        public void onFile(String path, String textAttachment) {}
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mEntersTime = System.currentTimeMillis();
        mUsername = "Hans";

        // Pub Player, if nearflyService connected
        new Thread(() -> {
            try {
                nearflyServiceStartedSignal.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            pubSearchPlayerNumber();
        }).start();
        // pubOwnPlayerNumber();

    }


    private void pubSearchPlayerNumber(){
        JSONObject msg = new JSONObject();
        try {
            msg.put("entersTime", mEntersTime);
            msg.put("username", mUsername);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        nearflyService.pubIt(DEFAULT_CHANNEL+"searchPlayerNumber", msg.toString());
    }

    private void onSearchPlayerNumber(String jsonStr){
        JSONObject msg = null;
        try {
            msg = new JSONObject(jsonStr);
        } catch (JSONException e) {
            e.printStackTrace(System.err);
        }

        try {
            long entersList = msg.getLong("entersTime");
            String username = msg.getString("username");

            playerList.add(new Player(username, entersList));
            logIt("playerList: " + playerList);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void logIt(String str){
        Log.v(TAG, str);
    }
}
