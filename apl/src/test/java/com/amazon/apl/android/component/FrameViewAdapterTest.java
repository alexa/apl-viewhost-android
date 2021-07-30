/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.graphics.Color;
import android.view.View;

import com.amazon.apl.android.APLGradientDrawable;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.Frame;
import com.amazon.apl.android.PropertyMap;
import com.amazon.apl.android.primitive.Dimension;
import com.amazon.apl.android.primitive.Radii;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.android.views.APLAbsoluteLayout;
import com.amazon.apl.enums.ComponentType;
import com.amazon.apl.enums.PropertyKey;

import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class FrameViewAdapterTest extends AbstractComponentViewAdapterTest<Frame, APLAbsoluteLayout> {
    @Mock
    Frame mFrame;
    @Mock
    PropertyMap<Component, PropertyKey> mockPropertyMap;

    @Override
    Frame component() {
        return mFrame;
    }

    @Override
    void componentSetup() {
        // Do nothing
    }

    private APLGradientDrawable getBackground() {
        return (APLGradientDrawable) getView().getBackground();
    }

    @Config(sdk=24)
    @Test
    public void test_defaults() {
        super.test_defaults();
    }

    @Override
    public void assertDefaults() {
        assertEquals(Color.TRANSPARENT, getBackground().getColor().getDefaultColor());
        assertEquals(Color.TRANSPARENT, getBackground().getBorderColor());
        assertEquals(0, getBackground().getBorderWidth());
        try {
            float[] radii = getBackground().getCornerRadii();
            if (radii != null) {
                fail("CornerRadii should not be set.");
            }
        } catch (NullPointerException e) {
            // Either the radii is null or an NPE is thrown
            // Both cases indicate that setRadii was not called.
        }
    }

    @Config(sdk=28)
    @Test
    public void test_backgroundColor() {
        when(component().getBackgroundColor()).thenReturn(Color.BLUE);

        applyAllProperties();

        APLGradientDrawable background = (APLGradientDrawable) getView().getBackground();
        assertEquals(Color.BLUE, background.getColor().getDefaultColor());
    }

    @Config(sdk=24)
    @Test
    public void test_borderRadii() {
        // TODO tests for primitives
        Radii mockRadii = mock(Radii.class);
        float[] radii = new float[]{10f, 10f, 20f, 20f, 30f, 30f, 40f, 40f};
        when(mockRadii.toFloatArray()).thenReturn(radii);
        when(component().getBorderRadii()).thenReturn(mockRadii);

        applyAllProperties();

        assertArrayEquals(radii, getBackground().getCornerRadii(), 0.01f);
    }

    @Test
    public void test_borderWidth() {
        Dimension mockDimension = mock(Dimension.class);
        when(mockDimension.intValue()).thenReturn(10);
        when(component().getBorderWidth()).thenReturn(mockDimension);

        applyAllProperties();

        assertEquals(10, getBackground().getBorderWidth());
        assertEquals(Color.TRANSPARENT, getBackground().getBorderColor());
    }

    @Test
    public void test_borderColor_noWidth() {
        when(component().getBorderColor()).thenReturn(Color.BLUE);
        when(component().hasProperty(PropertyKey.kPropertyBorderColor)).thenReturn(true);

        applyAllProperties();

        assertEquals(0, getBackground().getBorderWidth());
        assertEquals(Color.BLUE, getBackground().getBorderColor());
    }

    @Test
    public void test_borderColor() {
        Dimension mockDimension = mock(Dimension.class);
        when(mockDimension.intValue()).thenReturn(15);
        when(component().getBorderWidth()).thenReturn(mockDimension);
        when(component().getBorderColor()).thenReturn(Color.BLUE);
        when(component().hasProperty(PropertyKey.kPropertyBorderColor)).thenReturn(true);

        applyAllProperties();

        assertEquals(15, getBackground().getBorderWidth());
        assertEquals(Color.BLUE, getBackground().getBorderColor());
    }

    @Config(sdk=28)
    @Test
    public void test_refresh_backgroundColor() {
        when(component().getBackgroundColor()).thenReturn(Color.RED);

        refreshProperties(PropertyKey.kPropertyBackgroundColor);

        //Test that only methods related to background are called when backgroundColor is dirty
        verify(component()).getBackgroundColor();
        verifyNoMoreInteractions(component());

        assertEquals(Color.RED, getBackground().getColor().getDefaultColor());
    }

    @Test
    public void test_refresh_borderColor() {
        Dimension mockDimension = mock(Dimension.class);
        when(mockDimension.intValue()).thenReturn(10);
        when(component().getBorderWidth()).thenReturn(mockDimension);
        when(component().getBorderColor()).thenReturn(Color.RED);
        when(component().hasProperty(PropertyKey.kPropertyBorderColor)).thenReturn(true);

        refreshProperties(PropertyKey.kPropertyBorderColor);

        verify(component()).getBorderColor();
        verify(component()).getBorderWidth();
        verify(component()).hasProperty(PropertyKey.kPropertyBorderColor);
        verifyNoMoreInteractions(component());

        assertEquals(10, getBackground().getBorderWidth());
        assertEquals(Color.RED, getBackground().getBorderColor());
    }

    @Test
    public void test_refresh_borderWidth() {
        Dimension mockDimension = mock(Dimension.class);
        when(mockDimension.intValue()).thenReturn(20);
        when(component().getBorderWidth()).thenReturn(mockDimension);
        when(component().hasProperty(PropertyKey.kPropertyBorderColor)).thenReturn(true);

        refreshProperties(PropertyKey.kPropertyBorderWidth);

        verify(component()).getBorderColor();
        verify(component()).getBorderWidth();
        verify(component()).hasProperty(PropertyKey.kPropertyBorderColor);
        verifyNoMoreInteractions(component());

        assertEquals(20, getBackground().getBorderWidth());
        assertEquals(Color.TRANSPARENT, getBackground().getBorderColor());
    }

    @Config(sdk=24)
    @Test
    public void test_refresh_borderRadii() {
        Radii mockRadii = mock(Radii.class);
        when(component().getBorderRadii()).thenReturn(mockRadii);
        float[] radii = new float[]{15f, 15f, 25f, 25f, 35f, 35f, 45f, 45f};
        when(mockRadii.toFloatArray()).thenReturn(radii);

        refreshProperties(PropertyKey.kPropertyBorderRadii);

        verify(component()).getBorderRadii();
        verifyNoMoreInteractions(component());

        assertArrayEquals(radii, getBackground().getCornerRadii(), 0.01f);
    }

    @Test
    public void test_refresh_displayedChildren() {
        Component mockChild = mock(Frame.class);
        View childView = new View(getApplication());
        when(mockChild.getComponentType()).thenReturn(ComponentType.kComponentTypeFrame);
        when(mockChild.getBounds()).thenReturn(Rect.builder().width(10).height(10).left(0).top(0).build());

        when(component().getDisplayedChildCount()).thenReturn(1);
        when(component().getDisplayedChildren()).thenReturn(Arrays.asList(mockChild));
        when(component().getProperties()).thenReturn(mockPropertyMap);
        when(mockPropertyMap.get(PropertyKey.kPropertyNotifyChildrenChanged)).thenReturn(new Object[]{});
        when(mMockPresenter.inflateComponentHierarchy(mockChild)).thenReturn(childView);

        // Check that view has no children
        assertEquals(0, getView().getChildCount());

        refreshProperties(PropertyKey.kPropertyNotifyChildrenChanged);

        assertEquals(1, getView().getChildCount());
        assertEquals(childView, getView().getChildAt(0));
    }
}
