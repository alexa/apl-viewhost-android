/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.views;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.graphics.Canvas;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import com.amazon.apl.android.primitive.SGRect;
import com.amazon.apl.android.scenegraph.APLLayer;
import com.amazon.apl.android.sgcontent.Node;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

// TODO: Hardware acceleration needs to be handled better
@Ignore
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 22, manifest = Config.NONE)
public class APLViewTest {
    private APLView mAPLView;

    @Mock
    private APLLayer mockAPLLayer;
    @Mock
    private SGRect mockSGRect;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(mockSGRect.intWidth()).thenReturn(200);
        when(mockSGRect.intHeight()).thenReturn(100);
        when(mockAPLLayer.getBounds()).thenReturn(mockSGRect);
        when(mockAPLLayer.getContent()).thenReturn(new Node[]{});
        mAPLView = new APLView(ApplicationProvider.getApplicationContext(), mockAPLLayer);
        // Test construction
        assertFalse(mAPLView.willNotDraw());
        assertEquals(200, mAPLView.getLayoutParams().width);
        assertEquals(100, mAPLView.getLayoutParams().height);
    }

    @Test
    public void testOnDraw_disables_hardware_acceleration() {
        Canvas canvas = new Canvas();
        mAPLView.onDraw(canvas);
        assertEquals(View.LAYER_TYPE_SOFTWARE, mAPLView.getLayerType());
    }

    @Test
    public void testDrawChild_enables_hardware_acceleration() {
        Canvas canvas = new Canvas();
        mAPLView.drawChild(canvas, mock(View.class), 0);
        assertEquals(View.LAYER_TYPE_HARDWARE, mAPLView.getLayerType());
    }
}
