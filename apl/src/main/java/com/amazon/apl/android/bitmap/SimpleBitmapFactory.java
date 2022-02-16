/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.bitmap;

import android.graphics.Bitmap;
import android.graphics.Matrix;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Simple bitmap factory - no caching.
 */
public class SimpleBitmapFactory implements IBitmapFactory {

    static final Bitmap.Config CONFIG = Bitmap.Config.ARGB_8888;

    @Override
    public Bitmap createBitmap(int width, int height) throws BitmapCreationException {
        return Bitmap.createBitmap(width, height, CONFIG);
    }

    @Override
    public Bitmap createBitmap(Bitmap sourceBitmap) throws BitmapCreationException {
        return Bitmap.createBitmap(sourceBitmap);
    }

    @Override
    public Bitmap createScaledBitmap(@NonNull Bitmap src, int dstWidth, int dstHeight, boolean filter)
            throws BitmapCreationException {
        return Bitmap.createScaledBitmap(src, dstWidth, dstHeight, filter);
    }

    @Override
    public Bitmap createBitmap(@NonNull Bitmap source, int x, int y, int width, int height,
                               @Nullable Matrix m, boolean filter) throws BitmapCreationException {
        return Bitmap.createBitmap(source, x, y, width, height, m, filter);
    }

    @Override
    public Bitmap copy(@NonNull Bitmap source, boolean isMutable) throws BitmapCreationException {
        return source.copy(CONFIG, isMutable);
    }

    @Override
    public void disposeBitmap(@NonNull Bitmap bitmap) {
        bitmap.recycle();
    }
}