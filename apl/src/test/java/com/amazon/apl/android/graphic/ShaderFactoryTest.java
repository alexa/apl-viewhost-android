/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.graphic;

import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;

import com.amazon.apl.android.APLVersionCodes;
import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.android.primitive.Gradient;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.enums.GradientSpreadMethod;
import com.amazon.apl.enums.GradientType;
import com.amazon.apl.enums.GradientUnits;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import static org.mockito.Mockito.when;

public class ShaderFactoryTest extends ViewhostRobolectricTest {
    @Mock
    private RenderingContext mockRenderingContext;
    @Mock
    private Gradient mockLinearGraphicGradient;
    @Mock
    private Gradient mockRadialGraphicGradient;
    @Mock
    private Rect mockBounds;
    @Mock
    private Matrix mockTransform;

    @Before
    public void setup() {
        when(mockRenderingContext.getDocVersion()).thenReturn(APLVersionCodes.APL_1_6);
        when(mockLinearGraphicGradient.getType()).thenReturn(GradientType.LINEAR);
        when(mockLinearGraphicGradient.getSpreadMethod()).thenReturn(GradientSpreadMethod.PAD);
        when(mockLinearGraphicGradient.getX1()).thenReturn(0.3f);
        when(mockLinearGraphicGradient.getY1()).thenReturn(0.3f);
        when(mockLinearGraphicGradient.getX2()).thenReturn(0.7f);
        when(mockLinearGraphicGradient.getY2()).thenReturn(0.7f);
        when(mockLinearGraphicGradient.getUnits()).thenReturn(GradientUnits.kGradientUnitsBoundingBox);
        int[] colorRange = new int[] {Color.argb(1, 255, 0, 0),
                Color.argb(1, 0, 255, 0),
                Color.argb(1, 0, 0, 255)};
        when(mockLinearGraphicGradient.getColorRange()).thenReturn(colorRange);
        float[] inputRange = new float[]{0.0f, 0.5f, 1.0f};
        when(mockLinearGraphicGradient.getInputRange()).thenReturn(inputRange);

        when(mockRadialGraphicGradient.getType()).thenReturn(GradientType.RADIAL);
        when(mockRadialGraphicGradient.getSpreadMethod()).thenReturn(GradientSpreadMethod.PAD);
        when(mockRadialGraphicGradient.getRadius()).thenReturn(0.5f);
        when(mockRadialGraphicGradient.getCenterX()).thenReturn(0.5f);
        when(mockRadialGraphicGradient.getCenterY()).thenReturn(0.5f);
        when(mockRadialGraphicGradient.getColorRange()).thenReturn(colorRange);
        when(mockRadialGraphicGradient.getInputRange()).thenReturn(inputRange);
        when(mockRadialGraphicGradient.getUnits()).thenReturn(GradientUnits.kGradientUnitsBoundingBox);

        when(mockBounds.width()).thenReturn(100);
        when(mockBounds.height()).thenReturn(100);
    }

    @Test
    public void testGetInstance() {
        assertNotNull(ShaderFactory.getInstance());
    }

    @Test
    public void getShader_linearGradient() {
        final Shader shader = ShaderFactory.getInstance().getShader(mockLinearGraphicGradient, mockBounds, mockTransform, mockRenderingContext);
        assertNotNull(shader);
        assertThat(shader, instanceOf(LinearGradient.class));
    }

    @Test
    public void getShader_radialGradient() {
        final Shader shader = ShaderFactory.getInstance().getShader(mockRadialGraphicGradient, mockBounds, mockTransform, mockRenderingContext);
        assertNotNull(shader);
        assertThat(shader, instanceOf(RadialGradient.class));
    }

    @Test
    public void getShader_radialGradientV2() {
        when(mockRenderingContext.getDocVersion()).thenReturn(APLVersionCodes.APL_1_7);
        final Shader shader = ShaderFactory.getInstance().getShader(mockRadialGraphicGradient, mockBounds, new Matrix(), mockRenderingContext);
        assertNotNull(shader);
        assertThat(shader, instanceOf(RadialGradient.class));
    }
}
