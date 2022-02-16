/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.content;

import android.net.Uri;

import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.dependencies.IContentRetriever;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class MediaCallbackContentRetrieverDecoratorTest {

    private static final String TEST_URL = "https://test.com/json";

    private MediaCallbackContentRetrieverDecorator mWrappedContentRetriever;

    @Mock
    private IContentRetriever<Uri, String> mockContentRetriever;

    @Mock
    private IAPLViewPresenter mockViewPresenter;

    @Mock
    private Uri mockUri;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mWrappedContentRetriever = MediaCallbackContentRetrieverDecorator.create(mockViewPresenter, mockContentRetriever);
        when(mockUri.toString()).thenReturn(TEST_URL);
    }

    @Test
    public void wrappedRetriever_callsMediaLoaded_onSuccess() {
        // Given
        doAnswer(invocation -> {
            Uri source = invocation.getArgument(0);
            IContentRetriever.SuccessCallback<Uri, String> successCallback = invocation.getArgument(2);
            successCallback.onSuccess(source, "test");
            return null;
        }).when(mockContentRetriever).fetchV2(any(), any(), any(), any());

        // When
        mWrappedContentRetriever.fetchV2(mockUri, (uri, result) -> {}, (uri, errorMessage, errorCode) -> {});

        // Then
        verify(mockViewPresenter).mediaLoaded(eq(TEST_URL));
    }

    @Test
    public void wrappedRetriever_callsMediaLoadFailed_onError() {
        // Given
        String failureMessage = "I Failed";
        int errorCode = 123;

        doAnswer(invocation -> {
            Uri source = invocation.getArgument(0);
            IContentRetriever.FailureCallbackV2<Uri> failureCallbackV2 = invocation.getArgument(3);
            failureCallbackV2.onFailure(source, failureMessage, errorCode);
            return null;
        }).when(mockContentRetriever).fetchV2(any(), any(), any(), any());

        // When
        mWrappedContentRetriever.fetchV2(mockUri, (uri, result) -> {}, (uri, errorMessage, failureCode) -> {});

        // Then
        verify(mockViewPresenter).mediaLoadFailed(eq(TEST_URL), eq(errorCode), eq(failureMessage));
    }

    @Test
    public void wrappedRetriever_callsMediaLoadFailed_withDefaultErrorCodeV1() {
        // Given
        String failureMessage = "I Failed";

        doAnswer(invocation -> {
            Uri source = invocation.getArgument(0);
            IContentRetriever.FailureCallbackV2<Uri> failureCallbackV2 = invocation.getArgument(3);
            failureCallbackV2.onFailure(source, failureMessage, IContentRetriever.DEFAULT_ERROR_CODE);
            return null;
        }).when(mockContentRetriever).fetchV2(any(), any(), any(), any());

        // When
        mWrappedContentRetriever.fetch(mockUri, (uri, result) -> {}, (uri, errorMessage) -> {});

        // Then
        verify(mockViewPresenter).mediaLoadFailed(eq(TEST_URL), eq(IContentRetriever.DEFAULT_ERROR_CODE), eq(failureMessage));
    }

    @Test
    public void wrappedRetriever_callsFetchV2_withHeaders() {
        // Given
        Map<String, String> headers = Collections.singletonMap("key", "value");

        // When
        mWrappedContentRetriever.fetchV2(mockUri, headers, (uri, result) -> {}, (uri, errorMessage, failureCode) -> {});

        // Then
        verify(mockContentRetriever).fetchV2(eq(mockUri), eq(headers), any(IContentRetriever.SuccessCallback.class), any(IContentRetriever.FailureCallbackV2.class));
    }
}
