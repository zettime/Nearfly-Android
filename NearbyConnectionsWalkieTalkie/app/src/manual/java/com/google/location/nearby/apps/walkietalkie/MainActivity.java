package de.pmba.nearflyexample.nearbyconnections;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import android.text.format.DateFormat;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

import de.pbma.nearbyconnections.ConnectionsActivityWithPermissions;
import de.pbma.nearbyconnections.MyNearbyConnectionsClient;
import de.pbma.nearflyexample.R;

public class MainActivity extends ConnectionsActivityWithPermissions {
    MyNearbyConnectionsClient myNearbyConnectionsClient;

    /** If true, debug logs are shown on the device. */
    private static final boolean DEBUG = true;

    /** Displays the current state. */
    private TextView tvCurrentState;

    private TextView tvRootNode;

    /** A running log of debug messages. Only visible when DEBUG=true. */
    private TextView mDebugLogView;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myNearbyConnectionsClient = new MyNearbyConnectionsClient();

        mDebugLogView = findViewById(R.id.debug_log);
        mDebugLogView.setVisibility(DEBUG ? View.VISIBLE : View.GONE);
        mDebugLogView.setMovementMethod(new ScrollingMovementMethod());

        mDebugLogView.setText("test2 \n");
        mDebugLogView.append("test2 \n");

        /*getSupportActionBar()
                .setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.actionBar));*/

        tvCurrentState = findViewById(R.id.tv_current_state);
        tvRootNode = findViewById(R.id.tv_root_node);


        myNearbyConnectionsClient.onCreate(this, new MyNearbyConnectionsClient.MyConnectionsListener(){

            @Override
            public void onLogMessage(CharSequence msg) {
                mDebugLogView.append("\n");
                mDebugLogView.append(DateFormat.format("hh:mm", System.currentTimeMillis()) + ": ");
                mDebugLogView.append(msg);
            }

            @Override
            public void onStateChanged(String state) {
                tvCurrentState.setText(state);
            }

            @Override
            public void onRootNodeChanged(String rootNode) {
                tvRootNode.setText(rootNode);
            }

            @Override
            public void onMessage(String msg) { }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        myNearbyConnectionsClient.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        myNearbyConnectionsClient.onStop();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        myNearbyConnectionsClient.onBackPressed();
    }


    public int cnt = 0;
    public void publish(View view){
        myNearbyConnectionsClient.pubIt(String.valueOf(++cnt), "test");
    }
}