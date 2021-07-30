/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.graphic;

import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.Shader;

import com.amazon.apl.android.primitive.Gradient;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.enums.GradientSpreadMethod;
import com.amazon.apl.enums.GradientType;
import com.amazon.apl.enums.GradientUnits;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import static org.mockito.Mockito.when;

public class RadialShaderWrapperTest extends ViewhostRobolectricTest {
    @Mock
    private Gradient mockRadialGraphicGradient;
    @Mock
    private Rect mockBounds;
    private Matrix mTransform;
    private int[] mColorRange;
    private float[] mInputRange;

    @Before
    public void setup() {
        when(mockRadialGraphicGradient.getType()).thenReturn(GradientType.RADIAL);
        when(mockRadialGraphicGradient.getSpreadMethod()).thenReturn(GradientSpreadMethod.PAD);
        when(mockRadialGraphicGradient.getRadius()).thenReturn(0.5f);
        when(mockRadialGraphicGradient.getCenterX()).thenReturn(0.5f);
        when(mockRadialGraphicGradient.getCenterY()).thenReturn(0.5f);
        when(mockRadialGraphicGradient.getUnits()).thenReturn(GradientUnits.kGradientUnitsBoundingBox);
        mColorRange = new int[]{Color.RED, Color.BLUE};
        when(mockRadialGraphicGradient.getColorRange()).thenReturn(mColorRange);
        mInputRange = new float[]{0.0f, 0.5f};
        when(mockRadialGraphicGradient.getInputRange()).thenReturn(mInputRange);
        mTransform = new Matrix();
        mTransform.postRotate(45);
        when(mockBounds.width()).thenReturn(100);
        when(mockBounds.height()).thenReturn(100);
    }

    /**
     * Verify that the radial gradient coordinates are calculated correctly.
     * https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-avg-format.html#centerxcentery
     * https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-avg-format.html#radius
     */
    @Test
    public void testShaderInput_boundingBox() {
        final Rect bounds = new Rect(10, 20, 100, 100);
        RadialShaderWrapper radialShaderWrapper = new RadialShaderWrapper(mockRadialGraphicGradient, bounds, mTransform);
        assertEquals(55, radialShaderWrapper.getCenterX(), 0.01f);
        assertEquals(60, radialShaderWrapper.getCenterY(), 0.01f);
        assertEquals(30.10f, radialShaderWrapper.getScaledRadius(bounds, mockRadialGraphicGradient.getRadius()), 0.01f);
        assertArrayEquals(mColorRange, radialShaderWrapper.getColorRange());
        assertArrayEquals(mInputRange, radialShaderWrapper.getInputRange(), 0.01f);
        assertEquals(mTransform, radialShaderWrapper.getTransform());
        assertEquals(Shader.TileMode.CLAMP, radialShaderWrapper.getTileMode());
    }

    /**
     * Verify that the radial gradient coordinates are calculated correctly.
     * https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-avg-format.html#centerxcentery
     * https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-avg-format.html#radius
     */
    @Test
    public void testShaderInput_userSpace() {
        final Rect bounds = new Rect(10, 20, 100, 100);
        when(mockRadialGraphicGradient.getUnits()).thenReturn(GradientUnits.kGradientUnitsUserSpace);
        when(mockRadialGraphicGradient.getCenterX()).thenReturn(40f);
        when(mockRadialGraphicGradient.getCenterY()).thenReturn(50f);
        RadialShaderWrapper radialShaderWrapper = new RadialShaderWrapper(mockRadialGraphicGradient, bounds, mTransform);
        assertEquals(50, radialShaderWrapper.getCenterX(), 0.01f);
        assertEquals(70, radialShaderWrapper.getCenterY(), 0.01f);
        assertEquals(30.10f, radialShaderWrapper.getScaledRadius(bounds, mockRadialGraphicGradient.getRadius()), 0.01f);
        assertArrayEquals(mColorRange, radialShaderWrapper.getColorRange());
        assertArrayEquals(mInputRange, radialShaderWrapper.getInputRange(), 0.01f);
        assertEquals(mTransform, radialShaderWrapper.getTransform());
        assertEquals(Shader.TileMode.CLAMP, radialShaderWrapper.getTileMode());
    }

    @Test
    public void testTransformIsApplied() {
        RadialShaderWrapper radialShaderWrapper = new RadialShaderWrapper(mockRadialGraphicGradient, mockBounds, mTransform);
        Matrix localMatrix = new Matrix();
        radialShaderWrapper.getShader().getLocalMatrix(localMatrix);
        assertEquals(mTransform, localMatrix);
    }


    @Test
    public void testHandlingWhenScaledRadiusIsSizeZero() {
        final Rect bounds = new Rect(0, 0, 0, 0);
        RadialShaderWrapper radialShaderWrapper = new RadialShaderWrapper(mockRadialGraphicGradient, bounds, mTransform);
        Assert.assertNull(radialShaderWrapper.getShader());
    }
}
