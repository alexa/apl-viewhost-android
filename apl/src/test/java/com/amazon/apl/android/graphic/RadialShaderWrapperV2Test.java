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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class RadialShaderWrapperV2Test extends ViewhostRobolectricTest {
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
        when(mockRadialGraphicGradient.getUnits()).thenReturn(GradientUnits.kGradientUnitsBoundingBox);
        mColorRange = new int[]{Color.RED, Color.BLUE};
        when(mockRadialGraphicGradient.getColorRange()).thenReturn(mColorRange);
        mInputRange = new float[]{0.0f, 0.5f};
        when(mockRadialGraphicGradient.getInputRange()).thenReturn(mInputRange);
        mTransform = new Matrix();
        when(mockBounds.width()).thenReturn(100);
        when(mockBounds.height()).thenReturn(100);
    }

    @Test
    public void testHandlingWhenScaledRadiusIsSizeZero() {
        final Rect bounds = new Rect(0, 0, 0, 0);
        RadialShaderWrapperV2 radialShaderWrapper = new RadialShaderWrapperV2(mockRadialGraphicGradient, bounds, mTransform);
        Assert.assertNull(radialShaderWrapper.getShader());
    }

    @Test
    public void testBoundingBoxUnitsSquare() {
        final Rect bounds = new Rect(0, 0, 100, 100);
        when(mockRadialGraphicGradient.getCenterX()).thenReturn(0.5f);
        when(mockRadialGraphicGradient.getCenterY()).thenReturn(0.5f);
        when(mockRadialGraphicGradient.getRadius()).thenReturn(0.5f);
        when(mockRadialGraphicGradient.getUnits()).thenReturn(GradientUnits.kGradientUnitsBoundingBox);
        RadialShaderWrapperV2 radialShaderWrapper = new RadialShaderWrapperV2(mockRadialGraphicGradient, bounds, mTransform);

        assertEquals(50, radialShaderWrapper.getCenterX(), 0.01f);
        assertEquals(50, radialShaderWrapper.getCenterY(), 0.01f);
        assertEquals(50f, radialShaderWrapper.getRadius(), 0.01f);
        assertArrayEquals(mColorRange, radialShaderWrapper.getColorRange());
        assertArrayEquals(mInputRange, radialShaderWrapper.getInputRange(), 0.01f);
        assertEquals(mTransform, radialShaderWrapper.getTransform());
        assertEquals(Shader.TileMode.CLAMP, radialShaderWrapper.getTileMode());

        // should not apply any scaling when the bounds are square
        Matrix localMatrix = new Matrix();
        radialShaderWrapper.getShader().getLocalMatrix(localMatrix);
        assertTrue(localMatrix.isIdentity());
    }

    @Test
    public void testBoundingBoxUnitsNonSquareWidth() {
        final Rect bounds = new Rect(0, 0, 200, 100);
        when(mockRadialGraphicGradient.getCenterX()).thenReturn(0.5f);
        when(mockRadialGraphicGradient.getCenterY()).thenReturn(0.5f);
        when(mockRadialGraphicGradient.getRadius()).thenReturn(0.5f);
        when(mockRadialGraphicGradient.getUnits()).thenReturn(GradientUnits.kGradientUnitsBoundingBox);
        RadialShaderWrapperV2 radialShaderWrapper = new RadialShaderWrapperV2(mockRadialGraphicGradient, bounds, mTransform);

        assertEquals(100, radialShaderWrapper.getCenterX(), 0.01f);
        assertEquals(50, radialShaderWrapper.getCenterY(), 0.01f);
        assertEquals(100f, radialShaderWrapper.getRadius(), 0.01f);
        assertArrayEquals(mColorRange, radialShaderWrapper.getColorRange());
        assertArrayEquals(mInputRange, radialShaderWrapper.getInputRange(), 0.01f);
        assertEquals(Shader.TileMode.CLAMP, radialShaderWrapper.getTileMode());

        // should scale Y by half
        Matrix localMatrix = new Matrix();
        radialShaderWrapper.getShader().getLocalMatrix(localMatrix);
        float[] localMatrixValues = new float[9];
        localMatrix.getValues(localMatrixValues);
        assertEquals(0.5f, localMatrixValues[4], 0.01f);
    }

    @Test
    public void testBoundingBoxUnitsNonSquareHeight() {
        final Rect bounds = new Rect(0, 0, 100, 200);
        when(mockRadialGraphicGradient.getCenterX()).thenReturn(0.5f);
        when(mockRadialGraphicGradient.getCenterY()).thenReturn(0.5f);
        when(mockRadialGraphicGradient.getRadius()).thenReturn(0.5f);
        when(mockRadialGraphicGradient.getUnits()).thenReturn(GradientUnits.kGradientUnitsBoundingBox);
        RadialShaderWrapperV2 radialShaderWrapper = new RadialShaderWrapperV2(mockRadialGraphicGradient, bounds, mTransform);

        assertEquals(50, radialShaderWrapper.getCenterX(), 0.01f);
        assertEquals(100, radialShaderWrapper.getCenterY(), 0.01f);
        assertEquals(100f, radialShaderWrapper.getRadius(), 0.01f);
        assertArrayEquals(mColorRange, radialShaderWrapper.getColorRange());
        assertArrayEquals(mInputRange, radialShaderWrapper.getInputRange(), 0.01f);
        assertEquals(Shader.TileMode.CLAMP, radialShaderWrapper.getTileMode());

        // should scale X by half
        Matrix localMatrix = new Matrix();
        radialShaderWrapper.getShader().getLocalMatrix(localMatrix);
        float[] localMatrixValues = new float[9];
        localMatrix.getValues(localMatrixValues);
        assertEquals(0.5f, localMatrixValues[0], 0.01f);
    }

    @Test
    public void testUserSpace() {
        final Rect bounds = new Rect(0, 0, 100, 100);
        when(mockRadialGraphicGradient.getUnits()).thenReturn(GradientUnits.kGradientUnitsUserSpace);
        when(mockRadialGraphicGradient.getCenterX()).thenReturn(50f);
        when(mockRadialGraphicGradient.getCenterY()).thenReturn(50f);
        when(mockRadialGraphicGradient.getRadius()).thenReturn(70f);
        when(mockRadialGraphicGradient.getUnits()).thenReturn(GradientUnits.kGradientUnitsUserSpace);
        RadialShaderWrapperV2 radialShaderWrapper = new RadialShaderWrapperV2(mockRadialGraphicGradient, bounds, mTransform);

        assertEquals(50, radialShaderWrapper.getCenterX(), 0.01f);
        assertEquals(50, radialShaderWrapper.getCenterY(), 0.01f);
        assertEquals(70, radialShaderWrapper.getRadius(), 0.01f);
        assertArrayEquals(mColorRange, radialShaderWrapper.getColorRange());
        assertArrayEquals(mInputRange, radialShaderWrapper.getInputRange(), 0.01f);
        assertEquals(mTransform, radialShaderWrapper.getTransform());
        assertEquals(Shader.TileMode.CLAMP, radialShaderWrapper.getTileMode());

        // should not apply any scaling in UserSpace mode
        Matrix localMatrix = new Matrix();
        radialShaderWrapper.getShader().getLocalMatrix(localMatrix);
        assertTrue(localMatrix.isIdentity());
    }

    @Test
    public void testUserSpaceNonSquare() {
        final Rect bounds = new Rect(0, 0, 200, 100);
        when(mockRadialGraphicGradient.getUnits()).thenReturn(GradientUnits.kGradientUnitsUserSpace);
        when(mockRadialGraphicGradient.getCenterX()).thenReturn(50f);
        when(mockRadialGraphicGradient.getCenterY()).thenReturn(50f);
        when(mockRadialGraphicGradient.getRadius()).thenReturn(70f);
        when(mockRadialGraphicGradient.getUnits()).thenReturn(GradientUnits.kGradientUnitsUserSpace);
        RadialShaderWrapperV2 radialShaderWrapper = new RadialShaderWrapperV2(mockRadialGraphicGradient, bounds, mTransform);

        assertEquals(50, radialShaderWrapper.getCenterX(), 0.01f);
        assertEquals(50, radialShaderWrapper.getCenterY(), 0.01f);
        assertEquals(70, radialShaderWrapper.getRadius(), 0.01f);
        assertArrayEquals(mColorRange, radialShaderWrapper.getColorRange());
        assertArrayEquals(mInputRange, radialShaderWrapper.getInputRange(), 0.01f);
        assertEquals(mTransform, radialShaderWrapper.getTransform());
        assertEquals(Shader.TileMode.CLAMP, radialShaderWrapper.getTileMode());

        // should not apply any scaling in UserSpace mode
        Matrix localMatrix = new Matrix();
        radialShaderWrapper.getShader().getLocalMatrix(localMatrix);
        assertTrue(localMatrix.isIdentity());
    }

}
