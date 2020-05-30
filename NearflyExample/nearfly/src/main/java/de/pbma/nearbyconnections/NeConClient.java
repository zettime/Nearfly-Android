package de.pbma.nearbyconnections;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.widget.Toast;

import androidx.annotation.CallSuper;
import androidx.annotation.IntDef;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.Payload;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * This Activity has 6 {@link NodeState}s.
 *
 * <p>{@link #STATE_STANDBY}: We cannot do anything while we're in this STATE_ The app is likely in
 * the background.
 *
 * <p>{@link #STATE_FINDROOT}: Our default state (after we've connected). We constantly listen for
 * a device to advertise near us.
 *
 * <p>{@link #STATE_ROOT}: This Node is the central component of the star-shaped network.
 *
 * <p>{@link #STATE_NODE}: This Node actively searches for root nodes.  For this he is
 * Discovering all the time.
 *
 * <p>{@link #STATE_CONNODE}: This Node found and is connected to a root-node. He continues
 *  discovering for a while, to see if a better noed joins that can be the new root.
 *
 * <p>{@link #STATE_BACKOFF}: This Node is trying to connect to a root, but failed already one
 * time. Maybe another node is connecting therefor he waits withing the {@link #BACKOFFF_TIME}
 *
 */

public class NeConClient extends NeConEssentials {

    private static final long INITIAL_DISCOVERY_TIME = 1000;
    private static final long SECONDS_TO_CONNODE_ECO = 60;
    private static final int RANDRANGE_COLAVOID = 1000;// AntiCollision Random max Range
    private static final String TAG = "NeConClient";
    private final int BACKOFFF_TIME = 1000;

    @Retention(SOURCE)
    @IntDef({STATE_FINDROOT, STATE_ROOT, STATE_NODE, STATE_CONNODE, STATE_STANDBY, STATE_BACKOFF})
    public @interface NodeState{}

    private static final int STATE_FINDROOT = 1;
    private static final int STATE_ROOT = 2;
    private static final int STATE_NODE = 3;
    private static final int STATE_CONNODE = 4;
    private static final int STATE_BACKOFF = 5;
    private static final int STATE_STANDBY = 6;

    private long mClientStartedTime = 0;

    private ArrayList<String> mSubscribedChannels = new ArrayList<>();

    /** AtomicInteger so that the async sleep-threads gain visibility **/
    private AtomicInteger mState = new AtomicInteger(STATE_STANDBY);

    // TODO: PublishForwarder
    ThreadPoolExecutor msgForwardExecutor;
    // PublishForwarder publishForwarder;
    public NeCon neCon = new NeCon();

    /**
     * A random UID used as this device's endpoint name.
     */
    private String mName;


    /*public void initClient(String SERVICE_ID) {
    }*/

    /*public NeConClient(){
        mName = new EndpointNameGenerator().generateRandomName_if_not_in_sharedPref(mContext);
        // FOR DEBUG PURPOSE
        // mName = new EndpointNameGenerator().getNamePendentFromTime();
        logV("MyEndpointName: "+mName);

        this.SERVICE_ID = SERVICE_ID;
        mClientStartedTime = System.currentTimeMillis();
    }*/

    public void registerListener(Context context, NeConListener neConListener){
        initService(context);
        this.mNeConListener = neConListener;
    }

    public void deregisterListener(){
        mNeConListener = null;
    }

    // TODO: Listener that includes all relevant Informations
    public interface NeConListener {
        void onLogMessage(CharSequence msg);

        void onStateChanged(String state);

        void onRootNodeChanged(String rootNode);

        void onMessage(String channel, byte[] message);

        void onStream(Payload payload);

        void onBinary(Payload payload);

        void onFile(String channel, String path, String textAttachment);
    }

    NeConListener mNeConListener;

    public NeConClient(){
        mName = new EndpointNameGenerator().generateRandomName();
        if (mName.matches("18.*"))
            mName = "0000 0000"+new Random().nextInt(10);

        // FOR DEBUG PURPOSE
        // mName = new EndpointNameGenerator().getNamePendentFromTimeTEST();
        // mName = new EndpointNameGenerator().getNamePendentFromTime();
        mClientStartedTime = System.currentTimeMillis();
        logV("MyEndpointName: "+mName);
    }

    /** Call this to Start the auto network creation process. **/
    public void startConnection(String SERVICE_ID) {
        this.SERVICE_ID = SERVICE_ID;

        setState(STATE_FINDROOT);
    }

    /** Call this to disconnect from other nodes, stop advertisment
     * and stop discovering.
     */
    public void stopConnection() {
        // Call this at Stop
        setState(STATE_STANDBY);
    }

    Endpoint root = new Endpoint("null", mName);

    private Endpoint findRoot() {
        logV("Searching Root...");

        Endpoint maxNode = new Endpoint("null", mName);

        for (Endpoint endpoint : getDiscoveredEndpoints()) {
            if (endpoint.getName().compareTo(maxNode.getName())>0)
                maxNode = endpoint;
        }

        logV("...found "+ maxNode);
        return maxNode;
    }

    /**
     * Connect to The Root of discovered Entpoints after delay time ends.
     * tries not to overlap with other nodes by waiting additionally a random time
     * **/
    private void connectToRoot(final Endpoint other, long delay){
        new Thread(() -> {
            if (isConnecting())
                return;

            final long waitTime = delay+new Random().nextInt(RANDRANGE_COLAVOID);
            logV("time COLAVOID: "+waitTime);
            if (!sleep(waitTime, STATE_NODE))
                return;

            root = findRoot();
            connectToEndpoint(root);
        }).start();
    }

    /***** TESTST *****/
    @Override
    protected void onEndpointDiscovered(Endpoint other) {
        /** TODO ------------------------------ **/
        // FIND OUT IF OTHER ROOT?
        if (getState()==STATE_FINDROOT) {
            if (other.getName().compareTo(mName) > 0){
                setState(STATE_NODE);
                // Wait before conenction if you can find annother node
                if ((mClientStartedTime-System.currentTimeMillis())<5000)
                    connectToRoot(other, INITIAL_DISCOVERY_TIME );
                else
                    connectToRoot(other, 0);
            }
        }

        if (getState()==STATE_CONNODE ){
            if (other.getName().compareTo(mName) > 0){
                setState(STATE_NODE);
                connectToRoot(other, 0);
            }
        }
    }

    private boolean tmpConnected = false;
    protected void changeConnectionState(boolean isConnected) {
        if (tmpConnected==isConnected)
                return;
        else
            tmpConnected=isConnected;

        mNeConListener.onLogMessage(isConnected?"connected":"disconnected");
    }

    @Override
    protected void onConnectionInitiated(Endpoint other, ConnectionInfo connectionInfo) {
        acceptConnection(other);
    }

    @Override
    protected void onEndpointConnected(Endpoint other) {
        Toast.makeText(
                mContext, "Connected to " + other.getName(), Toast.LENGTH_SHORT)
                .show();

        if (getConnectedEndpoints().size()==1)
            changeConnectionState(true);

        // Set new root
        if (getState() == STATE_NODE || getState() == STATE_BACKOFF){
            setState(STATE_CONNODE);
            root = other;
            attemptsInBackoff.set(0);
        }

        /** Only Roots are here still in FINDROOT-State **/
        if (getState() == STATE_FINDROOT)
            setState(STATE_ROOT);


        // TODO: Start the Executor *****************************************************
        if (getConnectedEndpoints().size() > 1) {
            if (msgForwardExecutor == null) {
                msgForwardExecutor = (ThreadPoolExecutor)
                        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 5);
                logE("Executor started");
            }
        }
        /**********************************************************************/
    }

    @Override
    protected void onEndpointDisconnected(Endpoint endpoint) {
        Toast.makeText(
                mContext, "Disconnected from " + endpoint.getName(), Toast.LENGTH_SHORT)
                .show();

        if (getConnectedEndpoints().isEmpty())
            changeConnectionState(false);

        if (getState() == STATE_ROOT && getConnectedEndpoints().isEmpty())
            setState(STATE_FINDROOT);

        if (getState() == STATE_CONNODE)
            setState(STATE_FINDROOT);

        /** Shutdown Executor ***/
        if (msgForwardExecutor != null) {
            msgForwardExecutor.shutdownNow();
            msgForwardExecutor = null;
            logE("Executor ended");
        }
    }

    /** Returns false if not anymore in the same State **/
    private boolean sleep(long millis, @NodeState int state){
        long checkTime = 500;

        try {
            for(int i=0; i<millis/checkTime; i++){
                Thread.sleep(checkTime);
                // logV("th "+state);

                if (getState() != state)
                    return false;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return true;
    }

    public AtomicInteger attemptsInBackoff = new AtomicInteger(0);
    @Override
    protected void onConnectionFailed(Endpoint endpoint) {
        // Let's try someone else.

        if (getState()==STATE_NODE)
            setState(STATE_BACKOFF);

        if (getState()!=STATE_BACKOFF)
            return;

        if (!getDiscoveredEndpoints().isEmpty() &&  attemptsInBackoff.get()<3){
            new Thread(() -> {
                final long waitTime = BACKOFFF_TIME+new Random().nextInt(BACKOFFF_TIME);
                logV("time BACKOFF: "+waitTime);
                if (!sleep(waitTime, STATE_BACKOFF))
                    return;

                root = findRoot();
                if (getState()==STATE_BACKOFF & !getConnectedEndpoints().contains(root) && !isConnecting()){
                    connectToEndpoint(root);
                    logV("attemptsInBackoff: "+ attemptsInBackoff.incrementAndGet());
                }
                /*else if (getState()==STATE_BACKOFF & attemptsInBackoff.get()%2==0){
                    setState(STATE_STANDBY);
                    setState(STATE_FINDROOT);
                }*/
            }).start();
        }
        else if (attemptsInBackoff.get()>3){
            setState(STATE_STANDBY);
            setState(STATE_FINDROOT);
            attemptsInBackoff.set(0);
        }
    }

    /**
     * The state has changed. I wonder what we'll be doing now.
     *
     * @param state The new STATE_
     */
    private void setState(@NodeState int state) {
        if (mState.get() == state) {
            logW("State set to " + getStateAsString(state) + " but already in that state");
            return;
        }

        logD("State set to " + getStateAsString(state));

        if (mNeConListener != null)
            mNeConListener.onStateChanged(getStateAsString(state));

        mState.set(state);
        onStateChanged(state);
    }

    private String getStateAsString(int state){
        switch (state){
            case STATE_FINDROOT: return "FINDROOT";
            case STATE_ROOT: return "ROOT";
            case STATE_NODE: return "NODE";
            case STATE_CONNODE: return "CONNODE";
            case STATE_BACKOFF: return "BACKOFF";
            case STATE_STANDBY: return "STANDBY";
            default: throw new RuntimeException("State not known");
        }
    }

    /**
     * @return The current STATE_
     */
    private int getState() {
        return mState.get();
    }

    private void disconnectFromAllEndpointsA(){
        disconnectFromAllEndpoints();
        changeConnectionState(false);
    }

    private void onStateChanged(@NodeState int newState) {

        // Update Nearby Connections to the new STATE_
        switch (newState) {
            case STATE_NODE:
                disconnectFromAllEndpointsA(); // NODES are not connected
                if (isAdvertising()) {
                    stopAdvertising();
                }
                // TODO: startDiscovery failed vermeiden, um overhead zu vermeiden
                if (!isDiscovering())
                    startDiscovering();
                break;
            case STATE_ROOT:
                if (isDiscovering()) {
                    stopDiscovering();
                }
                // disconnectFromAllEndpointsA();
                if (!isAdvertising())
                    startAdvertising();
                break;
            case STATE_CONNODE: // Cost Less Battery
                /*if (isDiscovering()) {
                    stopDiscovering();
                }*/
                new Thread(() -> { // TODO: provisorisch
                    if (!sleep(SECONDS_TO_CONNODE_ECO*1000, STATE_CONNODE))
                        return;

                    if (getState() == STATE_CONNODE && isDiscovering())
                         stopDiscovering();
                }).start();
                break;
            case STATE_STANDBY:
                if (isAdvertising()) {
                    stopAdvertising();
                }
                if (isDiscovering()) {
                    stopDiscovering();
                }
                disconnectFromAllEndpointsA();
                stopAllEndpoints();
                mClientStartedTime = 0;
                break;
            case STATE_FINDROOT:
                if (isAdvertising()) {
                    stopAdvertising();
                }
                if (isDiscovering()) {
                    stopDiscovering();
                }
                disconnectFromAllEndpointsA();
                stopAllEndpoints();

                startAdvertising();
                startDiscovering();
            default:
                // no-op
                break;
        }
    }

    public void publishIt(String channel, String message) {
        // NeConExtMessage neConExtMessage = new NeConExtMessage(message, channel, NeConExtMessage.BYTES);
        NeCon.BytesMessage textMessage = neCon.new BytesMessage(message.getBytes(), channel);
        logD(message + " published");
        // send(Payload.fromBytes(neConExtMessage.getBytes()));
        send(Payload.fromBytes(textMessage.getBytes()));
    }

    public void pubFile(String channel, Uri uri, String textAttachment) {
        /*NeConExtMessage neConExtMessage = new NeConExtMessage(message, channel);*/

        // Get ParcelFileDescriptor
        // Uri uri = Uri.fromFile(file);
        ContentResolver cr = mContext.getContentResolver();
        ParcelFileDescriptor pfd = null;
        // logD("####################### MIME:" + cr.getType(uri));
        String fileExtension = cr.getType(uri).split("\\/")[1]; // MimeType e.g. image/jpeg

        try {
            pfd = cr.openFileDescriptor(uri, "r");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Payload fileAsPayload = Payload.fromFile(pfd);
        // String fileinformations = fileExtension + "/" + fileAsPayload.getId() + "/" + textAttachment;

        // NeConExtMessage neConExtMessage = new NeConExtMessage(fileinformations, channel, NeConExtMessage.FILEINFORMATION);
        NeCon.FileInfMessage fileMessage = neCon.new FileInfMessage(channel, fileExtension,
                fileAsPayload.getId(), textAttachment);
        // send(Payload.fromBytes(neConExtMessage.getBytes()));
        send(Payload.fromBytes(fileMessage.getBytes()));
        send(fileAsPayload);

        logD(fileAsPayload.toString() + " published");
    }

    public void pubFileBytes(String channel, File file, String textAttachment) {
        Payload fileAsPayload = null;
        try {
            fileAsPayload = Payload.fromFile(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        String path = file.getAbsolutePath();
        String parts[] = path.split(".");
        String fileExtension = parts[parts.length-1];

        NeCon.FileInfMessage fileMessage = neCon.new FileInfMessage(channel, fileExtension,
                fileAsPayload.getId(), textAttachment);
        // send(Payload.fromBytes(neConExtMessage.getBytes()));
        send(Payload.fromBytes(fileMessage.getBytes()));
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
        if (!mSubscribedChannels.contains(channel))
            mSubscribedChannels.add(channel);
    }

    public void unsubscribe(String channel) {
        mSubscribedChannels.remove(channel);
    }

    /**
     * {@see ConnectionsActivity#onReceive(Endpoint, Payload)}
     */
    @Override
    @CallSuper
    protected void onReceive(Endpoint endpoint, NeCon.BytesMessage bytesMessage) {
        // logD(new String(payload.asBytes()) + "from" + endpoint);
        // NeConExtMessage msg = NeConExtMessage.createExtMessage(payload);
        // NeCon.BytesMessage msg = neCon.createTextMessage(bytesMessage);

        if (mSubscribedChannels.contains(bytesMessage.getChannel()) && mNeConListener != null) {
            mNeConListener.onMessage(bytesMessage.getChannel(), bytesMessage.getPayload());
        }

        /*if (payload.getType() == Payload.Type.STREAM)
            mNeConListener.onStream(payload);
        else
            // mNeConListener.onMessage(new String(payload.asBytes()));
            mNeConListener.onBinary(payload);*/


        /** Executor **/
        //if (msgForwardExecutor!=null)
        if (getConnectedEndpoints().size()>1) // Only Root has more than 2 Connected Endpoints
            forward(bytesMessage, endpoint.getId());
            // publishForwarder.newMessage(payload, endpointId);

    }

    private void forward(final NeCon.BytesMessage bytesMessage, final String excludedEntpointId) {
        // logE(++mCnt + " - forwarding Message: " + new String(payload.asBytes()));
        // final long starttime = System.currentTimeMillis();

        /* TODO: Removes Sender from list to avoid a endless loop */
        ArrayList<String> broadcastList = new ArrayList<>();

        for (Endpoint endpoint: getConnectedEndpoints()){
            if (!endpoint.getId().equals(excludedEntpointId)){
                broadcastList.add(endpoint.getId());
            }
        }
        //msgForwardExecutor.execute(() -> {
        send(Payload.fromBytes(bytesMessage.getBytes()), broadcastList);
        //});
    }

    @Override
    protected void onFile(Endpoint endpoint, String channel, String path, String textAttachment) {
        if (mSubscribedChannels.contains(channel) && mNeConListener != null) {
            mNeConListener.onFile(channel, path, textAttachment);
        }
    }

    protected void forwardFile(String excludedEntpointId, Payload fileAsPayload, String channel, String path, String textAttachment){
        String[] temp = path.split("\\/");
        String[] dotdot = temp[temp.length-1].split("\\.");
        String fileExtension = dotdot[dotdot.length-1];

        ArrayList<String> broadcastList = new ArrayList<>();
        for (Endpoint endpoint: getConnectedEndpoints()){
            if (!endpoint.getId().equals(excludedEntpointId)){
                broadcastList.add(endpoint.getId());
            }
        }

        String fileinformations = fileExtension + "/" + fileAsPayload.getId() + "/" + textAttachment;
        // NeConExtMessage neConExtMessage = new NeConExtMessage(fileinformations, channel, NeConExtMessage.FILEINFORMATION);
        NeCon.FileInfMessage fileMessage = neCon.new FileInfMessage(channel, fileExtension, fileAsPayload.getId(), textAttachment);

        logV("forwarding file");
        // send(Payload.fromBytes(neConExtMessage.getBytes()), broadcastList);
        send(Payload.fromBytes(fileMessage.getBytes()));
        send(fileAsPayload, broadcastList);


        /*msgForwardExecutor.execute(() -> {
            send(Payload.fromBytes(neConExtMessage.getBytes()), broadcastList);
            send(fileAsPayload, broadcastList);
        });*/
    }
    /*************************************************************************/

    @Override
    protected void onDiscoveryFailed() {
        super.onDiscoveryFailed();
        setState(STATE_STANDBY);
        setState(STATE_FINDROOT);
    }

    @Override
    protected String getName() {
        return mName;
    }

    @Override
    protected void logV(String msg) {
        super.logV(msg);
        if (mNeConListener != null)
            mNeConListener.onLogMessage(toColor(msg, 0xFFFFFFFF));
    }

    @Override
    protected void logD(String msg) {
        super.logD(msg);
        if (mNeConListener != null)
            mNeConListener.onLogMessage(toColor(msg, 0xFFEEEEEE));
    }

    @Override
    protected void logW(String msg) {
        super.logW(msg);
        if (mNeConListener != null)
            mNeConListener.onLogMessage(toColor(msg, 0xFFE57373));
    }

    @Override
    protected void logW(String msg, Throwable e) {
        super.logW(msg, e);
        if (mNeConListener != null)
            mNeConListener.onLogMessage(toColor(msg, 0xFFE57373));
    }

    @Override
    protected void logE(String msg, Throwable e) {
        super.logE(msg, e);
        if (mNeConListener != null)
            mNeConListener.onLogMessage(toColor(msg, 0xFFF44336));
    }

    protected void logE(String msg) {
        if (mNeConListener != null)
            mNeConListener.onLogMessage(toColor(msg, 0xFFF44336));
    }

    private static CharSequence toColor(String msg, int color) {
        SpannableString spannable = new SpannableString(msg);
        spannable.setSpan(new ForegroundColorSpan(color), 0, msg.length(), 0);
        return spannable;
    }

    /*@SuppressWarnings("unchecked")
    private static <T> T pickRandomElem(Collection<T> collection) {
        return (T) collection.toArray()[new Random().nextInt(collection.size())];
    }*/

    public boolean isConnected() {
        return (!getConnectedEndpoints().isEmpty());
        /*
        if (getState() == STATE_CONNODE || getState() == STATE_ROOT) {
            return true;
        }
        return false;*/
    }
}