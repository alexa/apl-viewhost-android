/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.events;

import com.amazon.apl.android.Event;
import com.amazon.apl.android.RootContext;

/**
 *  Document config requires to be refreshed. This usually includes checking if content isWaiting()
 *  and subsequently resolving required packages.
 */
public class RefreshEvent extends Event {

    private static final String TAG = "RefreshEvent";

    /**
     * Constructs the Event.
     *
     * @param nativeHandle Handle to the native event.
     * @param rootContext  RootContext.
     */
    private RefreshEvent(long nativeHandle, RootContext rootContext) {
        super(nativeHandle, rootContext);
    }

    static public RefreshEvent create(long nativeHandle, RootContext rootContext) {
        return new RefreshEvent(nativeHandle, rootContext);
    }

    @Override
    public void execute() {
        resolve();
    }

    @Override
    public void terminate() {
    }
}
