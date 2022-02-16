/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.bitmap;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.apl.android.providers.ITelemetryProvider;

/**
 * Pooled bitmap factory.  Maintains a bitmap pool and uses it to reuse bitmaps.
 *
 * This class can be accessed from multiple threads (e.g. FilterProcessing).
 */
public class PooledBitmapFactory implements IBitmapFactory {
    private static final String TAG = "PooledBitmapFactory";
    static final String METRIC_BITMAP_FAIL = TAG + ".bitmap.fail";
    static final String METRIC_BITMAP_SUCCESS = TAG + ".bitmap.success";

    private final ITelemetryProvider mTelemetryProvider;
    private final int cBitmapMetricSuccess;
    private final int cBitmapMetricFail;

    private final IBitmapPool pool;

    PooledBitmapFactory(ITelemetryProvider telemetryProvider, IBitmapPool pool) {
        mTelemetryProvider = telemetryProvider;
        this.pool = pool;
        cBitmapMetricSuccess = mTelemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN, METRIC_BITMAP_SUCCESS, ITelemetryProvider.Type.COUNTER);
        cBitmapMetricFail = mTelemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN, METRIC_BITMAP_FAIL, ITelemetryProvider.Type.COUNTER);
    }

    /**
     * Create an instance of a BitmapFactory with telemetry for success and fail metrics.
     * @param telemetryProvider telemetry provider for metrics
     * @return an instance of a BitmapFactory
     */
    public static PooledBitmapFactory create(ITelemetryProvider telemetryProvider, IBitmapPool pool) {
        return new PooledBitmapFactory(telemetryProvider, pool);
    }

    /**
     * See {@link Bitmap#createBitmap(int, int, Bitmap.Config)} with {@link Bitmap.Config#ARGB_8888}.
     *
     * @throws BitmapCreationException if there's not enough memory to create the bitmap
     */
    public synchronized Bitmap createBitmap(int width, int height) throws BitmapCreationException {
        try {
            Bitmap result = pool.get(width,height, Bitmap.Config.ARGB_8888);
            mTelemetryProvider.incrementCount(cBitmapMetricSuccess);
            return result;
        } catch (OutOfMemoryError e) {
            mTelemetryProvider.incrementCount(cBitmapMetricFail);
            throw new BitmapCreationException(createErrorMessage(width, height), e);
        }
    }

    /**
     * See {@link Bitmap#createBitmap(Bitmap)} with {@link Bitmap.Config#ARGB_8888}.
     *
     * @throws BitmapCreationException if there's not enough memory to create the bitmap
     */
    public synchronized Bitmap createBitmap(Bitmap sourceBitmap) throws BitmapCreationException {
        try {
            Bitmap result = pool.get(sourceBitmap.getWidth(), sourceBitmap.getHeight(), sourceBitmap.getConfig());
            copyBitmap(sourceBitmap, result);
            mTelemetryProvider.incrementCount(cBitmapMetricSuccess);
            return result;
        } catch (OutOfMemoryError e) {
            mTelemetryProvider.incrementCount(cBitmapMetricFail);
            throw new BitmapCreationException(createErrorMessage(sourceBitmap.getWidth(), sourceBitmap.getHeight()), e);
        }
    }

    private void copyBitmap(Bitmap source, Bitmap dest) {
        if (source.getWidth() != dest.getWidth()
        || source.getHeight() != dest.getHeight()
        || source.getConfig() != dest.getConfig()) {
            throw new IllegalArgumentException("both bitmaps must have same resolution and config");
        }

        Canvas temp = new Canvas(dest);
        temp.drawBitmap(source, new Matrix(), null);
        temp.setBitmap(null);
    }

    /**
     * See {@link Bitmap#createScaledBitmap(Bitmap, int, int, boolean)}.
     *
     * @throws BitmapCreationException if there's not enough memory to create the bitmap
     */
    public synchronized Bitmap createScaledBitmap(@NonNull Bitmap src, int dstWidth, int dstHeight, boolean filter) throws BitmapCreationException {
        try {
            Bitmap result = pool.get(dstWidth, dstHeight, Bitmap.Config.ARGB_8888);
            Bitmap scaledSource = Bitmap.createScaledBitmap(src, dstWidth, dstHeight, filter);
            copyBitmap(scaledSource, result);
            pool.put(scaledSource);
            mTelemetryProvider.incrementCount(cBitmapMetricSuccess);
            return result;
        } catch (OutOfMemoryError e) {
            mTelemetryProvider.incrementCount(cBitmapMetricFail);
            throw new BitmapCreationException(createErrorMessage(dstWidth, dstHeight), e);
        }
    }

    /**
     * See {@link Bitmap#createBitmap(Bitmap, int, int, int, int, Matrix, boolean)}
     *
     * @throws BitmapCreationException if there's not enough memory to create the bitmap
     */
    public synchronized Bitmap createBitmap(@NonNull Bitmap source, int x, int y, int width, int height, @Nullable Matrix m, boolean filter) throws BitmapCreationException {
        try {
            Bitmap trimmedSource = Bitmap.createBitmap(source, x, y, width, height, m, filter);
            Bitmap result = pool.get(trimmedSource.getWidth(), trimmedSource.getHeight(), trimmedSource.getConfig());
            copyBitmap(trimmedSource, result);
            pool.put(trimmedSource);
            mTelemetryProvider.incrementCount(cBitmapMetricSuccess);
            return result;
        } catch (OutOfMemoryError e) {
            mTelemetryProvider.incrementCount(cBitmapMetricFail);
            throw new BitmapCreationException(createErrorMessage(width, height), e);
        }
    }

    /**
     * See {@link Bitmap#copy(Bitmap.Config, boolean)}
     *
     * @throws BitmapCreationException if there's not enough memory to create the bitmap.
     */
    public synchronized Bitmap copy(@NonNull Bitmap source, boolean isMutable) throws BitmapCreationException {
        if (!isMutable) {
            // can't get immutable bitmaps from bitmap pool
            return source.copy(source.getConfig(), false);
        } else try {
            Bitmap result = pool.get(source.getWidth(), source.getHeight(), source.getConfig());
            copyBitmap(source, result);
            mTelemetryProvider.incrementCount(cBitmapMetricSuccess);
            return result;
        } catch (NullPointerException | OutOfMemoryError e) {
            mTelemetryProvider.incrementCount(cBitmapMetricFail);
            throw new BitmapCreationException(createErrorMessage(source.getWidth(), source.getHeight()), e);
        }
    }

    @Override
    public synchronized void disposeBitmap(@NonNull Bitmap bitmap) {
        pool.put(bitmap);
    }

    @SuppressWarnings("DefaultLocale")
    private static String createErrorMessage(int width, int height) {
        return String.format("Unable to create bitmap (%d, %d).", width, height);
    }
}
