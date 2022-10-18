/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.events;

import android.graphics.Rect;

import com.amazon.apl.android.Event;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.Text;
import com.amazon.apl.enums.EventProperty;

public class RequestLineBoundsEvent extends Event {

    /**
     * Constructs the Event.
     *
     * @param nativeHandle Handle to the native event.
     * @param rootContext  The root context for the event.
     */
    private RequestLineBoundsEvent(long nativeHandle, RootContext rootContext) {
        super(nativeHandle, rootContext);
    }


    static public RequestLineBoundsEvent create(long nativeHandle, RootContext rootContext) {
        return new RequestLineBoundsEvent(nativeHandle, rootContext);
    }

    /**
     * Execute the command.
     */
    @Override
    public void execute() {
        int rangeStart = mProperties.getInt(EventProperty.kEventPropertyRangeStart);
        int rangeEnd = mProperties.getInt(EventProperty.kEventPropertyRangeEnd);

        Text text = (Text) getComponent();
        Rect bounds = new Rect();

        int line = text.getLineNumberByRange(rangeStart, rangeEnd);
        if (line >= 0) {
            bounds = text.getLineBounds(line);
        }
        resolve(bounds.left, bounds.top, bounds.width(), bounds.height());
    }

    /**
     * Terminate the event process.
     */
    @Override
    public void terminate() {

    }
}