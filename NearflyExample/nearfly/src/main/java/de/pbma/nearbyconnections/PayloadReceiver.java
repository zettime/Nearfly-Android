package de.pbma.nearbyconnections;

import android.content.ContentResolver;
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * TODO -------------------------------------------------------------------------------------
 **/
abstract class PayloadReceiver extends PayloadCallback {
    private final Context context;
    private final SimpleArrayMap<Long, Payload> incomingFilePayloads = new SimpleArrayMap<>();
    private final SimpleArrayMap<Long, Payload> incomingStreamPayload = new SimpleArrayMap<>();
    private final SimpleArrayMap<Long, Payload> completedFilePayloads = new SimpleArrayMap<>();
    private final SimpleArrayMap<Long, NeCon.FileInfMessage> mFileInformation = new SimpleArrayMap<>();
    private long timeMeasureBegin = 0;
    private final String TAG = "PayloadCallback";
    /**
     * Name that precedes the date e.g. Nearfly 2020-05-02
     **/
    private final String FILEFORENAME = "Nearfly ";
    // private final ConcurrentLinkedQueue<Long> payloadSendBuffer;

    public NeCon neCon = new NeCon();
    /**String notDeprecatedAlternativeDestinationDirectory
     = mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
     // BUT IS NOT ACTUALLY THE SAME, IS DELETED WHEN APP IS DELETED**/
    private final String DESTINATION_DIRECTORY = Environment.getExternalStorageDirectory() + File.separator
            + Environment.DIRECTORY_DOWNLOADS + File.separator + "Nearby";

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

    public abstract void onByteMessage(String endpointId, NeCon.BytesMessage bytesMessage);

    // Used for Subscriptions
    public abstract void onControlMessage(String endpointId, NeCon.ControlMessage bytesMessage);

    protected abstract void onBigBytes(String channel, byte[] bigBytes);

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
            NeCon.Message message = neCon.createMessage(payload);
            if (message instanceof NeCon.FileInfMessage) {
                // String fileInformations = new String(payload.asBytes(), StandardCharsets.UTF_8);
                NeCon.FileInfMessage fileMessage = (NeCon.FileInfMessage) message;
                // String payloadFilenameMessage = new String(payload.asBytes(), StandardCharsets.UTF_8);

                // long payloadId = addPayloadFilename(channel, fileInformations);
                mFileInformation.put(fileMessage.getFileId(), fileMessage);

                processFilePayload(fileMessage.getFileId());
            } else if (message instanceof NeCon.BytesMessage) {
                NeCon.BytesMessage bytesMessage= (NeCon.BytesMessage) message;
                onByteMessage(endpointId, bytesMessage);
            } else if (message instanceof NeCon.ControlMessage) {
                NeCon.ControlMessage controlMessage = (NeCon.ControlMessage) message;
                onControlMessage(endpointId, controlMessage);
            } else {
                throw new RuntimeException("Unknown NeConExtMessage File type Received");
            }
        } else if (payload.getType() == Payload.Type.FILE) {
            // Add this to our tracking map, so that we can retrieve the payload later.
            incomingFilePayloads.put(payload.getId(), payload);
        }// TODO: in work
        /*else if (payload.getType() == Payload.Type.STREAM){
            incomingFilePayloads.put(payload.getId(), payload);
        }*/
        logD("incomingPayloads: " + payload.toString());
        timeMeasureBegin = System.currentTimeMillis();
    }

    /** {@link de.pbma.mqtt.MqttAdapter#readBytes(InputStream)} **/
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

    private fileRelatedData processFilePayload(long payloadId) {
        // BYTES and FILE could be received in any order, so we call when either the BYTES or the FILE
        // payload is completely received. The file payload is considered complete only when both have
        // been received.
        Payload filePayload = completedFilePayloads.get(payloadId);
        NeCon.FileInfMessage fileMessage = mFileInformation.get(payloadId);

        String textAttachment = fileMessage.getTextAttachment();
        String channel = fileMessage.getChannel();

        // TODO provisorisch ---------------------------------------------
        if (filePayload != null && textAttachment != null &&
                textAttachment.equals(NeConAdapter.BIG_BYTES_BYPASS)){
            completedFilePayloads.remove(payloadId);
            mFileInformation.remove(payloadId);

            FileInputStream fileInputStream = null;
            //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                fileInputStream = new FileInputStream(
                        filePayload.asFile().asParcelFileDescriptor().getFileDescriptor());
                filePayload.asFile().asJavaFile();
            /*}else{
                File payloadFile = filePayload.asFile().asJavaFile();
                try {
                    fileInputStream = new FileInputStream(payloadFile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }*/

            byte[] fileAsBytes = null;

            try {
                fileAsBytes = readBytes(fileInputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }

            File oldFile = new File(DESTINATION_DIRECTORY, String.valueOf(payloadId));
            oldFile.delete();
            onBigBytes(channel, fileAsBytes);
            return null;
        }// TODO provisorisch ----------------------------------------

        if (filePayload != null && textAttachment != null) {
            completedFilePayloads.remove(payloadId);
            mFileInformation.remove(payloadId);

            // Get the received file (which will be in the Downloads folder)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ParcelFileDescriptor pfd = filePayload.asFile().asParcelFileDescriptor();
                String filename = "";
                try {
                    // Copy the file to a new location.
                    InputStream in = new FileInputStream(pfd.getFileDescriptor());
                    InputStream bufin = new BufferedInputStream(in); // Faster than fileInputStream

                    // File destFile = mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS + File.separator + "Nearby/"+filename);

                    filename = makeFileName(fileMessage.getFileExtension());
                    Log.v("NearbyTest", context.getCacheDir().getAbsolutePath().toString());

                    copyStream(bufin, new FileOutputStream(new File(DESTINATION_DIRECTORY, filename)));
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
                    File oldFile = new File(DESTINATION_DIRECTORY, String.valueOf(payloadId));
                    oldFile.delete();
                }
                // onReceive("?", new Payload("Datei erhalten".getBytes()));

                // return movedFile.getAbsolutePath();
                return new fileRelatedData(
                        channel,
                        new File(DESTINATION_DIRECTORY, filename).getAbsolutePath(),
                        textAttachment
                );

                /*File payloadFile = filePayload.asFile().asJavaFile();
                logD(payloadFile.getAbsolutePath());>

                Uri uri = Uri.fromFile(new File(payloadFile.getAbsolutePath()));
                ContentResolver cr = mContext.getContentResolver();
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
        }
        return null;
    }

    public String makeFileName(String fileExtension) {
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
        long payloadId = update.getPayloadId();

        switch (update.getStatus()) {
            case PayloadTransferUpdate.Status.SUCCESS:
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
                        Log.v("measureTest", "path: " + fileRelatedData.path);
                        try {
                            forwardFile(endpointId, Payload.fromFile(from), fileRelatedData.channel, fileRelatedData.path, fileRelatedData.textAttachment);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
                /* // FAILED FOR QOS
                else{
                    int size = payloadSendBuffer.size();
                    if (size>1){
                        Log.d("testy","payloadSendBuffer: "+ size + " ###################################");
                    }
                    payloadSendBuffer.remove(payloadId);
                }*/
                /*else if (payload.getType() == Payload.Type.STREAM){
                    try {
                        onBigBytes(toByteArray(payload.asStream().asInputStream()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }*/

                logD("SUCCESS " + update.toString());
                logD(" -> time needed " + (System.currentTimeMillis() - timeMeasureBegin));
                break;
            case PayloadTransferUpdate.Status.IN_PROGRESS:
                logD(String.format("Progress - onPayloadTransferUpdate(endpointId=%s, update=%s)", endpointId, update));
                logD(String.format("Byte Transfered: " + update.getBytesTransferred()));
                break;
            case PayloadTransferUpdate.Status.FAILURE:
            case PayloadTransferUpdate.Status.CANCELED:
                logD(String.format((update.getStatus()==PayloadTransferUpdate.Status.FAILURE?
                        "Failure":"Canceled")+" - onPayloadTransferUpdate(endpointId=%s, update=%s)", endpointId, update));
                if (incomingFilePayloads.containsKey(payloadId)){
                    incomingFilePayloads.remove(payloadId);
                    File file = new File(DESTINATION_DIRECTORY, ""+payloadId);
                    file.delete();
                }
                if (mFileInformation.containsKey(payloadId))
                    mFileInformation.remove(payloadId);
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

    public static byte[] toByteArray(InputStream in) throws IOException {

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];
        int len;

        // read bytes from the input stream and store them in buffer
        while ((len = in.read(buffer)) != -1) {
            // write bytes from the buffer into output stream
            os.write(buffer, 0, len);
        }

        return os.toByteArray();
    }


    private void logD(String output) {
        Log.d(TAG, output);
    }
}