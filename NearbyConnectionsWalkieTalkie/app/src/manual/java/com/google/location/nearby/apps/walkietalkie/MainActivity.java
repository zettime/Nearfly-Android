package com.google.location.nearby.apps.walkietalkie;

import android.animation.Animator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.Strategy;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Collection;
import java.util.Random;

public class MainActivity extends ConnectionsActivityWithPermissions {
    NearflyService nearflyService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        nearflyService = new NearflyService();
        nearflyService.onCreate(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        nearflyService.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        nearflyService.onStop();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        nearflyService.onBackPressed();
    }
}
