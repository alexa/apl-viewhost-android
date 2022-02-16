/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.content.Context;

import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.NoOpComponent;
import com.amazon.apl.android.views.APLAbsoluteLayout;

/**
 * No-op adapter that wires basic component properties to a basic layout
 */
public class NoOpViewAdapter extends ComponentViewAdapter<NoOpComponent, APLAbsoluteLayout> {
    private static NoOpViewAdapter INSTANCE;

    public static NoOpViewAdapter getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NoOpViewAdapter();
        }
        return INSTANCE;
    }

    NoOpViewAdapter() {
        super();
    }

    @Override
    public APLAbsoluteLayout createView(Context context, IAPLViewPresenter presenter) {
        return new APLAbsoluteLayout(context, presenter);
    }

    @Override
    void applyPadding(NoOpComponent component, APLAbsoluteLayout layout) {
    }
}
