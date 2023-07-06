/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.events;

import com.amazon.apl.android.Event;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.dependencies.ISendEventCallbackV2;
import com.amazon.apl.enums.EventProperty;

import java.util.Map;


/**
 * APL Send Event
 * See @{link <a https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-standard-commands.html#sendevent-command>
 * APL Command Specification</a>}
 */
public class SendEvent extends Event {
    private static final String TAG = "SendEvent";

    private ISendEventCallbackV2 mSendEventCallback;

    /**
     * Constructs the Event.
     *
     * @param nativeHandle Handle to the native event.
     * @param rootContext  The root context for the event.
     */
    private SendEvent(long nativeHandle, RootContext rootContext, ISendEventCallbackV2 sendEventCallback) {
        super(nativeHandle, rootContext);
        mSendEventCallback = sendEventCallback;
    }


    static public SendEvent create(long nativeHandle, RootContext rootContext,
                                   ISendEventCallbackV2 sendEventCallback) {
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
        Map<String, Object> flags = mProperties.get(EventProperty.kEventPropertyFlags);
        // Our visual context may be up to 500 ms stale for performance reasons
        // so this pushes an update to the runtime.
        mRootContext.notifyContext();
        mSendEventCallback.onSendEvent(args, components, sources, flags);
    }

    /**
     * Terminate the event process.
     */
    @Override
    public void terminate() {

    }

    /**
     * A helper function for the new viewhost abstraction to take over handling of this event
     */
    public void overrideCallback(ISendEventCallbackV2 callback) {
        mSendEventCallback = callback;
    }
}
