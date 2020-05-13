package de.pbma.nearfly;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

// import de.pbma.nearfly.NearflyListener;
/**
 * Helps the integration of {@link NearflyService}, manages the binding of the
 * {@link NearflyService} and queries the necessary permissions at the beginning of the
 * activity if necessary.
 * <p></p>
 * <h2>Attention</h2>
 * The necessary permissions must also be entered in the AndroidManifest
 * <pre>
 *     {@code
 *        <manifest xmlns:android="http://schemas.android.com/apk/res/android">
 *
 *        <!-- Rquired for Nearby Connections -->
 *        <uses-permission android:name="android.permission.BLUETOOTH" />
 *        <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
 *        <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
 *        <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
 *        <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
 *        <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
 *
 *        <!-- Required for MQTT -->
 *        <uses-permission android:name="android.permission.INTERNET" />
 *
 *        <!-- Optional: Only required if File transfer API used -->
 *        <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE " />
 *        <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
 *        <uses-permission android:name="android.permission.ACCESS_MEDIA_LOCATION" />
 *        <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
 *
 *        <application
 *            <activity
 *            <!-- .... -->
 *            </activity>
 *
 *            <service
 *                android:name="de.pbma.nearfly.NearflyService"
 *                android:enabled="true" />
 *        </application>
 *    </manifest>
 *
 *
 *     }
 * </pre>
 */
public abstract class NearflyBindingActivity extends AppCompatActivity {
    /** If true, debug logs are shown on the device. */
    private static final boolean DEBUG = true;

    private final String TAG = "NearflyBindingActivity";

    public NearflyService nearflyService;
    private boolean nearflyServiceBound;
    private boolean mNearflyServiceStarted = false;

    /** Try to force the dev to override the nearflyListener **/
    /*private NearflyListener nearflyListener;
    public abstract NearflyListener getNearflyListener();*/


    public abstract void onNearflyServiceBound();
    public abstract void onNearflyServiceUnbound();

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            logIt("onServiceConnected");
            nearflyService = ((NearflyService.LocalBinder) service).getNearflyService();
            onNearflyServiceBound();
            nearflyServiceBound = true;

            if (!mNearflyServiceStarted)
                startNearflyService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            logIt("onServiceDisconnected");
            onNearflyServiceUnbound();
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
