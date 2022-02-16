/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters;

import android.content.Context;
import android.renderscript.RenderScript;

import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class RenderScriptProviderTest extends ViewhostRobolectricTest {
    @Mock
    Context context;
    @Mock
    RenderScript renderScript;

    @Spy
    RenderScriptFactory factory = new RenderScriptFactory() {
        @Override
        public RenderScript create(Context context) {
            return renderScript;
        }
    };

    RenderScriptProvider renderScriptProvider;

    @Before
    public void setup() {
        renderScriptProvider = new RenderScriptProvider(factory, context);
    }

    @Test
    public void testGet() {
        RenderScript result1 = renderScriptProvider.get();
        RenderScript result2 = renderScriptProvider.get();

        verify(factory, times(1)).create(context);
        assertEquals(renderScript, result1);
        assertEquals(renderScript, result2);
    }

    @Test
    public void testDestroy() {
        renderScriptProvider.destroy();
        renderScriptProvider.get();
        renderScriptProvider.destroy();

        verify(renderScript, times(1)).destroy();
    }

}
