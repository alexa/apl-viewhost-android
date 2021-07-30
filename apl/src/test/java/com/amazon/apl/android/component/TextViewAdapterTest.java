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

import com.amazon.apl.android.ITextMeasurementCache;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.StaticLayoutBuilder;
import com.amazon.apl.android.Text;
import com.amazon.apl.android.views.APLTextView;
import com.amazon.apl.enums.LayoutDirection;
import com.amazon.apl.enums.PropertyKey;

import org.junit.Test;
import org.mockito.Mock;

import static com.amazon.apl.enums.TextAlignVertical.kTextAlignVerticalAuto;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class TextViewAdapterTest extends AbstractComponentViewAdapterTest<Text, APLTextView> {

    private static final String MOCK_TEXT_COMPONENT_ID = "mockTextComponentId";
    @Mock
    private Text mockText;
    @Mock
    private ITextMeasurementCache mockTextMeasurementCache;

    private StaticLayout mStaticLayout;

    @Override
    Text component() {
        return mockText;
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
        when(mockTextMeasurementCache.getStaticLayout(MOCK_TEXT_COMPONENT_ID)).thenReturn(mStaticLayout);
        when(component().getTextMeasurementCache()).thenReturn(mockTextMeasurementCache);
        when(component().getComponentId()).thenReturn(MOCK_TEXT_COMPONENT_ID);
        when(component().getTextAlignVertical()).thenReturn(kTextAlignVerticalAuto);
        when(mockText.getLayoutDirection()).thenReturn(LayoutDirection.kLayoutDirectionRTL);
    }

    @Test
    public void testApplyProperty() {
        applyAllProperties();
        verify(component()).getLayoutDirection();
        verify(component()).measureTextContent(0.0f, 198.0f, RootContext.MeasureMode.Exactly, 48.0f, RootContext.MeasureMode.Exactly);
        verify(component()).getTextMeasurementCache();
        verify(mockTextMeasurementCache).getStaticLayout(MOCK_TEXT_COMPONENT_ID);
        verify(component()).getComponentId();
        verify(component()).getTextMeasurementCache();
        assertEquals(mStaticLayout, getView().getLayout());
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
        verify(component()).getInnerBounds();
        verify(component()).measureTextContent(0.0f, 198.0f, RootContext.MeasureMode.Exactly, 48.0f, RootContext.MeasureMode.Exactly);
        verify(mockTextMeasurementCache).getStaticLayout(MOCK_TEXT_COMPONENT_ID);
        verify(component()).getComponentId();
        verify(component()).getTextMeasurementCache();
        assertEquals(mStaticLayout, textView.getLayout());
        verifyNoMoreInteractions(component());
    }
}
