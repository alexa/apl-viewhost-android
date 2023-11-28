/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.graphics.Color;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.graphics.drawable.shapes.Shape;
import android.view.View;

import com.amazon.apl.android.Component;
import com.amazon.apl.android.Frame;
import com.amazon.apl.android.PropertyMap;
import com.amazon.apl.android.primitive.Dimension;
import com.amazon.apl.android.primitive.Gradient;
import com.amazon.apl.android.primitive.Radii;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.android.views.APLAbsoluteLayout;
import com.amazon.apl.enums.ComponentType;
import com.amazon.apl.enums.GradientType;
import com.amazon.apl.enums.PropertyKey;

import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
        when(component().getDrawnBorderWidth()).thenReturn(Dimension.create(0));
    }

    private android.graphics.Rect getBorderInset() {
        android.graphics.Rect insets = new android.graphics.Rect();
        LayerDrawable parentLayout = (LayerDrawable)getView().getBackground();
        InsetDrawable drawable = (InsetDrawable)parentLayout.getDrawable(1);

        drawable.getPadding(insets);

        return insets;
    }

    private ShapeDrawable getBackground() {
        LayerDrawable parentLayout = (LayerDrawable)getView().getBackground();
        InsetDrawable borderInset = (InsetDrawable)parentLayout.getDrawable(1);
        return (ShapeDrawable)borderInset.getDrawable();
    }

    private ShapeDrawable getBorder() {
        return (ShapeDrawable) ((LayerDrawable)getView().getBackground()).getDrawable(0);
    }

    private float[] getBorderRadii() {
        try {
            Shape borderShape = getBorder().getShape();
            assertTrue(borderShape instanceof RoundRectShape);
            Class obj = borderShape.getClass();
            Field field = obj.getDeclaredField("mOuterRadii");
            field.setAccessible(true);

            return (float[])field.get(borderShape);
        } catch (Exception e) {
            return new float[]{};
        }
    }

    @Config(sdk=24)
    @Test
    public void test_defaults() {
        super.test_defaults();
    }

    @Override
    public void assertDefaults() {
        assertEquals(Color.TRANSPARENT, getBackground().getPaint().getColor());
        assertEquals(Color.TRANSPARENT, getBorder().getPaint().getColor());
        assertEquals(0, getBorder().getPaint().getStrokeWidth(), 0);

        Shape borderShape = getBorder().getShape();
        assertNull(borderShape);
    }

    @Config(sdk=28)
    @Test
    public void test_backgroundColor() {
        when(component().getBackgroundColor()).thenReturn(Color.BLUE);

        applyAllProperties();

        assertEquals(Color.BLUE, getBackground().getPaint().getColor());
    }

    @Config(sdk=24)
    @Test
    public void test_borderRadii() {
        // TODO tests for primitives
        Radii mockRadii = mock(Radii.class);
        float[] radii = new float[]{10f, 10f, 20f, 20f, 30f, 30f, 40f, 40f};
        when(mockRadii.toFloatArray()).thenReturn(radii);
        when(component().getBorderRadii()).thenReturn(mockRadii);
        when(mockRadii.inset(anyFloat())).thenReturn(mockRadii);

        applyAllProperties();

        assertEquals(radii, getBorderRadii());
    }

    @Test
    public void test_borderWidth_noColor() {
        Dimension mockDimension = mock(Dimension.class);
        when(mockDimension.intValue()).thenReturn(10);
        when(component().getDrawnBorderWidth()).thenReturn(mockDimension);

        applyAllProperties();

        assertEquals(new android.graphics.Rect(10, 10, 10, 10), getBorderInset());
    }

    @Test
    public void test_borderColor_noWidth() {
        when(component().getBorderColor()).thenReturn(Color.BLUE);
        when(component().hasProperty(PropertyKey.kPropertyBorderColor)).thenReturn(true);

        applyAllProperties();

        assertEquals(0, getBorder().getPaint().getStrokeWidth(), 0);
        assertEquals(Color.BLUE, getBorder().getPaint().getColor());
    }

    @Test
    public void test_borderColor() {
        Dimension mockDimension = mock(Dimension.class);
        when(mockDimension.intValue()).thenReturn(15);
        when(component().getDrawnBorderWidth()).thenReturn(mockDimension);
        when(component().getBorderColor()).thenReturn(Color.BLUE);
        when(component().hasProperty(PropertyKey.kPropertyBorderColor)).thenReturn(true);

        applyAllProperties();

        assertEquals(new android.graphics.Rect(15, 15, 15, 15), getBorderInset());
        assertEquals(Color.BLUE, getBorder().getPaint().getColor());
    }

    @Config(sdk=28)
    @Test
    public void test_refresh_backgroundColor() {
        when(component().getBackgroundColor()).thenReturn(Color.RED);

        refreshProperties(PropertyKey.kPropertyBackground);

        //Test that only methods related to background are called when backgroundColor is dirty
        verify(component()).getBackgroundColor();
        verify(component()).getDrawnBorderWidth();
        verify(component()).isGradientBackground();
        verifyNoMoreInteractions(component());

        assertEquals(Color.RED, getBackground().getPaint().getColor());
    }

    @Test
    public void test_backgroundGradient() {
        when(component().isGradientBackground()).thenReturn(true);
        Gradient linearGradient = Gradient.builder().type(GradientType.LINEAR).angle(20).inputRange(new float[] {1, 2}).colorRange(new int[] {1, 2}).build();
        when(component().getBackgroundGradient()).thenReturn(linearGradient);
        when(component().getDrawnBorderWidth()).thenReturn(Dimension.create(0));

        applyAllProperties();

        verify(component(), atLeast(1)).isGradientBackground();
        verify(component()).getBackgroundGradient();
        assertTrue(getView().getBackground() instanceof LayerDrawable);
    }

    @Test
    public void test_backgroundGradientWithBorderColorUpdate() {
        when(component().isGradientBackground()).thenReturn(true);
        Gradient linearGradient = Gradient.builder().type(GradientType.LINEAR).angle(20).inputRange(new float[] {1, 2}).colorRange(new int[] {1, 2}).build();
        when(component().getBackgroundGradient()).thenReturn(linearGradient);
        when(component().getDrawnBorderWidth()).thenReturn(Dimension.create(5));
        when(component().getBorderRadii()).thenReturn(Radii.create(10));
        when(component().getBorderColor()).thenReturn(Color.GREEN);

        applyAllProperties();

        LayerDrawable parentLayout = (LayerDrawable)getView().getBackground();
        InsetDrawable borderInset = (InsetDrawable)parentLayout.getDrawable(1);
        ShapeDrawable borderDrawable = (ShapeDrawable)parentLayout.getDrawable(0);
        ShapeDrawable backgroundDrawable = (ShapeDrawable)borderInset.getDrawable();

        assertNotNull(backgroundDrawable.getPaint().getShader());
        assertEquals(Color.GREEN, borderDrawable.getPaint().getColor());

        when(component().getBorderColor()).thenReturn(Color.RED);

        refreshProperties(PropertyKey.kPropertyBorderColor);

        assertEquals(Color.RED, borderDrawable.getPaint().getColor());
    }

    @Config(sdk=28)
    @Test
    public void test_backgroundGradientToColor() {
        when(component().isGradientBackground()).thenReturn(true);
        Gradient linearGradient = Gradient.builder().type(GradientType.LINEAR).angle(20).inputRange(new float[] {1, 2}).colorRange(new int[] {1, 2}).build();
        when(component().getBackgroundGradient()).thenReturn(linearGradient);
        when(component().getDrawnBorderWidth()).thenReturn(Dimension.create(0));

        applyAllProperties();

        verify(component(), atLeast(1)).isGradientBackground();
        verify(component()).getBackgroundGradient();

        LayerDrawable parentLayout = (LayerDrawable)getView().getBackground();
        InsetDrawable borderInset = (InsetDrawable)parentLayout.getDrawable(1);
        ShapeDrawable backgroundDrawable = (ShapeDrawable)borderInset.getDrawable();
        assertNotNull(backgroundDrawable.getPaint().getShader());

        when(component().isGradientBackground()).thenReturn(false);
        when(component().getBackgroundColor()).thenReturn(Color.RED);

        refreshProperties(PropertyKey.kPropertyBackground);

        assertEquals(Color.RED, backgroundDrawable.getPaint().getColor());
        assertNull(backgroundDrawable.getPaint().getShader());
    }

    @Config(sdk=28)
    @Test
    public void test_backgroundColorToGradient() {
        when(component().isGradientBackground()).thenReturn(false);
        when(component().getBackgroundColor()).thenReturn(Color.RED);
        when(component().getDrawnBorderWidth()).thenReturn(Dimension.create(0));

        applyAllProperties();

        LayerDrawable parentLayout = (LayerDrawable)getView().getBackground();
        InsetDrawable borderInset = (InsetDrawable)parentLayout.getDrawable(1);
        ShapeDrawable backgroundDrawable = (ShapeDrawable)borderInset.getDrawable();
        assertNull(backgroundDrawable.getPaint().getShader());
        assertEquals(Color.RED, backgroundDrawable.getPaint().getColor());


        when(component().isGradientBackground()).thenReturn(true);
        Gradient linearGradient = Gradient.builder().type(GradientType.LINEAR).angle(20).inputRange(new float[] {1, 2}).colorRange(new int[] {1, 2}).build();
        when(component().getBackgroundGradient()).thenReturn(linearGradient);

        refreshProperties(PropertyKey.kPropertyBackground);

        verify(component(), atLeast(1)).isGradientBackground();
        verify(component()).getBackgroundGradient();
        assertNotNull(backgroundDrawable.getPaint().getShader());
    }

    @Test
    public void test_refresh_borderColor() {
        Dimension mockDimension = mock(Dimension.class);
        when(mockDimension.intValue()).thenReturn(10);
        when(component().getDrawnBorderWidth()).thenReturn(mockDimension);
        when(component().getBorderColor()).thenReturn(Color.RED);
        when(component().hasProperty(PropertyKey.kPropertyBorderColor)).thenReturn(true);

        refreshProperties(PropertyKey.kPropertyBorderColor);

        verify(component()).getBorderColor();
        verifyNoMoreInteractions(component());

//        assertEquals(10, getBorder().getPaint().getStrokeWidth(), 0);
        assertEquals(Color.RED, getBorder().getPaint().getColor());
    }

    @Test
    public void test_refresh_borderWidth() {
        Dimension mockDimension = mock(Dimension.class);
        when(mockDimension.intValue()).thenReturn(20);
        when(component().getDrawnBorderWidth()).thenReturn(mockDimension);
        when(component().hasProperty(PropertyKey.kPropertyBorderColor)).thenReturn(true);

        refreshProperties(PropertyKey.kPropertyDrawnBorderWidth);

        verify(component(), times(2)).getDrawnBorderWidth();
        verify(component()).getBorderRadii();
        verify(component()).isGradientBackground();
        verify(component()).getBackgroundColor();
        verifyNoMoreInteractions(component());

        assertEquals(new android.graphics.Rect(20, 20, 20, 20), getBorderInset());
    }

    @Config(sdk=24)
    @Test
    public void test_refresh_borderRadii() {
        Radii mockRadii = mock(Radii.class);
        when(component().getBorderRadii()).thenReturn(mockRadii);
        float[] radii = new float[]{15f, 15f, 25f, 25f, 35f, 35f, 45f, 45f};
        when(mockRadii.toFloatArray()).thenReturn(radii);
        when(mockRadii.inset(anyFloat())).thenReturn(mockRadii);

        refreshProperties(PropertyKey.kPropertyBorderRadii);

        verify(component()).getBorderRadii();
        verify(component()).getDrawnBorderWidth();
        verifyNoMoreInteractions(component());

        assertEquals(radii, getBorderRadii());
        assertTrue(getView().isChildClippingPathUpdateRequested());
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

    @Test
    public void test_zeroBorderWidthSetsBorderDrawableOpacityZero() {
        when(component().getDrawnBorderWidth()).thenReturn(Dimension.create(0));
        applyAllProperties();
        assertEquals(getBorder().getAlpha(), 0);
    }

    @Test
    public void test_nonZeroBorderWidthSetsBorderDrawableOpacity255() {
        when(component().getDrawnBorderWidth()).thenReturn(Dimension.create(10));
        applyAllProperties();
        assertEquals(getBorder().getAlpha(), 255);
    }
}
