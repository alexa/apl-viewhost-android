/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.events;

import com.amazon.apl.android.Event;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.dependencies.ISendEventCallback;
import com.amazon.apl.enums.EventProperty;

import java.util.Map;


/**
 * APL Set Page Event
 * See @{link <a https://developer.amazon.com/docs/alexa-presentation-language/apl-standard-commands.html#setpage-command>
 * APL Command Specification</a>}
 */
public class SendEvent extends Event {
    private static final String TAG = "SendEvent";

    private final ISendEventCallback mSendEventCallback;

    /**
     * Constructs the Event.
     *
     * @param nativeHandle Handle to the native event.
     * @param rootContext  The root context for the event.
     */
    private SendEvent(long nativeHandle, RootContext rootContext, ISendEventCallback sendEventCallback) {
        super(nativeHandle, rootContext);
        mSendEventCallback = sendEventCallback;
    }


    static public SendEvent create(long nativeHandle, RootContext rootContext,
                                   ISendEventCallback sendEventCallback) {
        return new SendEvent(nativeHandle, rootContext, sendEventCallback);
    }

    /**
     * Execute the command.
     */
    @Override
    public void execute() {
        Object[] args = mProperties.get(EventProperty.kEventPropertyArguments);
        Map<String, Object> components = mProperties.get(EventProperty.kEventPropertyComponents);
        Map<String, Object> sources = mProperties.get(EventProperty.kEventPropertySource);
        // Our visual context may be up to 500 ms stale for performance reasons
        // so this pushes an update to the runtime.
        mRootContext.notifyVisualContext();
        mSendEventCallback.onSendEvent(args, components, sources);
    }

    /**
     * Terminate the event process.
     */
    @Override
    public void terminate() {

    }
}
