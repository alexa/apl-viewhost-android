/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.views;


import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.amazon.apl.android.Component;
import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.primitive.Rect;

/**
 * Simple absolute layout.
 */
@SuppressLint("ViewConstructor")
public class APLExtensionView extends FrameLayout {
    private static final String TAG = "APLExtensionView";


    final IAPLViewPresenter mPresenter;


    /**
     * Construct an absolute layout for a Component.
     *
     * @param context The Android Context.
     */
    public APLExtensionView(Context context, IAPLViewPresenter presenter) {
        super(context);
        setClipChildren(true);
        setStaticTransformationsEnabled(true);
        mPresenter = presenter;
    }

    /**
     * Measure the Component bounds and set the measured dimensions.
     *
     * @param widthMeasureSpec  overridden by bounds
     * @param heightMeasureSpec overridden by bounds
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // set size to absolute Component bounds
        Component component = mPresenter.findComponent(this);
        if (component == null) {
            Log.e(TAG, "onMeasure. Component is null. Cannot retrieve component bounds." + this);
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        Rect bounds = component.getBounds();
        int w = (int) bounds.getWidth();
        int h = (int) bounds.getHeight();

        // The extension view may be a ViewGroup of any Android Views.  Set this
        // container to exactly match the Component, and let Android layout the children.
        int wSpec = MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY);
        int hSpec = MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY);
        super.onMeasure(wSpec, hSpec);
    }


    /**
     * Returns a set of layout parameters that match the component bounds.
     * @return
     */
    @NonNull
    @Override
    protected FrameLayout.LayoutParams generateDefaultLayoutParams() {
        Component component = mPresenter.findComponent(this);
        if (component == null) {
            Log.e(TAG, "generateDefaultLayoutParams. Component is null");
            return new FrameLayout.LayoutParams(0, 0);
        }
        Rect bounds = component.getBounds();
        int w = (int) bounds.getWidth();
        int h = (int) bounds.getHeight();
        return new FrameLayout.LayoutParams(w, h);
    }


}


