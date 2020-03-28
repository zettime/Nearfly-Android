package com.google.location.nearby.apps.walkietalkie;

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
public class MainActivityOldNew extends MainActivityWorking {

}
