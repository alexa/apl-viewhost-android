/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image;


import com.amazon.apl.enums.ImageScale;

/**
 * Calculator which determine the right scale based on the scale option.
 */
public class ImageScaleCalculator {

    /**
     *
     * @param imageScale Scale option on how the image will be resized to fit in the view box.
     * @param vWidth The view width.
     * @param vHeight The view height.
     * @param dWidth The drawable width.
     * @param dHeight The drawable height.
     * @return An array of two elements (scaleX and scaleY)
     */
    public static float[] getScale(ImageScale imageScale, int vWidth, int vHeight, int dWidth, int dHeight) {
        float scaleX = 1.0f;
        float scaleY = 1.0f;


        switch (imageScale) {
            case kImageScaleNone:
                // Do not scale the image. The align property is used to position the image in the
                // bounding box. The portions of the image that fall outside of the bounding box are clipped.
                //
                // => Do nothing.
                break;
            case kImageScaleFill:
                // Scale the image non-uniformly so that the width matches the bounding box width
                // and the height matches the bounding box height.
                scaleX = (float) vWidth / (float) dWidth;
                scaleY = (float) vHeight / (float) dHeight;
                break;
            case kImageScaleBestFill:
                // Default. Scale the image uniformly up or down so that the bounding box is
                // completely covered. The "align" property is used to position the scaled image
                // within the bounding box.
                if (dWidth * vHeight > vWidth * dHeight) {
                    scaleX = scaleY = (float) vHeight / (float) dHeight;
                } else {
                    scaleX = scaleY = (float) vWidth / (float) dWidth;
                }
                break;
            case kImageScaleBestFit:
                // Scale the image uniformly up or down so that the entire image fits within the
                // bounding box. The "align" property is used to position the scaled image within
                // the bounding box.
                scaleX = scaleY = Math.min((float) vWidth / (float) dWidth,
                                           (float) vHeight / (float) dHeight);
                break;
            case kImageScaleBestFitDown:
                // Scale the image uniformly as per best-fit, but only allow down-scaling, never
                // up-scaling. This ensures that the image will not appear pixelated.
                if (dWidth >= vWidth || dHeight >= vHeight) {
                    scaleX = scaleY = Math.min((float) vWidth / (float) dWidth,
                                               (float) vHeight / (float) dHeight);
                }
                break;
        }

        return new float[] { scaleX, scaleY };
    }
}
