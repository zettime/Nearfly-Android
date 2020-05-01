package de.pbma.paperthrow;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;


class MainActivity2 extends NearflyBindingAktivity {
//    private final String TAG = getClass().getCanonicalName();
//
//    // Player Distinction
//    private int mPlayerToken;
//    private int mManagerToken;
//    private int mSpectatorToken;
//
//    private long mEntersTime;
//    private String mUsername;
//
//    private final String DEFAULT_CHANNEL = "19moa18/test/";
//
//
//    // Nearfly Specific
//    private boolean neaflyServiceConnectCalled = false;
//
//    @Override
//    public void onNearflyServiceConnected() {
//        if (!neaflyServiceConnectCalled) {
//            nearflyService.addSubCallback(nearflyListener);
//            nearflyService.connect("PAPER", NearflyService.USE_NEARBY);
//            nearflyService.subIt(DEFAULT_CHANNEL);
//            neaflyServiceConnectCalled = true;
//        }
//    }
//
//    @Override
//    public void onNearflyServiceDisconnected() {
//    }
//
//    NearflyListener nearflyListener = new NearflyListener() {
//        @Override
//        public void onLogMessage(CharSequence output) {
//        }
//
//        @Override
//        public void onMessage(String channel, String message) {
//            String[] channelSegmented = channel.split("/");
//            String eventType = channelSegmented[channelSegmented.length-1];
//
//            switch (eventType){
//                case "searchPlayerNumber":
//                    logIt("kak");
//                    break;
//            }
//        }
//
//        @Override
//        public void onFile(String path, String textAttachment) {}
//    };
//
//    @Override
//    protected void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.main);
//
//        mEntersTime = System.currentTimeMillis();
//        mUsername = "Hans";
//
//        // pubSearchPlayerNumber();
//        // pubOwnPlayerNumber();
//
//    }
//
//
//    public void pubSearchPlayerNumber(){
//        JSONObject msg = new JSONObject();
//        try {
//            msg.put("entersTime", mEntersTime);
//            msg.put("username", mUsername);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//
//        nearflyService.pubIt(DEFAULT_CHANNEL+"searchPlayerNumber", msg.toString());
//    }
//
//    public void logIt(String str){
//        Log.v(TAG, str);
//    }
}
