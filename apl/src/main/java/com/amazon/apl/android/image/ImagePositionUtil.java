/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image;

public class ImagePositionUtil {
    /**
     * Gets the center position for a scaled image
     * @param imageDimension
     * @param croppedDimension
     * @return center position
     */
    public static int center(int imageDimension, int croppedDimension) {
        return Math.round(0.5f * (float) (imageDimension - croppedDimension));
    }

    /**
     * Gets the end position for a scaled image
     * @param imageDimension
     * @param croppedDimension
     * @return end position
     */
    public static int end(int imageDimension, int croppedDimension) {
        return Math.max(imageDimension - croppedDimension, 0);
    }
}
