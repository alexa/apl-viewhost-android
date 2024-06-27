/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Transformation;

import androidx.annotation.NonNull;

import com.amazon.apl.android.APLLayout;
import com.amazon.apl.android.APLLayoutParams;

public class APLRootView extends ViewGroup {
    int mWidth;
    int mHeight;
    float mScaling;

    public APLRootView(final Context context, int w, int h, float scaling) {
        super(context);
        mWidth = w;
        mHeight = h;
        mScaling = scaling;

        setLayoutParams(new APLLayoutParams(mWidth, mHeight, 0,0));
        setWillNotDraw(true);
        setStaticTransformationsEnabled(true);
    }

    @Override
    protected boolean getChildStaticTransformation(View child, Transformation t) {
        if (child instanceof APLView) {
            Matrix m = ((APLView) child).mAplLayer.getTransform();
            t.setTransformationType(Transformation.TYPE_MATRIX);
            t.getMatrix().set(m);
            return true;
        }
        return false;
    }

    public void clearContent() {
    }

    @NonNull
    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new APLAbsoluteLayout.LayoutParams(getSuggestedMinimumWidth(), getSuggestedMinimumHeight(), 0, 0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.makeMeasureSpec(mWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(mHeight, MeasureSpec.EXACTLY));
        // measure children
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                LayoutParams lp = child.getLayoutParams();
                child.measure(MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY));
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int w  = r-l;
        int h = b-t;
        setLayoutParams(new APLLayoutParams(w, h, 0, 0));
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View viewChild = getChildAt(i);
            if (viewChild.getVisibility() != GONE) {
                viewChild.layout(0,0, w, h);
            }
        }
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        canvas.scale(mScaling, mScaling);
        boolean result = super.drawChild(canvas, child, drawingTime);
        return result;
    }
}
