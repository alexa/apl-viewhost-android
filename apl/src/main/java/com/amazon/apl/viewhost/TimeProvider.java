/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost;

import java.lang.ref.WeakReference;

/**
 * Provides time and timezone-related information to the viewhost.
 */
public interface TimeProvider {
    /**
     * Registers a new listener instance. This listener should be notified when the local time offset is adjusted,
     * e.g. as a result of daylight savings.
     *
     * @param listener The listener to be notified of changes
     */
    void addListener(WeakReference<LocalTimeOffsetListener> listener);

    /**
     * @return The offset in milliseconds between the local time and the UTC time.
     */
    long getLocalTimeOffset();

    /**
     * @return The number of milliseconds since the UTC epoch.
     */
    long getUTCTime();

    /**
     * Listener used to notify the viewhost of changes to the current time, e.g. in response to daylight savings updates.
     */
    interface LocalTimeOffsetListener {
        void onLocalTimeOffsetUpdated(long newOffset);
    }
}
