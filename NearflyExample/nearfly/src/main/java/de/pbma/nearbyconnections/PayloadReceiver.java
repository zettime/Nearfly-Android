package de.pbma.nearbyconnections;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.collection.SimpleArrayMap;

import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;

/**
 * TODO -------------------------------------------------------------------------------------
 **/
abstract class PayloadReceiver extends PayloadCallback {
    private final Context context;
    private final SimpleArrayMap<Long, Payload> incomingFilePayloads = new SimpleArrayMap<>();
    private final SimpleArrayMap<Long, Payload> completedFilePayloads = new SimpleArrayMap<>();
    /* private final SimpleArrayMap<Long, String> filePayloadTextAttachment = new SimpleArrayMap<>();
     private final SimpleArrayMap<Long, String> filePayloadFileExtension = new SimpleArrayMap<>();
     private final SimpleArrayMap<Long, String> filePayloadChannel = new SimpleArrayMap<>();*/
    private final SimpleArrayMap<Long, NeCon.FileInfMessage> mFileInformation = new SimpleArrayMap<>();
    private long timeMeasureBegin = 0;
    private final String TAG = "PayloadCallback";
    /**
     * Name that precedes the date e.g. Nearfly 2020-05-02
     **/
    private final String FILEFORENAME = "Nearfly ";

    public NeCon neCon = new NeCon();


    public PayloadReceiver(Context context) {
        this.context = context;
    }

    /**
     * Functions which are needed from {@link NeConEssentials}
     **/
    public abstract void onFile(String endpointId, String channel, String path, String textAttachment);

    public abstract void forwardFile(String endpointId, Payload payload,
                                     String channel, String path,
                                     String textAttachment);

    /**
     * TODO: onFile Beii anderen ändern
     **/
    public abstract void onByteMessage(String endpointId, Payload payload);

    class fileRelatedData {
        public String channel;
        public String path;
        public String textAttachment;

        public fileRelatedData(String channel, String path, String textAttachment) {
            this.channel = channel;
            this.path = path;
            this.textAttachment = textAttachment;
        }
    }

    @Override
    public void onPayloadReceived(String endpointId, Payload payload) {
        if (payload.getType() == Payload.Type.BYTES) {

            //NeConExtMessage neConExtMessage = NeConExtMessage.createExtMessage(payload);
            int messageType = NeCon.getMessageType(payload);
            if (messageType == NeCon.FILEINFORMATION) {
                // String fileInformations = new String(payload.asBytes(), StandardCharsets.UTF_8);
                NeCon.FileInfMessage fileMessage = neCon.createFileInfMessage(payload);
                // String payloadFilenameMessage = new String(payload.asBytes(), StandardCharsets.UTF_8);

                // long payloadId = addPayloadFilename(channel, fileInformations);
                mFileInformation.put(fileMessage.getFileId(), fileMessage);

                processFilePayload(fileMessage.getFileId());
            } else if (messageType == NeCon.STRING) {
                onByteMessage(endpointId, payload);
            } else {
                throw new RuntimeException("Unknown NeConExtMessage File type Received");
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

    /**
     * Pentant {@link NeConClient#pubFile(String, Uri, String)}
     **/
    /*private long addPayloadFilename(String channel, String payloadFilenameMessage) {
        String[] parts = payloadFilenameMessage.split("\\/", 3);
        String fileExtension = parts[0];
        long payloadId = Long.parseLong(parts[1]);
        String textAttachment = parts[2];
        filePayloadFileExtension.put(payloadId, fileExtension);
        filePayloadTextAttachment.put(payloadId, textAttachment);
        filePayloadChannel.put(payloadId, channel);
        return payloadId;
    }*/

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
    private fileRelatedData processFilePayload(long payloadId) {
        // BYTES and FILE could be received in any order, so we call when either the BYTES or the FILE
        // payload is completely received. The file payload is considered complete only when both have
        // been received.
        Payload filePayload = completedFilePayloads.get(payloadId);
        NeCon.FileInfMessage fileMessage = mFileInformation.get(payloadId);

        String textAttachment = fileMessage.getTextAttachment();
        String channel = fileMessage.getChannel();

        if (filePayload != null && textAttachment != null) {
            completedFilePayloads.remove(payloadId);
            mFileInformation.remove(payloadId);
            /*filePayloadTextAttachment.remove(payloadId);
            filePayloadChannel.remove(payloadId);*/

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
                // Uri uri = android.net.Uri.parse(("content://" + Environment.DIRECTORY_DOWNLOADS +"/"+ payloadId));
                // Uri uri = Uri.fromFile(new File("storage/emulated/0/Download/Nearby/-8818418826558062309"));
                // File payloadFile = filePayload.asFile().asJavaFile();
                // Uri uri = Uri.fromFile(payloadFile);
                // Uri uri = android.net.Uri.parse(("content://sdcard/Download/Nearby/-8818418826558062309"));
                ParcelFileDescriptor pfd = filePayload.asFile().asParcelFileDescriptor();
                String filename = "";
                String destinationDirectory = "";
                try {
                    // Copy the file to a new location.
                    InputStream in = new FileInputStream(pfd.getFileDescriptor());
                    InputStream bufin = new BufferedInputStream(in); // Faster than fileInputStream

                    /**String notDeprecatedAlternativeDestinationDirectory
                     = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
                     // BUT IS NOT ACTUALLY THE SAME, IS DELETED WHEN APP IS DELETED**/
                    destinationDirectory = Environment.getExternalStorageDirectory() + File.separator
                            + Environment.DIRECTORY_DOWNLOADS + File.separator + "Nearby";

                    // File destFile = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS + File.separator + "Nearby/"+filename);

                    filename = makeFileName(fileMessage.getFileExtension());
                    Log.v("NearbyTest", context.getCacheDir().getAbsolutePath().toString());

                    copyStream(bufin, new FileOutputStream(new File(destinationDirectory, filename)));
                    /*File from = new File(destinationDirectory, ""+payloadId);
                    File to = new File(destinationDirectory, filename);
                    from.renameTo(to);*/

                    in.close();
                    bufin.close();
                } catch (IOException e) {
                    // Log the error.
                    // Log.e("FileReceiveTest", ""+e.getStackTrace());
                    e.printStackTrace();
                } finally {
                    // Delete the original file.
                    File oldFile = new File(destinationDirectory, String.valueOf(payloadId));
                    oldFile.delete();
                }
                // onReceive("?", new Payload("Datei erhalten".getBytes()));

                // return movedFile.getAbsolutePath();
                return new fileRelatedData(
                        channel,
                        new File(destinationDirectory, filename).getAbsolutePath(),
                        textAttachment
                );

                /*File payloadFile = filePayload.asFile().asJavaFile();
                logD(payloadFile.getAbsolutePath());>

                Uri uri = Uri.fromFile(new File(payloadFile.getAbsolutePath()));
                ContentResolver cr = context.getContentResolver();
                try {
                    ParcelFileDescriptor pfd = cr.openFileDescriptor(uri, "r");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                );*/
            } else {

                File payloadFile = filePayload.asFile().asJavaFile();
                logD(payloadFile.getAbsolutePath());

                // Rename the file.
                String filename = makeFileName(fileMessage.getFileExtension());
                File movedFile = new File(payloadFile.getParentFile(), filename);
                payloadFile.renameTo(movedFile);
                logD("named to " + filename);
                logD("textAttachment " + textAttachment);
                // onReceive("?", new Payload("Datei erhalten".getBytes()));

                // return movedFile.getAbsolutePath();
                return new fileRelatedData(
                        channel,
                        movedFile.getAbsolutePath(),
                        textAttachment
                );
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

    public String makeFileName(String fileExtension) {
        // Get Mime Type of File
        /*URLConnection connection = null;
        try {
            URL url = file.toURI().toURL();
            connection = url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String mimeType = connection.getContentType();
        String fileExtension = mimeType.replace(" ", "").split("/")[1];*/

        // String fileExtension = filePayloadFileExtension.remove(payloadId);

        // Make File  Name
        Date today = Calendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-hh.mm.ss");
        String fileName = FILEFORENAME + formatter.format(today);

        return fileName + "." + fileExtension;
    }

    public static String getMimeTypeOfImage(String pathName) {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pathName, opt);
        return opt.outMimeType;
    }


    @Override
    public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
        switch (update.getStatus()) {
            case PayloadTransferUpdate.Status.SUCCESS:
                long payloadId = update.getPayloadId();
                Payload payload = incomingFilePayloads.remove(payloadId);
                if (payload == null)
                    return;

                completedFilePayloads.put(payloadId, payload);
                if (payload.getType() == Payload.Type.FILE) {
                    fileRelatedData fileRelatedData = processFilePayload(payloadId);
                    // TODO *****
                    if (fileRelatedData != null) {
                        onFile(endpointId, fileRelatedData.channel, fileRelatedData.path, fileRelatedData.textAttachment);


                        File from = new File(fileRelatedData.path);
                        Log.v("test", "path: " + fileRelatedData.path);
                        try {
                            forwardFile(endpointId, Payload.fromFile(from), fileRelatedData.channel, fileRelatedData.path, fileRelatedData.textAttachment);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
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

    private void logD(String output) {
        Log.d(TAG, output);
    }
}