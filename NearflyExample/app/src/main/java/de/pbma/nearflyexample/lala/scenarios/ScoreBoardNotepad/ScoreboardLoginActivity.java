package de.pbma.nearflyexample.lala.scenarios.ScoreBoardNotepad;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import de.pbma.nearflyexample.R;
import de.pbma.nearflyexample.lala.scenarios.Messenger.MessengerActivity;

public class ScoreboardLoginActivity extends AppCompatActivity {
    private EditText mEditText;
    private Button mBtnConfirmation;
    private String mUsername;
    public final String MAIN_ACTIVITY_NAME = "ScoreboardActivity";

    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mSharedPrefEdit;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.scoreboard_login);
        mEditText = findViewById(R.id.et_username);
        mBtnConfirmation = findViewById(R.id.btn_confirmation);

        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }


            @Override
            public void afterTextChanged(Editable s) {
                String str = mEditText.getText().toString();
                if (str == null)
                    return;
                if (str.replace(" ", "").equals(""))
                    return;

                /** Get rid of all leadings and trailing spaces **/
                mUsername = str.trim();
            }
        });


        mBtnConfirmation.setOnClickListener((v) -> onClick());

        String tempUsername = getSavedUsername();
        if (tempUsername!=null)
            mEditText.setText(tempUsername);
    }

    private void onClick() {
        if (mUsername != null) {
            Intent returnIntent = new Intent();
            setResult(MessengerActivity.RESULT_OK, returnIntent);

            mSharedPrefEdit = mSharedPreferences.edit();
            mSharedPrefEdit.putString("username", mUsername);
            mSharedPrefEdit.commit();

            finish();
        }else{
            Toast.makeText(this, "You forgot to enter the username", Toast.LENGTH_LONG).show();
        }
    }

    public String getSavedUsername() {
        mSharedPreferences = getSharedPreferences(MAIN_ACTIVITY_NAME, Context.MODE_PRIVATE);
        return mSharedPreferences.getString("username", null);
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(getApplicationContext(), "Please choose a username",
                Toast.LENGTH_SHORT).show();
    }
}
