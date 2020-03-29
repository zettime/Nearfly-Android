package de.pbma.nearflyexample.lala.old;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;

import de.pbma.notused.nearbymessanger.ExtMessage;
import de.pbma.notused.nearbymessanger.NearbyService;
import de.pbma.nearflyexample.R;


public class MainActivityNearby extends AppCompatActivity {
    private NearbyService nearbyService;
    private boolean nearflyServiceBound = false;


    String TAG = "stdout";
    TextView tvOutput;
    /********************************************/
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            nearbyService = ((NearbyService.LocalBinder) service).getNearflyService();
            nearflyServiceBound = true;
            Log.v(TAG, "Service successfully bounded");

            // Create myService
            nearbyService.initService(MainActivityNearby.this, new MessageListener() {
                @Override
                public void onFound(Message message) {
                    ExtMessage extMessage = ExtMessage.createExtMessage(message);
                    tvOutput.append("Found message: " + extMessage.getChannel() + " " + extMessage.getPayload() + "\n");
                }

                @Override
                public void onLost(Message message) {
                    ExtMessage extMessage = ExtMessage.createExtMessage(message);
                    tvOutput.append("Lost sight of message: " + new String(message.getContent()) + "\n");
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            nearflyServiceBound = false;
            unbindNearflyService();
        }
    };
    /**********************/
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        tvOutput = findViewById(R.id.tv_output);

        Intent intent = new Intent(this, NearbyService.class);
        intent.setAction(NearbyService.USE_NEARBY);
    }

    public void pubIt(View v){
        if (nearbyService !=null)
            nearbyService.pubIt();
        else
            Log.d(TAG, "not bound");
    }
    public void unpubIt(View v){
    }

    public void subIt(View v){
        if (nearbyService !=null)
            nearbyService.subIt();
        // Nearby.getMessagesClient(this).subscribe(mMessageListener);
    }
    public void unsubIt(View v){
        if (nearbyService !=null)
            nearbyService.unsubIt();
        // Nearby.getMessagesClient(this).unsubscribe(mMessageListener);
    }
    /*private void publish() {
        // Log.i(TAG, "Publishing message: " + message);
        // mActiveMessage = new Message(message.getBytes());
        mActiveMessage = new Message("Hallo".getBytes());
        Nearby.getMessagesClient(this).publish(mActiveMessage);
    }*/


    /***********************************************/
    @Override
    public void onStart() {
        super.onStart();
        bindNearflyService();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // unbindNearflyService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // unbindNearflyService();
    }

    private void bindNearflyService(){
        Intent intent = new Intent(this, NearbyService.class);
        intent.setAction(NearbyService.USE_NEARBY);
        nearflyServiceBound = bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        if (!nearflyServiceBound) {
            Log.w(TAG, "could not try to bind service, will not be bound");
        }
    }

    private void unbindNearflyService() {
        nearflyServiceBound = false;
        unbindService(serviceConnection);
    }
}
