/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.content;

import com.amazon.apl.android.APLJSONData;
import com.amazon.apl.android.Content;
import com.amazon.apl.android.dependencies.IContentRetriever;
import com.amazon.apl.android.dependencies.IPackageCache;
import com.amazon.apl.android.dependencies.IPackageLoader;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class CachingPackageLoaderTest extends ViewhostRobolectricTest {

    @Mock
    private IPackageCache mPackageCache;
    @Mock
    private IPackageLoader mDelegate;
    @Mock
    Content.ImportRequest mImportRequest;
    @Mock
    Content.ImportRequest mImportRequestTwo;
    @Mock
    APLJSONData mJSONData;
    @Mock
    IContentRetriever.SuccessCallback<Content.ImportRequest, APLJSONData> successCallback;
    @Mock
    IContentRetriever.SuccessCallback<Content.ImportRequest, APLJSONData> successCallbackTwo;
    @Mock
    IContentRetriever.FailureCallback<Content.ImportRequest> failureCallback;

    private CachingPackageLoader mPackageLoader;

    @Before
    public void setup() {
        mPackageLoader = new CachingPackageLoader(mDelegate, mPackageCache);
    }

    @Test
    public void testSimpleRequest_success() {
        when(mImportRequest.getImportRef()).thenReturn(Content.ImportRef.create("a", "1"));
        doAnswer(invocation -> {
            Content.ImportRequest request = invocation.getArgument(0);
            IContentRetriever.SuccessCallback<Content.ImportRequest, APLJSONData> successCallback = invocation.getArgument(1);
            successCallback.onSuccess(request, mJSONData);
            return null;
        }).when(mDelegate).fetch(any(), any(), any());


        mPackageLoader.fetch(mImportRequest, successCallback, failureCallback);

        verify(successCallback).onSuccess(eq(mImportRequest), eq(mJSONData));
        verify(mPackageCache).put(eq(Content.ImportRef.create("a", "1")), eq(mJSONData));
    }

    @Test
    public void testSimpleRequest_failure() {
        when(mImportRequest.getImportRef()).thenReturn(Content.ImportRef.create("a", "1"));
        doAnswer(invocation -> {
            Content.ImportRequest request = invocation.getArgument(0);
            IContentRetriever.FailureCallback<Content.ImportRequest> failureCallback = invocation.getArgument(2);
            failureCallback.onFailure(request, "fail");
            return null;
        }).when(mDelegate).fetch(any(), any(), any());

        mPackageLoader.fetch(mImportRequest, successCallback, failureCallback);

        verify(failureCallback).onFailure(eq(mImportRequest), eq("fail"));
        verify(mPackageCache, never()).put(any(), any());
    }

    @Test
    public void testSimpleRequest_inCache() {
        when(mImportRequest.getImportRef()).thenReturn(Content.ImportRef.create("a", "1"));
        when(mPackageCache.get(Content.ImportRef.create("a", "1"))).thenReturn(mJSONData);

        mPackageLoader.fetch(mImportRequest, successCallback, failureCallback);

        verify(mPackageCache).get(Content.ImportRef.create("a", "1"));
        verify(successCallback).onSuccess(eq(mImportRequest), eq(mJSONData));
    }

    @Test
    public void testMultipleRequests_sendsOneRequest() throws Exception {
        when(mImportRequest.getImportRef()).thenReturn(Content.ImportRef.create("a", "1"));
        when(mImportRequestTwo.getImportRef()).thenReturn(Content.ImportRef.create("a", "1"));

        CountDownLatch innerDone = new CountDownLatch(1);
        CountDownLatch outerDone = new CountDownLatch(1);
        doAnswer(invocation -> {
            new Thread(() -> {
                try {
                    outerDone.await(1, TimeUnit.SECONDS);
                    Content.ImportRequest request = invocation.getArgument(0);
                    IContentRetriever.SuccessCallback<Content.ImportRequest, APLJSONData> successCallback = invocation.getArgument(1);
                    successCallback.onSuccess(request, mJSONData);
                    innerDone.countDown();
                } catch (Exception e) {
                    fail("Exception encountered: " + e);
                }
            }).start();
            return null;
        }).when(mDelegate).fetch(any(), any(), any());

        mPackageLoader.fetch(mImportRequest, successCallback, failureCallback);
        mPackageLoader.fetch(mImportRequestTwo, successCallbackTwo, failureCallback);

        outerDone.countDown();
        innerDone.await(1, TimeUnit.SECONDS);

        verify(mDelegate).fetch(eq(mImportRequest), any(), any());
        verify(successCallback).onSuccess(eq(mImportRequest), eq(mJSONData));
        verify(successCallbackTwo).onSuccess(eq(mImportRequestTwo), eq(mJSONData));
        verify(mPackageCache, times(2)).put(eq(Content.ImportRef.create("a", "1")), eq(mJSONData));
        verifyZeroInteractions(failureCallback);
    }

    @Test
    public void testMultipleRequests_usesCache() {
        when(mImportRequest.getImportRef()).thenReturn(Content.ImportRef.create("a", "1"));
        when(mImportRequestTwo.getImportRef()).thenReturn(Content.ImportRef.create("a", "1"));
        doAnswer(invocation -> {
            Content.ImportRequest request = invocation.getArgument(0);
            IContentRetriever.SuccessCallback<Content.ImportRequest, APLJSONData> successCallback = invocation.getArgument(1);
            successCallback.onSuccess(request, mJSONData);
            return null;
        }).when(mDelegate).fetch(any(), any(), any());

        mPackageLoader.fetch(mImportRequest, successCallback, failureCallback);
        verify(successCallback).onSuccess(eq(mImportRequest), eq(mJSONData));
        verify(mPackageCache).put(eq(Content.ImportRef.create("a", "1")), eq(mJSONData));

        when(mPackageCache.get(Content.ImportRef.create("a", "1"))).thenReturn(mJSONData);

        mPackageLoader.fetch(mImportRequestTwo, successCallbackTwo, failureCallback);

        verify(mPackageCache, times(2)).get(eq(Content.ImportRef.create("a", "1")));
        verify(successCallbackTwo).onSuccess(eq(mImportRequestTwo), eq(mJSONData));
    }

    @Test
    public void testRepeatedLoads_noCache() {
        when(mImportRequest.getImportRef()).thenReturn(Content.ImportRef.create("a", "1"));
        when(mImportRequestTwo.getImportRef()).thenReturn(Content.ImportRef.create("a", "1"));
        doAnswer(invocation -> {
            Content.ImportRequest request = invocation.getArgument(0);
            IContentRetriever.SuccessCallback<Content.ImportRequest, APLJSONData> successCallback = invocation.getArgument(1);
            successCallback.onSuccess(request, mJSONData);
            return null;
        }).when(mDelegate).fetch(any(), any(), any());

        mPackageLoader.fetch(mImportRequest, successCallback, failureCallback);
        verify(successCallback).onSuccess(eq(mImportRequest), eq(mJSONData));

        mPackageLoader.fetch(mImportRequestTwo, successCallbackTwo, failureCallback);
        verify(successCallbackTwo).onSuccess(eq(mImportRequestTwo), eq(mJSONData));
    }
}
