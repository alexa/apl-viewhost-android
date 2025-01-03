/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.viewhost;

import android.app.Activity;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.amazon.apl.android.APLLayout;

/**
 * Sets up a minimal activity with an APL view in it.
 */
public class BasicActivity extends Activity {
    // Robolectric would normally give us a 0x0 view, but we want a nonzero size
    public static final int APL_VIEW_WIDTH = 640;
    public static final int APL_VIEW_HEIGHT = 480;

    private APLLayout mView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int width = APL_VIEW_WIDTH;
        int height = APL_VIEW_HEIGHT;

        if (savedInstanceState != null) {
            width = savedInstanceState.getInt("width", APL_VIEW_WIDTH);
            height = savedInstanceState.getInt("height", APL_VIEW_HEIGHT);
        }

        mView = new APLLayout(this);
        mView.setLayoutParams(new FrameLayout.LayoutParams(width, height));

        LinearLayout parent = new LinearLayout(this);
        parent.addView(mView);
        setContentView(parent);
    }

    public APLLayout getView() {
        return mView;
    }
}
