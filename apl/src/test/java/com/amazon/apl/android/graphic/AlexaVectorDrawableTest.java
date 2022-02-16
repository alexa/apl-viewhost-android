/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.graphic;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;

import org.junit.Before;
import org.junit.Test;
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

    private AlexaVectorDrawable.VectorDrawableCompatState mVectorState;

    private AlexaVectorDrawable mAlexaVectorDrawable;

    @Before
    public void setUp() {
        mVectorState = new AlexaVectorDrawable.VectorDrawableCompatState(mPathRenderer, mBitmapFactory);
        mAlexaVectorDrawable = new AlexaVectorDrawable(mVectorState);
        when(mPathRenderer.getRootGroup()).thenReturn(mGraphicContainerElement);
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
}
