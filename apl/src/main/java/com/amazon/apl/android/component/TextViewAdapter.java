/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.content.Context;
import android.text.StaticLayout;
import android.view.Gravity;
import android.view.View;

import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.Text;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.android.views.APLTextView;
import com.amazon.apl.enums.LayoutDirection;
import com.amazon.apl.enums.PropertyKey;
import com.amazon.apl.enums.TextAlignVertical;

public class TextViewAdapter extends ComponentViewAdapter<Text, APLTextView> {
    private static TextViewAdapter INSTANCE;

    private TextViewAdapter() {
        super();
        putPropertyFunction(PropertyKey.kPropertyLang, this::applyLayout);
        putPropertyFunction(PropertyKey.kPropertyText, this::applyLayout);
        putPropertyFunction(PropertyKey.kPropertyColor, this::applyLayout);
        putPropertyFunction(PropertyKey.kPropertyColorKaraokeTarget, this::applyLayout);
        putPropertyFunction(PropertyKey.kPropertyColorNonKaraoke, this::applyLayout);
        putPropertyFunction(PropertyKey.kPropertyInnerBounds, this::applyProperties);
        putPropertyFunction(PropertyKey.kPropertyLayoutDirection, this::applyTextLayoutDirection);
    }

    /**
     * @return the TextViewAdapter instance.
     */
    public static TextViewAdapter getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new TextViewAdapter();
        }
        return INSTANCE;
    }

    @Override
    public APLTextView createView(Context context, IAPLViewPresenter presenter) {
        return new APLTextView(context, presenter);
    }

    @Override
    void applyPadding(Text component, APLTextView view) {
        setPaddingFromBounds(component, view, false);
    }

    @Override
    public void applyAllProperties(Text component, APLTextView view) {
        super.applyAllProperties(component, view);
        applyProperties(component, view);
    }

    @Override
    public void requestLayout(Text component, APLTextView view) {
        super.requestLayout(component, view);
        applyProperties(component, view);
    }

    private void applyTextLayoutDirection(Text component, APLTextView view) {
        applyLayoutDirection(component, view);
        applyLayout(component, view);
    }

    private void applyProperties(Text component, APLTextView view) {
        applyLayoutDirection(component, view);
        applyLayout(component, view);
        applyGravity(component, view);
    }

    private void applyLayoutDirection(Text component, APLTextView view) {
        int layoutDirection = LayoutDirection.kLayoutDirectionRTL == component.getLayoutDirection() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR;
        view.setLayoutDirection(layoutDirection);
    }

    private void applyLayout(Text component, APLTextView view) {
        // If the text was never measured, build the layout now
        final Rect bounds = component.getInnerBounds();

        component.measureTextContent(
                view.getDensity(),
                bounds.getWidth(), RootContext.MeasureMode.Exactly,
                bounds.getHeight(), RootContext.MeasureMode.Exactly
        );

        // Besides measuring in measureTextContent a new layout might be created due to input
        // changes (ex. karaoke). So getting layout from cache has to be done always after measureTextContent.
        final StaticLayout textLayout = component.getTextMeasurementCache().getStaticLayout(component.getComponentId());
        view.setLayout(textLayout);
    }

    private void applyGravity(Text component, APLTextView view) {
        final TextAlignVertical textAlignVertical = component.getTextAlignVertical();
        int gravityVertical = view.getVerticalGravity() & Gravity.VERTICAL_GRAVITY_MASK;
        switch (textAlignVertical) {
            case kTextAlignVerticalTop:
                gravityVertical = Gravity.TOP;
                break;
            case kTextAlignVerticalCenter:
                gravityVertical = Gravity.CENTER_VERTICAL;
                break;
            case kTextAlignVerticalBottom:
                gravityVertical = Gravity.BOTTOM;
                break;
            case kTextAlignVerticalAuto:
                gravityVertical = Gravity.TOP;
                break;
        }
        view.setVerticalGravity(gravityVertical);
    }
}
