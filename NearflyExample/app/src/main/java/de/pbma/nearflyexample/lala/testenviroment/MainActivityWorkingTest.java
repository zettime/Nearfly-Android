package de.pbma.nearflyexample.lala.testenviroment;

import android.animation.Animator;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Random;

import de.pbma.nearflyexample.R;

/**
 * Our WalkieTalkie Activity. This Activity has 4 {@link State}s.
 *
 * <p>{@link State#UNKNOWN}: We cannot do anything while we're in this state. The app is likely in
 * the background.
 *
 * <p>{@link State#DISCOVERING}: Our default state (after we've connected). We constantly listen for
 * a device to advertise near us.
 *
 * <p>{@link State#ADVERTISING}: If a user shakes their device, they enter this state. We advertise
 * our device so that others nearby can discover us.
 *
 * <p>{@link State#CONNECTED}: We've connected to another device. We can now talk to them by holding
 * down the volume keys and speaking into the phone. We'll continue to advertise (if we were already
 * advertising) so that more people can connect to us.
 */
public class MainActivityWorkingTest extends ConnectionsActivityTest {
//    /** If true, debug logs are shown on the device. */
//    private static final boolean DEBUG = true;
//
//    /* TODO: LALALA */
//    private int connectionAttemp = 0;
//
//    /**
//     * The connection strategy we'll use for Nearby Connections. In this case, we've decided on
//     * P2P_STAR, which is a combination of Bluetooth Classic and WiFi Hotspots.
//     */
//    private static final Strategy STRATEGY = Strategy.P2P_STAR;
//
//    /** Acceleration required to detect a shake. In multiples of Earth's gravity. */
//    private static final float SHAKE_THRESHOLD_GRAVITY = 2;
//
//    /**
//     * Advertise for 30 seconds before going back to discovering. If a client connects, we'll continue
//     * to advertise indefinitely so others can still connect.
//     */
//    private static final long ADVERTISING_DURATION = 60000;
//
//    /** How long to vibrate the phone when we change states. */
//    private static final long VIBRATION_STRENGTH = 500;
//
//    /** Length of state change animations. */
//    private static final long ANIMATION_DURATION = 600;
//
//    /**
//     * This service id lets us find other nearby devices that are interested in the same thing. Our
//     * sample does exactly one thing, so we hardcode the ID.
//     */
//    private static final String SERVICE_ID =
//            "com.google.location.nearby.apps.walkietalkie.manual.SERVICE_ID";
//
//    /**
//     * The state of the app. As the app changes states, the UI will update and advertising/discovery
//     * will start/stop.
//     */
//    private State mState = State.UNKNOWN;
//
//    /** A random UID used as this device's endpoint name. */
//    private String mName;
//
//    /** Displays the previous state during animation transitions. */
//    private TextView mPreviousStateView;
//
//    /** Displays the current state. */
//    // private TextView mCurrentStateView;
//    // TODO 2503
//    private TextView tvCurrentState;
//    private TextView tvRootNode;
//
//    /** An animator that controls the animation from previous state to current state. */
//    @Nullable private Animator mCurrentAnimator;
//
//    /** A running log of debug messages. Only visible when DEBUG=true. */
//    private TextView mDebugLogView;
//
//    /** The SensorManager gives us access to sensors on the device. */
//    // private SensorManager mSensorManager;
//
//    /** The accelerometer sensor allows us to detect device movement for shake-to-advertise. */
//    // private Sensor mAccelerometer;
//
//    /** Listens to holding/releasing the volume rocker. */
//  /*private final GestureDetector mGestureDetector =
//      new GestureDetector(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP) {
//        @Override
//        protected void onHold() {
//          logV("onHold");
//          startRecording();
//        }
//
//        @Override
//        protected void onRelease() {
//          logV("onRelease");
//          stopRecording();
//        }
//      };*/
//
//    /** For recording audio as the user speaks. */
//    // @Nullable private AudioRecorder mRecorder;
//
//    /** For playing audio from other users nearby. */
//    // private final Set<AudioPlayer> mAudioPlayers = new HashSet<>();
//
//    /** The phone's original media volume. */
//    // private int mOriginalVolume;
//
//    /**
//     * A Handler that allows us to post back on to the UI thread. We use this to resume discovery
//     * after an uneventful bout of advertising.
//     */
//    private final Handler mUiHandler = new Handler(Looper.getMainLooper());
//
//    /** Starts discovery. Used in a postDelayed manor with {@link #mUiHandler}. */
//    private final Runnable mDiscoverRunnable =
//            new Runnable() {
//                @Override
//                public void run() {
//                    setState(State.DISCOVERING);
//                }
//            };
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//    /*getSupportActionBar()
//        .setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.actionBar));*/
//
//        // mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
//        // mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//
//        // mPreviousStateView = (TextView) findViewById(R.id.previous_state);
//        // mCurrentStateView = (TextView) findViewById(R.id.current_state);
//        tvCurrentState = findViewById(R.id.tv_current_state);
//        tvRootNode = findViewById(R.id.tv_root_node);
//
//        mDebugLogView = (TextView) findViewById(R.id.debug_log);
//        mDebugLogView.setVisibility(DEBUG ? View.VISIBLE : View.GONE);
//        mDebugLogView.setMovementMethod(new ScrollingMovementMethod());
//
//        mName = generateRandomName();
//
//        ((TextView) findViewById(R.id.name)).setText(mName);
//    }
//
//  /*@Override
//  public boolean dispatchKeyEvent(KeyEvent event) {
//    if (mState == State.CONNECTED && mGestureDetector.onKeyEvent(event)) {
//      return true;
//    }
//    return super.dispatchKeyEvent(event);
//  }*/
//
//    /*@Override
//    protected void onStart() {
//        super.onStart();
//        // mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
//
//        // Set the media volume to max.
//    /*setVolumeControlStream(AudioManager.STREAM_MUSIC);
//    AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//    // mOriginalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
//    audioManager.setStreamVolume(
//        AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
//*/
//        // TODO: 2503
//        //setState(State.FINDROOT);
//        // setState(State.DISCOVERING);
//    //}
//
//    @Override
//    protected void onStop() {
//        // mSensorManager.unregisterListener(this);
//
//        // Restore the original volume.
//        // AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//        // audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mOriginalVolume, 0);
//        // setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
//
///*    if (isRecording()) {
//      stopRecording();
//    }
//    if (isPlaying()) {
//      stopPlaying();
//    }
//*/
//        setState(State.UNKNOWN);
//        mUiHandler.removeCallbacksAndMessages(null);
//
///*    if (mCurrentAnimator != null && mCurrentAnimator.isRunning()) {
//      mCurrentAnimator.cancel();
//    }*/
//
//        super.onStop();
//    }
//
//    @Override
//    public void onBackPressed() {
//        if (getState() == State.CONNECTED || getState() == State.ADVERTISING) {
//            // TODO
//            setState(State.FINDROOT);
//            return;
//        }
//        super.onBackPressed();
//    }
//
//    @Override
//    protected void onEndpointDiscovered(Endpoint endpoint) {
//        // We found an advertiser!
//        if (!isConnecting()) {
//
//            // TODO 2503: Advertiser gefunden -> Wechsle zum Discovering
//            // if (getState() == State.FINDROOT && getState() != State.DISCOVERING)
//            //   setState(State.DISCOVERING);
//
//            if (endpoint.getName().compareTo(getName()) > 0 ){
//                if (getState() == State.FINDROOT && getState() != State.DISCOVERING)
//                    setState(State.DISCOVERING);
//                if (getState() == State.CONNECTED && getState() != State.DISCOVERING)
//                    setState(State.FINDROOT);
//
//                // Nur senden, wenn anderer Root
//                connectToEndpoint(endpoint);
//            }
//        }
//    }
//
//    @Override
//    protected void onConnectionInitiated(Endpoint endpoint, ConnectionInfo connectionInfo) {
//        // TODO 2503: Discovering Device found -> Switch to Advertising Mode
//        if (getName().compareTo(endpoint.getName()) > 0) {
//            if (getState() == State.FINDROOT && getState() != State.ADVERTISING)
//                setState(State.ADVERTISING);
//        }
//
//        // A connection to another device has been initiated! We'll accept the connection immediately.
//        acceptConnection(endpoint);
//    }
//
//    @Override
//    protected void onEndpointConnected(Endpoint endpoint) {
//        Toast.makeText(
//                this, getString(R.string.toast_connected, endpoint.getName()), Toast.LENGTH_SHORT)
//                .show();
//        setState(State.CONNECTED);
//
//        /* TODO */
//        // connectionAttemp=0;
//        if (!isAdvertising())
//            tvRootNode.setText(endpoint.getName());
//        else
//            tvRootNode.setText("self");
//    }
//
//    @Override
//    protected void onEndpointDisconnected(Endpoint endpoint) {
//        Toast.makeText(
//                this, getString(R.string.toast_disconnected, endpoint.getName()), Toast.LENGTH_SHORT)
//                .show();
//
//        // If we lost all our endpoints, then we should reset the state of our app and go back
//        // to our initial state (discovering).
//        if (getConnectedEndpoints().isEmpty()) {
//            // TODO: New init State
//            // setState(State.DISCOVERING);
//            setState(State.FINDROOT);
//        }
//    }
//
//    @Override
//    protected void onConnectionFailed(Endpoint endpoint) {
//        // Let's try someone else.
//        if (getState() == State.DISCOVERING && !getDiscoveredEndpoints().isEmpty()) {
//            connectToEndpoint(pickRandomElem(getDiscoveredEndpoints()));
//            setState(State.FINDROOT);
//            /*connectionAttemp++; /* TODO */
//        }
//
//        /* TODO: Endpoint seems not to be an advisor */
//    /*if (connectionAttemp>1){
//      setState(State.FINDROOT);
//      connectionAttemp = 0;
//    }*/
//    }
//
//    /**
//     * The state has changed. I wonder what we'll be doing now.
//     *
//     * @param state The new state.
//     */
//    private void setState(State state) {
//        if (mState == state) {
//            logW("State set to " + state + " but already in that state");
//            return;
//        }
//
//        logD("State set to " + state);
//        // TODO: 2503
//        tvCurrentState.setText(state.toString()
//        );
//
//        State oldState = mState;
//        mState = state;
//        onStateChanged(oldState, state);
//    }
//
//    /** @return The current state. */
//    private State getState() {
//        return mState;
//    }
//
//    /**
//     * State has changed.
//     *
//     * @param oldState The previous state we were in. Clean up anything related to this state.
//     * @param newState The new state we're now in. Prepare the UI for this state.
//     */
//    private void onStateChanged(State oldState, State newState) {
//    /*if (mCurrentAnimator != null && mCurrentAnimator.isRunning()) {
//      mCurrentAnimator.cancel();
//    }*/
//
//        // Update Nearby Connections to the new state.
//        switch (newState) {
//            case DISCOVERING:
//                if (isAdvertising()) {
//                    stopAdvertising();
//                }
//                // TODO: startDiscovery failed vermeiden, um overhead zu vermeiden
//                // disconnectFromAllEndpoints();
//                if (!isDiscovering())
//                    startDiscovering();
//                break;
//            case ADVERTISING:
//                if (isDiscovering()) {
//                    stopDiscovering();
//                }
//                // disconnectFromAllEndpoints();
//                startAdvertising();
//                break;
//            case CONNECTED:
//                if (isDiscovering()) {
//                    // stopDiscovering();
//                } else if (isAdvertising()) {
//                    // Continue to advertise, so others can still connect,
//                    // but clear the discover runnable.
//                    removeCallbacks(mDiscoverRunnable);
//                }
//                break;
//            case UNKNOWN:
//                stopAllEndpoints();
//                break;
//            // TODO 2303
//            case FINDROOT:
//                if (isAdvertising()) {
//                    stopAdvertising();
//                }
//                if (isDiscovering()) {
//                    stopDiscovering();
//                }
//                disconnectFromAllEndpoints();
//                startAdvertising();
//                startDiscovering();
//            default:
//                // no-op
//                break;
//        }
//    }
//
//    public void startAdvertising(View view){
//        setState(State.ADVERTISING);
//        postDelayed(mDiscoverRunnable, ADVERTISING_DURATION);
//    }
//
//    public int cnt = 0;
//    public void publish(View view){
//        /*ExtMessage extMessage = new ExtMessage(String.valueOf(++cnt), "test");
//
//        send(Payload.fromBytes(extMessage.getBytes()));*/
//        showImageChooser();
//
//        logD(cnt + " published");
//    }
//
//
//    /** {@see ConnectionsActivity#onReceive(Endpoint, Payload)} */
//    @Override
//    protected void onReceive(Endpoint endpoint, Payload payload) {
//    }
//
//    @Override
//    protected void onDiscoveryFailed() {
//        super.onDiscoveryFailed();
//        setState(State.UNKNOWN);
//        setState(State.DISCOVERING);
//        mUiHandler.removeCallbacksAndMessages(null);
//    }
//    /**
//     * Queries the phone's contacts for their own profile, and returns their name. Used when
//     * connecting to another device.
//     */
//    @Override
//    protected String getName() {
//        return mName;
//    }
//
//    /** {@see ConnectionsActivity#getServiceId()} */
//    @Override
//    public String getServiceId() {
//        return SERVICE_ID;
//    }
//
//    /** {@see ConnectionsActivity#getStrategy()} */
//    @Override
//    public Strategy getStrategy() {
//        return STRATEGY;
//    }
//
//    /** {@see Handler#post()} */
//    protected void post(Runnable r) {
//        mUiHandler.post(r);
//    }
//
//    /** {@see Handler#postDelayed(Runnable, long)} */
//    protected void postDelayed(Runnable r, long duration) {
//        mUiHandler.postDelayed(r, duration);
//    }
//
//    /** {@see Handler#removeCallbacks(Runnable)} */
//    protected void removeCallbacks(Runnable r) {
//        mUiHandler.removeCallbacks(r);
//    }
//
//    @Override
//    protected void logV(String msg) {
//        super.logV(msg);
//        appendToLogs(toColor(msg, getResources().getColor(R.color.log_verbose)));
//    }
//
//    @Override
//    protected void logD(String msg) {
//        super.logD(msg);
//        appendToLogs(toColor(msg, getResources().getColor(R.color.log_debug)));
//    }
//
//    @Override
//    protected void logW(String msg) {
//        super.logW(msg);
//        appendToLogs(toColor(msg, getResources().getColor(R.color.log_warning)));
//    }
//
//    @Override
//    protected void logW(String msg, Throwable e) {
//        super.logW(msg, e);
//        appendToLogs(toColor(msg, getResources().getColor(R.color.log_warning)));
//    }
//
//    @Override
//    protected void logE(String msg, Throwable e) {
//        super.logE(msg, e);
//        appendToLogs(toColor(msg, getResources().getColor(R.color.log_error)));
//    }
//
//    private void appendToLogs(CharSequence msg) {
//        mDebugLogView.append("\n");
//        mDebugLogView.append(DateFormat.format("hh:mm", System.currentTimeMillis()) + ": ");
//        mDebugLogView.append(msg);
//    }
//
//    private static CharSequence toColor(String msg, int color) {
//        SpannableString spannable = new SpannableString(msg);
//        spannable.setSpan(new ForegroundColorSpan(color), 0, msg.length(), 0);
//        return spannable;
//    }
//
//    // TODO: Provisorsich CPU auslesen ****************************************/
//    /* maximum speeds.
//     *
//     * @return cpu frequency in MHz
//     */
//    public static int getMaxCPUFreqMHz() {
//
//        int maxFreq = -1;
//        try {
//
//            RandomAccessFile reader = new RandomAccessFile( "/sys/devices/system/cpu/cpu0/cpufreq/stats/time_in_state", "r" );
//
//            boolean done = false;
//            while ( ! done ) {
//                String line = reader.readLine();
//                if ( null == line ) {
//                    done = true;
//                    break;
//                }
//                String[] splits = line.split( "\\s+" );
//                assert ( splits.length == 2 );
//                int timeInState = Integer.parseInt( splits[1] );
//                if ( timeInState > 0 ) {
//                    int freq = Integer.parseInt( splits[0] ) / 1000;
//                    if ( freq > maxFreq ) {
//                        maxFreq = freq;
//                    }
//                }
//            }
//
//        } catch ( IOException ex ) {
//            // ex.printStackTrace();
//        }
//
//        return maxFreq;
//    }
//    /******************************************************************/
//    private static String generateRandomName() {
//        String name = "";
//        Random random = new Random();
//        for (int i = 0; i < 5; i++) {
//            name += random.nextInt(10);
//        }
//        return /*TODO*/getMaxCPUFreqMHz() + " " + name;
//    }
//
//    @SuppressWarnings("unchecked")
//    private static <T> T pickRandomElem(Collection<T> collection) {
//        return (T) collection.toArray()[new Random().nextInt(collection.size())];
//    }
//
//    /**
//     * Provides an implementation of Animator.AnimatorListener so that we only have to override the
//     * method(s) we're interested in.
//     */
//  /*private abstract static class AnimatorListener implements Animator.AnimatorListener {
//    @Override
//    public void onAnimationStart(Animator animator) {}
//
//    @Override
//    public void onAnimationEnd(Animator animator) {}
//
//    @Override
//    public void onAnimationCancel(Animator animator) {}
//
//    @Override
//    public void onAnimationRepeat(Animator animator) {}
//  }*/
//
//    /** States that the UI goes through. */
//    public enum State {
//        UNKNOWN,
//        DISCOVERING,
//        ADVERTISING,
//        CONNECTED,
//        FINDROOT
//    }
//
//    /* TODO //////////////////////////////////////////////////////////////////////////////////////// */
//    private static final int READ_REQUEST_CODE = 42;
//
//    /**
//     * Fires an intent to spin up the file chooser UI and select an image for sending to endpointId.
//     */
//    private void showImageChooser() {
//        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        intent.setType("image/*");
//        // intent.putExtra(ENDPOINT_ID_EXTRA, endpointId);
//        startActivityForResult(intent, READ_REQUEST_CODE);
//    }
//
//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
//        if (requestCode == READ_REQUEST_CODE
//                && resultCode == Activity.RESULT_OK
//                && resultData != null) {
//            // String endpointId = resultData.getStringExtra(ENDPOINT_ID_EXTRA);
//
//            // The URI of the file selected by the user.
//            Uri uri = resultData.getData();
//
//            Payload filePayload;
//            try {
//                // Open the ParcelFileDescriptor for this URI with read access.
//                ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
//                filePayload = Payload.fromFile(pfd);
//            } catch (FileNotFoundException e) {
//                Log.e("MyApp", "File not found", e);
//                return;
//            }
//
//            // Construct a simple message mapping the ID of the file payload to the desired filename.
//            String filenameMessage = filePayload.getId() + ":" + uri.getLastPathSegment();
//
//            // Send the filename message as a bytes payload.
//            Payload filenameBytesPayload =
//                    Payload.fromBytes(filenameMessage.getBytes(StandardCharsets.UTF_8));
//            // Nearby.getConnectionsClient(context).sendPayload(endpointId, filenameBytesPayload);
//            send(filenameBytesPayload);
//
//            // Finally, send the file payload.
//            // Nearby.getConnectionsClient(context).sendPayload(endpointId, filePayload);
//            send(filePayload);
//        }
//    }
}
