package de.pbma.nearbyconnections;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.CallSuper;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.Strategy;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import de.pbma.nearfly.ExtMessage;

/**
 * This Activity has 5 {@link State}s.
 *
 * <p>{@link State#STANDBY}: We cannot do anything while we're in this state. The app is likely in
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
 *
 * <p>{@link State#FINDROOT}: Find the Root that shapes the RootNode of the Start-Network
 */

public class MyNearbyConnectionsClient extends MyNearbyConnectionsAbstract {
    private static final String STATE_UNKNOWN = "unknown";
    private static final String STATE_DISCOVERING = "discovering";
    private static final String STATE_ADVERTISING = "advertising";
    private static final String STATE_CONNECTED = "connected";
    private static final String STATE_FINDROOT = "findroot";

    private ArrayList<String> subscribedChannels = new ArrayList<>();
    /**
     * The connection strategy we'll use for Nearby Connections. In this case, we've decided on
     * P2P_STAR, which is a combination of Bluetooth Classic and WiFi Hotspots.
     */
    private static final Strategy STRATEGY = Strategy.P2P_STAR;

    /**
     * This service id lets us find other nearby devices that are interested in the same thing. Our
     * sample does exactly one thing, so we hardcode the ID.
     */
    // private String SERVICE_ID;

    /**
     * The state of the app. As the app changes states, the UI will update and advertising/discovery
     * will start/stop.
     */
    private State mState = State.FINDROOT;

    /**
     * A random UID used as this device's endpoint name.
     */
    private String mName;


    /**
     * Current state.
     */
    private String rootNode;

    /**
     * A Handler that allows us to post back on to the UI thread. We use this to resume discovery
     * after an uneventful bout of advertising.
     */
    // private final Handler mUiHandler = new Handler(Looper.getMainLooper());

    /**
     * Starts discovery. Used in a postDelayed manor with {@link #mUiHandler}.
     */
    /*private final Runnable mDiscoverRunnable =
            new Runnable() {
                @Override
                public void run() {
                    setState(State.DISCOVERING);
                }
            };*/

    /*public MyNearbyConnectionsClient(Context context, MyConnectionsListener myConnectionsListener){
        initService(context);
        this.myConnectionsListener = myConnectionsListener;
        mName = new EndpointNameGenerator().generateRandomName_if_not_in_sharedPref(context);
        rootNode = mName;
        this.SERVICE_ID = context.getClass().getCanonicalName();
    }*/

    /*public MyNearbyConnectionsClient(String SERVICE_ID){
        super(SERVICE_ID);
    }*/


    public void initClient(Context context, MyConnectionsListener myConnectionsListener, String SERVICE_ID) {
        initService(context);
        this.myConnectionsListener = myConnectionsListener;
        mName = new EndpointNameGenerator().generateRandomName_if_not_in_sharedPref(context);
        rootNode = mName;
        this.SERVICE_ID = SERVICE_ID;
    }

    // TODO: Listener that includes all relevant Informations
    public interface MyConnectionsListener {
        void onLogMessage(CharSequence msg);

        void onStateChanged(String state);

        void onRootNodeChanged(String rootNode);

        void onMessage(String channel, String msg);

        void onStream(Payload payload);

        void onBinary(Payload payload);

        void onFile(String path, String textAttachment);
    }

    MyConnectionsListener myConnectionsListener;

    /*public void initClient(Context context, MyConnectionsListener myConnectionsListener) {
        initService(context);
        this.myConnectionsListener = myConnectionsListener;
        mName = new EndpointNameGenerator().generateRandomName_if_not_in_sharedPref(context);
        rootNode = mName;
    }*/


    /**
     * Just One possible
     **/
    // public void registerNearbyListener(MyConnectionsListener myConnectionsListener){
    // }

    /*public void deregisterNearbyListener(MyConnectionsListener myConnectionsListener){
        if (myConnectionsListener.equals(this.myConnectionsListener))
            this.myConnectionsListener = null;
    }*/
    public void setRootNode(String endpointId) {
        rootNode = endpointId;
        if (myConnectionsListener != null)
            myConnectionsListener.onRootNodeChanged(endpointId);
    }


    public void startConnection() {
        // Call this at Start
        setState(State.STANDBY);
        setState(State.FINDROOT);
    }

    public void stopConnection() {
        // Call this at Stop
        setState(State.STANDBY);
    }

    public void onBackPressed() {
        /*if (getState() == State.CONNECTED || getState() == State.ADVERTISING) {
            setState(State.FINDROOT);
            return;
        }*/
    }

    @Override
    protected void onEndpointDiscovered(Endpoint endpoint) {
        // TODO 2903
        /*if (getName().compareTo(rootNode)>0){
            rootNode = getName(); // self
        }

        if (endpoint.getName().compareTo(rootNode) > 0){
            rootNode = endpoint.getName();
            disconnectFromAllEndpoints();
            connectToEndpoint(endpoint);
            setState(State.CONNECTED);
        }*/

        // We found an advertiser!
        /*if (!isConnecting()) {
            /** TODO 2503: Advertiser gefunden -> Wechsle zum Discovering
             Gehe in den Discovery-Zustand wenn gemerkt das jemand geeigneter ist**/
            /*if (endpoint.getName().compareTo(getName()) > 0){
                if (getState() != State.NODE)
                    setState(State.NODE);
                    setRootNode(endpoint.getName());

                // Nur senden, wenn anderer Root
                connectToEndpoint(endpoint);
            }*/
        // Nur senden, wenn anderer Root
        if (getState() == State.FINDROOT) {
            findOutRightState(endpoint.getName());
            connectToEndpoint(endpoint);
        }

        if (endpoint.getName().compareTo(rootNode) > 0) {
            disconnectFromAllEndpoints();
            stopAllEndpoints();
            connectToEndpoint(endpoint);
        }
        Log.v("qua", getDiscoveredEndpoints().toString());


        // if (getState()==State.NODE){

        //}
    }

    @Override
    protected void onConnectionInitiated(Endpoint endpoint, ConnectionInfo connectionInfo) {
        // TODO 2503: Discovering Device found -> Switch to Advertising Mode
        /*if (getName().compareTo(endpoint.getName()) > 0) {
            if (getState() == State.FINDROOT && getState() != State.ADVERTISING)
                setState(State.ADVERTISING);
        }*/
        /*if (getName().compareTo(rootNode)>0){
            rootNode = getName(); // self
        }*/
        // Gehe in Discovery, wenn jemand geeigneter ist
        /*if (getName().compareTo(endpoint.getName()) > 0) {
            if (getState() == State.FINDROOT)
                setState(State.ROOT);
        }else{
            setState(State.STANDBY);
            setState(State.NODE);
        }*/

        // if (endpoint.getName().compareTo(getName()) < 0 ) {
        // A connection to another device has been initiated! We'll accept the connection immediately.
        acceptConnection(endpoint);
        if (getState() == State.FINDROOT)
            findOutRightState(endpoint.getName());

        if (getState() == State.NODE)
            setState(State.CONNODE);
        // }

    }

    public void findOutRightState(String endpointId) {
        if (endpointId.compareTo(getName()) > 0) {
            setRootNode(endpointId);

            setState(State.NODE); // Wechselt in Zustand Node
        } else {
            setState(State.ROOT);
        }
    }

    @Override
    protected void onEndpointConnected(Endpoint endpoint) {
        Toast.makeText(
                context, "Connected to " + endpoint.getName(), Toast.LENGTH_SHORT)
                .show();

        /* TODO */
        /*if (!isAdvertising())
            rootNode = endpoint.getName();
        else
            rootNode = "self";

        if (!isAdvertising())
            myConnectionsListener.onRootNodeChanged(endpoint.getName());
        else
            myConnectionsListener.onRootNodeChanged("self");*/
        /*if (endpoint.getName().compareTo(rootNode) > 0){
            setRootNode(endpoint.getName());
        }*/
    }

    @Override
    protected void onEndpointDisconnected(Endpoint endpoint) {
        Toast.makeText(
                context, "Disconnected from " + endpoint.getName(), Toast.LENGTH_SHORT)
                .show();

        // If we lost all our endpoints, then we should reset the state of our app and go back
        // to our initial state (discovering).
        // if (getConnectedEndpoints().isEmpty()) {
        if (rootNode != getName()) {
            // TODO: New init State
            setState(State.FINDROOT);
            setRootNode(getName());
        }
    }

    @Override
    protected void onConnectionFailed(Endpoint endpoint) {
        // Let's try someone else.
        if (!getDiscoveredEndpoints().isEmpty())
            connectToEndpoint(pickRandomElem(getDiscoveredEndpoints()));
        setState(State.FINDROOT);/*
        if (getState() == State.NODE && !getDiscoveredEndpoints().isEmpty()) {
            connectToEndpoint(pickRandomElem(getDiscoveredEndpoints()));

            // Occurs only for Nodes, cause Root don't ask
            //disconnectFromAllEndpoints();
            //stopAllEndpoints();
            setState(State.FINDROOT);

            /*connectionAttemp++; /* TODO *//*
        }*/

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
        if (myConnectionsListener != null)
            myConnectionsListener.onStateChanged(state.toString());

        State oldState = mState;
        mState = state;
        onStateChanged(oldState, state);
    }

    /**
     * @return The current state.
     */
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
            case NODE:
                if (isAdvertising()) {
                    stopAdvertising();
                }
                // TODO: startDiscovery failed vermeiden, um overhead zu vermeiden
                // disconnectFromAllEndpoints();
                if (!isDiscovering())
                    startDiscovering();
                break;
            case ROOT:
                if (isDiscovering()) {
                    stopDiscovering();
                }
                // disconnectFromAllEndpoints();
                if (!isAdvertising())
                    startAdvertising();
                break;
            /*case CONNODE: // Cost Less Battery
                if (isDiscovering()) {
                    stopDiscovering();
                }*/
            case STANDBY:
                /*if (isAdvertising()) {
                    stopAdvertising();
                }
                if (isDiscovering()) {
                    stopDiscovering();
                }
                disconnectFromAllEndpoints()*/
                stopAllEndpoints();
                /*if (isDiscovering()) {
                    stopDiscovering();
                }
                if (isAdvertising())
                    stopAdvertising();
                stopAllEndpoints();*/
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

    public void publishIt(String channel, String message) {
        ExtMessage extMessage = new ExtMessage(message, channel, ExtMessage.STRING);
        send(Payload.fromBytes(extMessage.getBytes()));
        logD(message + " published");
    }

    public void pubFile(String channel, Uri uri, String textAttachment) {
        /*ExtMessage extMessage = new ExtMessage(message, channel);*/

        // Get ParcelFileDescriptor
        // Uri uri = Uri.fromFile(file);
        ContentResolver cr = context.getContentResolver();
        ParcelFileDescriptor pfd = null;
        // logD("####################### MIME:" + cr.getType(uri));
        String fileExtension = cr.getType(uri).split("\\/")[1]; // MimeType e.g. image/jpeg

        try {
            pfd = cr.openFileDescriptor(uri, "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Payload fileAsPayload = Payload.fromFile(pfd);
        // String fileExtension =

        String fileinformations = fileExtension + "/" + fileAsPayload.getId() + "/" + textAttachment;
        // Payload channelPayload = Payload.fromBytes(fileinformations.getBytes(StandardCharsets.UTF_8));

        ExtMessage extMessage = new ExtMessage(fileinformations, channel, ExtMessage.FILEINFORMATION);
        // send(channelPayload);
        send(Payload.fromBytes(extMessage.getBytes()));
        send(fileAsPayload);

        logD(fileAsPayload.toString() + " published");
    }

    public void pubBinaryTST(byte[] bytes) {
        send(Payload.fromStream(new ByteArrayInputStream(bytes)));
        logD(bytes + " published");
    }

    private byte[] ByteUnboxing(Byte[] bytes) {
        byte[] primitiveBytes = new byte[bytes.length];
        int j = 0;

        for (Byte b : primitiveBytes)
            bytes[j++] = b.byteValue();

        return primitiveBytes;
    }

    public void pubStream(Payload stream) {
        send(stream);
        logD(stream.toString() + " published");
    }

    public void subscribe(String channel) {
        if (!subscribedChannels.contains(channel))
            subscribedChannels.add(channel);
    }

    public void unsubscribe(String channel) {
        subscribedChannels.remove(channel);
    }

    /**
     * {@see ConnectionsActivity#onReceive(Endpoint, Payload)}
     */
    @Override
    @CallSuper
    protected void onReceive(Endpoint endpoint, Payload payload) {
        // logD(new String(payload.asBytes()) + "from" + endpoint);
        ExtMessage msg = ExtMessage.createExtMessage(payload);

        if (subscribedChannels.contains(msg.getChannel()) && myConnectionsListener != null) {
            myConnectionsListener.onMessage(msg.getChannel(), msg.getPayload());
        }

        /*if (payload.getType() == Payload.Type.STREAM)
            myConnectionsListener.onStream(payload);
        else
            // myConnectionsListener.onMessage(new String(payload.asBytes()));
            myConnectionsListener.onBinary(payload);*/
    }

    @Override
    protected void onFile(Endpoint endpoint, String path, String textAttachment) {
        myConnectionsListener.onFile(path, textAttachment);
    }

    @Override
    protected void onDiscoveryFailed() {
        super.onDiscoveryFailed();
        setState(State.STANDBY);
        setState(State.FINDROOT);
        // mUiHandler.removeCallbacksAndMessages(null);
    }

    /**
     * Queries the phone's contacts for their own profile, and returns their name. Used when
     * connecting to another device.
     */
    @Override
    protected String getName() {
        return mName;
    }

    @Override
    protected void logV(String msg) {
        super.logV(msg);
        if (myConnectionsListener != null)
            myConnectionsListener.onLogMessage(toColor(msg, 0xFFFFFFFF));
    }

    @Override
    protected void logD(String msg) {
        super.logD(msg);
        if (myConnectionsListener != null)
            myConnectionsListener.onLogMessage(toColor(msg, 0xFFEEEEEE));
    }

    @Override
    protected void logW(String msg) {
        super.logW(msg);
        if (myConnectionsListener != null)
            myConnectionsListener.onLogMessage(toColor(msg, 0xFFE57373));
    }

    @Override
    protected void logW(String msg, Throwable e) {
        super.logW(msg, e);
        if (myConnectionsListener != null)
            myConnectionsListener.onLogMessage(toColor(msg, 0xFFE57373));
    }

    @Override
    protected void logE(String msg, Throwable e) {
        super.logE(msg, e);
        if (myConnectionsListener != null)
            myConnectionsListener.onLogMessage(toColor(msg, 0xFFF44336));
    }

    private static CharSequence toColor(String msg, int color) {
        SpannableString spannable = new SpannableString(msg);
        spannable.setSpan(new ForegroundColorSpan(color), 0, msg.length(), 0);
        return spannable;
    }

    @SuppressWarnings("unchecked")
    private static <T> T pickRandomElem(Collection<T> collection) {
        return (T) collection.toArray()[new Random().nextInt(collection.size())];
    }

    /**
     * possible States of Application
     */
    public enum State {
        STANDBY,
        NODE,
        ROOT,
        FINDROOT,
        CONNODE
    }

    public boolean isConnected() {
        if (getState() == State.CONNODE || getState() == State.ROOT) {
            return true;
        }
        return false;
    }
}
