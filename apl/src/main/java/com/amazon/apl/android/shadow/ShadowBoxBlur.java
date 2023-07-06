/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.shadow;


import android.graphics.Bitmap;
import android.util.Log;

import com.amazon.apl.android.BuildConfig;

/**
 * Box-blur algorithm used by {@link ShadowBitmapRenderer} to approximate a gaussian blur
 *
 * See: https://www.w3.org/TR/SVG11/filters.html#feGaussianBlurElement for details
 */
public class ShadowBoxBlur {

    private static final String TAG = ShadowBoxBlur.class.toString();

    /**
     * Blurs the given bitmap by applying 3 passes of a box blur. This closely
     * approximates a Gaussian blur.
     *
     * See https://www.w3.org/TR/SVG11/filters.html#feGaussianBlurElement
     *
     * You probably don't want to do this on UI thread for large bitmap or radius.
     *
     * @param bitmap The bitmap to blur, this will be overwritten with the resulting blur
     */
    public static void blur(final Bitmap bitmap, final int blurRadius) {
        if(blurRadius <= 0) {
            Log.w(TAG, "blurRadius must be > 0, was given " + blurRadius);
            return;
        }
        nativeBoxBlur(bitmap, blurRadius);
    }

    private static native void nativeBoxBlur(Bitmap in, int blurRadius);
}
