/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.utils;

import android.graphics.Color;

public class ColorUtils {
    private ColorUtils() { }

    /**
     * @param colorLong the argb long value.
     * @return a color.
     */
    public static int toARGB(long colorLong) {
        int r = (int) ((colorLong >> 24) & 0xff);
        int g = (int) ((colorLong >> 16) & 0xff);
        int b = (int) ((colorLong >> 8) & 0xff);
        int a = (int) (colorLong & 0xff);
        return Color.argb(a, r, g, b);
    }
}
