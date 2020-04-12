package de.pbma.nearbyconnections;

import android.content.Context;
import android.icu.text.SimpleDateFormat;
import android.os.Build;
import android.util.Log;

import androidx.collection.SimpleArrayMap;

import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;

import de.pbma.nearfly.ExtMessage;

/**
 * TODO -------------------------------------------------------------------------------------
 **/
abstract class ReceiveFilePayloadCallback extends PayloadCallback {
    private final Context context;
    private final SimpleArrayMap<Long, Payload> incomingFilePayloads = new SimpleArrayMap<>();
    private final SimpleArrayMap<Long, Payload> completedFilePayloads = new SimpleArrayMap<>();
    private final SimpleArrayMap<Long, String> filePayloadFilenames = new SimpleArrayMap<>();
    private long timeMeasureBegin = 0;
    private final String TAG = "PayloadCallback";

    public ReceiveFilePayloadCallback(Context context) {
        this.context = context;
    }

    /** Functions which are needed from {@link MyNearbyConnectionsAbstract} **/
    public abstract void onFile(String endpointId, String path);
    public abstract void onByteMessage(String endpointId, Payload payload);

    @Override
    public void onPayloadReceived(String endpointId, Payload payload) {
        if (payload.getType() == Payload.Type.BYTES) {

            ExtMessage extMessage = ExtMessage.createExtMessage(payload);
            if (extMessage.getType().equals(ExtMessage.FILEINFORMATION)){
                // String fileInformations = new String(payload.asBytes(), StandardCharsets.UTF_8);
                String fileInformations = extMessage.getPayload();
                // String payloadFilenameMessage = new String(payload.asBytes(), StandardCharsets.UTF_8);

                long payloadId = addPayloadFilename(fileInformations);
                processFilePayload(payloadId);
            }else if (extMessage.getType().equals(ExtMessage.STRING)){
                onByteMessage(endpointId, payload);
            }else{
                throw new RuntimeException("Unknown ExtMessage File type Received");
            }
        } else if (payload.getType() == Payload.Type.FILE) {
            // Add this to our tracking map, so that we can retrieve the payload later.
            incomingFilePayloads.put(payload.getId(), payload);
        }

        logD("incomingPayloads: " + payload.toString());
        timeMeasureBegin = System.currentTimeMillis();
    }

    /**
     * Extracts the payloadId and filename from the message and stores it in the
     * filePayloadFilenames map. The format is payloadId:filename.
     */
        /*private long addPayloadFilename(String payloadFilenameMessage) {
            String[] parts = payloadFilenameMessage.split(":");
            long payloadId = Long.parseLong(parts[0]);
            String filename = parts[1];
            filePayloadFilenames.put(payloadId, filename);
            return payloadId;
        }*/

    private long addPayloadFilename(String payloadFilenameMessage) {
        String[] parts = payloadFilenameMessage.split(":");
        String channel = parts[0];
        long payloadId = Long.parseLong(parts[1]);
        filePayloadFilenames.put(payloadId, channel);
        return payloadId;
    }

        /*private long addFileinformations(String fileInformations) {
            JSONObject jsonObject;
            try {
                jsonObject = new JSONObject(fileInformations);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String typ = jsonObject.getString("");
            String channel = jsonObject.getString("channel");
            String fileId = jsonObject.getString("id");
            filePayloadFilenames.put(payloadId, channel);
        }*/

    private String processFilePayload(long payloadId) {
        // BYTES and FILE could be received in any order, so we call when either the BYTES or the FILE
        // payload is completely received. The file payload is considered complete only when both have
        // been received.
        Payload filePayload = completedFilePayloads.get(payloadId);
        String channel = filePayloadFilenames.get(payloadId);
        if (filePayload != null && channel != null) {
            completedFilePayloads.remove(payloadId);
            filePayloadFilenames.remove(payloadId);

            // Rename the file.
            // payloadFile.renameTo(new File(payloadFile.getParentFile(), filename));
            // TODO .......................
            // Get the received file (which will be in the Downloads folder)
                /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Because of https://developer.android.com/preview/privacy/scoped-storage, we are not
                    // allowed to access filepaths from another process directly. Instead, we must open the
                    // uri using our ContentResolver.
                    // Uri uri = android.net.Uri.parse(filePayload.asFile().asJavaFile().toURI().toString());
                    File file = new File(android.os.Environment.DIRECTORY_DOWNLOADS + "/Nearby/" + String.valueOf(payloadId));
                    Uri uri = android.net.Uri.parse(file.toURI().toString());
                    String filename = makeFileName(file);
                    File movedFile = new File(android.os.Environment.DIRECTORY_DOWNLOADS + "/Nearby/image.png");
                    boolean success = file.renameTo(movedFile);

                    logD(file + " renamed to " + filename + " successful? " + success);
                } else {*/


            // Get the received file (which will be in the Downloads folder)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // TODO: Umbennen für Android API 29 derzeitg nicht möglich
                // Because of https://developer.android.com/preview/privacy/scoped-storage, we are not
                // allowed to access filepaths from another process directly. Instead, we must open the
                // uri using our ContentResolver.
                    /*Uri uri = android.net.Uri.parse(("file:/Download/" + payloadId));
                    try {
                        // Copy the file to a new location.
                        InputStream in = context.getContentResolver().openInputStream(uri);
                        copyStream(in, new FileOutputStream(new File(context.getCacheDir(), "testtt.png")));
                    } catch (IOException e) {
                        // Log the error.
                    } finally {
                        // Delete the original file.
                        context.getContentResolver().delete(uri, null, null);
                    }*/
            } else {

                File payloadFile = filePayload.asFile().asJavaFile();
                logD(payloadFile.getAbsolutePath());

                // Rename the file.
                String filename = makeFileName(payloadFile);
                File movedFile = new File(payloadFile.getParentFile(), filename);
                payloadFile.renameTo(movedFile);
                logD("named to " + filename);
                logD("channel " + channel);
                // onReceive("?", new Payload("Datei erhalten".getBytes()));

                return movedFile.getAbsolutePath();
            }
            // TODO .......................

        }
        return null;
    }

        /*public void myRenameFile(long payloadId) {
            Payload filePayload = completedFilePayloads.get(payloadId);
            //String filename = filePayloadFilenames.get(payloadId);
            completedFilePayloads.remove(payloadId);

            File payloadFile = filePayload.asFile().asJavaFile();

            // Rename the file.
            String filename = makeFileName(payloadFile);
            payloadFile.renameTo(new File(payloadFile.getParentFile(), filename));
            logD("named to " + filename);
        }*/

    public String makeFileName(File payloadFile) {
        // Get Mime Type of File
        URLConnection connection = null;
        try {
            URL url = payloadFile.toURI().toURL();
            connection = url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String mimeType = connection.getContentType();
        String extension = mimeType.replace(" ", "").split("/")[1];

        // Make File  Name
        Date today = Calendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-hh.mm.ss");
        String filename = "Nearfly " + formatter.format(today);

        return filename + "." + extension;
    }

    @Override
    public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
        switch (update.getStatus()){
            case PayloadTransferUpdate.Status.SUCCESS:
                long payloadId = update.getPayloadId();
                Payload payload = incomingFilePayloads.remove(payloadId);
                if (payload == null)
                    return;

                completedFilePayloads.put(payloadId, payload);
                if (payload.getType() == Payload.Type.FILE) {
                    String path = processFilePayload(payloadId);
                    if (path!=null) // TODO
                        onFile(endpointId, path);
                    //onFile(mEstablishedConnections.get(endpointId), path);
                    // myRenameFile(payloadId);
                }

                logD("SUCCESS " + update.toString());
                logD(" -> time needed " + (System.currentTimeMillis() - timeMeasureBegin));
                break;
            case PayloadTransferUpdate.Status.IN_PROGRESS:
                logD(String.format("Progress - onPayloadTransferUpdate(endpointId=%s, update=%s)", endpointId, update));
                logD(String.format("Byte Transfered: " + update.getBytesTransferred()));
                break;
            case PayloadTransferUpdate.Status.FAILURE:
                logD(String.format("Failure - onPayloadTransferUpdate(endpointId=%s, update=%s)", endpointId, update));
                break;
            case PayloadTransferUpdate.Status.CANCELED:
                logD(String.format("Canceled - onPayloadTransferUpdate(endpointId=%s, update=%s)", endpointId, update));
                break;
        }
    }

    /**
     * Copies a stream from one location to another.
     */
    private void copyStream(InputStream in, OutputStream out) throws IOException {
        try {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        } finally {
            in.close();
            out.close();
        }
    }
    
    private void logD(String output){
        Log.d(TAG, output);
    }
}