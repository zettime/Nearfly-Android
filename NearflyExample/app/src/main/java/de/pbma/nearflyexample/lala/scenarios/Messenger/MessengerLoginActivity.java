package de.pbma.nearflyexample.lala.scenarios.Messenger;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import de.pbma.nearflyexample.R;

public class MessengerLoginActivity extends AppCompatActivity {
    private EditText mEditText;
    private Button mBtnConfirmation;
    private String mUsername;
    public final String MAIN_ACTIVITY_NAME = "MessengerActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.messenger_login);
        mEditText = findViewById(R.id.et_sername);
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
    }

    private void onClick() {
        if (mUsername != null) {
            Intent returnIntent = new Intent();
            returnIntent.putExtra(MAIN_ACTIVITY_NAME + ".newUsername", mUsername);
            setResult(MessengerActivity.RESULT_OK, returnIntent);
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(getApplicationContext(), "Please choose a username",
                Toast.LENGTH_SHORT).show();
    }
}
