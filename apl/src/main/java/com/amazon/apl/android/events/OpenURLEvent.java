/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.events;

import com.amazon.apl.android.Event;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.dependencies.IOpenUrlCallback;
import com.amazon.apl.enums.EventProperty;


/**
 * APL OpenUrl Event
 * See @{link <a https://developer.amazon.com/docs/alexa-presentation-language/apl-standard-commands.html>
 *     APL Command Specification</a>}
 */
public class OpenURLEvent extends Event {

    private static IOpenUrlCallback mOpenURLCallback;

    /**
     * Constructs the Event.
     *  @param nativeHandle Handle to the native event.
     * @param rootContext The root context for the event.
     * @param openURLCallback Callback for URL open.
     */
    private OpenURLEvent(long nativeHandle, RootContext rootContext, IOpenUrlCallback openURLCallback) {
        super(nativeHandle, rootContext);
        mOpenURLCallback = openURLCallback;
    }


    static public OpenURLEvent create(long nativeHandle, RootContext rootContext,
                                      IOpenUrlCallback openURLCallback) {
        return new OpenURLEvent(nativeHandle, rootContext, openURLCallback);
    }

    /**
     * Execute the command.
     */
    @Override
    public void execute() {
        String url = mProperties.getString(EventProperty.kEventPropertySource);
        mOpenURLCallback.onOpenUrl(url, succeeded -> {
            mRootContext.post(() -> {
                resolve(succeeded ? 0 : 1);
            });
        });
    }

    /**
     * Terminate the event process.
     */
    @Override
    public void terminate() {

    }
}
