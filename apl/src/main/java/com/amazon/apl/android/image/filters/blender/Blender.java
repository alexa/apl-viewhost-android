/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters.blender;

import android.graphics.Bitmap;

import com.amazon.apl.enums.BlendMode;

public abstract class Blender {

    private static final String TAG = "Blender";

    protected BlendMode mBlendMode;

    public Blender(BlendMode blendMode) {
        mBlendMode = blendMode;
    }

    // TODO: Migrate all low level pixel operations to JNI layer for speed.
    // APL Spec: https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-filters.html#blend
    public Bitmap performBlending(Bitmap source, Bitmap destination, Bitmap result) {
        int width = Math.min(source.getWidth(), destination.getWidth());
        int height = Math.min(source.getHeight(), destination.getHeight());

        final int[] srcPixels = new int[width * height];
        final int[] destPixels = new int[width * height];
        final int[] resultPixels = new int[width * height];

        source.getPixels(srcPixels, 0, width, 0, 0, width, height);
        destination.getPixels(destPixels, 0, width, 0, 0, width, height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgbS = srcPixels[y * width + x];
                int rgbD = destPixels[y * width + x];

                resultPixels[y * width + x] = blendPixels(rgbS, rgbD);
            }
        }

        result.setPixels(resultPixels, 0, result.getWidth(), 0, 0, result.getWidth(), result.getHeight());
        return result;
    }

    abstract int blendPixels(int sourceColor, int destinationColor);
}
