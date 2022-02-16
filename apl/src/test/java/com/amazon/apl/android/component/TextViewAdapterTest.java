/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.Gravity;
import android.view.View;

import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.android.StaticLayoutBuilder;
import com.amazon.apl.android.Text;
import com.amazon.apl.android.TextLayoutFactory;
import com.amazon.apl.android.TextMeasure;
import com.amazon.apl.android.TextProxy;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.android.views.APLTextView;
import com.amazon.apl.enums.LayoutDirection;
import com.amazon.apl.enums.PropertyKey;

import org.junit.Test;
import org.mockito.Mock;

import static com.amazon.apl.enums.TextAlignVertical.kTextAlignVerticalAuto;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class TextViewAdapterTest extends AbstractComponentViewAdapterTest<Text, APLTextView> {

    private static final String MOCK_TEXT_HASH = "mockTextHash";
    @Mock
    private Text mockText;
    @Mock
    private TextLayoutFactory mockLayoutFactory;
    @Mock
    private TextProxy mockTextProxy;
    @Mock
    private RenderingContext mMockRenderingContext;

    private StaticLayout mStaticLayout;

    @Override
    Text component() {
        return mockText;
    }

    TextProxy proxy() {
        return mockTextProxy;
    }

    @Override
    void componentSetup() throws StaticLayoutBuilder.LayoutBuilderException {
        mStaticLayout = StaticLayoutBuilder.create().
                textPaint(new TextPaint()).
                innerWidth(10).
                alignment(Layout.Alignment.ALIGN_CENTER).
                limitLines(false).
                maxLines(10).
                ellipsizedWidth(10).
                build();
        when(component().getProxy()).thenReturn(proxy());
        when(component().getRenderingContext()).thenReturn(mMockRenderingContext);
        when(mMockRenderingContext.getTextLayoutFactory()).thenReturn(mockLayoutFactory);
        when(mockLayoutFactory.getOrCreateTextLayout(anyInt(), eq(proxy()), anyInt(), any(), anyInt(), any()))
                .thenReturn(mStaticLayout);
        when(proxy().getTextAlignVertical()).thenReturn(kTextAlignVerticalAuto);
        when(proxy().getLayoutDirection()).thenReturn(LayoutDirection.kLayoutDirectionRTL);
        when(proxy().getVisualHash()).thenReturn(MOCK_TEXT_HASH);
        when(proxy().getInnerBounds()).thenReturn(Rect.builder().width(30).height(20).left(0).top(0).build());
    }

    /**
     * Apply properties should get a layout from the layout factory and add it to the view
     */
    @Test
    public void testApplyProperty() {
        applyAllProperties();
        verify(component()).getLayoutDirection();
        verifyComponentLayoutOnView(getView());
        assertEquals(Gravity.TOP, getView().getVerticalGravity());
    }

    @Test
    public void testRefreshProperties_color() {
        refreshProperties(PropertyKey.kPropertyColor);
        verifyComponentLayout();
    }

    @Test
    public void testRefreshProperties_colorNonKaraoke() {
        refreshProperties(PropertyKey.kPropertyColorNonKaraoke);
        verifyComponentLayout();
    }

    @Test
    public void testRefreshProperties_colorKaraokeTarget() {
        refreshProperties(PropertyKey.kPropertyColorKaraokeTarget);
        verifyComponentLayout();
    }

    @Test
    public void testRefreshProperties_text() {
        refreshProperties(PropertyKey.kPropertyText);
        verifyComponentLayout();
    }

    @Test
    public void testRefreshProperties_fontLanguage() {
        refreshProperties(PropertyKey.kPropertyLang);
        verifyComponentLayout();
    }

    @Test
    public void test_refresh_layoutDirection() {
        when(component().getLayoutDirection()).thenReturn(LayoutDirection.kLayoutDirectionRTL);
        APLTextView spyView = spy(getView());

        refreshProperties(spyView, PropertyKey.kPropertyLayoutDirection);

        verify(spyView).setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        verify(component()).getLayoutDirection();
        verifyComponentLayoutOnView(spyView);
    }

    private void verifyComponentLayout() {
        verifyComponentLayoutOnView(getView());
    }

    private void verifyComponentLayoutOnView(APLTextView textView) {
        verify(component()).getRenderingContext();
        verify(component()).getKaraokeLineSpan();
        verify(mockLayoutFactory).getOrCreateTextLayout(0, mockTextProxy,
                30, TextMeasure.MeasureMode.Exactly, 20, null);
        assertEquals(mStaticLayout, textView.getLayout());
    }
}
