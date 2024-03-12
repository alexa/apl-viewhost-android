/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.graphic;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.android.bitmap.BitmapCreationException;
import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.enums.VectorGraphicScale;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.Collections;
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
    public void test_draw_hardwareAcceleration_unset_draws_on_bitmap() throws BitmapCreationException{
        GraphicContainerElement graphicContainerElement = mock(GraphicContainerElement.class);
        PathRenderer pathRenderer = mock(PathRenderer.class);
        when(pathRenderer.getRootGroup()).thenReturn(graphicContainerElement);
        mAlexaVectorDrawable.setBounds(0, 0, 10, 10);

        // draw
        mAlexaVectorDrawable.draw(mockCanvas);
        // verify that bitmap was created instead of drawing straight into the canvas
        verify(mBitmapFactory).createBitmap(10, 10);
    }

    @Test
    public void test_draw_hardwareAcceleration_disabled_draws_on_bitmap() throws BitmapCreationException{
        RenderingContext rc = RenderingContext.builder()
                .isHardwareAccelerationForVectorGraphicsEnabled(false)
                .build();
        when(mGraphicContainerElement.getRenderingContext()).thenReturn(rc);
        GraphicContainerElement graphicContainerElement = mock(GraphicContainerElement.class);
        PathRenderer pathRenderer = mock(PathRenderer.class);
        when(pathRenderer.getRootGroup()).thenReturn(graphicContainerElement);
        mAlexaVectorDrawable.setBounds(0, 0, 10, 10);

        // draw
        mAlexaVectorDrawable.draw(mockCanvas);
        // verify that bitmap was created instead of drawing straight into the canvas
        verify(mBitmapFactory).createBitmap(10, 10);
    }

    @Test
    public void test_draw_hardwareAcceleration_enabled_draws_on_canvas() throws BitmapCreationException {
        RenderingContext rc = RenderingContext.builder()
                .isHardwareAccelerationForVectorGraphicsEnabled(true)
                .build();
        when(mGraphicContainerElement.getRenderingContext()).thenReturn(rc);
        when(mPathRenderer.getRootGroup()).thenReturn(mGraphicContainerElement);
        when(mBitmapFactory.createBitmap(10, 10)).thenReturn(mBitmap);
        mVectorState = new AlexaVectorDrawable.VectorDrawableCompatState(mPathRenderer, mBitmapFactory);
        mAlexaVectorDrawable = new AlexaVectorDrawable(mVectorState);
        mAlexaVectorDrawable.setBounds(0, 0, 10, 10);

        // draw
        mAlexaVectorDrawable.draw(mockCanvas);

        ArgumentCaptor<Rect> captor = ArgumentCaptor.forClass(Rect.class);
        verify(mockCanvas, atLeast(1)).clipRect(captor.capture());
        verify(mPathRenderer, atLeast(1)).draw(mockCanvas, 10, 10, mBitmapFactory, true);
        Rect rect = captor.getValue();
        assertEquals(0, rect.left);
        assertEquals(0, rect.top);
        assertEquals(10, rect.right);
        assertEquals(10, rect.bottom);
        verifyNoInteractions(mBitmapFactory);
    }
}
