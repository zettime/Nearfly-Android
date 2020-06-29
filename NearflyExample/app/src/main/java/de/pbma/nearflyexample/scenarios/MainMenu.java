package de.pbma.nearflyexample.scenarios;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import de.pbma.nearflyexample.R;
import de.pbma.nearflyexample.scenarios.BouncingBall.BouncingBallActivity;
import de.pbma.nearflyexample.scenarios.Messenger.MessengerActivity;
import de.pbma.nearflyexample.scenarios.ScoreBoardNotepad.ScoreboardActivity;

public class MainMenu extends AppCompatActivity {

    Button mBtn1, mBtn2, mBtn3;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.mainmenu);

        mBtn1 = findViewById(R.id.button1);
        mBtn1.setOnClickListener(v ->
            startActivity(new Intent(this, BouncingBallActivity.class)));

        mBtn2 = findViewById(R.id.button2);
        mBtn2.setOnClickListener(v ->
                startActivity(new Intent(this, MessengerActivity.class)));

        mBtn3 = findViewById(R.id.button3);
        mBtn3.setOnClickListener(v ->
                startActivity(new Intent(this, ScoreboardActivity.class)));
    }
}
