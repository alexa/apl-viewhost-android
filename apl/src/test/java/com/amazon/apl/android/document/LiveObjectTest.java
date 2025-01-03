/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.document;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.Content;
import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.bitmap.ShadowCache;
import com.amazon.apl.android.scaling.ViewportMetrics;
import com.amazon.apl.android.utils.APLTrace;

import org.junit.Assert;
import org.mockito.Mockito;

import java.lang.reflect.Array;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public abstract class LiveObjectTest extends AbstractDocUnitTest {
    /**
     * Replace the stock "loadDocument" command with one that creates a LiveObject.
     * The LiveObject is initially empty.
     */
    protected void loadDocument(String doc, APLOptions options, ViewportMetrics metrics) {
        Content content = null;
        try {
            content = Content.create(doc);
        } catch (Content.ContentException e) {
            Assert.fail(e.getMessage());
        }
        assertTrue(content.isReady());
        mOptions = options;

        RootConfig rootConfig = RootConfig.create("Live Object Test", "1.0");
        registerLiveData(rootConfig);

        mAPLPresenter = mock(IAPLViewPresenter.class);
        mShadowCache = new ShadowCache();
        when(mockShadowRenderer.getCache()).thenReturn(mShadowCache);
        when(mAPLPresenter.getShadowRenderer()).thenReturn(mockShadowRenderer);
        when(mAPLPresenter.getAPLTrace()).thenReturn(mock(APLTrace.class));
        when(mMetricsRecorder.createCounter(anyString())).thenReturn(mCounter);
        when(mMetricsRecorder.startTimer(anyString(), any())).thenReturn(mTimer);
        mRootContext = Mockito.spy(RootContext.create(metrics, content, rootConfig, mOptions, mAPLPresenter, mMetricsRecorder, mFluidityIncidentReporter));
        assertNotNull(mRootContext);

        mRootContext.initTime();
        mTime = 100;
        mRootContext.onTick(mTime);
    }

    abstract void registerLiveData(RootConfig rootConfig);

    void advance() {
        mTime += 1;
        mRootContext.onTick(mTime);
    }

    /**
     * The value should be an array or a list. Return as an array of objects
     */
    private static Object[] toArray(Object a) {
        if (a instanceof Object[])
            return (Object[])a;

        if (a instanceof List<?>)
            return ((List) a).toArray();

        // Give up and copy it here.
        int len = Array.getLength(a);
        Object[] result = new Object[len];
        for (int i = 0 ; i < len ; i++)
            result[i] = Array.get(a, i);
        return result;
    }

    /**
     * Convenient method for comparing arrays of arrays
     */
    private static void assertInnerArrayEqual(Object[] a, Object b, Object c) {
        Object[] bArray = toArray(b);
        Object[] cArray = toArray(c);

        assertEquals(a.length, bArray.length);
        assertEquals(a.length, cArray.length);
        for (int i = 0 ; i < a.length ; i++)
            assertObjectTriple(a[i], bArray[i], cArray[i]);
    }

    /**
     * Compare three objects to see if they are the same.  Numeric values are converted to
     * doubles.
     */
    static void assertObjectTriple(Object a, Object b, Object c) {
        if (a instanceof Object[] || a instanceof List<?>)
            assertInnerArrayEqual(toArray(a), b, c);
        else if (a instanceof Number) {
            assertEquals(((Number) a).doubleValue(), ((Number) b).doubleValue(), 0.000001);
            assertEquals(((Number) a).doubleValue(), ((Number) c).doubleValue(), 0.000001);
        }
        else {
            assertEquals(a, b);
            assertEquals(a, c);
        }
    }
}
