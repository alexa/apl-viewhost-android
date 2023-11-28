/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.content.Context;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.RoundRectShape;

import com.amazon.apl.android.primitive.Gradient;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.android.views.APLAbsoluteLayout;
import com.amazon.apl.android.Frame;
import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.primitive.Dimension;
import com.amazon.apl.android.primitive.Radii;

import static com.amazon.apl.enums.PropertyKey.kPropertyBackground;
import static com.amazon.apl.enums.PropertyKey.kPropertyBorderColor;
import static com.amazon.apl.enums.PropertyKey.kPropertyBorderRadii;
import static com.amazon.apl.enums.PropertyKey.kPropertyBorderWidth;
import static com.amazon.apl.enums.PropertyKey.kPropertyDrawnBorderWidth;

/**
 * ComponentViewAdapter responsible for applying {@link Frame} properties to an {@link APLAbsoluteLayout}.
 */
public class FrameViewAdapter extends MultiChildViewAdapter<Frame> {
    private static FrameViewAdapter INSTANCE;

    private FrameViewAdapter() {
        super();
        putPropertyFunction(kPropertyBackground, this::applyBackground);
        putPropertyFunction(kPropertyBorderColor, this::applyBorderColor);
        putPropertyFunction(kPropertyBorderWidth, this::applyBorder);
        putPropertyFunction(kPropertyDrawnBorderWidth, this::applyDrawnBorder);
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
        frame.setBackground(createDrawables());
        return frame;
    }

    private Drawable createDrawables() {
        InsetDrawable borderInsetDrawable = new InsetDrawable(
                new ShapeDrawable(new RectShape()),
                0, 0, 0, 0);
        LayerDrawable parentDrawable = new LayerDrawable(new Drawable[]{
                new ShapeDrawable(),
                borderInsetDrawable
        });
        parentDrawable.setId(1, 1);
        return parentDrawable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applyAllProperties(Frame component, APLAbsoluteLayout view) {
        super.applyAllProperties(component, view);
        applyBackground(component, view);
        applyBorderRadii(component, view);
        applyBorderColor(component, view);
        applyDrawnBorderInternal(component, view);
    }

    private ShapeDrawable getBackgroundDrawable(APLAbsoluteLayout view) {
        LayerDrawable parentLayout = (LayerDrawable)view.getBackground();
        InsetDrawable borderInset = (InsetDrawable)parentLayout.getDrawable(1);
        return (ShapeDrawable)borderInset.getDrawable();
    }

    private ShapeDrawable getBorderDrawable(APLAbsoluteLayout view) {
        return (ShapeDrawable) ((LayerDrawable)view.getBackground()).getDrawable(0);
    }

    private void applyDrawableBorderVisibility(ShapeDrawable drawable, int inset) {
        Boolean currentlyVisible = drawable.getAlpha() > 0;
        Boolean shouldShowDrawable = inset > 0;

        if (shouldShowDrawable && !currentlyVisible) {
            // show
            drawable.setAlpha(255);
        } else if (!shouldShowDrawable && currentlyVisible) {
            // hide
            drawable.setAlpha(0);
        }
    }

    private void applyBackground(Frame component, APLAbsoluteLayout view) {
        ShapeDrawable backgroundDrawable = getBackgroundDrawable(view);
        Dimension drawnBorderWidth = component.getDrawnBorderWidth();
        backgroundDrawable.getPaint().setShader(null);

        if (component.isGradientBackground()) {
            Rect shaderSize = component.getBounds().inset(drawnBorderWidth.value());
            Gradient gradient = component.getBackgroundGradient();
            backgroundDrawable.getPaint().setShader(gradient.getShader(
                    shaderSize.intWidth(), shaderSize.intHeight()));
        } else {
            backgroundDrawable.getPaint().setColor(component.getBackgroundColor());
        }

        backgroundDrawable.invalidateSelf();
    }

    private RectF getInsetRect(Frame component) {
        Dimension drawnBorderWidth = component.getDrawnBorderWidth();
        float inset = drawnBorderWidth.value();
        return new RectF(inset, inset, inset, inset);
    }

    private void applyBorderRadii(Frame component, APLAbsoluteLayout view) {
        Radii radii = component.getBorderRadii();
        if (radii != null) {
            RectF inset = getInsetRect(component);

            ShapeDrawable gradientDrawable = getBackgroundDrawable(view);
            ShapeDrawable borderDrawable = getBorderDrawable(view);
            Radii backgroundInsetRadii = radii.inset(inset.left);

            borderDrawable.setShape(new RoundRectShape(radii.toFloatArray(), inset, backgroundInsetRadii.toFloatArray()));
            gradientDrawable.setShape(new RoundRectShape(backgroundInsetRadii.toFloatArray(), null, null));
            view.requestChildClippingPathUpdate();
        }
    }

    private void applyDrawnBorderInternal(Frame component, APLAbsoluteLayout view) {
        Dimension drawnBorderWidth = component.getDrawnBorderWidth();
        ShapeDrawable borderDrawable = getBorderDrawable(view);
        borderDrawable.getPaint().setStrokeWidth(drawnBorderWidth.value());

        int inset = drawnBorderWidth.intValue();

        applyDrawableBorderVisibility(borderDrawable, inset);

        // Replace inset
        LayerDrawable parentLayout = (LayerDrawable)view.getBackground();
        ShapeDrawable backgroundDrawable = getBackgroundDrawable(view);
        parentLayout.setDrawableByLayerId(1, new InsetDrawable(
                backgroundDrawable,
                inset, inset, inset, inset));
    }

    private void applyDrawnBorder(Frame component, APLAbsoluteLayout view) {
        applyDrawnBorderInternal(component, view);

        // Re-adjust radii as it should take drawn border inset into account
        applyBorderRadii(component, view);

        // Also resize the shader to properly fit (as it fills inside of the drawn border)
        applyBackground(component, view);
    }

    private void applyBorderColor(Frame component, APLAbsoluteLayout view) {
        ShapeDrawable borderDrawable = getBorderDrawable(view);
        borderDrawable.getPaint().setColor(component.getBorderColor());
        borderDrawable.invalidateSelf();
    }

    private void applyBorder(Frame component, APLAbsoluteLayout view) {
        view.requestChildClippingPathUpdate();
    }

    /**
     * {@inheritDoc}
     */
    void applyPadding(Frame component, APLAbsoluteLayout view) {
        // No need to calculate padding as it is calculated by layout positioning
        // Furthermore, padding causes clipping that is incompatible with Flexbox layouts.
    }
}