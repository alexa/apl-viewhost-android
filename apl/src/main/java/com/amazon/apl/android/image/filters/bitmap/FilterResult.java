/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters.bitmap;

import android.graphics.Bitmap;

/**
 * Interface for a result of a FilterOperation.
 */
public interface FilterResult {
    /**
     * Get's the size of this Filtered result.
     * @return the size.
     */
    Size getSize();

    /**
     * Get's a bitmap with the specified size.
     * @param size the size of the bitmap.
     * @return the bitmap with the requested size.
     */
    Bitmap getBitmap(Size size);

    default Bitmap getBitmap() {
        if (!isBitmap()) {
            throw new IllegalArgumentException("Trying to get bitmap of a zero-size bitmap.");
        }

        return getBitmap(getSize());
    }

    default boolean isBitmap() {
        return !Size.ZERO.equals(getSize());
    }
}
