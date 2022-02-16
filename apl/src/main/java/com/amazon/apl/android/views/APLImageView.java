/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.Image;
import com.amazon.apl.android.image.ImageProcessingAsyncTask;
import com.amazon.apl.android.image.ImageScaleCalculator;
import com.amazon.apl.android.primitive.Gradient;
import com.amazon.apl.enums.ImageAlign;
import com.amazon.apl.enums.ImageScale;

@SuppressLint("AppCompatCustomView")
public class APLImageView extends ImageView {

    private IAPLViewPresenter mPresenter;
    private AsyncTask mImageProcessingAsyncTask;
    private boolean mLayoutRequestsEnabled = true;
    private float mBorderRadius = 0f;
    private ImageScale mImageScale = ImageScale.kImageScaleBestFit;
    private ImageAlign mImageAlign = ImageAlign.kImageAlignCenter;
    private Gradient mOverlayGradient = null;
    private Paint mOverlayPaint = null;
    private Paint mOverlayGradientPaint = null;
    private Path mClipPath = new Path();
    private final RectF mDrawableBoundsF = new RectF();
    private final Rect mDrawableBounds = new Rect();
    private final Rect mClipBounds = new Rect();

    private boolean mFrameSet = false;

    public APLImageView(Context context, final IAPLViewPresenter presenter) {
        super(context);
        mPresenter = presenter;
        setScaleType(ScaleType.MATRIX);
    }

    public IAPLViewPresenter getPresenter() {
        return mPresenter;
    }

    @Nullable
    public AsyncTask getImageProcessingAsyncTask() {
        return mImageProcessingAsyncTask;
    }

    public void setImageProcessingAsyncTask(ImageProcessingAsyncTask task) {
        mImageProcessingAsyncTask = task;
    }

    /**
     * Enable or disable layout requests.
     *
     * ImageView assumes certain operations require a layout pass. As a workaround this
     * allows ignoring requestLayout() calls for when we know a layout is not actually
     * required.
     *
     * @param value true/false if layout requests are enabled
     */
    public void setLayoutRequestsEnabled(boolean value) {
        mLayoutRequestsEnabled = value;
    }

    @Override
    public void requestLayout() {
        if (mLayoutRequestsEnabled) {
            super.requestLayout();
        }
    }

    public void setImageScale(ImageScale imageScale) {
        mImageScale = imageScale;
        updateMatrix();
    }

    public ImageScale getImageScale() {
        return mImageScale;
    }

    public void setImageAlign(ImageAlign imageAlign) {
        mImageAlign = imageAlign;
        updateMatrix();
    }

    public ImageAlign getImageAlign() {
        return mImageAlign;
    }

    public void setBorderRadius(float radius) {
        mBorderRadius = radius;
        updateBounds();
    }

    public float getBorderRadius() {
        return mBorderRadius;
    }

    @VisibleForTesting
    public void setClipPath(Path clipPath) {
        mClipPath = clipPath;
    }

    @VisibleForTesting
    public Path getClipPath() {
        return mClipPath;
    }

    private void updateBounds() {
        mClipPath.reset();
        Drawable drawable = getDrawable();
        int vwidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int vheight = getHeight() - getPaddingTop() - getPaddingBottom();

        if (drawable != null) {
            Matrix scaleMatrix = getImageMatrix();
            drawable.copyBounds(mDrawableBounds);
            mDrawableBoundsF.set(mDrawableBounds);
            scaleMatrix.mapRect(mDrawableBoundsF);
            mDrawableBoundsF.round(mDrawableBounds);

            // Set the clip bounds to the inner rectangle of the transformed drawable's Rect
            int leftOffset = Math.max(0, mDrawableBounds.left);
            int topOffset = Math.max(0, mDrawableBounds.top);
            mClipBounds.set(
                    leftOffset,
                    topOffset,
                    leftOffset + Math.min(vwidth, mDrawableBounds.width()),
                    topOffset + Math.min(vheight, mDrawableBounds.height()));
            // We need to notify the image that it's shadow bounds have changed.
            // This triggers a new shadow bitmap to be prepared and the parent to
            // be invalidated.
            Image image = (Image) getPresenter().findComponent(this);
            if (image != null) {
                image.setShadowBounds(mClipBounds);
            }
        }

        if (mBorderRadius > 0f) {
            mClipPath.addRoundRect(mClipBounds.left, mClipBounds.top, mClipBounds.right, mClipBounds.bottom, mBorderRadius, mBorderRadius, Path.Direction.CCW);
        } else {
            mClipPath.addRect(mClipBounds.left, mClipBounds.top, mClipBounds.right, mClipBounds.bottom, Path.Direction.CCW);
        }
        // Update the shader if we're changing bounds.
        if (mOverlayGradientPaint != null) {
            mOverlayGradientPaint.setShader(mOverlayGradient.getShader(mClipBounds.width(), mClipBounds.height()));
        }
        invalidate();
    }

    @VisibleForTesting
    @Override
    public boolean setFrame(int l, int t, int r, int b) {
        boolean changed = super.setFrame(l, t, r, b);
        mFrameSet = true;
        updateMatrix();
        return changed;
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
        updateMatrix();
    }

    @VisibleForTesting
    public void drawToCanvas(Canvas canvas) {
        Drawable drawable = getDrawable();
        if (drawable == null) {
            return;
        }

        final int saveCount = canvas.getSaveCount();
        canvas.save();


        // Clip path is applied after padding but pre image scaling
        // since the rounded corners should not be scaled
        canvas.translate(getPaddingLeft(), getPaddingTop());
        canvas.clipPath(mClipPath);

        canvas.save();
        Matrix drawMatrix = getImageMatrix();
        if (drawMatrix != null) {
            canvas.concat(drawMatrix);
        }

        drawable.draw(canvas);
        canvas.restore();

        // Overlay paint and gradient are applied without image scaling
        // to avoid distortions to the gradient
        if (mOverlayPaint != null) {
            canvas.drawRect(0, 0, getWidth(), getHeight(), mOverlayPaint);
        }
        if (mOverlayGradientPaint != null) {
            // Move to scaled drawable position then draw gradient
            canvas.translate(mClipBounds.left, mClipBounds.top);
            canvas.drawRect(0, 0, mClipBounds.width(), mClipBounds.height(), mOverlayGradientPaint);
            canvas.translate(-mClipBounds.left, -mClipBounds.top);
        }
        canvas.restoreToCount(saveCount);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawToCanvas(canvas);
    }

    public Paint getOverlayPaint() {
        return mOverlayPaint;
    }

    public Paint getOverlayGradientPaint() {
        return mOverlayGradientPaint;
    }

    public Gradient getOverlayGradient() {
        return mOverlayGradient;
    }

    public void setOverlayColor(int overlayColor) {
        if (overlayColor != Color.TRANSPARENT) {
            mOverlayPaint = new Paint();
            mOverlayPaint.setStyle(Paint.Style.FILL);
            mOverlayPaint.setColor(overlayColor);
            mOverlayPaint.setAlpha(Color.alpha(overlayColor));
        } else {
            mOverlayPaint = null;
        }
        invalidate();
    }

    public void setOverlayGradient(@Nullable Gradient overlayGradient) {
        mOverlayGradient = overlayGradient;
        if (overlayGradient != null) {
            mOverlayGradientPaint = new Paint();
            mOverlayGradientPaint.setStyle(Paint.Style.FILL);
        } else {
            mOverlayGradientPaint = null;
        }
        updateBounds();
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

        if (vwidth == dwidth && vheight == dheight) {
            setImageMatrix(null);
        } else {
            float[] scale = ImageScaleCalculator.getScale(mImageScale, vwidth, vheight, dwidth, dheight);
            int deltaX = deltaLeft(vwidth, dwidth * scale[0]);
            int deltaY = deltaTop(vheight, dheight * scale[1]);
            Matrix m = new Matrix();
            m.setScale(scale[0], scale[1]);
            m.postTranslate(deltaX, deltaY);
            setImageMatrix(m);
        }
        updateBounds();
    }

    /**
     * Calculates how much the center of the drawable should translate
     * in the X-axis
     * @param viewWidth the width of the parent view.
     * @param drawableWidth the width of the drawable.
     * @return how much the center of the drawable should translate in the X-axis.
     */
    private int deltaLeft(int viewWidth, float drawableWidth) {
        switch (mImageAlign) {
            case kImageAlignBottom:
            case kImageAlignCenter:
            case kImageAlignTop:
                return Math.round((viewWidth - drawableWidth) * 0.5f);

            case kImageAlignBottomRight:
            case kImageAlignRight:
            case kImageAlignTopRight:
                return Math.round(viewWidth - drawableWidth);
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
    private int deltaTop(int viewHeight, float drawableHeight) {
        switch (mImageAlign) {
            case kImageAlignLeft:
            case kImageAlignCenter:
            case kImageAlignRight:
                return Math.round((viewHeight - drawableHeight) * 0.5f);

            case kImageAlignBottomLeft:
            case kImageAlignBottom:
            case kImageAlignBottomRight:
                return Math.round(viewHeight - drawableHeight);
        }

        return 0;
    }
}
