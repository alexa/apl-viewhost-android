/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.content;

import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;

import com.amazon.apl.android.dependencies.IContentRetriever;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ContentRetrieverTest extends ViewhostRobolectricTest {

    private ContentRetriever<String> mContentRetriever;

    @Before
    public void setup() {
        // Setup a content retriever that runs everything on the calling thread.
        mContentRetriever = new ContentRetriever<>(Runnable::run);
    }

    @Test
    public void handlerForScheme_invokesSuccess() {
        // Arrange
        Uri uri = Uri.parse("https://somedata");
        ContentRetriever.RequestHandler<String> httpsRequestHandler = setupMockHandler("https");
        mContentRetriever.addRequestHandler(httpsRequestHandler);
        AtomicBoolean successCalled = new AtomicBoolean();

        // Act
        mContentRetriever.fetch(uri,
                (source, result) -> {
                    assertEquals(uri, source);
                    assertEquals("httpsData", result);
                    successCalled.set(true);
                },
                (source, failure) -> fail("Failure shouldn't be invoked.")
        );

        // Assert
        assertTrue(successCalled.get());
        verify(httpsRequestHandler).fetchV2(eq(uri), anyMap(), any(), any());
    }

    @Test
    public void noHandlerForScheme_invokesFailure() {
        // Arrange
        Uri uri = Uri.parse("http://somedata");
        ContentRetriever.RequestHandler<String> httpsRequestHandler = setupMockHandler("https");
        mContentRetriever.addRequestHandler(httpsRequestHandler);
        AtomicBoolean failureCalled = new AtomicBoolean();

        // Act
        mContentRetriever.fetch(uri,
                (source, result) -> fail("Success shouldn't be invoked."),
                (source, failure) -> {
                    assertEquals(uri, source);
                    assertFalse(TextUtils.isEmpty(failure));
                    failureCalled.set(true);
                }
        );

        // Assert
        assertTrue(failureCalled.get());
        verify(httpsRequestHandler, never()).fetchV2(any(), anyMap(), any(), any());
    }

    @Test
    public void multiSchemeHandler_invoked() {
        // Arrange
        Uri aUri = Uri.parse("foo://myobj");
        Uri bUri = Uri.parse("bar://otherobj");
        ContentRetriever.RequestHandler<String> multiRequestHandler = setupMockHandler("foo", "bar");
        mContentRetriever.addRequestHandler(multiRequestHandler);
        AtomicBoolean uriACalled = new AtomicBoolean();
        AtomicBoolean uriBCalled = new AtomicBoolean();

        // Act
        mContentRetriever.fetch(aUri,
                (source, result) -> {
                    assertEquals(aUri, source);
                    assertEquals("fooData", result);
                    uriACalled.set(true);
                },
                (source, failure) -> fail("Failure shouldn't be invoked.")
        );
        mContentRetriever.fetch(bUri,
                (source, result) -> {
                    assertEquals(bUri, source);
                    assertEquals("barData", result);
                    uriBCalled.set(true);
                },
                (source, failure) -> fail("Failure shouldn't be invoked.")
        );

        // Assert
        assertTrue(uriACalled.get());
        assertTrue(uriBCalled.get());
        verify(multiRequestHandler).fetchV2(eq(aUri), anyMap(), any(), any());
        verify(multiRequestHandler).fetchV2(eq(bUri), anyMap(), any(), any());
    }

    @Test
    public void multipleSchemeHandlers_invoked() {
        // Arrange
        Uri httpsUri = Uri.parse("https://myobj");
        Uri contentUri = Uri.parse("content://otherobj");
        ContentRetriever.RequestHandler<String> httpsRequestHandler = setupMockHandler("https");
        ContentRetriever.RequestHandler<String> contentRequestHandler = setupMockHandler("content");
        mContentRetriever.addRequestHandler(httpsRequestHandler)
                .addRequestHandler(contentRequestHandler);
        AtomicBoolean httpsFetch = new AtomicBoolean();
        AtomicBoolean contentFetch = new AtomicBoolean();

        // Act
        mContentRetriever.fetch(httpsUri,
                (source, result) -> {
                    assertEquals(httpsUri, source);
                    assertEquals("httpsData", result);
                    httpsFetch.set(true);
                },
                (source, failure) -> fail("Failure shouldn't be invoked.")
        );
        mContentRetriever.fetch(contentUri,
                (source, result) -> {
                    assertEquals(contentUri, source);
                    assertEquals("contentData", result);
                    contentFetch.set(true);
                },
                (source, failure) -> fail("Failure shouldn't be invoked.")
        );

        // Assert
        assertTrue(httpsFetch.get());
        assertTrue(contentFetch.get());
        // Check that the uris were passed to the correct handlers
        verify(httpsRequestHandler).fetchV2(eq(httpsUri), anyMap(), any(), any());
        verify(contentRequestHandler).fetchV2(eq(contentUri), anyMap(), any(), any());
    }

    @Test
    public void failureCallback_invoked() {
        // Arrange
        Uri uri = Uri.parse("https://myobj");
        ContentRetriever.RequestHandler<String> mockHandler = mock(ContentRetriever.RequestHandler.class);
        when(mockHandler.supportedSchemes()).thenReturn(Arrays.asList("https"));
        doAnswer(invocation -> {
            Uri source = invocation.getArgument(0);
            IContentRetriever.FailureCallback<Uri> failureCallback = invocation.getArgument(2);
            failureCallback.onFailure(source, source.getScheme() + "Fail");
            return null;
        }).when(mockHandler).fetch(any(),  any(), any());
        doAnswer(invocation -> {
            Uri source = invocation.getArgument(0);
            IContentRetriever.FailureCallbackV2<Uri> failureCallback = invocation.getArgument(3);
            failureCallback.onFailure(source, source.getScheme() + "Fail", 0);
            return null;
        }).when(mockHandler).fetchV2(any(), anyMap(), any(), any());
        mContentRetriever.addRequestHandler(mockHandler);
        AtomicBoolean failureCalled = new AtomicBoolean();

        // Act
        mContentRetriever.fetch(uri,
                (source, result) -> fail("Success shouldn't be invoked."),
                (source, failure) -> {
                    assertEquals(uri, source);
                    assertEquals("httpsFail", failure);
                    failureCalled.set(true);
                });

        // Assert
        assertTrue(failureCalled.get());
        verify(mockHandler).fetchV2(eq(uri), anyMap(), any(), any());
    }

    @Test
    public void failureCallback_invokedV2() {
        // Arrange
        int errorCodeTest = 123;
        Uri uri = Uri.parse("https://myobj");
        ContentRetriever.RequestHandler<String> mockHandler = mock(ContentRetriever.RequestHandler.class);
        when(mockHandler.supportedSchemes()).thenReturn(Arrays.asList("https"));
        doAnswer(invocation -> {
            Uri source = invocation.getArgument(0);
            IContentRetriever.FailureCallbackV2<Uri> failureCallback = invocation.getArgument(3);
            failureCallback.onFailure(source, source.getScheme() + "Fail", errorCodeTest);
            return null;
        }).when(mockHandler).fetchV2(any(), anyMap(), any(), any());
        mContentRetriever.addRequestHandler(mockHandler);
        AtomicBoolean failureCalled = new AtomicBoolean();

        // Act
        mContentRetriever.fetchV2(uri,
                Collections.emptyMap(),
                (source, result) -> fail("Success shouldn't be invoked."),
                (source, failure, errorCode) -> {
                    assertEquals(uri, source);
                    assertEquals("httpsFail", failure);
                    assertEquals(errorCodeTest, errorCode);
                    failureCalled.set(true);
                });

        // Assert
        assertTrue(failureCalled.get());
        verify(mockHandler).fetchV2(eq(uri), anyMap(), any(), any());
    }

    @Test
    public void bitmapHandler() {
        // Arrange
        Uri uri = Uri.parse("https://someimage");
        Bitmap expectedResult = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        ContentRetriever<Bitmap> bitmapContentRetriever = new ContentRetriever<>(Runnable::run);
        ContentRetriever.RequestHandler<Bitmap> mockHandler = setupMockHandler((source) -> expectedResult, "https");
        bitmapContentRetriever.addRequestHandler(mockHandler);
        AtomicBoolean callbackInvoked = new AtomicBoolean();

        // Act
        bitmapContentRetriever.fetchV2(uri,
                (source, result) -> {
                    assertEquals(uri, source);
                    assertEquals(expectedResult, result);
                    callbackInvoked.set(true);
                },
                (source, failure, errorCode) -> fail("Failure shouldn't be invoked."));

        // Assert
        assertTrue(callbackInvoked.get());
        verify(mockHandler).fetchV2(eq(uri), anyMap(), any(), any());
    }

    @Test
    public void nullScheme_handled() {
        Uri uri = Uri.parse("anything");
        ContentRetriever.RequestHandler<String> mockHandler = mock(ContentRetriever.RequestHandler.class);
        mContentRetriever.fetch(uri,
                (source, success) -> fail("success should not be invoked"),
                (source, failure) -> {
                    assertEquals(uri, source);
                    assertEquals("No scheme for source", failure);
                });
    }

    private ContentRetriever.RequestHandler<String> setupMockHandler(String... schemes) {
        return setupMockHandler((uri) -> uri.getScheme() + "Data", schemes);
    }

    /**
     * Handler that invokes the success callback with result, schemeData
     * @param schemes schemes this handler supports.
     * @return the mock handler
     */
    private <T> ContentRetriever.RequestHandler<T> setupMockHandler(Function<Uri,T> result, String... schemes) {
        ContentRetriever.RequestHandler<T> mockHandler = mock(ContentRetriever.RequestHandler.class);
        when(mockHandler.supportedSchemes()).thenReturn(Arrays.asList(schemes));
        doAnswer(invocation -> {
            Uri source = invocation.getArgument(0);
            IContentRetriever.SuccessCallback<Uri, T> successCallback = invocation.getArgument(1);
            successCallback.onSuccess(source, result.apply(source));
            return null;
        }).when(mockHandler).fetch(any(), any(), any());
        doAnswer(invocation -> {
            Uri source = invocation.getArgument(0);
            IContentRetriever.SuccessCallback<Uri, T> successCallback = invocation.getArgument(1);
            successCallback.onSuccess(source, result.apply(source));
            return null;
        }).when(mockHandler).fetchV2(any(), any(), any());
        doAnswer(invocation -> {
            Uri source = invocation.getArgument(0);
            IContentRetriever.SuccessCallback<Uri, T> successCallback = invocation.getArgument(2);
            successCallback.onSuccess(source, result.apply(source));
            return null;
        }).when(mockHandler).fetchV2(any(), anyMap(), any(), any());
        return mockHandler;
    }
}
