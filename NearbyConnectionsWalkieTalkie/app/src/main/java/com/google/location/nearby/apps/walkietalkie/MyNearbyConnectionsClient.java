package com.google.location.nearby.apps.walkietalkie;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collection;
import java.util.Random;

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
public class MyNearbyConnectionsClient extends MyNearbyConnectionsAbstract {
    /**
     * The connection strategy we'll use for Nearby Connections. In this case, we've decided on
     * P2P_STAR, which is a combination of Bluetooth Classic and WiFi Hotspots.
     */
    private static final Strategy STRATEGY = Strategy.P2P_STAR;

    /** Acceleration required to detect a shake. In multiples of Earth's gravity. */
    private static final float SHAKE_THRESHOLD_GRAVITY = 2;

    /**
     * Advertise for 30 seconds before going back to discovering. If a client connects, we'll continue
     * to advertise indefinitely so others can still connect.
     */
    private static final long ADVERTISING_DURATION = 60000;

    /**
     * This service id lets us find other nearby devices that are interested in the same thing. Our
     * sample does exactly one thing, so we hardcode the ID.
     */
    private static final String SERVICE_ID =
            "com.google.location.nearby.apps.walkietalkie.manual.SERVICE_ID";

    /**
     * The state of the app. As the app changes states, the UI will update and advertising/discovery
     * will start/stop.
     */
    private State mState = State.UNKNOWN;

    /** A random UID used as this device's endpoint name. */
    private String mName;

    /** Displays the previous state during animation transitions. */
    private TextView mPreviousStateView;

    /** Displays the current state. */
    // private TextView mCurrentStateView;
    // TODO 2503
    // private TextView tvCurrentState;
    private String rootNode;
    /**
     * A Handler that allows us to post back on to the UI thread. We use this to resume discovery
     * after an uneventful bout of advertising.
     */
    private final Handler mUiHandler = new Handler(Looper.getMainLooper());

    /** Starts discovery. Used in a postDelayed manor with {@link #mUiHandler}. */
    private final Runnable mDiscoverRunnable =
            new Runnable() {
                @Override
                public void run() {
                    setState(State.DISCOVERING);
                }
            };

    // TODO: Listener that includes all relevant Informations
    public interface MyConnectionsListener{
        void onLogMessage(CharSequence msg);
        void onStateChanged(String state);
        void onRootNodeChanged(String rootNode);
        void onMessage(String msg);
    }
    MyConnectionsListener myConnectionsListener;

    // TODO: NearbyConnections Callback Listener
    PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
            myConnectionsListener.onMessage(new String(payload.asBytes()));
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String s, @NonNull PayloadTransferUpdate payloadTransferUpdate) {}
    };


    public void onCreate(Context context, MyConnectionsListener myConnectionsListener) {
        this.myConnectionsListener = myConnectionsListener;
        initService(context);
        mName = generateRandomName();
    }


    public void onStart() {
        // Call this at Start
        setState(State.FINDROOT);
    }

    public void onStop() {
        // Call this at Stop
        setState(State.UNKNOWN);
    }

    public void onBackPressed() {
        if (getState() == State.CONNECTED || getState() == State.ADVERTISING) {
            setState(State.FINDROOT);
            return;
        }
    }

    @Override
    protected void onEndpointDiscovered(Endpoint endpoint) {
        // We found an advertiser!
        if (!isConnecting()) {

            // TODO 2503: Advertiser gefunden -> Wechsle zum Discovering
            if (endpoint.getName().compareTo(getName()) > 0 ){
                if (getState() == State.FINDROOT && getState() != State.DISCOVERING)
                    setState(State.DISCOVERING);
                if (getState() == State.CONNECTED && getState() != State.DISCOVERING)
                    setState(State.FINDROOT);

                // Nur senden, wenn anderer Root
                connectToEndpoint(endpoint);
            }
        }
    }

    @Override
    protected void onConnectionInitiated(Endpoint endpoint, ConnectionInfo connectionInfo) {
        // TODO 2503: Discovering Device found -> Switch to Advertising Mode
        if (getName().compareTo(endpoint.getName()) > 0) {
            if (getState() == State.FINDROOT && getState() != State.ADVERTISING)
                setState(State.ADVERTISING);
        }

        // A connection to another device has been initiated! We'll accept the connection immediately.
        acceptConnection(endpoint);
    }

    @Override
    protected void onEndpointConnected(Endpoint endpoint) {
        Toast.makeText(
                context, "Connected to " + endpoint.getName(), Toast.LENGTH_SHORT)
                .show();
        setState(State.CONNECTED);

        /* TODO */
        if (!isAdvertising())
            rootNode = endpoint.getName();
        else
            rootNode = "self";

        if (!isAdvertising())
            myConnectionsListener.onRootNodeChanged(endpoint.getName());
        else
            myConnectionsListener.onRootNodeChanged("self");
    }

    @Override
    protected void onEndpointDisconnected(Endpoint endpoint) {
        Toast.makeText(
                context, "Disconnected from " + endpoint.getName(), Toast.LENGTH_SHORT)
                .show();

        // If we lost all our endpoints, then we should reset the state of our app and go back
        // to our initial state (discovering).
        if (getConnectedEndpoints().isEmpty()) {
            // TODO: New init State
            setState(State.FINDROOT);
        }
    }

    @Override
    protected void onConnectionFailed(Endpoint endpoint) {
        // Let's try someone else.
        if (getState() == State.DISCOVERING && !getDiscoveredEndpoints().isEmpty()) {
            connectToEndpoint(pickRandomElem(getDiscoveredEndpoints()));
            setState(State.FINDROOT);
            /*connectionAttemp++; /* TODO */
        }

        /* TODO: Endpoint seems not to be an advisor */
    /*if (connectionAttemp>1){
      setState(State.FINDROOT);
      connectionAttemp = 0;
    }*/
    }

    /**
     * The state has changed. I wonder what we'll be doing now.
     *
     * @param state The new state.
     */
    private void setState(State state) {
        if (mState == state) {
            logW("State set to " + state + " but already in that state");
            return;
        }

        logD("State set to " + state);
        // TODO: 2503
        // tvCurrentState.setText(state.toString()
        // );
        myConnectionsListener.onStateChanged(state.toString());

        State oldState = mState;
        mState = state;
        onStateChanged(oldState, state);
    }

    /** @return The current state. */
    private State getState() {
        return mState;
    }

    /**
     * State has changed.
     *
     * @param oldState The previous state we were in. Clean up anything related to this state.
     * @param newState The new state we're now in. Prepare the UI for this state.
     */
    private void onStateChanged(State oldState, State newState) {

        // Update Nearby Connections to the new state.
        switch (newState) {
            case DISCOVERING:
                if (isAdvertising()) {
                    stopAdvertising();
                }
                // TODO: startDiscovery failed vermeiden, um overhead zu vermeiden
                // disconnectFromAllEndpoints();
                if (!isDiscovering())
                    startDiscovering();
                break;
            case ADVERTISING:
                if (isDiscovering()) {
                    stopDiscovering();
                }
                // disconnectFromAllEndpoints();
                startAdvertising();
                break;
            case CONNECTED:
                if (isDiscovering()) {
                    // stopDiscovering();
                } else if (isAdvertising()) {
                    // Continue to advertise, so others can still connect,
                    // but clear the discover runnable.
                    removeCallbacks(mDiscoverRunnable);
                }
                break;
            case UNKNOWN:
                stopAllEndpoints();
                break;
            // TODO 2303
            case FINDROOT:
                if (isAdvertising()) {
                    stopAdvertising();
                }
                if (isDiscovering()) {
                    stopDiscovering();
                }
                disconnectFromAllEndpoints();
                startAdvertising();
                startDiscovering();
            default:
                // no-op
                break;
        }
    }

    /*public void startAdvertising(View view){
        setState(State.ADVERTISING);
        postDelayed(mDiscoverRunnable, ADVERTISING_DURATION);
    }*/

    public void pubIt(String channel, String message){
        ExtMessage extMessage = new ExtMessage(message, channel);
        send(Payload.fromBytes(extMessage.getBytes()));
        logD(message + " published");
    }

    /** {@see ConnectionsActivity#onReceive(Endpoint, Payload)} */
    @Override
    protected void onReceive(Endpoint endpoint, Payload payload) {
        // logD(new String(payload.asBytes()) + "from" + endpoint);
    }

    @Override
    protected void onDiscoveryFailed() {
        super.onDiscoveryFailed();
        setState(State.UNKNOWN);
        setState(State.DISCOVERING);
        mUiHandler.removeCallbacksAndMessages(null);
    }

    /**
     * Queries the phone's contacts for their own profile, and returns their name. Used when
     * connecting to another device.
     */
    @Override
    protected String getName() {
        return mName;
    }

    /** {@see ConnectionsActivity#getServiceId()} */
    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    /** {@see ConnectionsActivity#getStrategy()} */
    @Override
    public Strategy getStrategy() {
        return STRATEGY;
    }

    /** {@see Handler#post()} */
    protected void post(Runnable r) {
        mUiHandler.post(r);
    }

    /** {@see Handler#postDelayed(Runnable, long)} */
    protected void postDelayed(Runnable r, long duration) {
        mUiHandler.postDelayed(r, duration);
    }

    /** {@see Handler#removeCallbacks(Runnable)} */
    protected void removeCallbacks(Runnable r) {
        mUiHandler.removeCallbacks(r);
    }

    @Override
    protected void logV(String msg) {
        super.logV(msg);
        myConnectionsListener.onLogMessage(toColor(msg, 0xFFFFFFFF));
    }

    @Override
    protected void logD(String msg) {
        super.logD(msg);
        myConnectionsListener.onLogMessage(toColor(msg, 0xFFEEEEEE));
    }

    @Override
    protected void logW(String msg) {
        super.logW(msg);
        myConnectionsListener.onLogMessage(toColor(msg, 0xFFE57373));
    }

    @Override
    protected void logW(String msg, Throwable e) {
        super.logW(msg, e);
        myConnectionsListener.onLogMessage(toColor(msg, 0xFFE57373));
    }

    @Override
    protected void logE(String msg, Throwable e) {
        super.logE(msg, e);
        myConnectionsListener.onLogMessage(toColor(msg, 0xFFF44336));
    }

    private static CharSequence toColor(String msg, int color) {
        SpannableString spannable = new SpannableString(msg);
        spannable.setSpan(new ForegroundColorSpan(color), 0, msg.length(), 0);
        return spannable;
    }

    // TODO: Provisorsich CPU auslesen ****************************************/
    /* maximum speeds.
     *
     * @return cpu frequency in MHz
     */
    public static int getMaxCPUFreqMHz() {

        int maxFreq = -1;
        try {

            RandomAccessFile reader = new RandomAccessFile( "/sys/devices/system/cpu/cpu0/cpufreq/stats/time_in_state", "r" );

            boolean done = false;
            while ( ! done ) {
                String line = reader.readLine();
                if ( null == line ) {
                    done = true;
                    break;
                }
                String[] splits = line.split( "\\s+" );
                assert ( splits.length == 2 );
                int timeInState = Integer.parseInt( splits[1] );
                if ( timeInState > 0 ) {
                    int freq = Integer.parseInt( splits[0] ) / 1000;
                    if ( freq > maxFreq ) {
                        maxFreq = freq;
                    }
                }
            }

        } catch ( IOException ex ) {
            // ex.printStackTrace();
        }

        return maxFreq;
    }
    /******************************************************************/
    private static String generateRandomName() {
        String name = "";
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            name += random.nextInt(10);
        }
        return /*TODO*/getMaxCPUFreqMHz() + " " + name;
    }

    @SuppressWarnings("unchecked")
    private static <T> T pickRandomElem(Collection<T> collection) {
        return (T) collection.toArray()[new Random().nextInt(collection.size())];
    }

    /** possible States of Application */
    public enum State {
        UNKNOWN,
        DISCOVERING,
        ADVERTISING,
        CONNECTED,
        FINDROOT
    }
}
