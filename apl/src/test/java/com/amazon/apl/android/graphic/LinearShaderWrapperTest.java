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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import static org.mockito.Mockito.when;

public class LinearShaderWrapperTest extends ViewhostRobolectricTest {
    @Mock
    private Gradient mockLinearGraphicGradient;
    @Mock
    private Rect mockBounds;
    private Matrix mTransform;
    private int[] mColorRange;
    private float[] mInputRange;

    @Before
    public void setup() {
        when(mockLinearGraphicGradient.getType()).thenReturn(GradientType.LINEAR);
        when(mockLinearGraphicGradient.getSpreadMethod()).thenReturn(GradientSpreadMethod.PAD);
        when(mockLinearGraphicGradient.getX1()).thenReturn(0.3f);
        when(mockLinearGraphicGradient.getY1()).thenReturn(0.3f);
        when(mockLinearGraphicGradient.getX2()).thenReturn(0.7f);
        when(mockLinearGraphicGradient.getY2()).thenReturn(0.7f);
        when(mockLinearGraphicGradient.getUnits()).thenReturn(GradientUnits.kGradientUnitsBoundingBox);
        mColorRange = new int[] {Color.argb(1, 255, 0, 0),
                Color.argb(1, 0, 255, 0),
                Color.argb(1, 0, 0, 255)};
        when(mockLinearGraphicGradient.getColorRange()).thenReturn(mColorRange);
        mInputRange = new float[]{0.0f, 0.5f, 1.0f};
        when(mockLinearGraphicGradient.getInputRange()).thenReturn(mInputRange);
        mTransform = new Matrix();
        mTransform.postRotate(45);
        when(mockBounds.width()).thenReturn(100);
        when(mockBounds.height()).thenReturn(100);
    }

    /**
     * Verify that the linear gradient coordinates are calculated correctly.
     * https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-avg-format.html#linear
     */
    @Test
    public void testShaderInput_boundingBox() {
        final Rect bounds = new Rect(10, 20, 100, 100);
        LinearShaderWrapper linearShaderWrapper = new LinearShaderWrapper(mockLinearGraphicGradient, bounds, mTransform);
        assertEquals(37, linearShaderWrapper.getX1(), 0.01f);
        assertEquals(44, linearShaderWrapper.getY1(), 0.01f);
        assertEquals(73, linearShaderWrapper.getX2(), 0.01f);
        assertEquals(76, linearShaderWrapper.getY2(), 0.01f);
        assertArrayEquals(mColorRange, linearShaderWrapper.getColorRange());
        assertArrayEquals(mInputRange, linearShaderWrapper.getInputRange(), 0.01f);
        assertEquals(mTransform, linearShaderWrapper.getTransform());
        assertEquals(Shader.TileMode.CLAMP, linearShaderWrapper.getTileMode());
    }

    /**
     * Verify that the linear gradient coordinates are calculated correctly.
     * https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-avg-format.html#linear
     */
    @Test
    public void testShaderInput_userSpace() {
        when(mockLinearGraphicGradient.getUnits()).thenReturn(GradientUnits.kGradientUnitsUserSpace);
        final Rect bounds = new Rect(10, 20, 210, 220);
        when(mockLinearGraphicGradient.getX1()).thenReturn(100f);
        when(mockLinearGraphicGradient.getY1()).thenReturn(0f);
        when(mockLinearGraphicGradient.getX2()).thenReturn(0f);
        when(mockLinearGraphicGradient.getY2()).thenReturn(100f);
        LinearShaderWrapper linearShaderWrapper = new LinearShaderWrapper(mockLinearGraphicGradient, bounds, mTransform);
        assertEquals(110, linearShaderWrapper.getX1(), 0.01f);
        assertEquals(20, linearShaderWrapper.getY1(), 0.01f);
        assertEquals(10, linearShaderWrapper.getX2(), 0.01f);
        assertEquals(120, linearShaderWrapper.getY2(), 0.01f);
        assertArrayEquals(mColorRange, linearShaderWrapper.getColorRange());
        assertArrayEquals(mInputRange, linearShaderWrapper.getInputRange(), 0.01f);
        assertEquals(mTransform, linearShaderWrapper.getTransform());
        assertEquals(Shader.TileMode.CLAMP, linearShaderWrapper.getTileMode());
    }

    /**
     * Verify that the right {@link Shader.TileMode} is selected based on the spreadMethod.
     * https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-avg-format.html#spreadmethod
     */
    @Test
    public void testGetTileMode() {
        when(mockLinearGraphicGradient.getSpreadMethod()).thenReturn(GradientSpreadMethod.PAD);
        LinearShaderWrapper linearShaderWrapper = new LinearShaderWrapper(mockLinearGraphicGradient, mockBounds, mTransform);
        assertEquals(Shader.TileMode.CLAMP, linearShaderWrapper.getTileMode());
        when(mockLinearGraphicGradient.getSpreadMethod()).thenReturn(GradientSpreadMethod.REFLECT);
        linearShaderWrapper = new LinearShaderWrapper(mockLinearGraphicGradient, mockBounds, mTransform);
        assertEquals(Shader.TileMode.MIRROR, linearShaderWrapper.getTileMode());
        when(mockLinearGraphicGradient.getSpreadMethod()).thenReturn(GradientSpreadMethod.REPEAT);
        linearShaderWrapper = new LinearShaderWrapper(mockLinearGraphicGradient, mockBounds, mTransform);
        assertEquals(Shader.TileMode.REPEAT, linearShaderWrapper.getTileMode());
    }

    @Test
    public void testTransformIsApplied() {
        LinearShaderWrapper linearShaderWrapper = new LinearShaderWrapper(mockLinearGraphicGradient, mockBounds, mTransform);
        Matrix localMatrix = new Matrix();
        linearShaderWrapper.getShader().getLocalMatrix(localMatrix);
        assertEquals(mTransform, localMatrix);
    }
}
