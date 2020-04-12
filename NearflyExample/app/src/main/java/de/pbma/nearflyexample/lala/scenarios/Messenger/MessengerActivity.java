package de.pbma.nearflyexample.lala.scenarios.Messenger;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.renderscript.ScriptGroup;
import android.text.method.ScrollingMovementMethod;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.gms.common.util.IOUtils;
import com.google.android.gms.nearby.connection.Connections;
import com.google.android.gms.nearby.connection.Payload;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.pbma.nearfly.ExtMessage;
import de.pbma.nearfly.NearflyListener;
import de.pbma.nearflyexample.R;
import de.pbma.nearflyexample.lala.scenarios.NearflyBindingAktivity;
import de.pbma.nearflyexample.lala.scenarios.Touchpoint.CustomView;

public class MessengerActivity extends NearflyBindingAktivity {
    /**
     * If true, debug logs are shown on the device.
     */
    private static final boolean DEBUG = true;

    private final String TAG = "MessengerActivity";

    private static final int READ_REQUEST_CODE = 42;

    final int HEADER_SIZE = 30;
    final int MAX_CHUNK_SIZE = 500_000 - HEADER_SIZE;


    /**
     * components used for the gameloop
     **/
    private TextView mTextView;
    private Button mBtnSendTxt;
    private Button mBtnSendImg;
    private ImageView mImageView;

    private final String PUB_CHANNEL = "19moa18/test";

    private String TEXT = "text";
    private String BINARY = "binary";

    @Override
    public void onNearflyServiceConnected() {
        nearflyService.addSubCallback(nearflyListener);
        nearflyService.subIt(PUB_CHANNEL);
    }

    @Override
    public void onNearflyServiceDisconnected() {
    }

    @Override
    public void onBackPressed() {
    }


    @Override
    public NearflyListener getNearflyListener() {
        return nearflyListener;
    }

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
        public void onMessage(String string) {
            //onMessageForBase64(strJson);
            // OR
            //onMessageForBinary(strJson);
            // OR

            // Create new image View from Layout
            LinearLayout layout = (LinearLayout) findViewById(R.id.message_history);
            TextView textView= (TextView ) LayoutInflater.from(getApplicationContext()).inflate(R.layout.messenger_text, null);
            layout.addView(textView);
            textView.setText(string);
        }

        @Override
        public void onStream(Payload payload) {
            try {
                byte[] bytes = readBytes(payload.asStream().asInputStream());
                onMessageForBinary(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
            /*final InputStream imageStream = payload.asStream().asInputStream();

            final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            selectedImage.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            mImageView.setImageBitmap(selectedImage);

            try {
                imageStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }*/
        }

        @Override
        public void onBinary(Payload payload) {
            onMessageForBinary(payload.asBytes());
        }

        @Override
        public void onFile(String path){
            File file = new File(path);

            // Create new image View from Layout
            LinearLayout layout = (LinearLayout) findViewById(R.id.message_history);
            ImageView imageView = (ImageView) LayoutInflater.from(getApplicationContext()).inflate(R.layout.messenger_image, null);
            layout.addView(imageView);

            // add Image to LinearLayout
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), bmOptions);
            imageView.setImageBitmap(bitmap);
        }
    };

    private void onMessageForBase64(String strJson){
        logIt(strJson);

        JSONObject jObj = null;
        String username = "";
        int userColorIndex = 1;
        String msgType = "";
        String msg = "";

        try {
            jObj = new JSONObject(new String(strJson));

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
        } else {
            final String mustBeFinal = msg;
            runOnUiThread(() -> {
                mTextView.append('\n' + mustBeFinal);
            });
        }
    }

    // ArrayList<Byte> incomingBinary = new ArrayList<>();
    // byte[] incomingBinary = new byte[0];
    LinkedList<Byte> incomingBinary = new LinkedList<>();
    HashMap<String, Integer> headerMap;

    private void onMessageForBinary(byte[] payload){ /** {@link #pubChunkedBinary(Intent )} **/
        String[] header = new String(Arrays.copyOfRange(payload, 0, HEADER_SIZE)).replace(" ", "").split("\n");
        headerMap = new HashMap<>();

        for (String str : header){
            String[] attr = str.split(":");
            headerMap.put(attr[0], Integer.valueOf(attr[1]));
        }

        int seqNr = headerMap.get("sqn");
        int maxSqnNr= headerMap.get("max");
        int nice= headerMap.get("nice");
        byte[] body = Arrays.copyOfRange(payload, HEADER_SIZE, payload.length);

        logIt(headerMap.toString());

        for (int i=HEADER_SIZE; i<payload.length; i++){
            incomingBinary.add(payload[i]);
        }
        // incomingBinary = new byte[maxSqnNr*MAX_CHUNK_SIZE];
        // Copy body to incoming Binary
        // System.arraycopy(body, 0, incomingBinary, seqNr*MAX_CHUNK_SIZE, body.length);

        if (seqNr==maxSqnNr-1){

            byte[] bytes = new byte[incomingBinary.size()];
            for (int i=0; i<incomingBinary.size(); i++){
                bytes[i] = incomingBinary.get(i);
            }
            Bitmap image = BitmapFactory.decodeByteArray(bytes, 0, incomingBinary.size());

            mImageView.setImageBitmap(image);
            // incomingBinary = new byte[0];
            incomingBinary.clear();
        }
    }

    /*private static final Pattern p = Pattern.compile("^([a-zA-Z]+)([0-9]+)(.*)");
    public static void main(String[] args) {
        // create matcher for pattern p and given string
        Matcher m = p.matcher("Testing123Testing");

         // if an occurrence if a pattern was found in a given string...
        if (m.find()) {
            // ...then you can use group() methods.
            System.out.println(m.group(0)); // whole matched expression
            System.out.println(m.group(1)); // first expression from round brackets (Testing)
            System.out.println(m.group(2)); // second one (123)
            System.out.println(m.group(3)); // third one (Testing)
        }
    }*/


    public void sendMessage(String username, int userColorIndex, String message) {
        JSONObject msg = new JSONObject();
        try {
            msg.put("username", username);
            msg.put("username", username);
            msg.put("userColorIndex", userColorIndex);
            msg.put("msgType", "text");
            msg.put("message", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        nearflyService.pubIt(PUB_CHANNEL, msg.toString());
    }

    public void sendImage(String username, int userColorIndex, String message) {
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

        nearflyService.pubIt(PUB_CHANNEL, msg.toString());
    }

    public void sendBinary(ParcelFileDescriptor pfd) {
        // String encodedBinary = new String(Base64.encode(binary, Base64.DEFAULT));

        /*JSONObject msg = new JSONObject();
        try {
            msg.put("username", username);
            msg.put("userColorIndex", userColorIndex);
            msg.put("msgType", BINARY);
            msg.put("message", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }*/

        // nearflyService.pubBinary("19moa18/test", pfd);
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
        mBtnSendImg.setOnClickListener((v) -> openDocumentTaker());

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


    public void openPhotoTaker() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, READ_REQUEST_CODE);
    }

    /** Not used yet **/
    private void openDocumentTaker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == READ_REQUEST_CODE
                && resultCode == Activity.RESULT_OK
                && resultData != null) {

            // 2 Möglichkeiten
            // pubStreamOnResult(resultData);
            // OR
            pubFileOnResult(resultData);
            // OR
            // pubAsBase64(resultData);
            // OR
            /*try {
                pubChunkedBinary(resultData);
            } catch (IOException e) {
                e.printStackTrace();
            }*/
        } else {
            Toast.makeText(this, "You haven't picked Image", Toast.LENGTH_LONG).show();
        }
    }

    private void pubStreamOnResult(Intent resultData) {
        final Uri imageUri = resultData.getData();
        final InputStream imageStream;
        try {
            imageStream = getContentResolver().openInputStream(imageUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            logIt("Error, when reading InputStream");
            return;
        }

        // final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
        // Payload streamPayload = Payload.fromStream(imageStream);
        // nearflyService.pubStream(streamPayload);
        // Finally, send the file payload.

            /*byte[] imageBytes;
            try {
                imageBytes = readBytes(imageStream);  // IOUtils.toByteArray(imageStream);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }*/

        if (/*imageBytes.length*/1 <= 16384) {
            Payload streamPayload = Payload.fromStream(imageStream);
            nearflyService.pubStream(streamPayload);
        } else {
                /*for(int i=0; i<bytes.length; i++){
                    byte[] partBytes = Arrays.copyOfRange(bytes, i, (bytes.length<i*16384?bytes.length:i*16384));

                }*/
            logIt("zu groß");

        }
    }

    /** Don't work at this moment **/
    private void pubFileOnResult(Intent resultData) {
        // The URI of the file selected by the user.
        Uri uri = resultData.getData();

        ContentResolver cr = getContentResolver();
        try {
            // Open the ParcelFileDescriptor for this URI with read access.
            ParcelFileDescriptor pfd = cr.openFileDescriptor(uri, "r");
            nearflyService.pubFile(PUB_CHANNEL, pfd);
        } catch (FileNotFoundException e) {
            Log.e("MyApp", "File not found", e);
            return;
        }
    }

    /** Tried to pub as Base64 **/
    private void pubAsBase64(Intent resultData) {
        try {
            final Uri imageUri = resultData.getData();
            final InputStream imageStream = getContentResolver().openInputStream(imageUri);
            final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            selectedImage.compress(Bitmap.CompressFormat.PNG, 80, byteArrayOutputStream);
            mImageView.setImageBitmap(selectedImage);

            File fileToSend = new File(this.getFilesDir(), "hello.txt");
            copyInputStreamToFile(imageStream, fileToSend);
            /** TODO **/ // nearflyService.pubBinary(PUB_CHANNEL, fileToSend);

            imageStream.close();

                /*final Uri imageUri = data.getData();
                final ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(imageUri, "r");
                final FileDescriptor pd = pfd.getFileDescriptor();
                final Bitmap selectedImage = BitmapFactory.decodeFileDescriptor(pd);
                sendBinary(pfd);
                pfd.close();*/

            // mImageView.setImageBitmap(selectedImage);

            //
            sendImage("jiren", 1, bitmapToBase64(selectedImage));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show();
        }
    }

    /** Publish the Data with HEADER and BODY **/ /** {@link #onMessageForBinary(byte[] )} **/
    private void pubChunkedBinary(Intent resultData) throws IOException {
        final Uri imageUri = resultData.getData();
        final InputStream fileStream = getContentResolver().openInputStream(imageUri);
        // final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);

        byte[] fileBinary = readBytes(fileStream);
        fileStream.close(); // Don't need anymore

        int numberOfChunks =(int) Math.ceil(
                Float.valueOf((float)fileBinary.length/ (float) MAX_CHUNK_SIZE)
        );  // e.g. 50_000/20_000 = 2 + 1
        /*if ((float)((int)fileBinary.length/ MAX_CHUNK_SIZE)==(float) fileBinary.length/ (float)MAX_CHUNK_SIZE)
            numberOfChunks++;*/

        // ArrayList<byte[]> chunkList = new ArrayList<>();
        for (int sqn = 0; sqn < numberOfChunks; sqn++) {  // e.g. 0 - 1*16381; 1*16381 - 2*16381; ...

            byte[] chunk;

            /** ADD HEADER + BODY**/
            String unformatHeader = new String(
                    "sqn:" + sqn + "\n"
                    + "max:" + numberOfChunks + "\n"
                    + "nice:" + 0);

            int from = sqn * MAX_CHUNK_SIZE;
            int to = (fileBinary.length < (sqn+1) * MAX_CHUNK_SIZE? fileBinary.length : (sqn+1) * MAX_CHUNK_SIZE);
            // int to = (sqn==numberOfChunks-1) ? fileBinary.length : MAX_CHUNK_SIZE;

            byte[] header = stringPadding(unformatHeader, HEADER_SIZE).getBytes();
            byte[] body  = Arrays.copyOfRange(fileBinary, from, to );

            /** PREPARE TO SEND**/
            byte[] payload = new byte[header.length+body.length];
            System.arraycopy(header, 0, payload, 0, header.length);
            System.arraycopy(body, 0, payload, header.length, body.length);

            nearflyService.pubBinaryTST(payload);
        }
    }

    private String stringPadding(String toPad, int width){
        return new String(new char[width - toPad.length()]).replace('\0', ' ') + toPad;
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private Bitmap base64ToBitmap(String b64) {
        byte[] imageAsBytes = Base64.decode(b64.getBytes(), Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length);
    }

    private void copyInputStreamToFile(InputStream in, File file) {
        try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void downloadFile(View view) {
        // File payloadFile = filePayload.asFile().asJavaFile();

        // Rename the file.
        // payloadFile.renameTo(new File(payloadFile.getParentFile(), "hello.png"));

        /*File file = new File(android.os.Environment.DIRECTORY_DOWNLOADS + "/Nearby/image.png");


        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), bmOptions);
        mImageView.setImageBitmap(bitmap);*/

        File file = new File(getFilesDir(), "abc.png");
        logIt(getFilesDir().toString());
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), bmOptions);
        mImageView.setImageBitmap(bitmap);
        logIt("can read" + file.canRead());
        logIt("can write" + file.canWrite());
        /*File movedFile = new File(android.os.Environment.DIRECTORY_DOWNLOADS + "/Nearby", "image.png");
        boolean success = file.renameTo(movedFile);
        logIt("rename process: " + success);*/

        /*String filename = makeFileName(payloadFile);*/
        File movedFile = new File(file.getParentFile(), "image.png");
        file.renameTo(movedFile);
        boolean success = file.renameTo(movedFile);
        logIt("rename process: " + success);

        // bitmap = Bitmap.createScaledBitmap(bitmap,parent.getWidth(),parent.getHeight(),true);

    }

    /**
     * Read the Bytes from a inputStream
     **/
    public byte[] readBytes(InputStream inputStream) throws IOException {
        // this dynamically extends to take the bytes you read
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        // this is storage overwritten on each iteration with bytes
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        // we need to know how may bytes were read to write them to the byteBuffer
        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }

        // and then we can return your byte array.
        return byteBuffer.toByteArray();
    }


    public void logIt(String str) {
        super.logIt(str);
    }
}
