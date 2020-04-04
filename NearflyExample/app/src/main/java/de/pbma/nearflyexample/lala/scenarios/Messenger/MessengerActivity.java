package de.pbma.nearflyexample.lala.scenarios.Messenger;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

import de.pbma.nearfly.NearflyListener;
import de.pbma.nearflyexample.R;
import de.pbma.nearflyexample.lala.scenarios.NearflyBindingAktivity;
import de.pbma.nearflyexample.lala.scenarios.Touchpoint.CustomView;

public class MessengerActivity extends NearflyBindingAktivity {
    /** If true, debug logs are shown on the device. */
    private static final boolean DEBUG = true;

    private final String TAG = "MessengerActivity";

    /** components used for the gameloop **/
    private TextView mTextView;
    private Button mBtnSendTxt;
    private Button mBtnSendImg;
    private ImageView mImageView;

    private String TEXT = "text";
    private String BINARY = "binary";

    @Override
    public void onNearflyServiceConnected() {
        nearflyService.addSubCallback(nearflyListener);
        nearflyService.subIt("19moa18/test");
    }

    @Override
    public void onNearflyServiceDisconnected() {
    }

    @Override
    public void onBackPressed() {
    }



    @Override
    public NearflyListener getNearflyListener() { return nearflyListener; }
    NearflyListener nearflyListener = new NearflyListener() {
        @Override
        public void onLogMessage(CharSequence msg) {
        }

        @Override
        public void onStateChanged(String state) {
        }

        @Override
        public void onRootNodeChanged(String rootNode) {
        }

        @Override
        public void onMessage(String strJson) {
            logIt(strJson);

            JSONObject jObj = null;
            String username = "";
            int userColorIndex = 1;
            String msgType = "";
            String msg = "";

            try {
                jObj= new JSONObject(new String(strJson));

                username = jObj.getString("username");
                userColorIndex = jObj.getInt("userColorIndex");
                msgType = jObj.getString("msgType");
                msg = jObj.getString("message");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            logIt(msgType);

            if (msgType.equals(BINARY)) {
                /*byte[] bytes = android.util.Base64.decode(msg,
                        android.util.Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);*/
                Bitmap bitmap = base64ToBitmap(msg);
                runOnUiThread(() -> {
                    mImageView.setImageBitmap(bitmap);
                });
            }else{
                final String mustBeFinal = msg;
                runOnUiThread(() -> {
                    mTextView.append('\n' + mustBeFinal);
                });
            }
        }
    };

    public void sendMessage(String username, int userColorIndex, String message){
        JSONObject msg = new JSONObject();
        try {
            msg.put("username", username);
            msg.put("userColorIndex", userColorIndex);
            msg.put("msgType", "text");
            msg.put("message", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        nearflyService.pubIt("19moa18/test", msg.toString());
    }

    public void sendImage(String username, int userColorIndex, String message){
        // String encodedBinary = new String(Base64.encode(binary, Base64.DEFAULT));

        JSONObject msg = new JSONObject();
        try {
            msg.put("username", username);
            msg.put("userColorIndex", userColorIndex);
            msg.put("msgType", BINARY);
            msg.put("message", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        nearflyService.pubIt("19moa18/test", msg.toString());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.messenger);

        mTextView = findViewById(R.id.textview);
        mTextView.setMovementMethod(new ScrollingMovementMethod());
        mTextView.setText("dsfsdaaf\n}sdfdsfasdffsfda\nsdfadsfdasf");

        mBtnSendTxt = findViewById(R.id.btn_send_text);
        mBtnSendTxt.setOnClickListener((v) -> {
            sendMessage("watson", 1, "hello");
        });

        mBtnSendImg = findViewById(R.id.btn_send_image);
        mBtnSendImg.setOnClickListener((v) -> openPhotoTaker());

        mImageView = findViewById(R.id.imageview);

        /** Listener for the {@linkplain CustomView} **/
        /*gameView = new CustomView(this, new CustomView.CustomViewListener() {
            @Override
            public void sendTouchpoint(float percTpX, float percTpY, int tpColorIndex) {
                publish(percTpX, percTpY, tpColorIndex);
            }
        });
        setContentView(gameView);*/

        /** Creates the Gameloop that is updated every {@link #FRAME_RATE} seconds **/
        /*Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        gameView.invalidate();
                    }
                });
            }
        }, 0, FRAME_RATE);*/
    }



    public void openPhotoTaker(){Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, 234);
    }


    @Override
    protected void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);


        if (resultCode == RESULT_OK) {
            try {
                /*final Uri imageUri = data.getData();
                final InputStream imageStream = getContentResolver().openInputStream(imageUri);
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                mImageView.setImageBitmap(selectedImage);*/

                final Uri imageUri = data.getData();
                final InputStream imageStream = getContentResolver().openFileDescriptor()
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                mImageView.setImageBitmap(selectedImage);


                sendImage("jiren", 1, bitmapToBase64(selectedImage));

            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
            }

        }else {
            Toast.makeText(this, "You haven't picked Image",Toast.LENGTH_LONG).show();
        }
    }


    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream .toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private Bitmap base64ToBitmap(String b64) {
        byte[] imageAsBytes = Base64.decode(b64.getBytes(), Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length);
    }

    public void logIt(String str){
        super.logIt(str);
    }
}
