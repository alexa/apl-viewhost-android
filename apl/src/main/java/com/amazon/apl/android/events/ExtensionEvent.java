/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.events;

import com.amazon.apl.android.Event;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.dependencies.IExtensionEventCallback;
import com.amazon.apl.enums.EventProperty;

import java.util.Map;


/**
 * APL Extension Event
 * See @{link <a https://developer.amazon.com/docs/alexa-presentation-language/apl-standard-commands.html#custom-command>
 * APL Command Specification</a>}
 */
public class ExtensionEvent extends Event {
    private final IExtensionEventCallback mExtensionEventCallback;

    /**
     * Constructs the Event.
     *
     * @param nativeHandle        Handle to the native event.
     * @param rootContext         The root context for the event.
     * @param customEventCallback The consumer callback for event handling.
     */
    private ExtensionEvent(long nativeHandle, RootContext rootContext, IExtensionEventCallback customEventCallback) {
        super(nativeHandle, rootContext);
        mExtensionEventCallback = customEventCallback;
    }


    static public ExtensionEvent create(long nativeHandle, RootContext rootContext,
                                        IExtensionEventCallback customEventCallback) {
        return new ExtensionEvent(nativeHandle, rootContext, customEventCallback);
    }

    /**
     * Execute the command.
     */
    @Override
    public void execute() {

        String name = mProperties.get(EventProperty.kEventPropertyName);
        String uri = mProperties.get(EventProperty.kEventPropertyExtensionURI);
        Map<String, Object> source = mProperties.get(EventProperty.kEventPropertySource);
        Map<String, Object> custom = mProperties.get(EventProperty.kEventPropertyExtension);

        IExtensionEventCallback.IExtensionEventCallbackResult resultCallback =
                succeeded -> mRootContext.post(() -> resolve(succeeded ? 0 : 1));
        mExtensionEventCallback.onExtensionEvent(name, uri, this, source, custom, resultCallback);
    }

    /**
     * Terminate the event process.
     */
    @Override
    public void terminate() {

    }
}
