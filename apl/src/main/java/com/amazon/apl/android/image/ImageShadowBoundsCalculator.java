/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image;

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
                top = bottom - imageBounds().height();
                left += ImagePositionUtil.center(innerBounds.intWidth(), (int) imageBounds().width());
                right = left + imageBounds().width();
                break;
            case kImageAlignTop:
                bottom = top + imageBounds().height();
                left += ImagePositionUtil.center(innerBounds.intWidth(), (int) imageBounds().width());
                right = left + imageBounds().width();
                break;
            case kImageAlignBottomLeft:
                top = bottom - imageBounds().height();
                right = left + imageBounds().width();
                break;
            case kImageAlignBottomRight:
                top = bottom - imageBounds().height();
                left = right - imageBounds().width();
                break;
            case kImageAlignLeft:
                right = left + imageBounds().width();
                top += ImagePositionUtil.center(innerBounds.intHeight(), imageBounds().height());
                bottom = top + imageBounds().height();
                break;
            case kImageAlignRight:
                left = right - imageBounds().width();
                top += ImagePositionUtil.center(innerBounds.intHeight(), imageBounds().height());
                bottom = top + imageBounds().height();
                break;
            case kImageAlignTopLeft:
                right = left + imageBounds().width();
                bottom = top + imageBounds().height();
                break;
            case kImageAlignTopRight:
                left = right - imageBounds().width();
                bottom = top + imageBounds().height();
                break;
            default:
                // same default case as image gravity
                top += ImagePositionUtil.center(innerBounds.intHeight(), imageBounds().height());
                left += ImagePositionUtil.center(innerBounds.intWidth(), imageBounds().width());
                right = left + imageBounds().width();
                bottom = top + imageBounds().height();
                break;
        }

        shadowBounds.set(left + offsetX(), top + offsetY(), right + offsetX(), bottom + offsetY());
        return shadowBounds;
    }

    public abstract android.graphics.Rect imageBounds();
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
        public abstract ImageShadowBoundsCalculator.Builder imageBounds(android.graphics.Rect bounds);
        public abstract ImageShadowBoundsCalculator.Builder align(ImageAlign align);
        public abstract ImageShadowBoundsCalculator.Builder bounds(Rect bounds);
        public abstract ImageShadowBoundsCalculator.Builder innerBounds(Rect innerBounds);
        public abstract ImageShadowBoundsCalculator.Builder offsetX(int offsetX);
        public abstract ImageShadowBoundsCalculator.Builder offsetY(int offsetY);
        public abstract ImageShadowBoundsCalculator build();
    }
}
