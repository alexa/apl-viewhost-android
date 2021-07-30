/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.events;

import android.graphics.Rect;

import com.amazon.apl.android.Event;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.Text;


/**
 * APL Set Page Event
 * See @{link <a https://developer.amazon.com/docs/alexa-presentation-language/apl-standard-commands.html#setpage-command>
 * APL Command Specification</a>}
 */
public class RequestFirstLineBounds extends Event {
    private static final String TAG = "RequestFirstLineBounds";

    /**
     * Constructs the Event.
     *
     * @param nativeHandle Handle to the native event.
     * @param rootContext  The root context for the event.
     */
    private RequestFirstLineBounds(long nativeHandle, RootContext rootContext) {
        super(nativeHandle, rootContext);
    }


    static public RequestFirstLineBounds create(long nativeHandle, RootContext rootContext) {
        return new RequestFirstLineBounds(nativeHandle, rootContext);
    }

    /**
     * Execute the command.
     */
    @Override
    public void execute() {
        Text text = (Text) getComponent();
        text.setCurrentKaraokeLine(mRootContext.getViewPresenter(), 0);
        Rect bounds = text.getLineBounds(0);
        resolve(bounds.left, bounds.top, bounds.width(), bounds.height());
    }

    /**
     * Terminate the event process.
     */
    @Override
    public void terminate() {

    }
}
