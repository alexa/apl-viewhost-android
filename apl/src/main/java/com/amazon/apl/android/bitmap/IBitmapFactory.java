/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.bitmap;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface IBitmapFactory {
    Bitmap createBitmap(int width, int height) throws BitmapCreationException;

    Bitmap createBitmap(Bitmap sourceBitmap) throws BitmapCreationException;

    Bitmap createScaledBitmap(@NonNull Bitmap src, int dstWidth, int dstHeight, boolean filter) throws BitmapCreationException;

    Bitmap createBitmap(@NonNull Bitmap source, int x, int y, int width, int height, @Nullable Matrix m, boolean filter) throws BitmapCreationException;

    Bitmap copy(@NonNull Bitmap source, boolean isMutable) throws BitmapCreationException;

    void disposeBitmap(@NonNull Bitmap bitmap);
}
