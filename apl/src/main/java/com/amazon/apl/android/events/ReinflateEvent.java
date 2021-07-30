/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.events;

import com.amazon.apl.android.Event;
import com.amazon.apl.android.RootContext;

public class ReinflateEvent extends Event {

    private static final String TAG = "ReinflateEvent";
    /**
     * Constructs the Event.
     *
     * @param nativeHandle Handle to the native event.
     * @param rootContext The root context for the event.
     */
    private ReinflateEvent(long nativeHandle, RootContext rootContext) {
        super(nativeHandle, rootContext);
    }

    static public ReinflateEvent create(long nativeHandle, RootContext rootContext) {
        return new ReinflateEvent(nativeHandle, rootContext);
    }

    /**
     * Execute the reinflate command.
     */
    @Override
    public void execute() {
        mRootContext.reinflate();
    }

    /**
     * Terminate the event process.
     */
    @Override
    public void terminate() {

    }
}
