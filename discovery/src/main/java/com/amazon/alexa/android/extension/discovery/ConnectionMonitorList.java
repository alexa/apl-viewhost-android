/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.alexa.android.extension.discovery;

import android.os.Build;
import android.os.IInterface;
import android.os.RemoteCallbackList;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class is augments {@link RemoteCallbackList} to provide accessor methods
 * suitable for api levels < 26.
 *
 * @param <E>
 */
@SuppressWarnings("unused")
public class ConnectionMonitorList<E extends IInterface, C> extends RemoteCallbackList<E> {

    /**
     * Get a registered connection based on the cookie used at registration.
     *
     * @param cookie Optional additional data to be associated with this callback.
     * @return The object registered for the cookie.  Null if no registration exists,
     * the registered connection died, or the cookie is null.
     */
    synchronized public E get(final C cookie) {

        if (cookie == null) {
            return null;
        }

        E result = null;


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // unfortunately we can't lock on more narrow object.  The internal
            // death callback for RemoteCallbackList modifies the callback collection
            // and is not accessed from this class

            // find existing instance using registration access
            for (int idx = 0; idx < getRegisteredCallbackCount(); idx++) {

                /*
                  Return any cookie associated with a currently registered callback.
                  This method returns the current cookie registered at the given
                  index.  This means that it is not itself thread-safe:
                  any call to {@link #register} or {@link #unregister} will change these indices,
                  so we must sync the object.
                 */
                Object c = getRegisteredCallbackCookie(idx);
                if (c.equals(cookie)) {
                    result = getRegisteredCallbackItem(idx);
                    break;
                }
            }

        } else {

            /*
              This creates a copy of the callback list, which can retrieve items
              from using {@link #getBroadcastItem}. Any call to {@link #register} or
              {@link #unregister} will change these indices, so we must sync on the whole object.

              Additionally,  only one broadcast can be active at a time, we need to block access
              to new broadcast creation.
             */
            final int N = beginBroadcast();
            for (int idx = 0; idx < N; idx++) {

                Object c = getBroadcastCookie(idx);
                if (c.equals(cookie)) {
                    result = getBroadcastItem(idx);
                    break;
                }
            }
            finishBroadcast();
        }

        return result;

    }


    /**
     * Get a registered connection based on the index.
     *
     * @param index The index
     * @return The object registered for the cookie.  Null if no registration exists,
     * the registered connection died, or the cookie is null.
     */
    synchronized public E getByIndex(int index) {

        if (index >= getRegisteredCallbackCount()) {
            return null;
        }

        E result;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            result = getRegisteredCallbackItem(index);
        } else {

            /*
              This creates a copy of the callback list, which can retrieve items
              from using {@link #getBroadcastItem}. Any call to {@link #register} or
              {@link #unregister} will change these indices, so we must sync on the whole object.

              Additionally,  only one broadcast can be active at a time, we need to block access
              to new broadcast creation.
             */
            final int N = beginBroadcast();
            result = getBroadcastItem(index);
            finishBroadcast();
        }

        return result;

    }


    /**
     * Overrides super class for use with Generics.
     *
     * @param callback the monitored object
     * @param cookie   the assigned cookie
     */
    @Override
    public void onCallbackDied(E callback, Object cookie) {
        //noinspection unchecked
        onDied(callback, (C) cookie);
    }


    /**
     * Called when the monitored object dies.
     *
     * @param callback the monitored object
     * @param cookie   the assigned cookie
     */
    public void onDied(E callback, final C cookie) {

    }


    AtomicBoolean mKilled = new AtomicBoolean(false);

    /**
     * Disable this monitor list.  All registered callbacks are unregistered,
     * and the list is disabled so that future calls to {@link #register} will
     * fail.
     *
     * @see #register
     */
    public void kill() {
        mKilled.set(true);
        super.kill();
    }


    /**
     * @return true when this monitor has been killed.
     */
    public boolean isKilled() {
        return mKilled.get();
    }
}
