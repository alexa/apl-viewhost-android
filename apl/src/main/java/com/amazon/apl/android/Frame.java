/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;

import com.amazon.apl.android.primitive.Dimension;
import com.amazon.apl.android.primitive.Gradient;
import com.amazon.apl.android.primitive.Radii;
import com.amazon.apl.enums.PropertyKey;

import static com.amazon.apl.enums.PropertyKey.kPropertyBackground;
import static com.amazon.apl.enums.PropertyKey.kPropertyBorderColor;
import static com.amazon.apl.enums.PropertyKey.kPropertyBorderRadii;
import static com.amazon.apl.enums.PropertyKey.kPropertyBorderWidth;
import static com.amazon.apl.enums.PropertyKey.kPropertyDrawnBorderWidth;


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
     * @return Corner radius for rounded-rectangle variant. Defaults to 0.
     */
    @Nullable
    public Radii getBorderRadii() {
        // border radii is not a defaulted property, check if it is set
        if (!mProperties.hasProperty(kPropertyBorderRadii))
            return null;
        return mProperties.getRadii(kPropertyBorderRadii);
    }


    /**
     * @return Width of the border. Defaults to 0 (no border)
     */
    @NonNull
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
