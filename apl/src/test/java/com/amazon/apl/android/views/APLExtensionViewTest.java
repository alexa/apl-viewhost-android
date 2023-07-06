/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.views;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import android.view.ViewGroup;

import com.amazon.apl.android.Frame;
import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class APLExtensionViewTest extends ViewhostRobolectricTest {
    private APLExtensionView mExtensionView;

    @Mock
    private IAPLViewPresenter mockPresenter;

    @Mock
    private Frame mockComponent;

    @Before
    public void setup() {
        mExtensionView = new APLExtensionView(getApplication(), mockPresenter);
        when(mockPresenter.findComponent(mExtensionView)).thenReturn(mockComponent);
    }

    @Test
    public void testView_MeasureRoundsFloats() {
        when(mockComponent.getBounds()).thenReturn(Rect.builder().width(.9f).height(1.2f).left(1).top(1).build());
        mExtensionView.onMeasure(1, 2);

        assertEquals(1, mExtensionView.getMeasuredHeight());
        assertEquals(1, mExtensionView.getMeasuredWidth());
    }

    @Test
    public void testView_DefaultLayoutRoundsFloats() {
        when(mockComponent.getBounds()).thenReturn(Rect.builder().width(.9f).height(1.2f).left(1).top(1).build());
        ViewGroup.LayoutParams params = mExtensionView.generateDefaultLayoutParams();

        assertEquals(1, params.height);
        assertEquals(1, params.width);
    }
}
