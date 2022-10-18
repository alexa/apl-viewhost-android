/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.events;

import com.amazon.apl.android.Component;
import com.amazon.apl.android.Event;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.Text;
import com.amazon.apl.enums.ComponentType;
import com.amazon.apl.enums.EventProperty;

public class LineHighlightEvent extends Event {
    /**
     * Constructs the Event.
     *
     * @param nativeHandle Handle to the native event.
     * @param rootContext  The root context for the event.
     */
    private LineHighlightEvent(long nativeHandle, RootContext rootContext) {
        super(nativeHandle, rootContext);
    }


    static public LineHighlightEvent create(long nativeHandle, RootContext rootContext) {
        return new LineHighlightEvent(nativeHandle, rootContext);
    }

    /**
     * Execute the command.
     */
    @Override
    public void execute() {
        int rangeStart = mProperties.getInt(EventProperty.kEventPropertyRangeStart);
        int rangeEnd = mProperties.getInt(EventProperty.kEventPropertyRangeEnd);

        Component component = getComponent();
        if (component.getComponentType() == ComponentType.kComponentTypeText) {
            Text text = (Text) component;

            if (rangeStart < 0) {
                removeLineHighlighting();
                resolve();
                return;
            }

            int line = text.getLineNumberByRange(rangeStart, rangeEnd);
            if (line >= 0) {
                text.setCurrentKaraokeLine(mRootContext.getViewPresenter(), line);
            }
        }
        resolve();
    }

    private void removeLineHighlighting() {
        Component component = getComponent();
        if (component.getComponentType() == ComponentType.kComponentTypeText) {
            Text text = (Text) component;
            text.setCurrentKaraokeLine(mRootContext.getViewPresenter(), null);
            text.invalidateNullLine(mRootContext.getViewPresenter());
        }
    }

    /**
     * Terminate the event process.
     */
    @Override
    public void terminate() {
        removeLineHighlighting();
    }
}