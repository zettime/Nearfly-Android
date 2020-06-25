package de.pbma.moa.nearflydemo;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Date;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    final static String TAG = MainActivity.class.getCanonicalName();


    private TextView tvType;
    private TextView tvLog;
    private TextView tvMessage;

    private LocalService.NFMessageListener nfMessageListener = new LocalService.NFMessageListener() {
        @Override
        public void onMessage(String channel, String message) {
            Log.v(TAG, "onMessage: channel=" + channel + ", message=" + message);
            runOnUiThread(() -> tvMessage.setText(channel + ": " + message));
        }
    };

    private boolean localServiceBound = false;
    private LocalService localService;
    private ServiceConnection localServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.v(TAG, "onServiceConnected");
            localService = ((LocalService.LocalBinder) service).getLocalService();
            updateType();
            localService.registerNFMessageListener(nfMessageListener);
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.v(TAG, "onServiceDisconnected");
            doUnbindService();
        }
    };


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate");
        setContentView(R.layout.main);
        tvLog = findViewById(R.id.tv_log);
        tvType = findViewById(R.id.tv_type);
        tvMessage = findViewById(R.id.tv_type);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v(TAG, "onResume");
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService();
    }

    public void startService() {
        Log.v(TAG, "startService");
        Intent intent = new Intent(this, LocalService.class);
        intent.setAction(LocalService.CMD_START);
        startService(intent);
    }

    public void stopService() {
        Log.v(TAG, "stopService");
        Intent intent = new Intent(this, LocalService.class);
        intent.setAction(LocalService.CMD_STOP);
        stopService(intent);
    }

    public void bindService() {
        Log.v(TAG, "bindService");
        Intent intent = new Intent(this, LocalService.class);
        localServiceBound = bindService(intent, localServiceConnection, Context.BIND_AUTO_CREATE);
        if (!localServiceBound) {
            Log.w(TAG, "could not try to bind service, will not be bound");
        }
    }

    private void unbindService() {
        Log.v(TAG, "unbindService");
        if (localServiceBound) {
            Log.v(TAG, "unbindService: do it");
            unbindService(localServiceConnection);
            doUnbindService();
        }
    }

    private void doUnbindService() {
        if (localService != null) {
            localService.deregisterNFMessageListener(nfMessageListener);
        }
        localServiceBound = false;
        localService = null;
    }


    public void connect() {
        Log.v(TAG, "connect");
        if (localService == null) {
            Log.w(TAG, "connect: not bound");
            return;
        }
        localService.connect(this);
    }

    final static int REQUEST_CODE_REQUIRED_PERMISSIONS = 18504;
    private void requestPermissions() {
        String[] REQUIRED_PERMISSIONS = new String[] {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
        };
        requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
    }
    @Override public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_REQUIRED_PERMISSIONS) {
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(this, R.string.error_missing_permissions, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            }
            recreate();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void addLog(String msg) {
        runOnUiThread(() -> {
            tvLog.append("\n" + msg);
        });
    }

    public void onConnect(View view) {
        Log.v(TAG, "onConnect");
        addLog("onConnect");
        connect();
    }

    private static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.contains("SDK")) { // Emulator
            return String.format("emu%5d", new Random().nextInt(100000));
        }
        if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
            return lowerTrim(model);
        } else {
            return lowerTrim(manufacturer)+lowerTrim(model);
        }
    }

    private static String lowerTrim(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Character ch : s.toCharArray()) {
            ch = Character.toLowerCase(ch);
            if (Character.isAlphabetic(ch) || Character.isDigit(ch)) {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    public void onStartService(View view) {
        Log.v(TAG, "onStartService");
        startService();
    }

    public void onStopService(View view) {
        Log.v(TAG, "onStopService");
        stopService();
    }

    public void onToggle(View view) {
        Log.v(TAG, "onToggle");
        if (localService == null) {
            Log.w(TAG, "onToogle, not yet connected");
            return;
        }
        localService.nextConnectionMode();
        updateType();
    }

    private void updateType() {
        if (localService != null) {
            tvType.setText(localService.getConnectionModeString());
        } else {
            Log.w(TAG, "not yet connected");
        }
    }

    public void onInfo(View view) {
        Log.v(TAG, "onInfo");
        if (localService == null) {
            Log.v(TAG, "service not yet connected");
            return;
        }
        addLog("connectionMode: " + localService.getConnectionMode() + ", " + localService.getConnectionModeString());
    }

    public void onSend(View view) {
        Log.v(TAG, "onSend");
        if (localService == null) {
            addLog("onSend, but not yet connected to service");
            return;
        }
        Date date = new java.util.Date();
        String msg = getDeviceName() + ": " + date.toString();
        addLog("onSend: pubit, " + msg);
        localService.publish(LocalService.CHANNEL_DEFAULT, msg);
    }

    public void onDisconnect(View view) {
        Log.v(TAG, "onDisconnect");
        addLog("onDisconnect");
    }

    public void onQuit(View view) {
        Log.v(TAG, "onQuit");
        finish();
    }

}
