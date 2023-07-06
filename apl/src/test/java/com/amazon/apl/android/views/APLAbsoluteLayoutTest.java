/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.views;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.view.KeyEvent;
import android.view.ViewGroup;

import com.amazon.apl.android.Frame;
import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class APLAbsoluteLayoutTest extends ViewhostRobolectricTest {
    private APLAbsoluteLayout mAbsoluteLayout;

    @Mock
    private IAPLViewPresenter mockPresenter;

    @Mock
    private Frame mockComponent;

    @Before
    public void setup() {
        mAbsoluteLayout = new APLAbsoluteLayout(getApplication(), mockPresenter);
        when(mockPresenter.findComponent(mAbsoluteLayout)).thenReturn(mockComponent);
    }

    @Test
    public void testView_MeasureRoundsFloats() {
        when(mockComponent.getBounds()).thenReturn(Rect.builder().width(.9f).height(1.2f).left(1).top(1).build());
        mAbsoluteLayout.onMeasure(1, 2);

        assertEquals(1, mAbsoluteLayout.getMeasuredHeight());
        assertEquals(1, mAbsoluteLayout.getMeasuredWidth());
    }

    @Test
    public void testView_DefaultLayoutRoundsFloats() {
        when(mockComponent.getBounds()).thenReturn(Rect.builder().width(.9f).height(1.2f).left(1).top(1).build());
        ViewGroup.LayoutParams params = mAbsoluteLayout.generateDefaultLayoutParams();

        assertEquals(1, params.height);
        assertEquals(1, params.width);
    }
}
