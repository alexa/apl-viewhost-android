/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.events;

import android.view.View;

import com.amazon.apl.android.Component;
import com.amazon.apl.android.Event;
import com.amazon.apl.android.RootContext;


/**
 * APL Focus Event
 * See @{link <a https://developer.amazon.com/docs/alexa-presentation-language/apl-standard-commands.html>
 *     APL Command Specification</a>}
 */
public class FocusEvent extends Event {

    /**
     * Constructs the Event.
     *
     * @param nativeHandle Handle to the native event.
     * @param rootContext The root context for the event.
     */
    private FocusEvent(long nativeHandle, RootContext rootContext) {
        super(nativeHandle, rootContext);
    }


    static public FocusEvent create(long nativeHandle, RootContext rootContext) {
        return new FocusEvent(nativeHandle, rootContext);
    }

    /**
     * Execute the command.
     */
    @Override
    public void execute() {
        // TODO handle request to clear focus when getComponent returns null.
        //  At present there are no use cases outside of the sample app, but
        //  this will be necessary to support multiple APL Widgets.
        Component component = getComponent();
        if(component != null && !component.isDisabled()) {
            View view = mRootContext.getViewPresenter().findView(component);
            view.requestFocus();
        }
    }

    /**
     * Terminate the event process.
     */
    @Override
    public void terminate() {

    }
}
