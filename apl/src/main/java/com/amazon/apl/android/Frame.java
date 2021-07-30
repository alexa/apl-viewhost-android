/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.apl.android.component.FrameViewAdapter;
import com.amazon.apl.android.primitive.Dimension;
import com.amazon.apl.android.primitive.Radii;
import com.amazon.apl.android.views.APLAbsoluteLayout;
import com.amazon.apl.enums.PropertyKey;

import java.util.List;

import static com.amazon.apl.enums.PropertyKey.kPropertyBackgroundColor;
import static com.amazon.apl.enums.PropertyKey.kPropertyBorderColor;
import static com.amazon.apl.enums.PropertyKey.kPropertyBorderRadii;
import static com.amazon.apl.enums.PropertyKey.kPropertyBorderWidth;


/**
 * APL Frame component.
 * See {@link <a https://developer.amazon.com/docs/alexa-presentation-language/apl-frame.html>
 * APL Frame Specification}
 */
public class Frame extends MultiChildComponent {

    /**
     * Frame constructor
     * @param nativeHandle The identifier for the view.
     */
    Frame(long nativeHandle, String componentId, RenderingContext renderingContext) {
        super(nativeHandle, componentId, renderingContext);
    }

    /**
     * @return Background color. Defaults to transparent.
     */
    public int getBackgroundColor() {
        return mProperties.getColor(kPropertyBackgroundColor);
    }


    /**
     * @return Corner radius for rounded-rectangle variant. Defaults to 0.
     */
    @Nullable
    public Radii getBorderRadii() {
        return mProperties.getRadii(kPropertyBorderRadii);
    }


    /**
     * @return Width of the border. Defaults to 0 (no border)
     */
    @Nullable
    public Dimension getBorderWidth() {
        return mProperties.getDimension(kPropertyBorderWidth);
    }

    /**
     * @return Background color. Defaults to transparent.
     */
    public int getBorderColor() {
        return mProperties.getColor(kPropertyBorderColor);
    }

    @Override
    public float[] getShadowCornerRadius() {
        final Radii radii = getBorderRadii();
        return new float[] {radii.topLeft(), radii.topRight(), radii.bottomRight(), radii.bottomLeft()};
    }
}
