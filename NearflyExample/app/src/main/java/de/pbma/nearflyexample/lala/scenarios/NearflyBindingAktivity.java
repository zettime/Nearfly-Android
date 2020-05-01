package de.pbma.nearflyexample.lala.scenarios;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import de.pbma.nearfly.ConnectionsActivityWithPermissions;
// import de.pbma.nearfly.NearflyListener;
import de.pbma.nearfly.NearflyService;

public abstract class NearflyBindingAktivity extends ConnectionsActivityWithPermissions {
    /** If true, debug logs are shown on the device. */
    private static final boolean DEBUG = true;

    private final String TAG = "NearflyBindingAktivity";


    public NearflyService nearflyService;
    private boolean nearflyServiceBound;
    private boolean mNearflyServiceStarted = false;

    /** Try to force the dev to override the nearflyListener **/
    /*private NearflyListener nearflyListener;
    public abstract NearflyListener getNearflyListener();*/


    public abstract void onNearflyServiceConnected();
    public abstract void onNearflyServiceDisconnected();

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            logIt("onServiceConnected");
            nearflyService = ((NearflyService.LocalBinder) service).getNearflyService();
            onNearflyServiceConnected();
            nearflyServiceBound = true;

            if (!mNearflyServiceStarted)
                startNearflyService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            logIt("onServiceDisconnected");
            onNearflyServiceDisconnected();
            nearflyServiceBound = false;
            unbindNearflyService();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        nearflyServiceBound = false;
    }

    @Override
    protected void onStop() {
        unbindNearflyService();
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!nearflyServiceBound)
            bindNearflyService();
    }

    protected void startNearflyService(){
        logIt("onStartService");
        Intent intent = new Intent(this, NearflyService.class);
        intent.setAction(NearflyService.ACTION_START);
        startService(intent);
        mNearflyServiceStarted = true;
    }


    public void stopNearflyService() {
        logIt( "onStopService");
        Intent intent = new Intent(this, NearflyService.class);
        intent.setAction(NearflyService.ACTION_STOP);
        startService(intent); // to stop
        mNearflyServiceStarted = false;
    }

    private void bindNearflyService() {
        logIt("bindMQTTService");
        Intent intent = new Intent(this, NearflyService.class);
        intent.setAction(NearflyService.ACTION_BIND);
        nearflyServiceBound = bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        if (!nearflyServiceBound) {
            logIt("could not try to bind service, will not be bound");
        }
    }

    private void unbindNearflyService() {
        if (nearflyServiceBound){
            unbindService(serviceConnection);
            nearflyServiceBound = false;
        }
    }

    public void logIt(String str){
        Log.v(TAG, str);
        // mDebugLogView.append(str + "\n");
    }

    /*public void changeTech(View view){
        if (nearflyService.USED_TECH==NearflyService.USE_MQTT)
            nearflyService.changeTech(NearflyService.USE_NEARBY);
        else
            nearflyService.changeTech(NearflyService.USE_MQTT);
    }*/
}
