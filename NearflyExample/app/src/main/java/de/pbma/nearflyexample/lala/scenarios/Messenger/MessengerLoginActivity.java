package de.pbma.nearflyexample.lala.scenarios.Messenger;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import de.pbma.nearflyexample.R;

public class MessengerLoginActivity extends AppCompatActivity {
    private EditText mEditText;
    private EditText mEditRoom;
    private Button mBtnConfirmation;
    private String mUsername;
    private String mRoom;
    public final String MAIN_ACTIVITY_NAME = "MessengerActivity";

    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mSharedPrefEdit;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.messenger_login);
        mEditText = findViewById(R.id.et_username);
        mEditRoom = findViewById(R.id.et_room);
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

        mEditRoom.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }


            @Override
            public void afterTextChanged(Editable s) {
                String str = mEditRoom.getText().toString();
                if (str == null)
                    return;
                if (str.replace(" ", "").equals(""))
                    return;

                /** Get rid of all leadings and trailing spaces **/
                mRoom = str.trim();
            }
        });

        mBtnConfirmation.setOnClickListener((v) -> onClick());

        String tempUsername = getSavedUsername();
        String tempRoom = getSavedRoom();
        if (tempUsername!=null)
            mEditText.setText(tempUsername);

        if (tempRoom!=null)
            mEditRoom.setText(tempRoom);
    }

    private void onClick() {
        if (mUsername != null && mRoom!=null) {
            Intent returnIntent = new Intent();
            returnIntent.putExtra(MAIN_ACTIVITY_NAME + ".newUsername", mUsername);
            returnIntent.putExtra(MAIN_ACTIVITY_NAME + ".newRoom", mRoom);
            setResult(MessengerActivity.RESULT_OK, returnIntent);

            // Save Data
            mSharedPrefEdit = mSharedPreferences.edit();
            mSharedPrefEdit.putString("username", mUsername);
            mSharedPrefEdit.putString("room", mRoom);
            mSharedPrefEdit.commit();

            finish();
        }
    }

    public String getSavedUsername() {
        mSharedPreferences = getSharedPreferences(MAIN_ACTIVITY_NAME, Context.MODE_PRIVATE);
        return mSharedPreferences.getString("username", null);
    }

    public String getSavedRoom() {
        mSharedPreferences = getSharedPreferences(MAIN_ACTIVITY_NAME, Context.MODE_PRIVATE);
        return mSharedPreferences.getString("room", null);
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(getApplicationContext(), "Please choose a username",
                Toast.LENGTH_SHORT).show();
    }
}
