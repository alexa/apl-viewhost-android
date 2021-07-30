/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.bitmap;


import android.graphics.Bitmap;
import android.graphics.Matrix;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.apl.android.providers.ITelemetryProvider;

import java.util.Objects;

/**
 * Class responsible for creating bitmap resources. Handles checking if there is enough memory for the resource
 * and emitting a metric on failure.
 */
@Deprecated
public class BitmapFactory implements IBitmapFactory {
    private static final String TAG = "BitmapFactory";
    static final String METRIC_BITMAP_FAIL = TAG + ".bitmap.fail";
    static final String METRIC_BITMAP_SUCCESS = TAG + ".bitmap.success";

    private final ITelemetryProvider mTelemetryProvider;
    private final int cBitmapMetricSuccess;
    private final int cBitmapMetricFail;

    private BitmapFactory(ITelemetryProvider telemetryProvider) {
        mTelemetryProvider = telemetryProvider;
        cBitmapMetricSuccess = mTelemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN, METRIC_BITMAP_SUCCESS, ITelemetryProvider.Type.COUNTER);
        cBitmapMetricFail = mTelemetryProvider.createMetricId(ITelemetryProvider.APL_DOMAIN, METRIC_BITMAP_FAIL, ITelemetryProvider.Type.COUNTER);
    }

    /**
     * Create an instance of a BitmapFactory with telemetry for success and fail metrics.
     * @param telemetryProvider telemetry provider for metrics
     * @return an instance of a BitmapFactory
     */
    public static BitmapFactory create(ITelemetryProvider telemetryProvider) {
        return new BitmapFactory(telemetryProvider);
    }

    /**
     * See {@link Bitmap#createBitmap(int, int, Bitmap.Config)} with {@link Bitmap.Config#ARGB_8888}.
     *
     * @throws BitmapCreationException if there's not enough memory to create the bitmap
     */
    @Override
    public Bitmap createBitmap(int width, int height) throws BitmapCreationException {
        try {
            Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
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
    @Override
    public Bitmap createBitmap(Bitmap sourceBitmap) throws BitmapCreationException {
        try {
            Bitmap result = Bitmap.createBitmap(sourceBitmap);
            mTelemetryProvider.incrementCount(cBitmapMetricSuccess);
            return result;
        } catch (OutOfMemoryError e) {
            mTelemetryProvider.incrementCount(cBitmapMetricFail);
            throw new BitmapCreationException(createErrorMessage(sourceBitmap.getWidth(), sourceBitmap.getHeight()), e);
        }
    }

    /**
     * See {@link Bitmap#createScaledBitmap(Bitmap, int, int, boolean)}.
     *
     * @throws BitmapCreationException if there's not enough memory to create the bitmap
     */
    @Override
    public Bitmap createScaledBitmap(@NonNull Bitmap src, int dstWidth, int dstHeight, boolean filter) throws BitmapCreationException {
        try {
            Bitmap result = Bitmap.createScaledBitmap(src, dstWidth, dstHeight, filter);
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
    @Override
    public Bitmap createBitmap(@NonNull Bitmap source, int x, int y, int width, int height, @Nullable Matrix m, boolean filter) throws BitmapCreationException  {
        try {
            Bitmap result = Bitmap.createBitmap(source, x, y, width, height, m, filter);
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
    @Override
    public Bitmap copy(@NonNull Bitmap source, boolean isMutable) throws BitmapCreationException {
        try {
            Bitmap result = Objects.requireNonNull(source.copy(source.getConfig(), isMutable));
            mTelemetryProvider.incrementCount(cBitmapMetricSuccess);
            return result;
        } catch (NullPointerException | OutOfMemoryError e) {
            mTelemetryProvider.incrementCount(cBitmapMetricFail);
            throw new BitmapCreationException(createErrorMessage(source.getWidth(), source.getHeight()), e);
        }
    }

    @Override
    public void disposeBitmap(@NonNull Bitmap bitmap) {
        if (! bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    @SuppressWarnings("DefaultLocale")
    private static String createErrorMessage(int width, int height) {
        return String.format("Unable to create bitmap (%d, %d).", width, height);
    }

}
