package de.pbma.nearbyconnections;

import android.content.Context;
import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static de.pbma.nearfly.Constants.TAG;

/**
 * A class that connects to Nearby Connections and provides convenience methods and callbacks.
 */
abstract class NeConEssentials {
    /** Don't changed this **/
    private final boolean DEBUG = true;
    /**
     * Our handler to Nearby Connections.
     */
    private ConnectionsClient mConnectionsClient;

    /** A random UID used as this device's endpoint name. */// TODO
    // private String mName;

    /**
     * The devices we've discovered near us.
     */
    private final Map<String, Endpoint> mDiscoveredEndpoints = new ConcurrentHashMap<>();

    // TODO
    /*private ArrayList<String> mNodeSubscriptionList;
    public getNodeSubscriptionList(){
        return mNodeSubscriptionList
    }*/


    // TODO
    protected Context mContext;

    // My Subscriptions
    /*private final ArrayList<String> mSubscribedChannels = new ArrayList<>();
    public final ArrayList<String> getSubscribedChannels(){
        return mSubscribedChannels;
    }*/

    /**
     * The devices we have pending connections to. They will stay pending until we call {@link
     * #acceptConnection(Endpoint)} or {@link #rejectConnection(Endpoint)}.
     */
    private final Map<String, Endpoint> mPendingConnections = new ConcurrentHashMap<>();

    /**
     * The devices we are currently connected to. For advertisers, this may be large. For discoverers,
     * there will only be one entry in this map.
     * TODO: Concurrent for visability
     */
    public final Map<String, Endpoint> mEstablishedConnections = new ConcurrentHashMap<>();

    /**
     * True if we are asking a discovered device to connect to us. While we ask, we cannot ask another
     * device.
     */
    private boolean mIsConnecting = false;
    private boolean mCachedDiscovery = false;

    private boolean mIsDiscovering = false;
    private boolean mIsAdvertising = false;

    // Failed for QoS
    // private final ConcurrentLinkedQueue<Long> payloadSendBuffer = new ConcurrentLinkedQueue<>();

    /** Stop discvering while connecting -> helps to accelerate the connecting-phase **/
    private void setConnecting(boolean tmpConnecting){
        if (mIsConnecting==tmpConnecting)
            return;

        if (tmpConnecting){
            mCachedDiscovery = mIsDiscovering;
            stopDiscovering();
            mIsConnecting=true;
        }else {
            if (mCachedDiscovery==true && mIsDiscovering==false)
                startDiscovering();
            mIsConnecting=false;
        }
    }


    /**
     * Callbacks for connections to other devices.
     */
    private final ConnectionLifecycleCallback mConnectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    /** Connection is requested **/
                    logD(
                            String.format(
                                    "onConnectionInitiated(endpointId=%s, endpointName=%s)",
                                    endpointId, connectionInfo.getEndpointName()));
                    Endpoint endpoint = new Endpoint(endpointId, connectionInfo.getEndpointName());
                    mPendingConnections.put(endpointId, endpoint);
                    NeConEssentials.this.onConnectionInitiated(endpoint, connectionInfo);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    logD(String.format("onConnectionResponse(endpointId=%s, result=%s)", endpointId, result));

                    // We're no longer connecting
                    setConnecting(false);

                    if (!result.getStatus().isSuccess()) {
                        logW(
                                String.format(
                                        "Connection failed. Received status %s.",
                                        NeConEssentials.toString(result.getStatus())));
                        onConnectionFailed(mPendingConnections.remove(endpointId));
                        return;
                    }
                    connectedToEndpoint(mPendingConnections.remove(endpointId));
                }

                @Override
                public void onDisconnected(String endpointId) {
                    if (!mEstablishedConnections.containsKey(endpointId)) {
                        logW("Unexpected disconnection from endpoint " + endpointId);
                        return;
                    }
                    disconnectedFromEndpoint(mEstablishedConnections.get(endpointId));
                }
            };

    /**
     * Callbacks for payloads (bytes of data) sent from another device to us.
     */
    private PayloadCallback mPayloadCallback;
    protected abstract void forwardFile(String endpointId, Payload payload,
                                        String channel, String path,
                                        String textAttachment);
    /**
     * ******************************************
     */

    public void initService(Context context) {
        this.mContext = context;
        mConnectionsClient = Nearby.getConnectionsClient(context);
        mPayloadCallback = new PayloadReceiver(context) {
            @Override
            public void onFile(String endpointId, String channel, String path, String textAttachment) {
                NeConEssentials.this.onFile(mEstablishedConnections.get(endpointId),
                        channel, path, textAttachment);
            }

            @Override
            public void forwardFile(String endpointId, Payload payload, String channel, String path, String textAttachment) {
                if (getConnectedEndpoints().size()>1)
                    NeConEssentials.this.forwardFile(endpointId, payload, channel, path, textAttachment);
            }

            @Override
            public void onByteMessage(String endpointId, NeCon.BytesMessage bytesMessage) {
                onReceive(mEstablishedConnections.get(endpointId), bytesMessage);
            }

            @Override
            public void onControlMessage(String endpointId, NeCon.ControlMessage controlMessage) {
                NeConEssentials.this.onControl(endpointId, controlMessage);
            }

            @Override
            protected void onBigBytes(String channel, byte[] bigBytes) {
                NeConEssentials.this.onBigBytes(channel, bigBytes);
            }
        };
    }

    /**
     * Sets the device to advertising mode. It will broadcast to other devices in discovery mode.
     * Either {@link #onAdvertisingStarted()} or {@link #onAdvertisingFailed()} will be called once
     * we've found out if we successfully entered this mode.
     */
    protected void startAdvertising() {
        mIsAdvertising = true;
        final String localEndpointName = getName();

        AdvertisingOptions.Builder advertisingOptions = new AdvertisingOptions.Builder();
        advertisingOptions.setStrategy(getStrategy());

        mConnectionsClient
                .startAdvertising(
                        localEndpointName,
                        getServiceId(),
                        mConnectionLifecycleCallback,
                        advertisingOptions.build())
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unusedResult) {
                                logV("Now advertising endpoint " + localEndpointName);
                                onAdvertisingStarted();
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                mIsAdvertising = false;
                                logW("startAdvertising() failed.", e);
                                onAdvertisingFailed();
                            }
                        });
    }

    /**
     * Stops advertising.
     */
    protected void stopAdvertising() {
        mIsAdvertising = false;
        mConnectionsClient.stopAdvertising();
    }

    /**
     * Returns {@code true} if currently advertising.
     */
    protected boolean isAdvertising() {
        return mIsAdvertising;
    }

    /**
     * Called when advertising successfully starts. Override this method to act on the event.
     */
    protected void onAdvertisingStarted() {
    }

    /**
     * Called when advertising fails to start. Override this method to act on the event.
     */
    protected void onAdvertisingFailed() {
    }

    /**
     * Called when a pending connection with a remote endpoint is created. Use {@link ConnectionInfo}
     * for metadata about the connection (like incoming vs outgoing, or the authentication token). If
     * we want to continue with the connection, call {@link #acceptConnection(Endpoint)}. Otherwise,
     * call {@link #rejectConnection(Endpoint)}.
     */
    protected void onConnectionInitiated(Endpoint endpoint, ConnectionInfo connectionInfo) {
    }

    /**
     * Accepts a connection request.
     */
    protected void acceptConnection(final Endpoint endpoint) {

        mConnectionsClient
                .acceptConnection(endpoint.getId(), mPayloadCallback)
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                logW("acceptConnection() failed.", e);
                            }
                        });
    }

    /**
     * Rejects a connection request.
     */
    protected void rejectConnection(Endpoint endpoint) {
        mConnectionsClient
                .rejectConnection(endpoint.getId())
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                logW("rejectConnection() failed.", e);
                            }
                        });
    }

    /**
     * Sets the device to discovery mode. It will now listen for devices in aRdvertising mode. Either
     * {@link #onDiscoveryStarted()} or {@link #onDiscoveryFailed()} will be called once we've found
     * out if we successfully entered this mode.
     */
    protected void startDiscovering() {
        mIsDiscovering = true;
        // mDiscoveredEndpoints.clear();
        DiscoveryOptions.Builder discoveryOptions = new DiscoveryOptions.Builder();
        discoveryOptions.setStrategy(getStrategy());
        mConnectionsClient
                .startDiscovery(
                        getServiceId(),
                        new EndpointDiscoveryCallback() {
                            @Override
                            public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                                if (getServiceId().equals(info.getServiceId())) {
                                    Endpoint endpoint = new Endpoint(endpointId, info.getEndpointName());
                                    // connectToEndpoint(endpoint);
                                    mDiscoveredEndpoints.put(endpointId, endpoint);
                                    // logV("dadada", "list: " + mDiscoveredEndpoints.toString());
                                    onEndpointDiscovered(endpoint);
                                }

                                logD(
                                        String.format(
                                                "onEndpointFound(endpointId=%s, serviceId=%s, endpointName=%s)",
                                                endpointId, info.getServiceId(), info.getEndpointName()));
                            }

                            @Override
                            public void onEndpointLost(String endpointId) {
                                logD(String.format("onEndpointLost(endpointId=%s)", endpointId));
                                // TODO: Delete endpoints from list
                                if (mDiscoveredEndpoints.containsKey(endpointId))
                                    mDiscoveredEndpoints.remove(endpointId);
                            }
                        },
                        discoveryOptions.build())
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unusedResult) {
                                onDiscoveryStarted();
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                mIsDiscovering = false;
                                logW("startDiscovering() failed.", e);
                                onDiscoveryFailed();
                            }
                        });
    }

    /**
     * Stops discovery.
     */
    protected void stopDiscovering() {
        mIsDiscovering = false;
        mConnectionsClient.stopDiscovery();
    }

    /**
     * Returns {@code true} if currently discovering.
     */
    protected boolean isDiscovering() {
        return mIsDiscovering;
    }

    /**
     * Called when discovery successfully starts. Override this method to act on the event.
     */
    protected void onDiscoveryStarted() {
    }

    /**
     * Called when discovery fails to start. Override this method to act on the event.
     */
    protected void onDiscoveryFailed() {
    }

    /**
     * Called when a remote endpoint is discovered. To connect to the device, call {@link
     * #connectToEndpoint(Endpoint)}.
     */
    protected void onEndpointDiscovered(Endpoint endpoint) {
    }

    /**
     * Disconnects from the given endpoint.
     */
    protected void disconnect(Endpoint endpoint) {
        mConnectionsClient.disconnectFromEndpoint(endpoint.getId());
        mEstablishedConnections.remove(endpoint.getId());
    }

    /**
     * Disconnects from all currently connected endpoints.
     */
    protected void disconnectFromAllEndpoints() {
        for (Endpoint endpoint : mEstablishedConnections.values()) {
            mConnectionsClient.disconnectFromEndpoint(endpoint.getId());
        }

        mEstablishedConnections.clear();
    }

    /**
     * Resets and clears all state in Nearby Connections.
     */
    protected void stopAllEndpoints() {
        if (mConnectionsClient==null)
            return;

        mConnectionsClient.stopAllEndpoints();
        mIsAdvertising = false;
        mIsDiscovering = false;
        setConnecting(false);
        mDiscoveredEndpoints.clear();
        mPendingConnections.clear();
        mEstablishedConnections.clear();
    }

    /**m
     * Sends a connection request to the endpoint. Either {@link #onConnectionInitiated(Endpoint,
     * ConnectionInfo)} or {@link #onConnectionFailed(Endpoint)} will be called once we've found out
     * if we successfully reached the device.
     */
    protected void connectToEndpoint(final Endpoint endpoint) {
        logV("Sending a connection request to endpoint " + endpoint);
        // Mark ourselves as connecting so we don't connect multiple times
        setConnecting(true);

        // Ask to connect
        mConnectionsClient
                .requestConnection(getName(), endpoint.getId(), mConnectionLifecycleCallback)
                .addOnFailureListener(
                        e -> {
                            logW("requestConnection() failed.", e);
                            setConnecting(false);
                            onConnectionFailed(endpoint);
                        });
    }

    /**
     * Returns {@code true} if we're currently attempting to connect to another device.
     */
    protected final boolean isConnecting() {
        return mIsConnecting;
    }

    private void connectedToEndpoint(Endpoint endpoint) {
        logD(String.format("connectedToEndpoint(endpoint=%s)", endpoint));
        // changeConnectionState(true);
        if (endpoint.getId()==null) // Error occurs sometimes in LG?
            return;

        mEstablishedConnections.put(endpoint.getId(), endpoint);

        onEndpointConnected(endpoint);
    }

    private void disconnectedFromEndpoint(Endpoint endpoint) {
        logD(String.format("disconnectedFromEndpoint(endpoint=%s)", endpoint));
        mEstablishedConnections.remove(endpoint.getId());

        // changeConnectionState(false);

        onEndpointDisconnected(endpoint);
    }

    /**
     * Called when a connection with this endpoint has failed.
     */
    protected void onConnectionFailed(Endpoint endpoint) {
    }

    /**
     * Called when someone has connected to us.
     */
    protected void onEndpointConnected(Endpoint endpoint) {
    }

    /**
     * Called when someone has disconnected.
     */
    protected void onEndpointDisconnected(Endpoint endpoint) {
    }

    /**
     * Returns a list of currently connected endpoints.
     */
    protected Set<Endpoint> getDiscoveredEndpoints() {
        return new HashSet<>(mDiscoveredEndpoints.values());
    }

    /**
     * Returns a list of currently connected endpoints.
     */
    protected Set<Endpoint> getConnectedEndpoints() {
        return new HashSet<>(mEstablishedConnections.values());
    }

    /**
     * Returns a list of currently connected endpoints.
     */
    protected Set<Endpoint> getPendingEndpoints() {
        return new HashSet<>(mPendingConnections.values());
    }



    /**
     * Sends a {@link Payload} to all currently connected endpoints.
     *
     * @param payload The data you want to send.
     */
    protected void send(Payload payload) {
        send(payload, mEstablishedConnections.keySet());
    }

    /** TODO: just try out **/
    public void sendSingle(Payload payload, String entpointId) {
        mConnectionsClient
                .sendPayload(entpointId, payload)
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                logW("sendPayload() failed.", e);
                            }
                        });
    }

    private void send(Payload payload, Set<String> endpoints) {
        ArrayList<String> entpointsList = new ArrayList<>(endpoints);
        // payloadSendBuffer.add(payload.getId());
        // TODO: provisorisch
        // entpointsList.add(id);

        mConnectionsClient
                .sendPayload(entpointsList, payload)
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                logW("sendPayload() failed.", e);
                            }
                        });
    }

    protected void send(Payload payload, ArrayList<String> endpointList){
        mConnectionsClient
                .sendPayload(endpointList, payload)
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                logW("sendPayload() failed.", e);
                            }
                        });
    }

    protected void onReceive(Endpoint endpoint, NeCon.BytesMessage bytesMessage) {
    }

    // TODO
    protected void onFile(Endpoint endpoint, String channel, String path, String textAttachment) {
    }

    protected void onBigBytes(String channel, byte[] bigBytes) {
    }

    protected void onControl(String endpoint, NeCon.ControlMessage controlMessage){
    }

    /** Returns the client's name. Visible to others when connecting. */
    // protected abstract String getName();

    /**
     * Returns the service id. This represents the action this connection is for. When discovering,
     * we'll verify that the advertiser has the same service id before we consider connecting to them.
     */
    // protected abstract String getServiceId();

    /**
     * Returns the strategy we use to connect to other devices. Only devices using the same strategy
     * and service id will appear when discovering. Stragies determine how many incoming and outgoing
     * connections are possible at the same time, as well as how much bandwidth is available for use.
     */
    // protected abstract Strategy getStrategy();

    /**
     * Transforms a {@link Status} into a English-readable message for logging.
     *
     * @param status The current status
     * @return A readable String. eg. [404]File not found.
     */
    private static String toString(Status status) {
        return String.format(
                Locale.US,
                "[%d]%s",
                status.getStatusCode(),
                status.getStatusMessage() != null
                        ? status.getStatusMessage()
                        : ConnectionsStatusCodes.getStatusCodeString(status.getStatusCode()));
    }


    @CallSuper
    protected void logV(String msg) {
        if (DEBUG)
            Log.v(TAG, msg);
    }

    @CallSuper
    protected void logD(String msg) {
        if (DEBUG)
            Log.d(TAG, msg);
    }

    @CallSuper
    protected void logW(String msg) {
        if (DEBUG)
            Log.w(TAG, msg);
    }

    @CallSuper
    protected void logW(String msg, Throwable e) {
        if (DEBUG)
            Log.w(TAG, msg, e);
    }

    @CallSuper
    protected void logE(String msg, Throwable e) {
        if (DEBUG)
            Log.e(TAG, msg, e);
    }

    private void logE(String msg){
        if (DEBUG)
            Log.e(TAG, msg);
    }

    /**
     * Represents a device we can talk to.
     */
    protected static class Endpoint {
        @NonNull
        private final String id;
        @NonNull
        private final String name;

        public  Endpoint(@NonNull String id, @NonNull String name) {
            this.id = id;
            this.name = name;
        }

        @NonNull
        public String getId() {
            return id;
        }

        @NonNull
        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Endpoint) {
                Endpoint other = (Endpoint) obj;
                return id.equals(other.id);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public String toString() {
            return String.format("Endpoint{id=%s, name=%s}", id, name);
        }
    }

    // TODO: VON AKTIVITY (NAME ERSTELLEN)
    /**
     * This service id lets us find other nearby devices that are interested in the same thing. Our
     * sample does exactly one thing, so we hardcode the ID.
     */
    static String SERVICE_ID;/*=
            "com.google.location.nearby.apps.walkietalkie.manual.SERVICE_ID";*/

    /**
     * A random UID used as this device's endpoint name.
     */
    private String mName;

    /**
     * Queries the phone's contacts for their own profile, and returns their name. Used when
     * connecting to another device.
     */
    protected String getName() {
        return mName;
    }

    /**
     * {@see ConnectionsActivity#getServiceId()}
     */
    public String getServiceId() {
        return SERVICE_ID;
    }

    /**
     * The used connection strategy for Nearby Connections. In this case P2P_STAR
     * (combination of Bluetooth Classic and WiFi Hotspots).
     */
    private static final Strategy STRATEGY = Strategy.P2P_STAR;

    /**
     * {@see ConnectionsActivity#getStrategy()}
     */
    public Strategy getStrategy() {
        return STRATEGY;
    }
}
