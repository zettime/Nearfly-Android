package com.google.location.nearby.apps.walkietalkie;

import com.google.android.gms.nearby.connection.Payload;

public class MsgForwarder implements Runnable {
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
