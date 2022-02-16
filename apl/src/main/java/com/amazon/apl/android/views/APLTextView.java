/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.text.Layout;
import android.text.StaticLayout;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

import com.amazon.apl.android.IAPLViewPresenter;

/**
 * TextView extension that uses a Text Layout for measureTextContent, layout, and rendering.
 */
@SuppressLint("AppCompatCustomView")
public class APLTextView extends View {


    // The Layout that drives measurement and drawing for text.
    private Layout mLayout;

    // The horizontal positional offset within the parent
    private int mDrawLeft = 0;

    // The vertical positional offset within the parent
    private int mDrawTop = 0;

    // The gravity for vertical text alignment.
    private int mVerticalGravity;

    public APLTextView(Context context, IAPLViewPresenter presenter) {
        super(context);
    }

    /**
     * @return The view Layout.
     */
    public Layout getLayout() {
        return mLayout;
    }

    /**
     * Sets a new layout into this view and marks as needing redraw.
     *
     * @param layout The layout.
     */
    @UiThread
    public void setLayout(Layout layout) {
        mLayout = layout;
        invalidate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // APL is absolute measurement, so set sizing directly for speed
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec),
                MeasureSpec.getSize(heightMeasureSpec));
    }

    /**
     * {@inheritDoc}
     * Uses the Layout to draw text.
     */
    @Override
    public void onDraw(@NonNull Canvas canvas) {
        if (mLayout == null) {
            return;
        }

        // TODO: Remove padding and use inner bounds of the component.
        // drawing position left edge is inset by padding
        mDrawLeft = getPaddingLeft();

        // drawing position top is adjusted for gravity
        if (mVerticalGravity == Gravity.CENTER_VERTICAL) {
            mDrawTop = (getMeasuredHeight() - getPaddingBottom() + getPaddingTop() - mLayout.getHeight()) / 2;
        } else if (mVerticalGravity == Gravity.BOTTOM) {
            mDrawTop = getMeasuredHeight() - getPaddingBottom() - mLayout.getHeight();
        } else {
            mDrawTop = getPaddingTop();
        }
        super.onDraw(canvas);
        canvas.save();
        canvas.translate(mDrawLeft + getScrollX(), mDrawTop + getScrollY());
        mLayout.draw(canvas);
        canvas.restore();
    }

    public int getVerticalGravity() {
        return mVerticalGravity;
    }

    public void setVerticalGravity(int gravityVertical) {
        mVerticalGravity = gravityVertical;
    }
}
