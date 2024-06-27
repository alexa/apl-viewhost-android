/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.viewhost.internal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.os.Handler;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amazon.alexaext.ExtensionRegistrar;
import com.amazon.alexaext.IExtensionProvider;
import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.DocumentSession;
import com.amazon.apl.android.ExtensionMediator;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.dependencies.IVisualContextListener;
import com.amazon.apl.android.document.AbstractDocUnitTest;
import com.amazon.apl.android.metrics.ICounter;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.viewhost.DocumentHandle;
import com.amazon.apl.viewhost.PreparedDocument;
import com.amazon.apl.viewhost.Viewhost;
import com.amazon.apl.viewhost.config.DocumentOptions;
import com.amazon.apl.viewhost.config.EmbeddedDocumentFactory;
import com.amazon.apl.viewhost.config.EmbeddedDocumentResponse;
import com.amazon.apl.viewhost.config.ViewhostConfig;
import com.amazon.apl.viewhost.message.BaseMessage;
import com.amazon.apl.viewhost.message.notification.VisualContextChanged;
import com.amazon.apl.viewhost.primitives.Decodable;
import com.amazon.apl.viewhost.primitives.JsonDecodable;
import com.amazon.apl.viewhost.primitives.JsonTranscoder;
import com.amazon.apl.viewhost.primitives.JsonStringDecodable;
import com.amazon.apl.viewhost.request.ExecuteCommandsRequest;
import com.amazon.apl.viewhost.request.PrepareDocumentRequest;
import com.amazon.apl.viewhost.utils.CapturingMessageHandler;
import com.amazon.apl.viewhost.utils.ManualExecutor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.shadows.ShadowSystemClock;

import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

@RunWith(AndroidJUnit4.class)
public class EmbeddedVisualContextTest extends AbstractDocUnitTest {
    private static final String HELLO_WORLD = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"2023.2\"," +
            "  \"mainTemplate\": {" +
            "    \"items\": {" +
            "      \"type\": \"Text\"," +
            "      \"text\": \"Hello, World!\"," +
            "      \"entities\": [" +
            "        \"hello\"" +
            "      ]," +
            "      \"id\": \"text1\"," +
            "      \"color\": \"white\"," +
            "      \"textAlign\": \"center\"," +
            "      \"textAlignVertical\": \"center\"" +
            "    }" +
            "  }" +
            "}";

    private static final String HOST_DOCUMENT = "{" +
        "  \"type\": \"APL\"," +
        "  \"version\": \"2023.2\"," +
        "  \"mainTemplate\": {" +
        "    \"items\": {" +
        "      \"type\": \"Container\"," +
        "      \"entities\": [" +
        "        \"primary\"" +
        "      ]," +
        "      \"items\": [" +
        "        {" +
        "          \"type\": \"Host\"," +
        "          \"entities\": [" +
        "            \"first\"" +
        "          ]," +
        "          \"source\": \"documentA\"," +
        "          \"width\": \"100vw\"," +
        "          \"height\": \"50vh\"" +
        "        }," +
        "        {" +
        "          \"type\": \"Host\"," +
        "          \"entities\": [" +
        "            \"second\"" +
        "          ]," +
        "          \"source\": \"documentB-attached\"," +
        "          \"width\": \"100vw\"," +
        "          \"height\": \"50vh\"" +
        "        }" +
        "      ]" +
        "    }" +
        "  }" +
        "}";

    @Mock
    private Handler mCoreWorker;
    @Mock
    private IVisualContextListener mVisualContextListener;
    @Mock
    DocumentOptions mDocumentOptions;
    @Mock
    private IExtensionProvider mExtensionProvider;
    @Mock
    ExtensionMediator.IExtensionGrantRequestCallback mExtensionGrantRequestCallback;

    private Viewhost mViewhost;
    private CapturingMessageHandler mMessageHandler;
    private ManualExecutor mRuntimeInteractionWorker;
    HashMap<String, DocumentHandle> mEmbeddedDocuments;
    private APLOptions mAplOptions;
    private String mGoodbyeCommands;
    @Mock
    private ITelemetryProvider mTelemetryProvider;
    @Mock
    private ICounter mCounter;

    @Before
    public void setup() throws JSONException {
        // Prepare plumbing for message publishing
        mMessageHandler = new CapturingMessageHandler();
        mRuntimeInteractionWorker = new ManualExecutor();
        mEmbeddedDocuments = new HashMap<>();
        when(mMetricsRecorder.createCounter(anyString())).thenReturn(mCounter);

        // Fake "core worker" executes everything immediately
        when(mCoreWorker.post(any(Runnable.class))).thenAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        });

        // Create new viewhost for handling embedded documents
        ViewhostConfig config = ViewhostConfig.builder().messageHandler(mMessageHandler).build();

        // Create primary document using APLController (legacy) method
        mRootConfig = RootConfig.create("Unit Test", "1.0");
        mViewhost = new ViewhostImpl(config, mRuntimeInteractionWorker, mCoreWorker);
        EmbeddedDocumentFactory factory = new HelloWorldEmbeddedDocumentFactory(mViewhost);
        mAplOptions = APLOptions.builder()
                .visualContextListener(mVisualContextListener)
                .embeddedDocumentFactory(factory)
                .viewhost(mViewhost)
                .build();
        // TODO: Needed because AbstractDocUnitTest uses a deprecated version of renderDocument
        mRootConfig.setDocumentManager(factory, mCoreWorker, mTelemetryProvider);

        // Create a command array JSON string, which changes the "Hello, World" text.
        mGoodbyeCommands = new JSONArray().put(new JSONObject()
                .put("type", "SetValue")
                .put("componentId", "text1")
                .put("property", "text")
                .put("value", "Goodbye!"))
                .toString();
    }

    @Test
    public void testHelloWorldLegacyPathway()  {
        // This test case double-checks that the regular legacy pathway continues to work
        // So we're just using the "Hello, world" directly, without Host components
        loadDocument(HELLO_WORLD, mAplOptions);
        mRootContext.executeCommands(mGoodbyeCommands);

        ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);

        // Step forward 50ms, which is not enough time to trigger an update
        ShadowSystemClock.advanceBy(Duration.ofMillis(50));
        update(50);
        verify(mVisualContextListener, times(0)).onVisualContextUpdate(any());

        // Step forward 400ms, which is enough time
        ShadowSystemClock.advanceBy(Duration.ofMillis(400));
        update(400);

        // Legacy visual context update has been called
        verify(mVisualContextListener).onVisualContextUpdate(captor.capture());
        JSONObject jsonObject = captor.getValue();
        assertNotNull(jsonObject);

        verifyNoMoreInteractions(mVisualContextListener);
    }

    @Test
    public void testEmbeddedLoadingSuccess() throws JSONException {
        loadDocument(HOST_DOCUMENT, mAplOptions);

        // Expect two document handles, registered by the test factory
        assertEquals(2, mEmbeddedDocuments.size());

        DocumentHandle handleA = mEmbeddedDocuments.get("documentA");
        DocumentHandle handleB = mEmbeddedDocuments.get("documentB-attached");

        update(100);

        assertFalse(mRuntimeInteractionWorker.isEmpty());
        mRuntimeInteractionWorker.flush();
        //4 from document state changed and 2 from visual context
        assertEquals(6, mMessageHandler.queue.size());
        Queue<BaseMessage> visualContextChangedQueue = new LinkedList<>();
        for (BaseMessage message: mMessageHandler.queue) {
            if (message instanceof  VisualContextChanged) {
                visualContextChangedQueue.add(message);
            }
        }

        assertEquals(2, visualContextChangedQueue.size());
        assertTrue(visualContextChangedQueue.peek() instanceof VisualContextChanged);
        VisualContextChanged message1 = (VisualContextChanged) visualContextChangedQueue.poll();
        boolean isDocumentA = message1.getDocument() == handleA;
        boolean isDocumentB = message1.getDocument() == handleB;
        assertTrue(isDocumentA || isDocumentB);

        assertTrue(visualContextChangedQueue.peek() instanceof VisualContextChanged);
        VisualContextChanged message2 = (VisualContextChanged) visualContextChangedQueue.poll();
        isDocumentA = message2.getDocument() == handleA;
        isDocumentB = message2.getDocument() == handleB;
        assertTrue(isDocumentA || isDocumentB);

        // Make sure we got one of each
        assertTrue(message1.getDocument() != message2.getDocument());

        // Let more time go by, make sure there are no further updates
        update(500);
        //clear message handler queue
        mMessageHandler.queue.clear();
        
        assertTrue(mRuntimeInteractionWorker.isEmpty());

        // Next, update document A specifically
        assertTrue(handleA.executeCommands(
                ExecuteCommandsRequest.builder()
                        .commands(new JsonStringDecodable(mGoodbyeCommands))
                        .build()));

        // We are not notified again because it's still dirty (we haven't pulled the context since)
        update(100);
        assertTrue(mRuntimeInteractionWorker.isEmpty());

        // Let's get the visual context for document B
        final int[] successFailCount = {0, 0};
        assertTrue(handleB.requestVisualContext(new DocumentHandle.VisualContextCallback() {
            @Override
            public void onSuccess(Decodable context) {
                assertTrue(context instanceof JsonDecodable);
                JsonTranscoder transcoder = new JsonTranscoder();
                assertTrue(context.transcode(transcoder));
                JSONObject jsonObject = transcoder.getJsonObject();
                assertNotNull(jsonObject);
                // The result is something like
                // {"id":"text1","uid":":1004","position":"1280x360+0+360:0","type":"text"}
                try {
                    assertEquals("text1", jsonObject.get("id"));
                    successFailCount[0]++;
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail();
                }
            }

            @Override
            public void onFailure(String reason) {
                successFailCount[1]++;
            }
        }));

        assertFalse(mRuntimeInteractionWorker.isEmpty());
        mRuntimeInteractionWorker.flush();
        assertArrayEquals(new int[]{1, 0}, successFailCount);

        // No messages are pending
        update(100);
        assertTrue(mRuntimeInteractionWorker.isEmpty());

        // Next, update document B specifically
        assertTrue(handleB.executeCommands(
                ExecuteCommandsRequest.builder()
                        .commands(new JsonStringDecodable(mGoodbyeCommands))
                        .build()));

        // Now we get a notification for B because we've recently checked it
        update(100);
        assertFalse(mRuntimeInteractionWorker.isEmpty());
        mRuntimeInteractionWorker.flush();
        assertEquals(1, mMessageHandler.queue.size());

        // This notification is specifically for document B
        assertTrue(mMessageHandler.queue.peek() instanceof VisualContextChanged);
        VisualContextChanged message3 = (VisualContextChanged) mMessageHandler.queue.poll();
        assertEquals(handleB, message3.getDocument());
    }

    @Test
    public void testAttachedEmbeddedDocument() throws JSONException {
        loadDocument(HOST_DOCUMENT, mAplOptions);
        assertEquals(2, mEmbeddedDocuments.size());

        mRootContext.notifyContext();

        ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
        verify(mVisualContextListener).onVisualContextUpdate(captor.capture());
        JSONObject jsonObject = captor.getValue();
        assertNotNull(jsonObject);

        // In general, the context looks like this
        // {
        //   "children": [
        //     {
        //       "entities": [
        //         "first"
        //       ],
        //       "tags": {
        //         "focused": false
        //       },
        //       "uid": ":1001",
        //       "position": "1280x360+0+0:0",
        //       "type": "empty"
        //     },
        //     {
        //       "children": [
        //         {
        //           "entities": [
        //             "hello"
        //           ],
        //           "id": "text1",
        //           "uid": ":1004",
        //           "position": "1280x360+0+360:0",
        //           "type": "text"
        //         }
        //       ],
        //       "entities": [
        //         "second"
        //       ],
        //       "tags": {
        //         "focused": false
        //       },
        //       "uid": ":1002",
        //       "position": "1280x360+0+360:0",
        //       "type": "text"
        //     }
        //   ],
        //   "entities": [
        //     "primary"
        //   ],
        //   "tags": {
        //     "viewport": {}
        //   },
        //   "uid": ":1000",
        //   "position": "1280x720+0+0:0",
        //   "type": "mixed"
        // }

        // We care that there are two children of the container
        JSONArray children = jsonObject.getJSONArray("children");
        assertEquals(2, children.length());

        // The second child is "attached" to the parent's visual context whereas the first child
        // is not (according to some logic in HelloWorldEmbeddedDocumentFactory).
        //
        // We'll just do a crude inspection based on entities.
        String firstChild = children.getString(0);
        String secondChild = children.getString(1);

        assertTrue(firstChild.contains("first"));
        assertTrue(secondChild.contains("second"));

        assertFalse(firstChild.contains("hello"));
        assertTrue(secondChild.contains("hello"));
    }

    @Test
    public void testRequestingVisualContextAfterViewhostIsGone() {
        DocumentHandle handle = new DocumentHandleImpl(null, mCoreWorker, null);

        final int[] successFailCount = {0, 0};
        assertFalse(handle.requestVisualContext(new DocumentHandle.VisualContextCallback() {
            @Override
            public void onSuccess(Decodable context) {
                successFailCount[0]++;
            }

            @Override
            public void onFailure(String reason) {
                successFailCount[1]++;
            }
        }));
        assertArrayEquals(new int[]{0, 0}, successFailCount);
    }

    @Test
    public void testRequestingVisualContextFailsFastForFinishedDocument() {
        loadDocument(HOST_DOCUMENT, mAplOptions);
        assertEquals(2, mEmbeddedDocuments.size());
        DocumentHandleImpl handle = (DocumentHandleImpl)mEmbeddedDocuments.get("documentA");
        handle.setDocumentState(DocumentState.FINISHED);

        final int[] successFailCount = {0, 0};
        assertFalse(handle.requestVisualContext(new DocumentHandle.VisualContextCallback() {
            @Override
            public void onSuccess(Decodable context) {
                successFailCount[0]++;
            }

            @Override
            public void onFailure(String reason) {
                successFailCount[1]++;
            }
        }));
        mRuntimeInteractionWorker.flush();
        assertArrayEquals(new int[]{0, 0}, successFailCount);

        assertFalse(((DocumentHandleImpl)handle).getAndClearHasVisualContextChanged());
    }

    @Test
    public void testRequestingVisualContextBeforeDocumentContextIsAvailable() {
        DocumentHandle handle = new DocumentHandleImpl((ViewhostImpl) mViewhost, mCoreWorker, null);

        final int[] successFailCount = {0, 0};
        assertTrue(handle.requestVisualContext(new DocumentHandle.VisualContextCallback() {
            @Override
            public void onSuccess(Decodable context) {
                successFailCount[0]++;
            }

            @Override
            public void onFailure(String reason) {
                assertTrue(reason.contains("not available"));
                successFailCount[1]++;
            }
        }));
        mRuntimeInteractionWorker.flush();
        assertArrayEquals(new int[]{0, 1}, successFailCount);

        assertFalse(((DocumentHandleImpl)handle).getAndClearHasVisualContextChanged());
    }

    @Test
    public void testRequestingVisualContextWithSerializationProblem() {
        ViewhostImpl viewhostImpl = (ViewhostImpl) mViewhost;
        DocumentHandleImpl handle = new DocumentHandleImpl(viewhostImpl, mCoreWorker, null);
        DocumentContext context = mock(DocumentContext.class);
        when(context.getId()).thenReturn((long)123);
        when(context.serializeVisualContext()).thenReturn("Invalid JSON");
        handle.setDocumentContext(context);

        final int[] successFailCount = {0, 0};
        assertTrue(handle.requestVisualContext(new DocumentHandle.VisualContextCallback() {
            @Override
            public void onSuccess(Decodable context) {
                successFailCount[0]++;
            }

            @Override
            public void onFailure(String reason) {
                assertTrue(reason.contains("serialization failure"));
                successFailCount[1]++;
            }
        }));
        mRuntimeInteractionWorker.flush();
        assertArrayEquals(new int[]{0, 1}, successFailCount);
    }

    /**
     * Produces the "Hello, World" document in response to every request.
     */
    private class HelloWorldEmbeddedDocumentFactory implements EmbeddedDocumentFactory {
        private final Viewhost mViewhost;
        HelloWorldEmbeddedDocumentFactory(Viewhost viewhost) {
            mViewhost = viewhost;
        }

        @Override
        public void onDocumentRequested(EmbeddedDocumentRequest request) {
            ExtensionRegistrar extensionRegistrar = new ExtensionRegistrar().addProvider(mExtensionProvider);
            when(mDocumentOptions.getExtensionGrantRequestCallback()).thenReturn(mExtensionGrantRequestCallback);
            when(mDocumentOptions.getExtensionRegistrar()).thenReturn(extensionRegistrar);

            PrepareDocumentRequest prepareDocumentRequest = PrepareDocumentRequest.builder()
                    .document(new JsonStringDecodable(HELLO_WORLD))
                    .documentSession(DocumentSession.create())
                    .documentOptions(mDocumentOptions)
                    .build();

            PreparedDocument preparedDocument = mViewhost.prepare(prepareDocumentRequest);
            assertNotNull(preparedDocument.getHandle());
            mEmbeddedDocuments.put(request.getSource(), preparedDocument.getHandle());

            EmbeddedDocumentResponse response = EmbeddedDocumentResponse.builder()
                    .preparedDocument(preparedDocument)
                    .visualContextAttached(request.getSource().contains("attached"))
                    .build();
            request.resolve(response);
        }
    }
}
