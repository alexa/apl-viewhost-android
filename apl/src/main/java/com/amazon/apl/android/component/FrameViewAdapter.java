/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.content.Context;

import com.amazon.apl.android.views.APLAbsoluteLayout;
import com.amazon.apl.android.APLGradientDrawable;
import com.amazon.apl.android.Frame;
import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.primitive.Dimension;
import com.amazon.apl.android.primitive.Radii;

import static com.amazon.apl.enums.PropertyKey.kPropertyBackgroundColor;
import static com.amazon.apl.enums.PropertyKey.kPropertyBorderColor;
import static com.amazon.apl.enums.PropertyKey.kPropertyBorderRadii;
import static com.amazon.apl.enums.PropertyKey.kPropertyBorderWidth;

/**
 * ComponentViewAdapter responsible for applying {@link Frame} properties to an {@link APLAbsoluteLayout}.
 */
public class FrameViewAdapter extends MultiChildViewAdapter<Frame> {
    private static FrameViewAdapter INSTANCE;

    private FrameViewAdapter() {
        super();
        putPropertyFunction(kPropertyBackgroundColor, this::applyBackgroundColor);
        putPropertyFunction(kPropertyBorderColor, this::applyBorder);
        putPropertyFunction(kPropertyBorderWidth, this::applyBorder);
        putPropertyFunction(kPropertyBorderRadii, this::applyBorderRadii);
    }

    /**
     * @return the FrameViewAdapter instance.
     */
    public static FrameViewAdapter getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new FrameViewAdapter();
        }
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public APLAbsoluteLayout createView(Context context, IAPLViewPresenter presenter) {
        final APLAbsoluteLayout frame = new APLAbsoluteLayout(context, presenter);
        frame.setBackground(new APLGradientDrawable());
        return frame;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applyAllProperties(Frame component, APLAbsoluteLayout view) {
        super.applyAllProperties(component, view);
        applyBackgroundColor(component, view);
        applyBorderRadii(component, view);
        applyBorder(component, view);
    }

    private void applyBackgroundColor(Frame component, APLAbsoluteLayout view) {
        APLGradientDrawable drawable = (APLGradientDrawable) view.getBackground();
        int color = component.getBackgroundColor();
        drawable.setColor(color);
    }

    private void applyBorderRadii(Frame component, APLAbsoluteLayout view) {
        APLGradientDrawable drawable = (APLGradientDrawable) view.getBackground();
        Radii radii = component.getBorderRadii();
        if (radii != null) {
            drawable.mutate();
            drawable.setCornerRadii(radii.toFloatArray());
        }
    }

    private void applyBorder(Frame component, APLAbsoluteLayout view) {
        APLGradientDrawable drawable = (APLGradientDrawable) view.getBackground();
        // Border Stroke = width + color
        int borderWidth = drawable.getBorderWidth();
        Dimension borderWidthDim = component.getBorderWidth();
        if (borderWidthDim != null) {
            borderWidth = borderWidthDim.intValue();
        }
        int borderColor = component.hasProperty(kPropertyBorderColor) ? component.getBorderColor() : drawable.getBorderColor();
        drawable.setStroke(borderWidth, borderColor);
    }

    /**
     * {@inheritDoc}
     */
    void applyPadding(Frame component, APLAbsoluteLayout view) {
        // No need to calculate padding as it is calculated by layout positioning
        // Furthermore, padding causes clipping that is incompatible with Flexbox layouts.
    }
}