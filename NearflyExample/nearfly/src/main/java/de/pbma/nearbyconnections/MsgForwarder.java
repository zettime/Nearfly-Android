package de.pbma.nearbyconnections;

import com.google.android.gms.nearby.connection.Payload;

@Deprecated
class MsgForwarder implements Runnable {
    Payload payload;
    String excludedEntpointId;

    public MsgForwarder(Payload payload, String excludedEntpointId) {
        this.payload = payload;
        this.excludedEntpointId = excludedEntpointId;
    }

    @Override
    public void run() {

    }
}