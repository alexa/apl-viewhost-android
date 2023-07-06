/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Defines the contract and base implementation for instances that support storing user data. If an
 * instance implements this contract, it guarantees that user data will be released before it is
 * destroyed, at the latest. Specific implementations may choose to release data earlier, however
 * (e.g. data associated with an APL document can be released when the document is permanently
 * removed from the screen).
 *
 * All calls from the base implementation are thread-safe.
 */
@ThreadSafe
public class UserDataHolder {
    private Object mUserData;

    /**
     * @return @c true if user data is currently set, @c false otherwise
     */
    public synchronized boolean hasUserData() {
        return mUserData != null;
    }

    /**
     * Returns user data previously set via #setUserData, or @c null if no data was previously set
     * or previously set data has already been released.
     *
     * @return @c the user data previously set, or @c nullptr
     */
    public synchronized Object getUserData() {
        return mUserData;
    }

    /**
     * Attempts to associates user data with the current instance. This data is never read by the
     * viewhost. 
     *
     * @param data New user data to set
     * @return @c true if the (possibly null) data was stored, @c false if this instance no longer
     * accepts user data
     */
    public synchronized boolean setUserData(Object data) {
        mUserData = data;
        return true;
    }
}
