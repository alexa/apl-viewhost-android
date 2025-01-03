/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.graphic;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.android.bitmap.BitmapCreationException;
import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.android.scaling.MetricsTransform;
import com.amazon.apl.enums.VectorGraphicScale;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AlexaVectorDrawableTest extends ViewhostRobolectricTest {
    @Mock
    private PathRenderer mPathRenderer;

    @Mock
    private IBitmapFactory mBitmapFactory;

    @Mock
    private GraphicContainerElement mGraphicContainerElement;

    @Mock
    private Canvas mockCanvas;

    private VectorGraphicScale mScale;

    private AlexaVectorDrawable.VectorDrawableCompatState mVectorState;

    private AlexaVectorDrawable mAlexaVectorDrawable;

    private Bitmap mBitmap;

    @Before
    public void setUp() throws BitmapCreationException {
        mBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
        mVectorState = new AlexaVectorDrawable.VectorDrawableCompatState(mPathRenderer, mBitmapFactory);
        mAlexaVectorDrawable = new AlexaVectorDrawable(mVectorState);
        when(mPathRenderer.getRootGroup()).thenReturn(mGraphicContainerElement);
        when(mBitmapFactory.createBitmap(10, 10)).thenReturn(mBitmap);
    }

    @Test
    public void test_updateDirtyGraphics_updatesViewport() {
        // Given
        Set<Integer> graphics = Collections.singleton(1);

        // When
        mAlexaVectorDrawable.updateDirtyGraphics(graphics);

        // Then
        verify(mPathRenderer).applyBaseAndViewportDimensions();
    }

    @Test
    public void test_non_uniform_scaling_hierarchy_draws_on_bitmap() throws BitmapCreationException{
        GraphicContainerElement graphicContainerElement = mock(GraphicContainerElement.class);
        RenderingContext rc = mock(RenderingContext.class);
        PathRenderer pathRenderer = mock(PathRenderer.class);
        ITelemetryProvider telemetryProvider = mock(ITelemetryProvider.class);
        when(pathRenderer.getRootGroup()).thenReturn(graphicContainerElement);
        when(graphicContainerElement.getRenderingContext()).thenReturn(rc);
        when(rc.getTelemetryProvider()).thenReturn(telemetryProvider);
        when(graphicContainerElement.getViewportWidthActual()).thenReturn(100f);
        when(graphicContainerElement.getViewportHeightActual()).thenReturn(200f);

        MetricsTransform metricsTransform = mock(MetricsTransform.class);
        when(rc.getMetricsTransform()).thenReturn(metricsTransform);
        when(metricsTransform.toViewhost(50f)).thenReturn(100f);
        when(metricsTransform.toViewhost(100f)).thenReturn(200f);

        when(graphicContainerElement.doesMapContainNonUniformScaling()).thenReturn(true);

        AlexaVectorDrawable.VectorDrawableCompatState state = new AlexaVectorDrawable.VectorDrawableCompatState(pathRenderer, mBitmapFactory);
        AlexaVectorDrawable mAlexaVectorDrawable = new AlexaVectorDrawable(state);
        mAlexaVectorDrawable.setBounds(0, 0, 10, 10);
        mAlexaVectorDrawable.draw(mockCanvas);

        verify(mBitmapFactory).createBitmap(10, 10);
    }

    @Test
    public void test_skew_draws_on_bitmap() throws BitmapCreationException{
        GraphicContainerElement graphicContainerElement = mock(GraphicContainerElement.class);
        RenderingContext rc = mock(RenderingContext.class);
        PathRenderer pathRenderer = mock(PathRenderer.class);
        ITelemetryProvider telemetryProvider = mock(ITelemetryProvider.class);
        when(pathRenderer.getRootGroup()).thenReturn(graphicContainerElement);
        when(graphicContainerElement.getRenderingContext()).thenReturn(rc);
        when(rc.getTelemetryProvider()).thenReturn(telemetryProvider);
        when(graphicContainerElement.getViewportWidthActual()).thenReturn(100f);
        when(graphicContainerElement.getViewportHeightActual()).thenReturn(200f);

        MetricsTransform metricsTransform = mock(MetricsTransform.class);
        when(rc.getMetricsTransform()).thenReturn(metricsTransform);
        when(metricsTransform.toViewhost(50f)).thenReturn(100f);
        when(metricsTransform.toViewhost(100f)).thenReturn(200f);

        when(graphicContainerElement.doesMapContainsSkew()).thenReturn(true);

        AlexaVectorDrawable.VectorDrawableCompatState state = new AlexaVectorDrawable.VectorDrawableCompatState(pathRenderer, mBitmapFactory);
        AlexaVectorDrawable mAlexaVectorDrawable = new AlexaVectorDrawable(state);
        mAlexaVectorDrawable.setBounds(0, 0, 10, 10);
        mAlexaVectorDrawable.draw(mockCanvas);

        verify(mBitmapFactory).createBitmap(10, 10);
    }

    @Test
    public void test_drop_shadow_present_draws_on_bitmap() throws BitmapCreationException{
        GraphicContainerElement graphicContainerElement = mock(GraphicContainerElement.class);
        RenderingContext rc = mock(RenderingContext.class);
        PathRenderer pathRenderer = mock(PathRenderer.class);
        ITelemetryProvider telemetryProvider = mock(ITelemetryProvider.class);
        when(pathRenderer.getRootGroup()).thenReturn(graphicContainerElement);
        when(graphicContainerElement.getRenderingContext()).thenReturn(rc);
        when(rc.getTelemetryProvider()).thenReturn(telemetryProvider);
        when(graphicContainerElement.getViewportWidthActual()).thenReturn(100f);
        when(graphicContainerElement.getViewportHeightActual()).thenReturn(200f);

        MetricsTransform metricsTransform = mock(MetricsTransform.class);
        when(rc.getMetricsTransform()).thenReturn(metricsTransform);
        when(metricsTransform.toViewhost(50f)).thenReturn(100f);
        when(metricsTransform.toViewhost(100f)).thenReturn(200f);

        when(graphicContainerElement.doesMapContainFilters()).thenReturn(true);

        AlexaVectorDrawable.VectorDrawableCompatState state = new AlexaVectorDrawable.VectorDrawableCompatState(pathRenderer, mBitmapFactory);
        AlexaVectorDrawable mAlexaVectorDrawable = new AlexaVectorDrawable(state);
        mAlexaVectorDrawable.setBounds(0, 0, 10, 10);
        mAlexaVectorDrawable.draw(mockCanvas);

        verify(mBitmapFactory).createBitmap(10, 10);
    }

    @Test
    public void test_non_uniform_scaling_draws_on_bitmap() throws BitmapCreationException {
        GraphicContainerElement graphicContainerElement = mock(GraphicContainerElement.class);
        RenderingContext rc = mock(RenderingContext.class);
        PathRenderer pathRenderer = mock(PathRenderer.class);
        ITelemetryProvider telemetryProvider = mock(ITelemetryProvider.class);
        when(pathRenderer.getRootGroup()).thenReturn(graphicContainerElement);
        when(graphicContainerElement.getRenderingContext()).thenReturn(rc);
        when(rc.getTelemetryProvider()).thenReturn(telemetryProvider);
        when(graphicContainerElement.getViewportWidthActual()).thenReturn(100f);
        when(graphicContainerElement.getViewportHeightActual()).thenReturn(200f);

        MetricsTransform metricsTransform = mock(MetricsTransform.class);
        when(rc.getMetricsTransform()).thenReturn(metricsTransform);
        // use different values for non uniform scaling
        when(metricsTransform.toViewhost(50f)).thenReturn(100f);
        when(metricsTransform.toViewhost(100f)).thenReturn(200f);

        AlexaVectorDrawable.VectorDrawableCompatState state = new AlexaVectorDrawable.VectorDrawableCompatState(pathRenderer, mBitmapFactory);
        AlexaVectorDrawable mAlexaVectorDrawable = new AlexaVectorDrawable(state);
        mAlexaVectorDrawable.setBounds(0, 0, 10, 10);
        mAlexaVectorDrawable.draw(mockCanvas);

        verify(mBitmapFactory).createBitmap(10, 10);
    }

    @Test
    public void test_multiple_draw_same_avg_telemetry() {
        GraphicContainerElement graphicContainerElement = mock(GraphicContainerElement.class);
        RenderingContext rc = mock(RenderingContext.class);
        PathRenderer pathRenderer = mock(PathRenderer.class);
        ITelemetryProvider telemetryProvider = mock(ITelemetryProvider.class);
        when(pathRenderer.getRootGroup()).thenReturn(graphicContainerElement);
        when(graphicContainerElement.getRenderingContext()).thenReturn(rc);
        when(rc.getTelemetryProvider()).thenReturn(telemetryProvider);
        when(graphicContainerElement.getViewportWidthActual()).thenReturn(100f);
        when(graphicContainerElement.getViewportHeightActual()).thenReturn(200f);

        MetricsTransform metricsTransform = mock(MetricsTransform.class);
        when(rc.getMetricsTransform()).thenReturn(metricsTransform);
        when(metricsTransform.toViewhost(50f)).thenReturn(100f);
        when(metricsTransform.toViewhost(100f)).thenReturn(200f);

        AlexaVectorDrawable.VectorDrawableCompatState state = new AlexaVectorDrawable.VectorDrawableCompatState(pathRenderer, mBitmapFactory);
        AlexaVectorDrawable mAlexaVectorDrawable = new AlexaVectorDrawable(state);
        mAlexaVectorDrawable.setBounds(0, 0, 10, 10);

        // Drawing multiple times with the same graphic element
        for (int i = 0; i < 5; i++) {
            mAlexaVectorDrawable.draw(mockCanvas);
        }

        // Verify telemetry increment count called once only
        verify(telemetryProvider, times(1)).incrementCount(anyInt());
    }

    @Test
    public void test_draw_hardwareAcceleration_disabled_draws_on_bitmap() throws BitmapCreationException{
        GraphicContainerElement graphicContainerElement = mock(GraphicContainerElement.class);
        RenderingContext rc = mock(RenderingContext.class);
        PathRenderer pathRenderer = mock(PathRenderer.class);
        ITelemetryProvider telemetryProvider = mock(ITelemetryProvider.class);
        when(pathRenderer.getRootGroup()).thenReturn(graphicContainerElement);
        when(graphicContainerElement.getRenderingContext()).thenReturn(rc);
        when(rc.getTelemetryProvider()).thenReturn(telemetryProvider);
        // set the runtime flag to false
        when(rc.isRuntimeHardwareAccelerationEnabled()).thenReturn(false);
        when(graphicContainerElement.getViewportWidthActual()).thenReturn(100f);
        when(graphicContainerElement.getViewportHeightActual()).thenReturn(200f);

        MetricsTransform metricsTransform = mock(MetricsTransform.class);
        when(rc.getMetricsTransform()).thenReturn(metricsTransform);
        when(metricsTransform.toViewhost(50f)).thenReturn(100f);
        when(metricsTransform.toViewhost(100f)).thenReturn(200f);

        AlexaVectorDrawable.VectorDrawableCompatState state = new AlexaVectorDrawable.VectorDrawableCompatState(pathRenderer, mBitmapFactory);
        AlexaVectorDrawable mAlexaVectorDrawable = new AlexaVectorDrawable(state);
        mAlexaVectorDrawable.setBounds(0, 0, 10, 10);

        mAlexaVectorDrawable.draw(mockCanvas);
        verify(mBitmapFactory).createBitmap(10, 10);
    }
}