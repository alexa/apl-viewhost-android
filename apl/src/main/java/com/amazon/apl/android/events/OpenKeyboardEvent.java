/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.events;

import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

import com.amazon.apl.android.Component;
import com.amazon.apl.android.Event;
import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.RootContext;

/**
 * Open the keyboard.
 */
public class OpenKeyboardEvent extends Event {

    protected OpenKeyboardEvent(long nativeHandle, RootContext rootContext) {
        super(nativeHandle, rootContext);
    }

    public static OpenKeyboardEvent create(long nativeHandle, RootContext rootContext) {
        return new OpenKeyboardEvent(nativeHandle, rootContext);
    }

    @Override
    public void execute() {
        Component component = getComponent();
        if (component == null) {
            return;
        }

        IAPLViewPresenter presenter = component.getViewPresenter();

        // Release the last pointer to have continuity with where the EditText is being touched.
        presenter.releaseLastMotionEvent();

        View target = presenter.findView(component);
        if (target == null) {
            return;
        }
        
        target.performAccessibilityAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK.getId(), null);
    }

    @Override
    public void terminate() {

    }
}
