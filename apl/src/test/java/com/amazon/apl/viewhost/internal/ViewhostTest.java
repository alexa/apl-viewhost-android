/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.viewhost.internal;

import static com.amazon.apl.viewhost.config.EmbeddedDocumentFactory.EmbeddedDocumentRequest;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.os.Handler;
import android.os.Looper;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amazon.apl.android.DocumentSession;
import com.amazon.apl.android.dependencies.IDataSourceFetchCallback;
import com.amazon.apl.android.dependencies.IOpenUrlCallback;
import com.amazon.apl.android.dependencies.ISendEventCallbackV2;
import com.amazon.apl.android.events.DataSourceFetchEvent;
import com.amazon.apl.android.events.OpenURLEvent;
import com.amazon.apl.android.events.PlayMediaEvent;
import com.amazon.apl.android.events.SendEvent;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.viewhost.DocumentHandle;
import com.amazon.apl.viewhost.PreparedDocument;
import com.amazon.apl.viewhost.Viewhost;
import com.amazon.apl.viewhost.config.DocumentOptions;
import com.amazon.apl.viewhost.config.ViewhostConfig;
import com.amazon.apl.viewhost.example.ExampleDocumentFactory;
import com.amazon.apl.viewhost.message.Message;
import com.amazon.apl.viewhost.message.MessageHandler;
import com.amazon.apl.viewhost.message.action.ActionMessage;
import com.amazon.apl.viewhost.message.action.FetchDataRequest;
import com.amazon.apl.viewhost.message.action.OpenURLRequest;
import com.amazon.apl.viewhost.message.action.ReportRuntimeErrorRequest;
import com.amazon.apl.viewhost.message.action.SendUserEventRequest;
import com.amazon.apl.viewhost.primitives.JsonStringDecodable;
import com.amazon.apl.viewhost.request.ExecuteCommandsRequest;
import com.amazon.apl.viewhost.request.FinishDocumentRequest;
import com.amazon.apl.viewhost.request.PrepareDocumentRequest;
import com.amazon.apl.viewhost.request.RenderDocumentRequest;
import com.amazon.apl.viewhost.utils.CapturingMessageHandler;
import com.amazon.apl.viewhost.utils.ManualExecutor;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class ViewhostTest extends ViewhostRobolectricTest {
    @Mock
    private DocumentHandle mDocumentHandle;
    @Mock
    private DocumentHandleImpl mDocumentHandleImpl;
    @Mock
    private DocumentContext mDocumentContext;

    private Viewhost mViewhost;
    private CapturingMessageHandler mMessageHandler;
    private ManualExecutor mRuntimeInteractionWorker;

    @Before
    public void setup() {
        mMessageHandler = new CapturingMessageHandler();
        mRuntimeInteractionWorker = new ManualExecutor();
        ViewhostConfig config = ViewhostConfig.builder().messageHandler(mMessageHandler).build();
        mViewhost = new ViewhostImpl(config, mRuntimeInteractionWorker, new Handler(Looper.getMainLooper()));
    }

    @Test
    public void testDocumentFactory() {
        ExampleDocumentFactory factory = new ExampleDocumentFactory(mViewhost);

        // A ProvideDocument directive comes in before it's requested
        EmbeddedDocumentRequest requestA = mock(EmbeddedDocumentRequest.class);
        when(requestA.getSource()).thenReturn("uriA");
        factory.onProvideDocument("uriA", "documentA");
        factory.onDocumentRequested(requestA);
        verify(requestA, times(1)).resolve(any(PreparedDocument.class));

        // A ProvideDocument directive comes in after it's requested
        EmbeddedDocumentRequest requestB = mock(EmbeddedDocumentRequest.class);
        when(requestB.getSource()).thenReturn("uriB");
        factory.onDocumentRequested(requestB);
        factory.onProvideDocument("uriB", "documentB");
        verify(requestB, times(1)).resolve(any(PreparedDocument.class));
    }

    @Test
    public void testPrepareDocument() {
        // Basic demonstration of a prepare document request, getting a document handle
        PrepareDocumentRequest request = PrepareDocumentRequest.builder()
                .token("mytoken")
                .document(new JsonStringDecodable("document"))
                .data(new JsonStringDecodable("data"))
                .documentSession(DocumentSession.create())
                .documentOptions(DocumentOptions.builder().build())
                .build();
        PreparedDocument preparedDocument = mViewhost.prepare(request);
        DocumentHandle handle = preparedDocument.getHandle();
        assertNotNull(handle);

        // Could be rendered at this point, if that pathway was implemented
        assertNull(mViewhost.render(preparedDocument));

        // Could finish document given a handle
        FinishDocumentRequest finishRequest = FinishDocumentRequest.builder()
                .token("mytoken")
                .build();
        handle.finish(finishRequest);

    }

    @Test
    public void testRenderDocument() {
        // Pathway not implemented
        DocumentHandle handle = mViewhost.render(mock(RenderDocumentRequest.class));
        assertNull(handle);
    }

    @Test
    public void testExecuteCommands() {
        // Basic demonstration of execute commands given a known document handle
        ExecuteCommandsRequest request =
                ExecuteCommandsRequest.builder()
                        .commands(new JsonStringDecodable("commands"))
                        .build();
        mDocumentHandle.executeCommands(request);
    }

    @Test
    public void testExampleMessage() {
        JsonStringDecodable payload = new JsonStringDecodable("test");
        Message message = new Message(mDocumentHandle, payload);

        assertEquals(mDocumentHandle, message.getDocument());
        assertEquals(payload, message.getPayload());
    }

    @Test
    public void testAllowsUnrelatedEventsToProceedWithoutOverriding() {
        SendEvent event = mock(SendEvent.class);
        updateDocumentMap(222, mDocumentHandleImpl, mDocumentContext);
        when(event.getDocumentContextId()).thenReturn((long)333);
        assertTrue(((ViewhostImpl)mViewhost).interceptEventIfNeeded(event));
        verify(event, never()).overrideCallback(any(ISendEventCallbackV2.class));
    }

    @Test
    public void testUnrecognizedEventsPertainingToKnownDocuments() {
        PlayMediaEvent event = mock(PlayMediaEvent.class);
        updateDocumentMap(456, mDocumentHandleImpl, mDocumentContext);
        when(event.getDocumentContextId()).thenReturn((long)456);
        assertFalse(((ViewhostImpl)mViewhost).interceptEventIfNeeded(event));
    }

    @Test
    public void testInterceptSendEventIfNeeded() {
        SendEvent event = mock(SendEvent.class);
        long key = 123;
        Object[] args = {"one", "two", "three"};
        updateDocumentMap(key, mDocumentHandleImpl, mDocumentContext);
        when(event.getDocumentContextId()).thenReturn(key);
        doAnswer(invocation -> {
            ISendEventCallbackV2 callback = invocation.getArgument(0);
            callback.onSendEvent(args, new HashMap<String, Object>(), new HashMap<String, Object>(), new HashMap<String, Object>());
            return null;
        }).when(event).overrideCallback(any(ISendEventCallbackV2.class));

        assertTrue(((ViewhostImpl)mViewhost).interceptEventIfNeeded(event));

        assertTrue(mRuntimeInteractionWorker.size() > 0);
        mRuntimeInteractionWorker.flush();

        assertEquals(1, mMessageHandler.queue.size());
        assertTrue(mMessageHandler.queue.peek() instanceof SendUserEventRequest);

        SendUserEventRequest message = (SendUserEventRequest) mMessageHandler.queue.poll();
        assertEquals(1, message.getId());
        assertEquals(mDocumentHandleImpl, message.getDocument());
        assertArrayEquals(args, message.getArguments());
        message.succeed();
    }

    @Test
    public void testInterceptDataSourceFetchEventIfNeeded() {
        long key = 123;
        updateDocumentMap(key, mDocumentHandleImpl, mDocumentContext);

        DataSourceFetchEvent event = mock(DataSourceFetchEvent.class);
        when(event.getDocumentContextId()).thenReturn(key);

        Map<String, Object> payload = new HashMap<>();
        payload.put("key1", "val1");
        payload.put("key2", "val2");

        doAnswer(invocation -> {
            IDataSourceFetchCallback callback = invocation.getArgument(0);
            callback.onDataSourceFetchRequest("dynamicIndexList", payload);
            return null;
        }).when(event).overrideCallback(any(IDataSourceFetchCallback.class));

        assertTrue(((ViewhostImpl)mViewhost).interceptEventIfNeeded(event));

        // Second event
        DataSourceFetchEvent event2 = mock(DataSourceFetchEvent.class);
        when(event2.getDocumentContextId()).thenReturn(key);
        doAnswer(invocation -> {
            IDataSourceFetchCallback callback = invocation.getArgument(0);
            callback.onDataSourceFetchRequest("dynamicTokenList", payload);
            return null;
        }).when(event2).overrideCallback(any(IDataSourceFetchCallback.class));

        assertTrue(((ViewhostImpl)mViewhost).interceptEventIfNeeded(event2));

        assertTrue(mRuntimeInteractionWorker.size() > 0);
        mRuntimeInteractionWorker.flush();

        assertEquals(2, mMessageHandler.queue.size());

        assertTrue(mMessageHandler.queue.peek() instanceof FetchDataRequest);
        FetchDataRequest message = (FetchDataRequest) mMessageHandler.queue.poll();
        assertEquals(1, message.getId());
        assertEquals("DYNAMIC_INDEX_LIST", message.getDataType());
        assertEquals(mDocumentHandleImpl, message.getDocument());
        assertEquals(payload, message.getParameters());
        message.succeed();

        assertTrue(mMessageHandler.queue.peek() instanceof FetchDataRequest);
        FetchDataRequest message2 = (FetchDataRequest) mMessageHandler.queue.poll();
        assertEquals(2, message2.getId());
        assertEquals("DYNAMIC_TOKEN_LIST", message2.getDataType());
        assertEquals(mDocumentHandleImpl, message2.getDocument());
        assertEquals(payload, message2.getParameters());
        message.fail("Could not publish event");
    }

    @Test
    public void testInterceptDataSourceFetchEventWithInvalidType() {
        long key = 123;
        updateDocumentMap(key, mDocumentHandleImpl, mDocumentContext);

        DataSourceFetchEvent event = mock(DataSourceFetchEvent.class);
        when(event.getDocumentContextId()).thenReturn(key);

        Map<String, Object> payload = new HashMap<>();
        payload.put("key1", "val1");
        payload.put("key2", "val2");

        doAnswer(invocation -> {
            IDataSourceFetchCallback callback = invocation.getArgument(0);
            callback.onDataSourceFetchRequest("invalidType", payload);
            return null;
        }).when(event).overrideCallback(any(IDataSourceFetchCallback.class));

        assertTrue(((ViewhostImpl)mViewhost).interceptEventIfNeeded(event));

        assertEquals(0, mRuntimeInteractionWorker.size());
        mRuntimeInteractionWorker.flush();
        assertEquals(0, mMessageHandler.queue.size());
    }

    @Test
    public void testInterceptOpenURLIfNeeded() {
        OpenURLEvent event = mock(OpenURLEvent.class);
        long key = 123;
        String source = "https://example.com/source.js";
        updateDocumentMap(key, mDocumentHandleImpl, mDocumentContext);
        when(event.getDocumentContextId()).thenReturn(key);

        IOpenUrlCallback.IOpenUrlCallbackResult callbackResult =
                mock(IOpenUrlCallback.IOpenUrlCallbackResult.class);
        
        doAnswer(invocation -> {
            IOpenUrlCallback callback = invocation.getArgument(0);
            callback.onOpenUrl(source, callbackResult);
            return null;
        }).when(event).overrideCallback(any(IOpenUrlCallback.class));

        assertTrue(((ViewhostImpl)mViewhost).interceptEventIfNeeded(event));

        assertTrue(mRuntimeInteractionWorker.size() > 0);
        mRuntimeInteractionWorker.flush();

        assertEquals(1, mMessageHandler.queue.size());
        assertTrue(mMessageHandler.queue.peek() instanceof OpenURLRequest);

        OpenURLRequest message = (OpenURLRequest) mMessageHandler.queue.poll();
        assertEquals(1, message.getId());
        assertEquals(mDocumentHandleImpl, message.getDocument());
        assertEquals(source, message.getSource());

        message.succeed();
        verify(callbackResult, times(1)).onResult(true);
    }

    @Test
    public void testInterceptOpenURLIfNeededWithFailedCallback() {
        OpenURLEvent event = mock(OpenURLEvent.class);
        long key = 123;
        String source = "https://example.com/source.js";
        updateDocumentMap(key, mDocumentHandleImpl, mDocumentContext);
        when(event.getDocumentContextId()).thenReturn(key);

        IOpenUrlCallback.IOpenUrlCallbackResult callbackResult =
                mock(IOpenUrlCallback.IOpenUrlCallbackResult.class);

        doAnswer(invocation -> {
            IOpenUrlCallback callback = invocation.getArgument(0);
            callback.onOpenUrl(source, callbackResult);
            return null;
        }).when(event).overrideCallback(any(IOpenUrlCallback.class));

        assertTrue(((ViewhostImpl)mViewhost).interceptEventIfNeeded(event));

        assertTrue(mRuntimeInteractionWorker.size() > 0);
        mRuntimeInteractionWorker.flush();

        assertEquals(1, mMessageHandler.queue.size());
        assertTrue(mMessageHandler.queue.peek() instanceof OpenURLRequest);

        OpenURLRequest message = (OpenURLRequest) mMessageHandler.queue.poll();
        assertEquals(1, message.getId());
        assertEquals(mDocumentHandleImpl, message.getDocument());
        assertEquals(source, message.getSource());

        message.fail("Reason for failure");
        verify(callbackResult, times(1)).onResult(false);
    }

    @Test
    public void testDataSourceError() throws JSONException {

    }

    @Test
    public void testEventsDroppedWithoutMessageHandler() {
        ViewhostConfig config = ViewhostConfig.builder().build();
        ViewhostImpl viewhost = new ViewhostImpl(config);

        when(mDocumentHandleImpl.getDocumentContext()).thenReturn(mDocumentContext);
        when(mDocumentContext.getId()).thenReturn((long)123);
        viewhost.updateDocumentMap(mDocumentHandleImpl);

        SendEvent sendEvent = mock(SendEvent.class);
        when(sendEvent.getDocumentContextId()).thenReturn((long)123);

        DataSourceFetchEvent dataSourceFetchEvent = mock(DataSourceFetchEvent.class);
        when(dataSourceFetchEvent.getDocumentContextId()).thenReturn((long)123);

        OpenURLEvent openURLEvent = mock(OpenURLEvent.class);
        when(openURLEvent.getDocumentContextId()).thenReturn((long)123);

        assertFalse(viewhost.interceptEventIfNeeded(sendEvent));
        assertFalse(viewhost.interceptEventIfNeeded(dataSourceFetchEvent));
        assertFalse(viewhost.interceptEventIfNeeded(openURLEvent));
    }

    private void updateDocumentMap(long key, DocumentHandleImpl documentHandle, DocumentContext documentContext) {
        when(documentHandle.getDocumentContext()).thenReturn(documentContext);
        when(documentHandle.isValid()).thenReturn(true);
        when(documentContext.getId()).thenReturn(key);
        ((ViewhostImpl)mViewhost).updateDocumentMap(documentHandle);
    }
}
