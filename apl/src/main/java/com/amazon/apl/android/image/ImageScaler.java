/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.util.Log;

import com.amazon.apl.android.bitmap.BitmapCreationException;
import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.enums.ImageAlign;
import com.amazon.apl.enums.ImageScale;

public abstract class ImageScaler {
    private static final String TAG = "ImageScaler";

    public static Bitmap getScaledBitmap(Rect innerBounds,
                                         ImageScale imageScale,
                                         ImageAlign align,
                                         IBitmapFactory bitmapFactory,
                                         Bitmap sourceBmp)
            throws BitmapCreationException {
        // 1. gravity - positioning in the bounding space
        // 2. scale - may override gravity for fill
        // 3. nCreate a scaled bitmap

        int imageWidth = sourceBmp.getWidth();
        int imageHeight = sourceBmp.getHeight();

        if (imageWidth == 0 || imageHeight == 0 || innerBounds.getWidth() == 0 || innerBounds.getHeight() == 0) {
            Log.e(TAG, String.format("Invalid image dimensions - imageWidth:%d imageHeight:%d bounds:%s",
                    imageWidth, imageHeight, innerBounds));
            return null;
        }
        float[] scale = ImageScaleCalculator.getScale(imageScale, innerBounds.intWidth(), innerBounds.intHeight(), imageWidth, imageHeight);
        float scaleX = scale[0];
        float scaleY = scale[1];

        if (scaleX == 0 || scaleY == 0) {
            Log.e(TAG, String.format("Invalid image dimensions - bounds:%s", innerBounds));
            return null;
        }
        //Find the subset to crop before scaling
        int croppedWidth = Math.round(Math.min(innerBounds.getWidth() / scaleX, imageWidth));
        int croppedHeight = Math.round(Math.min(innerBounds.getHeight() / scaleY, imageHeight));

        //also need to take into account alignment when slicing this bitmap before scaling
        int croppedX = 0;
        int croppedY = 0;

        switch (align) {
            case kImageAlignBottom:
                croppedX = ImagePositionUtil.center(imageWidth, croppedWidth);
                croppedY = ImagePositionUtil.end(imageHeight, croppedHeight);
                break;
            case kImageAlignBottomLeft:
                croppedY = ImagePositionUtil.end(imageHeight, croppedHeight);
                break;
            case kImageAlignBottomRight:
                croppedX = ImagePositionUtil.end(imageWidth, croppedWidth);
                croppedY = ImagePositionUtil.end(imageHeight, croppedHeight);
                break;
            case kImageAlignLeft:
                croppedY = ImagePositionUtil.center(imageHeight, croppedHeight);
                break;
            case kImageAlignRight:
                croppedX = ImagePositionUtil.end(imageWidth, croppedWidth);
                croppedY = ImagePositionUtil.center(imageHeight, croppedHeight);
                break;
            case kImageAlignTop:
                croppedX = ImagePositionUtil.center(imageWidth, croppedWidth);
                break;
            case kImageAlignTopLeft:
                break;
            case kImageAlignTopRight:
                croppedX = ImagePositionUtil.end(imageWidth, croppedWidth);
                break;
            case kImageAlignCenter:
            default:
                croppedX = ImagePositionUtil.center(imageWidth, croppedWidth);
                croppedY = ImagePositionUtil.center(imageHeight, croppedHeight);
        }

        if (croppedWidth == 0 || croppedHeight == 0) {
            Log.e(TAG, String.format("Invalid image dimensions - croppedWidth:%d croppedHeight:%d bounds:%s",
                    croppedWidth, croppedHeight, innerBounds));
            return null;
        }

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.postScale(scaleX, scaleY);

        //This creates a scaled image from a subset of the original image.
        Bitmap bmp = bitmapFactory.createBitmap(sourceBmp, croppedX, croppedY, croppedWidth,
                    croppedHeight, scaleMatrix, true);
        return bmp;
    }
}
