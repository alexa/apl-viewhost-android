/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.graphic;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import android.util.Log;
import android.widget.ImageView;

import com.amazon.apl.android.BuildConfig;
import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.VectorGraphic;
import com.amazon.apl.android.bitmap.BitmapCreationException;
import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.enums.VectorGraphicAlign;
import com.amazon.apl.enums.VectorGraphicScale;

/**
 * ImageView that supports scaling and alignment for {@link VectorGraphic}
 */

@SuppressLint({"AppCompatCustomView", "ViewConstructor"})
public class APLVectorGraphicView extends ImageView {
    private static final String TAG = "APLVectorGraphicView";

    private boolean mFrameSet;
    @NonNull
    private VectorGraphicScale mVectorGraphicScale = VectorGraphicScale.kVectorGraphicScaleNone;
    @NonNull
    private VectorGraphicAlign mVectorGraphicAlign = VectorGraphicAlign.kVectorGraphicAlignCenter;
    private final IBitmapFactory mBitmapFactory;

    /**
     * {@inheritDoc}
     */
    public APLVectorGraphicView(@NonNull Context context, @NonNull IAPLViewPresenter presenter) {
        super(context);
        mBitmapFactory = presenter.getBitmapFactory();
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
        updateMatrix();
    }

    @Override
    protected boolean setFrame(int l, int t, int r, int b) {
        boolean changed = super.setFrame(l, t, r, b);
        mFrameSet = true;
        updateMatrix();
        return changed;
    }

    /**
     * Updates the image view matrix based on the scale and align modes.
     */
    private void updateMatrix() {
        Drawable d = getDrawable();
        if (d == null || !mFrameSet)
            return;

        int dwidth = d.getIntrinsicWidth();
        int dheight = d.getIntrinsicHeight();

        int vwidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int vheight = getHeight() - getPaddingTop() - getPaddingBottom();

        if (mVectorGraphicScale == VectorGraphicScale.kVectorGraphicScaleFill ||
                (vwidth == dwidth && vheight == dheight)) {
            setBounds(0, 0, vwidth, vheight, d);
            setImageMatrix(null);
        } else {
            setBounds(0, 0, dwidth, dheight, d);
            float scale = calculateScale(vwidth, vheight, dwidth, dheight);
            int deltaX = deltaLeft(vwidth, dwidth * scale);
            int deltaY = deltaTop(vheight, dheight * scale);
            Matrix m = new Matrix();
            m.setScale(scale, scale);
            m.postTranslate(deltaX, deltaY);
            setImageMatrix(m);
        }
    }

    /**
     * Sets the bounds on the drawable. For some reason {@Drawable#setBounds} does
     * not work on {@link android.graphics.drawable.VectorDrawable}
     *
     * @param left
     * @param top
     * @param width
     * @param height
     * @param drawable
     */
    private void setBounds(int left, int top, int width, int height, Drawable drawable) {
        if (width <= 0 || height <= 0) {
            return;
        }
        Bitmap bitmap;
        try {
            bitmap = mBitmapFactory.createBitmap(width, height);
        } catch (BitmapCreationException e) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error creating bitmap for scaling.", e);
            }
            return;
        }
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
    }

    /**
     * Calculates how much the center of the drawable should translate
     * in the X-axis
     * @param viewWidth the width of the parent view.
     * @param drawableWidth the width of the drawable.
     * @return how much the center of the drawable should translate in the X-axis.
     */
    @VisibleForTesting
    int deltaLeft(int viewWidth, double drawableWidth) {
        switch (mVectorGraphicAlign) {
            case kVectorGraphicAlignBottom:
            case kVectorGraphicAlignCenter:
            case kVectorGraphicAlignTop:
                return (int) Math.round((viewWidth - drawableWidth) * 0.5);

            case kVectorGraphicAlignBottomRight:
            case kVectorGraphicAlignRight:
            case kVectorGraphicAlignTopRight:
                return (int) Math.round(viewWidth - drawableWidth);
        }

        return 0;
    }

    /**
     * Calculates how much the center of the drawable should translate
     * in the Y-axis
     * @param viewHeight the height of the parent view.
     * @param drawableHeight the width of the drawable.
     * @return how much the center of the drawable should translate in the Y-axis.
     */
    @VisibleForTesting
    int deltaTop(int viewHeight, double drawableHeight) {
        switch (mVectorGraphicAlign) {
            case kVectorGraphicAlignLeft:
            case kVectorGraphicAlignCenter:
            case kVectorGraphicAlignRight:
                return (int) Math.round((viewHeight - drawableHeight) * 0.5);

            case kVectorGraphicAlignBottomLeft:
            case kVectorGraphicAlignBottom:
            case kVectorGraphicAlignBottomRight:
                return (int) Math.round(viewHeight - drawableHeight);
        }

        return 0;
    }

    /**
     * Calculate evaluated scale based on scale mode.
     *
     * @param viewWidth      width of the view after padding is removed
     * @param viewHeight     height of the view after padding is removed
     * @param drawableWidth  intrinsic width of the drawable
     * @param drawableHeight intrinsic height of the drawable
     * @return scale value to apply to the image.
     */
    @VisibleForTesting
    float calculateScale(int viewWidth, int viewHeight, int drawableWidth, int drawableHeight) {
        switch (mVectorGraphicScale) {
            case kVectorGraphicScaleNone:
                return 1.0f;
            case kVectorGraphicScaleBestFill:
                return Math.max((float) viewWidth / (float) drawableWidth,
                        (float) viewHeight / (float) drawableHeight);

            case kVectorGraphicScaleBestFit:
                return Math.min((float) viewWidth / (float) drawableWidth,
                        (float) viewHeight / (float) drawableHeight);
        }
        return 1.0f;
    }

    /**
     * Set the scale mode for the vector drawable.
     * @param scale the scale mode.
     */
    public void setScale(@NonNull final VectorGraphicScale scale) {
        this.mVectorGraphicScale = scale;
    }

    /**
     * Set the align mode for the vector drawable.
     * @param align the align mode.
     */
    public void setAlign(@NonNull final VectorGraphicAlign align) {
        this.mVectorGraphicAlign = align;
    }

}
