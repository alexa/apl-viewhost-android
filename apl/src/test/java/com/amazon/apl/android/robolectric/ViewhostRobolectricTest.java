/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.robolectric;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowBitmap;
import org.robolectric.shadows.ShadowCanvas;

import java.nio.Buffer;
import java.nio.ByteBuffer;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 22, manifest = Config.NONE)
public abstract class ViewhostRobolectricTest {

    @Before
    public final void setupViewhostRobolectricTest() {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public final void teardownViewhostRobolectricTest() {
        Mockito.framework().clearInlineMocks();
    }

    public static Application getApplication() {
        return ApplicationProvider.getApplicationContext();
    }

    @Implements(Bitmap.class)
    public static class MyShadowBitmap extends ShadowBitmap {
        public MyShadowBitmap() {
            setConfig(Bitmap.Config.ARGB_8888);
        }
    }

    /**
     * Robolectric's Canvas is broken, and PooledBitmapFactory is using it to copy bitmaps.
     * This class implements just enough to make unit tests work.
     *
     * See: http://robolectric.org/javadoc/4.2/org/robolectric/shadows/ShadowCanvas.html
     */
    @Implements(Canvas.class)
    public static class MyShadowCanvas extends ShadowCanvas {
        Bitmap bitmap;

        @Override
        @Implementation
        protected void __constructor__(Bitmap bitmap) {
            super.__constructor__(bitmap);
            this.bitmap = bitmap;
        }

        @Override
        @Implementation
        protected void drawBitmap(Bitmap source, Matrix matrix, Paint paint) {
            Buffer buf = ByteBuffer.allocate(source.getByteCount());
            source.copyPixelsToBuffer(buf);
            buf.rewind();
            bitmap.copyPixelsFromBuffer(buf);
        }
    }
}
