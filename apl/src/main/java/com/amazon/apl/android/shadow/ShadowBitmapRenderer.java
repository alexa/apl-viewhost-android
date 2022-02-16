/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.shadow;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.AsyncTask;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import android.util.Log;
import android.view.View;

import com.amazon.apl.android.BuildConfig;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.bitmap.BitmapCreationException;
import com.amazon.apl.android.bitmap.IBitmapCache;
import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.bitmap.LruBitmapCache;
import com.amazon.apl.android.primitive.Rect;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Shadow's bitmap rendering manager: shadow bitmap's preparation, drawing and caching.
 */
public class ShadowBitmapRenderer {
    private static final String TAG = "ShadowBitmapRenderer";

    private final Paint mShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Map<Bitmap, BlurAsyncTask> mAsyncTasks = new HashMap<>();
    private final IBitmapCache mCache;
    private final IBitmapFactory mBitmapFactory;

    public ShadowBitmapRenderer(IBitmapCache cache, IBitmapFactory bitmapFactory) {
        mCache = cache;
        mBitmapFactory = bitmapFactory;
    }

    /**
     * Creates and caches a shadow bitmap for given Component. This should be called before a
     * Components drawing phase. Actual rendering of the bitmap to a canvas happens in
     * {@link #drawShadow(Canvas, Component, View)}. This is to avoid doing too much computation and
     * during a Components drawing frame.
     *
     * TODO consider capturing metrics
     * TODO broken up into smaller functions for clarity
     *
     * @param component The Component to create a Shadow bitmap for.
     */
    public void prepareShadow(final Component component) {
        final int offsetX = component.getShadowOffsetHorizontal();
        final int offsetY = component.getShadowOffsetVertical();
        if(offsetX == 0 && offsetY == 0 && component.getShadowRadius() <= 0) {
            return;
        }

        if(getShadowFromCache(component) != null) {
            return;
        }

        final ShadowBitmapKey key = new ShadowBitmapKey(component);
        final RectF shadowRect = component.getShadowRect();
        final int shadowBlurRadius = component.getShadowRadius();
        // if width or height of bitmap is zero, then return
        final int shadowBitmapWidth = Math.round(shadowRect.width() + 2 * shadowBlurRadius);
        final int shadowBitmapHeight = Math.round(shadowRect.height() + 2 * shadowBlurRadius);
        if (shadowBitmapWidth == 0 || shadowBitmapHeight == 0) {
            return;
        }

        final Bitmap shadowBitmap;
        try {
            shadowBitmap = mBitmapFactory.createBitmap(
                    shadowBitmapWidth,
                    shadowBitmapHeight);
        } catch (BitmapCreationException e) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error preparing shadow bitmap.", e);
            }
            return;
        }
        final Canvas bitmapCanvas = new Canvas(shadowBitmap);

        final int shadowColor = component.getShadowColor();
        mShadowPaint.setColor(shadowColor);
        mShadowPaint.setStyle(Paint.Style.FILL);

        final Paint clipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        clipPaint.setColor(Color.TRANSPARENT);
        clipPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        final Matrix matrix = new Matrix();
        matrix.setTranslate(shadowBlurRadius, shadowBlurRadius);

        final Path path = buildShadowPath(component);
        path.transform(matrix);

        bitmapCanvas.drawPath(path, mShadowPaint);
        if(shadowBlurRadius > 0) {
            blurAsync(shadowBitmap, shadowBlurRadius, () -> {
                // clip the shadow where it falls under child
                bitmapCanvas.translate(-offsetX, -offsetY);
                bitmapCanvas.drawPath(path, clipPaint);
                mAsyncTasks.remove(shadowBitmap);
            });
        }
        mCache.putBitmap(key, shadowBitmap);
    }

    /**
     * Use bitmap from mCache for a component's shadow.
     *
     * @param component
     * @return a shadow bitmap from the cache if present, null otherwise.
     */
    @Nullable
    @VisibleForTesting
    Bitmap getShadowFromCache(final Component component) {
        final ShadowBitmapKey key = new ShadowBitmapKey(component);
        return mCache.getBitmap(key);
    }

    private void blurAsync(final Bitmap bitmap, final int blurRadius, final Runnable onFinish) {
        final BlurAsyncTask async = new BlurAsyncTask(blurRadius, onFinish);
        mAsyncTasks.put(bitmap, async);
        async.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, bitmap);
    }

    private static boolean hasRoundCorners(final Component component) {
        final float[] radii = component.getShadowCornerRadius();
        return radii[0] > 0 || radii[1] > 0 || radii[2] > 0 || radii[3] > 0;
    }

    /**
     * Builds a path based off of the component's bounds
     *
     * @param component the Component to bound the path
     * @return the path of the shadow
     *
     * TODO consider capture metrics.
     * TODO consider using a pre-built Path object, calling Path.reset at the top of this method.
     * TODO instead of constructing a Path every time. Save for RectF.
     */
    private Path buildShadowPath(final Component component) {
        final Path path = new Path();
        final RectF shadowRect = component.getShadowRect();
        final RectF rect = new RectF(0, 0, shadowRect.width(), shadowRect.height());

        if(hasRoundCorners(component)) {
            final float[] shadowCornerRadius = component.getShadowCornerRadius();
            final float[] radii = new float[8];
            for(int i = 0; i < radii.length; i++) {
                radii[i] = shadowCornerRadius[i / 2];
            }
            path.addRoundRect(rect, radii, Path.Direction.CW);
        } else {
            path.addRect(rect, Path.Direction.CW);
        }

        return path;
    }

    /**
     * Draws a  shadow for a given Component onto given Canvas. The resulting shadow will be clipped
     * to the bounds of the Canvas. This Canvas should be the same that is passed to the components
     * PARENT drawing frame.
     *
     * It is assumed that {@link #prepareShadow(Component)} was previously called with the same
     * Component. If no shadow was prepared then nothing will be drawn to the Canvas.
     *
     * @param canvas The canvas to draw onto
     * @param component The component to draw a shadow for.
     * @param parent the parent view
     */
    public void drawShadow(final Canvas canvas, final Component component, final View parent) {
        final Bitmap shadowBitmap = getShadowFromCache(component);
        if(shadowBitmap == null) {
            return;
        }
        if(mAsyncTasks.containsKey(shadowBitmap)) {
            final BlurAsyncTask async = mAsyncTasks.get(shadowBitmap);
            if(async != null && !async.isCancelled()) {
                async.addOnFinishListener(new Runnable() {
                    @Override
                    public void run() {
                        parent.invalidate();
                    }
                });
                return;
            }
        }

        final Rect bounds = component.getBounds();

        final Matrix matrix = component.getTransform();
        /*
         * Move the pivot of the canvas to the center of the component relative to the canvas bounds.
         * The component transform initially uses the pivot as the center of the component relative
         * to the component bounds (width / 2, height / 2)
         */
        matrix.preTranslate(-((bounds.intLeft() + bounds.intRight()) / 2 - bounds.intWidth() / 2), -((bounds.intTop() + bounds.intBottom()) / 2 - bounds.intHeight() / 2));
        matrix.postTranslate((bounds.intLeft() + bounds.intRight()) / 2 - bounds.intWidth() / 2, ((bounds.intTop() + bounds.intBottom()) / 2 - bounds.intHeight() / 2));

        final RectF shadowRect = component.getShadowRect();
        final float shadowRadius = component.getShadowRadius();

        canvas.save();
        canvas.concat(matrix);
        canvas.drawBitmap(shadowBitmap, -shadowRadius + shadowRect.left, -shadowRadius + shadowRect.top, mShadowPaint);
        canvas.restore();
    }

    // Clears cache and releases unused resources
    public void cleanUp() {
        for(BlurAsyncTask async : mAsyncTasks.values()) {
            if(async != null && !async.isCancelled()) {
                async.cancel(true);
                async.listeners.clear();
            }
        }
    }

    static class BlurAsyncTask extends AsyncTask<Bitmap, Void, Void> {

        final int blurRadius;
        final List<Runnable> listeners = new LinkedList<>();

        BlurAsyncTask(final int blurRadius, final Runnable onFinish) {
            this.blurRadius = blurRadius;
            listeners.add(onFinish);
        }

        @Override
        protected Void doInBackground(Bitmap... bitmaps) {
            ShadowBoxBlur.blur(bitmaps[0], blurRadius);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            for(Runnable listener : listeners) {
                listener.run();
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        void addOnFinishListener(Runnable listener) {
            listeners.add(listener);
        }
    }
}