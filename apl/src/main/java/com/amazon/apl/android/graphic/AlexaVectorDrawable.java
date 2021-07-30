/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.graphic;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import android.util.Log;

import com.amazon.apl.android.BuildConfig;
import com.amazon.apl.android.bitmap.BitmapCreationException;
import com.amazon.apl.android.bitmap.IBitmapFactory;

import java.util.Set;

/**
 * Creates an AVG drawable. This class uses drawing logic from
 * {@link android.support.graphics.drawable.VectorDrawableCompat} with changes made
 * to match {@link com.amazon.apl.android.VectorGraphic}.
 */
public class AlexaVectorDrawable extends Drawable {
    private static final String TAG = "AlexaVectorDrawable";

    private static final Mode DEFAULT_TINT_MODE = Mode.SRC_IN;

    // Cap the bitmap size, such that it won't hurt the performance too much
    // and it won't crash due to a very large scale.
    // The drawable will look blurry above this size.
    private static final int MAX_CACHED_BITMAP_SIZE = 2048;

    private VectorDrawableCompatState mVectorState;

    @Nullable
    private PorterDuffColorFilter mTintFilter;
    private ColorFilter mColorFilter;

    private boolean mMutated;

    // Temp variable, only for saving "new" operation at the draw() time.
    private final float[] mTmpFloats = new float[9];
    private final Matrix mTmpMatrix = new Matrix();
    private final Rect mTmpBounds = new Rect();

    private AlexaVectorDrawable(GraphicContainerElement element) {
        mVectorState = new VectorDrawableCompatState(element);
    }

    private AlexaVectorDrawable(@NonNull VectorDrawableCompatState state) {
        mVectorState = state;
        updateTintFilter(state.mTint, state.mTintMode);
    }

    /**
     * Create a AlexaVectorDrawable object.
     * @param avg the root of the AVG object
     * @return instance of the {@link AlexaVectorDrawable} object.
     */
    @NonNull
    public static AlexaVectorDrawable create(@NonNull GraphicContainerElement avg) {
        final AlexaVectorDrawable drawable = new AlexaVectorDrawable(avg);
        drawable.inflate(avg);
        return drawable;
    }

    /**
     * Inflates the top level avg container element.
     *
     * Only call this if elements are added or removed from the tree.
     *
     * @param element the top level container element.
     */
    public void inflate(@NonNull GraphicContainerElement element) {
        final PathRenderer pathRenderer = new PathRenderer(element);
        pathRenderer.applyProperties();
        mVectorState.mPathRenderer = pathRenderer;
        mVectorState.mTint = null;
        mVectorState.mAutoMirrored = true;
        updateTintFilter(null, mVectorState.mTintMode);
    }

    /**
     * Applies {@link GraphicElement} properties to each dirty graphic supplied in the set of ids.
     * @param dirtyGraphicUniqueIds the set of unique ids of dirty graphics.
     */
    public void updateDirtyGraphics(@NonNull Set<Integer> dirtyGraphicUniqueIds) {
        mVectorState.setDirty(true);
        mVectorState.mPathRenderer.applyBaseDimensions();
        mVectorState.mPathRenderer.getRootGroup().applyDirtyProperties(dirtyGraphicUniqueIds);
        invalidateSelf();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        // We will offset the bounds for drawBitmap, so copyBounds() here instead
        // of getBounds().
        copyBounds(mTmpBounds);
        if (mTmpBounds.width() <= 0 || mTmpBounds.height() <= 0) {
            // Nothing to draw
            return;
        }

        // Color filters always override tint filters.
        final ColorFilter colorFilter = (mColorFilter == null ? mTintFilter : mColorFilter);

        // The imageView can scale the canvas in different ways, in order to
        // avoid blurry scaling, we have to draw into a bitmap with exact pixel
        // size first. This bitmap size is determined by the bounds and the
        // canvas scale.
        canvas.getMatrix(mTmpMatrix);
        mTmpMatrix.getValues(mTmpFloats);
        float canvasScaleX = Math.abs(mTmpFloats[Matrix.MSCALE_X]);
        float canvasScaleY = Math.abs(mTmpFloats[Matrix.MSCALE_Y]);

        float canvasSkewX = Math.abs(mTmpFloats[Matrix.MSKEW_X]);
        float canvasSkewY = Math.abs(mTmpFloats[Matrix.MSKEW_Y]);

        // When there is any rotation / skew, then the scale value is not valid.
        if (canvasSkewX != 0 || canvasSkewY != 0) {
            canvasScaleX = 1.0f;
            canvasScaleY = 1.0f;
        }

        int scaledWidth = (int) (mTmpBounds.width() * canvasScaleX);
        int scaledHeight = (int) (mTmpBounds.height() * canvasScaleY);
        scaledWidth = Math.min(MAX_CACHED_BITMAP_SIZE, scaledWidth);
        scaledHeight = Math.min(MAX_CACHED_BITMAP_SIZE, scaledHeight);

        if (scaledWidth <= 0 || scaledHeight <= 0) {
            return;
        }

        final int saveCount = canvas.save();
        canvas.translate(mTmpBounds.left, mTmpBounds.top);

        // Handle RTL mirroring.
        final boolean needMirroring = needMirroring();
        if (needMirroring) {
            canvas.translate(mTmpBounds.width(), 0);
            canvas.scale(-1.0f, 1.0f);
        }

        // At this point, canvas has been translated to the right position.
        // And we use this bound for the destination rect for the drawBitmap, so
        // we offset to (0, 0);
        mTmpBounds.offsetTo(0, 0);

        mVectorState.createOrEraseCachedBitmap(scaledWidth, scaledHeight);
        mVectorState.drawCachedBitmapWithRootAlpha(canvas, colorFilter, mTmpBounds);
        canvas.restoreToCount(saveCount);
    }

    @Override
    public int getAlpha() {
        return mVectorState.mPathRenderer.getRootAlpha();
    }

    @Override
    public void setAlpha(int alpha) {
        if (mVectorState.mPathRenderer.getRootAlpha() != alpha) {
            mVectorState.mPathRenderer.setRootAlpha(alpha);
            invalidateSelf();
        }
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mColorFilter = colorFilter;
        invalidateSelf();
    }

    @Override
    public void setTint(int tint) {
        setTintList(ColorStateList.valueOf(tint));
    }

    @Override
    public void setTintList(ColorStateList tint) {
        final VectorDrawableCompatState state = mVectorState;
        if (state.mTint != tint) {
            state.mTint = tint;
            updateTintFilter(state.mTint, state.mTintMode);
            invalidateSelf();
        }
    }

    @Override
    public void setTintMode(@NonNull Mode tintMode) {
        final VectorDrawableCompatState state = mVectorState;
        if (state.mTintMode != tintMode) {
            state.mTintMode = tintMode;
            updateTintFilter(state.mTint, state.mTintMode);
            invalidateSelf();
        }
    }

    @Override
    public boolean isStateful() {
        return super.isStateful() || (mVectorState != null && mVectorState.mTint != null
                && mVectorState.mTint.isStateful());
    }

    @Override
    protected boolean onStateChange(@NonNull int[] stateSet) {
        final VectorDrawableCompatState state = mVectorState;
        if (state.mTint != null && state.mTintMode != null) {
            updateTintFilter(state.mTint, state.mTintMode);
            invalidateSelf();
            return true;
        }
        return false;
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return (int) mVectorState.mPathRenderer.mBaseWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return (int) mVectorState.mPathRenderer.mBaseHeight;
    }

    // Don't support re-applying themes. The initial theme loading is working.
    @Override
    public boolean canApplyTheme() {
        return false;
    }

    @Override
    public boolean isAutoMirrored() {
        return mVectorState.mAutoMirrored;
    }

    @Override
    public void setAutoMirrored(boolean mirrored) {
        mVectorState.mAutoMirrored = mirrored;
    }

    @Override
    public @NonNull Drawable mutate() {
        if (!mMutated && super.mutate() == this) {
            mVectorState = new VectorDrawableCompatState(mVectorState);
            mMutated = true;
        }
        return this;
    }

    @Override
    public Drawable.ConstantState getConstantState() {
        return mVectorState;
    }

    private boolean needMirroring() {
            return isAutoMirrored()
                    && DrawableCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL;
    }

    /**
     * Ensures the tint filter is consistent with the current tint color and
     * mode.
     */
    private void updateTintFilter(@Nullable ColorStateList tint,
                                  @Nullable Mode tintMode) {
        if (tint == null || tintMode == null) {
            return;
        }
        // setMode, setColor of PorterDuffColorFilter are not public method in SDK v7.
        // Therefore we create a new one all the time here. Don't expect this is called often.
        final int color = tint.getColorForState(getState(), Color.TRANSPARENT);
        mTintFilter = new PorterDuffColorFilter(color, tintMode);
    }

    private static class VectorDrawableCompatState extends Drawable.ConstantState {
        PathRenderer mPathRenderer;
        @Nullable
        ColorStateList mTint = null;
        Mode mTintMode = DEFAULT_TINT_MODE;
        boolean mAutoMirrored;
        Bitmap mCachedBitmap;
        private boolean mIsDirty = true;
        private IBitmapFactory mBitmapFactory;

        /**
         * Temporary mPaint object used to draw cached bitmaps.
         */
        transient Paint mTempPaint;

        VectorDrawableCompatState(GraphicContainerElement element) {
            mPathRenderer = new PathRenderer(element);
            mBitmapFactory = element.getRenderingContext().getBitmapFactory();
        }

        // Deep copy for mutate() or implicitly mutate.
        VectorDrawableCompatState(@Nullable VectorDrawableCompatState copy) {
            if (copy != null) {
                mPathRenderer = new PathRenderer(copy.mPathRenderer);
                mTint = copy.mTint;
                mTintMode = copy.mTintMode;
                mAutoMirrored = copy.mAutoMirrored;
                mCachedBitmap = copy.mCachedBitmap;
                mBitmapFactory = copy.mBitmapFactory;
            }
        }

        @NonNull
        @Override
        public Drawable newDrawable() {
            return new AlexaVectorDrawable(this);
        }

        @NonNull
        @Override
        public Drawable newDrawable(Resources res) {
            return new AlexaVectorDrawable(this);
        }

        @Override
        public int getChangingConfigurations() {
            return 0;
        }

        /**
         * Creates and caches a bitmap with the specified dimensions or erases the current bitmap.
         *
         * @param width     width of bitmap
         * @param height    height of bitmap
         */
        void createOrEraseCachedBitmap(int width, int height) {
            if (mCachedBitmap == null || !canReuseBitmap(width, height)) {
                try {
                    mCachedBitmap = mBitmapFactory.createBitmap(width, height);
                } catch (BitmapCreationException e) {
                    if (BuildConfig.DEBUG) {
                        Log.e(TAG, "Error creating bitmap for AVG.", e);
                    }
                    return;
                }
                drawAVGToCachedBitmap();
            } else if (mIsDirty) {
                mCachedBitmap.eraseColor(Color.TRANSPARENT);
                drawAVGToCachedBitmap();
            }
        }

        /**
         * Draws the AVG to the bitmap. Needs to be done if bitmap is erased or recreated.
         */
        private void drawAVGToCachedBitmap() {
            final Canvas tmpCanvas = new Canvas(mCachedBitmap);
            mPathRenderer.draw(tmpCanvas, mCachedBitmap.getWidth(), mCachedBitmap.getHeight(), mBitmapFactory);
            setDirty(false);
        }

        /**
         * Set dirty flag to notify that cached bitmap is invalid.
         * @param isDirty true if next draw should erase and redraw the cached bitmap,
         *                false if next draw can reuse the cached bitmap.
         */
        void setDirty(boolean isDirty) {
            mIsDirty = isDirty;
        }

        void drawCachedBitmapWithRootAlpha(@NonNull Canvas canvas, ColorFilter filter,
                                                  @NonNull Rect originalBounds) {
            // The bitmap's size is the same as the bounds.
            final Paint p = getPaint(filter);
            if (mCachedBitmap != null) {
                canvas.drawBitmap(mCachedBitmap, null, originalBounds, p);
            }
        }

        boolean canReuseBitmap(int width, int height) {
            return width == mCachedBitmap.getWidth()
                    && height == mCachedBitmap.getHeight();
        }

        private boolean hasTranslucentRoot() {
            return mPathRenderer.getRootAlpha() < 255;
        }

        /**
         * @return null when there is no need for alpha mPaint.
         */
        @Nullable
        private Paint getPaint(@Nullable ColorFilter filter) {
            if (!hasTranslucentRoot() && filter == null) {
                return null;
            }

            if (mTempPaint == null) {
                mTempPaint = new Paint();
                mTempPaint.setFilterBitmap(true);
            }
            mTempPaint.setAlpha(mPathRenderer.getRootAlpha());
            mTempPaint.setColorFilter(filter);
            return mTempPaint;
        }
    }
}