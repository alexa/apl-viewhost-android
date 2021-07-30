/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image;

import android.graphics.Bitmap;
import android.graphics.RectF;

import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.enums.ImageAlign;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class ImageShadowBoundsCalculator {

    public RectF calculateShadowBounds() {
        RectF shadowBounds = new RectF();
        final Rect componentBounds = bounds();
        final Rect innerBounds = innerBounds();

        float left = componentBounds.getLeft() + innerBounds.getLeft(); // use innerbounds to take into account padding
        float top = componentBounds.getTop() + innerBounds.getTop();
        float right = left + innerBounds.getWidth();
        float bottom = top + innerBounds.getHeight();

        switch (align()) {
            case kImageAlignBottom:
                top = bottom - image().getHeight();
                left += ImagePositionUtil.center(innerBounds.intWidth(), image().getWidth());
                right = left + image().getWidth();
                break;
            case kImageAlignTop:
                bottom = top + image().getHeight();
                left += ImagePositionUtil.center(innerBounds.intWidth(), image().getWidth());
                right = left + image().getWidth();
                break;
            case kImageAlignBottomLeft:
                top = bottom - image().getHeight();
                right = left + image().getWidth();
                break;
            case kImageAlignBottomRight:
                top = bottom - image().getHeight();
                left = right - image().getWidth();
                break;
            case kImageAlignLeft:
                right = left + image().getWidth();
                top += ImagePositionUtil.center(innerBounds.intHeight(), image().getHeight());
                bottom = top + image().getHeight();
                break;
            case kImageAlignRight:
                left = right - image().getWidth();
                top += ImagePositionUtil.center(innerBounds.intHeight(), image().getHeight());
                bottom = top + image().getHeight();
                break;
            case kImageAlignTopLeft:
                right = left + image().getWidth();
                bottom = top + image().getHeight();
                break;
            case kImageAlignTopRight:
                left = right - image().getWidth();
                bottom = top + image().getHeight();
                break;
            default:
                // same default case as image gravity
                top += ImagePositionUtil.center(innerBounds.intHeight(), image().getHeight());
                left += ImagePositionUtil.center(innerBounds.intWidth(), image().getWidth());
                right = left + image().getWidth();
                bottom = top + image().getHeight();
                break;
        }

        shadowBounds.set(left + offsetX(), top + offsetY(), right + offsetX(), bottom + offsetY());
        return shadowBounds;
    }

    public abstract Bitmap image();
    public abstract ImageAlign align();
    public abstract Rect bounds();
    public abstract Rect innerBounds();
    public abstract int offsetX();
    public abstract int offsetY();

    public static ImageShadowBoundsCalculator.Builder builder() {
        return new AutoValue_ImageShadowBoundsCalculator.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract ImageShadowBoundsCalculator.Builder image(Bitmap image);
        public abstract ImageShadowBoundsCalculator.Builder align(ImageAlign align);
        public abstract ImageShadowBoundsCalculator.Builder bounds(Rect bounds);
        public abstract ImageShadowBoundsCalculator.Builder innerBounds(Rect innerBounds);
        public abstract ImageShadowBoundsCalculator.Builder offsetX(int offsetX);
        public abstract ImageShadowBoundsCalculator.Builder offsetY(int offsetY);
        public abstract ImageShadowBoundsCalculator build();
    }
}
