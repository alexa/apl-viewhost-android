/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.content.Context;
import android.text.Layout;
import android.view.Gravity;
import android.view.View;

import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.android.Text;
import com.amazon.apl.android.TextLayoutFactory;
import com.amazon.apl.android.TextMeasure;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.android.scaling.IMetricsTransform;
import com.amazon.apl.android.utils.APLTrace;
import com.amazon.apl.android.utils.TracePoint;
import com.amazon.apl.android.views.APLTextView;
import com.amazon.apl.enums.LayoutDirection;
import com.amazon.apl.enums.PropertyKey;
import com.amazon.apl.enums.TextAlignVertical;

public class TextViewAdapter extends ComponentViewAdapter<Text, APLTextView> {
    private static final String TAG = "TextViewAdapter";
    private static TextViewAdapter INSTANCE;

    private TextViewAdapter() {
        super();
        putPropertyFunction(PropertyKey.kPropertyLang, this::applyLayout);
        putPropertyFunction(PropertyKey.kPropertyText, this::applyLayout);
        putPropertyFunction(PropertyKey.kPropertyColor, this::applyLayout);
        putPropertyFunction(PropertyKey.kPropertyColorKaraokeTarget, this::applyLayout);
        putPropertyFunction(PropertyKey.kPropertyColorNonKaraoke, this::applyLayout);
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
        applyLayout(component, view);
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
        APLTrace trace = component.getViewPresenter().getAPLTrace();
        trace.startTrace(TracePoint.TEXT_APPLY_LAYOUT);
        RenderingContext ctx = component.getRenderingContext();
        TextLayoutFactory factory = ctx.getTextLayoutFactory();
        int versionCode = ctx.getDocVersion();
        final Rect bounds = component.getProxy().getInnerBounds();
        // bounds dimensions are in pixels, convert to dp before layout creation
        IMetricsTransform metricsTransform = ctx.getMetricsTransform();
        Layout textLayout = factory.getOrCreateTextLayout(
                versionCode,
                component.getProxy(),
                metricsTransform.toCore(bounds.getWidth()),
                TextMeasure.MeasureMode.Exactly,
                metricsTransform.toCore(bounds.getHeight()),
                TextMeasure.MeasureMode.Exactly,
                component.getKaraokeLineSpan(),
                ctx.getMetricsTransform()
        ).getLayout();
        view.setLayout(textLayout);
        trace.endTrace();
    }

    private void applyGravity(Text component, APLTextView view) {
        final TextAlignVertical textAlignVertical = component.getProxy().getTextAlignVertical();
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
