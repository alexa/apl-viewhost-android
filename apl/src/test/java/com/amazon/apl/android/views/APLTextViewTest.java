/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.views;

import android.graphics.Canvas;
import android.text.StaticLayout;
import android.view.Gravity;

import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class APLTextViewTest extends ViewhostRobolectricTest {
    private APLTextView mAplTextView;

    @Mock
    private IAPLViewPresenter mockPresenter;
    @Mock
    private Canvas mockCanvas;
    @Mock
    private StaticLayout mockLayout;

    @Before
    public void setup() {
        when(mockPresenter.getDensity()).thenReturn(1.0f);
        mAplTextView = new APLTextView(getApplication(), mockPresenter);
        assertEquals(mockPresenter.getDensity(), mAplTextView.getDensity(), 0.01f);
    }

    @Test
    public void testOnDraw_gravity_centerVertical() {
        mAplTextView.setLayout(mockLayout);
        mAplTextView.setVerticalGravity(Gravity.CENTER_VERTICAL);
        mAplTextView.onDraw(mockCanvas);
        InOrder inOrder = Mockito.inOrder(mockLayout, mockCanvas);
        inOrder.verify(mockCanvas).save();
        inOrder.verify(mockCanvas).translate(0,0);
        inOrder.verify(mockLayout).draw(mockCanvas);
        inOrder.verify(mockCanvas).restore();
    }
}
