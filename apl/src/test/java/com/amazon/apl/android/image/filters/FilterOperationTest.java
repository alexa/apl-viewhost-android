/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters;

import android.graphics.Bitmap;

import com.amazon.apl.android.bitmap.BitmapCreationException;
import com.amazon.apl.android.bitmap.BitmapFactory;
import com.amazon.apl.android.dependencies.IExtensionImageFilterCallback;
import com.amazon.apl.android.image.filters.bitmap.BitmapFilterResult;
import com.amazon.apl.android.image.filters.bitmap.FilterResult;
import com.amazon.apl.android.primitive.Filters;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;

import org.junit.Before;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

public abstract class FilterOperationTest<T extends FilterOperation> extends ViewhostRobolectricTest {

    protected FilterOperation mFilterOperation;
    @Mock
    protected BitmapFactory mBitmapFactory;
    @Mock
    protected RenderScriptWrapper mRenderScript;
    @Mock
    protected IExtensionImageFilterCallback mExtensionImageFilterCallback;
    private List<FilterResult> mSourceFilterResults;
    protected Bitmap mResultBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);

    @Before
    public void setup() {
        try {
            when(mBitmapFactory.createBitmap(anyInt(), anyInt())).thenReturn(mResultBitmap);
        } catch (BitmapCreationException e) {
            fail("Test shouldn't fail here.");
        }
    }

    public void init(List<FilterResult> sources, Filters.Filter filter) {
        mSourceFilterResults = sources;
        List<Future<FilterResult>> sourceFutures = new ArrayList<>();
        for (FilterResult result : sources) {
            sourceFutures.add(new MockFuture(result));
        }

        mFilterOperation = FilterOperationFactory.create(sourceFutures, filter, mBitmapFactory, mRenderScript, mExtensionImageFilterCallback);
    }

    @SuppressWarnings("unchecked")
    public T getFilterOperation() {
        return (T) mFilterOperation;
    }

    public FilterResult getSourceAt(int index) {
        return mSourceFilterResults.get(index);
    }

    FilterResult createDummyBitmap() {
        return new BitmapFilterResult(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888), mBitmapFactory);
    }

    static class MockFuture implements Future<FilterResult> {
        private final FilterResult mResult;

        MockFuture(FilterResult result) {
            mResult = result;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public FilterResult get() {
            return mResult;
        }

        @Override
        public FilterResult get(long timeout, TimeUnit unit) {
            return mResult;
        }
    }
}
