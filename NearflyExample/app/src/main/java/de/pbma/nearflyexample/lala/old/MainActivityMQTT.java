package de.pbma.nearflyexample.lala.old;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import de.pbma.nearflyexample.R;

public class MainActivityMQTT extends AppCompatActivity {
//    final static String TAG = MainActivityMQTT.class.getCanonicalName();
//
//    // the button was pressed, but the service not yet connected
//    private boolean pressOnHold;
//
//    private Integer cnt = 0;
//
//    private MQTTService mqttService;
//    private boolean mqttServiceBound;
//
//    private String EXAMPLE_TOPIC1 = "19moa18/test1";
//    private String EXAMPLE_TOPIC2 = "19moa18/test2";
//
//    // Handler handler = new Handler();
//    MyMqttListener myMqttListener = new MyMqttListener() {
//        @Override
//        public void onMessage(final String id, final String text) {
//            Log.v(TAG, "  MQTTService receives: " + id + " " + text);
//        }
//        @Override
//        public void onStatus(final boolean connected) {
//            Log.v(TAG, connected==true?"connected":"disconnected");
//            // tvOutput.append(connected==true?"connected":"disconnected");
//
//            // TODO
//            if (connected==true){
//                mqttService.subscribe(EXAMPLE_TOPIC1);
//                mqttService.subscribe(EXAMPLE_TOPIC2);
//                Log.v(TAG, "subscribed from Activity");
//            }
//        }
//
//        @Override
//        public void onLogMessage(final String message) {
//            // handler.post(() -> logView.addLine(message));
//            // Log.v("stdout", "got Msg");
//        }
//
//        @Override
//        public void onFile(String path, String textAttachment) {
//
//        }
//    };
//
//    private ServiceConnection serviceConnection = new ServiceConnection() {
//        @Override
//        public void onServiceConnected(ComponentName name, IBinder service) {
//            Log.v(TAG, "onServiceConnected");
//            mqttService = ((MQTTService.LocalBinder) service).getMQTTService();
//            // register listeners
//            mqttService.registerMqttListener(myMqttListener);
//            mqttServiceBound = true;
//
//            // if a press is on hold
//            if (pressOnHold) {
//                pressOnHold = false;
//                mqttService.publishIt(EXAMPLE_TOPIC1,"onServiceConnected");
//            }
//        }
//        @Override
//        public void onServiceDisconnected(ComponentName name) {
//            // unintentionally disconnected
//            Log.v(TAG, "onServiceDisconnected");
//            mqttServiceBound = false;
//            unbindMQTTService(); // cleanup
//        }
//    };
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.game);
//
//        myMqttListener.onStatus(false);
//        pressOnHold = false;
//
//        mqttServiceBound = false;
//    }
//
//
//    @Override
//    protected void onResume() {
//        Log.v(TAG, "onResume");
//        super.onResume();;
//    }
//
//    @Override
//    protected void onStart() {
//        Log.v(TAG, "onStart");
//        if (!mqttServiceBound)
//            bindMQTTService();
//        super.onStart();
//    }
//
//    @Override
//    protected void onPause() {
//        Log.v(TAG, "onPause");
//        super.onPause();
//        unbindMQTTService();
//    }
//
//    public void onStartService(View view) {
//        // Starte den LocalService
//        Log.v(TAG, "onStartService");
//        Intent intent = new Intent(this, MQTTService.class);
//        intent.setAction(MQTTService.ACTION_START);
//        startService(intent);
//    }
//
//    public void onStopService(View view) {
//        Log.v(TAG, "onStopService");
//        Intent intent = new Intent(this, MQTTService.class);
//        intent.setAction(MQTTService.ACTION_STOP);
//        startService(intent); // to stop
//
//        // TODO
//        mqttService.unsubscribe(EXAMPLE_TOPIC1);
//        mqttService.unsubscribe(EXAMPLE_TOPIC2);
//    }
//
//    public void onPress(View view) {
//        if (!mqttServiceBound) {
//            Log.e(TAG, "ignore press request, if not tried to bind");
//            return;
//        }
//        if (mqttService == null) {
//            Log.w(TAG, "tried to bind, but not yet successful, save for later");
//            pressOnHold = true;
//            return;
//        }
//
//        // Senden der MQTT-Nachricht
//        mqttService.publishIt("19moa18/test2", String.valueOf(++cnt));
//    }
//
//    private void bindMQTTService() {
//        Log.v(TAG, "bindMQTTService");
//        Intent intent = new Intent(this, MQTTService.class);
//        intent.setAction(MQTTService.ACTION_PRESS);
//        mqttServiceBound = bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
//
//        if (!mqttServiceBound) {
//            Log.w(TAG, "could not try to bind service, will not be bound");
//        }
//    }
//
//    private void unbindMQTTService() {
//        if (mqttServiceBound) {
//            if (mqttService != null) {
//                // deregister listeners, if there are any
//                mqttService.deregisterMqttListener(myMqttListener);
//            }
//            mqttServiceBound = false;
//            unbindService(serviceConnection);
//        }
//    }

}
