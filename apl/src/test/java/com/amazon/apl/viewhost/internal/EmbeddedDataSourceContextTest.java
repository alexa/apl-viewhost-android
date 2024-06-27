/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.viewhost.internal;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.os.Handler;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.Content;
import com.amazon.apl.android.DocumentSession;
import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.dependencies.IDataSourceContextListener;
import com.amazon.apl.android.dependencies.IDataSourceErrorCallback;
import com.amazon.apl.android.dependencies.IDataSourceFetchCallback;
import com.amazon.apl.android.document.AbstractDocUnitTest;
import com.amazon.apl.android.metrics.ICounter;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.scaling.ViewportMetrics;
import com.amazon.apl.android.utils.APLTrace;
import com.amazon.apl.enums.ScreenShape;
import com.amazon.apl.enums.ViewportMode;
import com.amazon.apl.viewhost.DocumentHandle;
import com.amazon.apl.viewhost.PreparedDocument;
import com.amazon.apl.viewhost.Viewhost;
import com.amazon.apl.viewhost.config.DocumentOptions;
import com.amazon.apl.viewhost.config.EmbeddedDocumentFactory;
import com.amazon.apl.viewhost.config.ViewhostConfig;
import com.amazon.apl.viewhost.message.BaseMessage;
import com.amazon.apl.viewhost.message.action.FetchDataRequest;
import com.amazon.apl.viewhost.message.action.ReportRuntimeErrorRequest;
import com.amazon.apl.viewhost.message.notification.DataSourceContextChanged;
import com.amazon.apl.viewhost.primitives.Decodable;
import com.amazon.apl.viewhost.primitives.JsonDecodable;
import com.amazon.apl.viewhost.primitives.JsonTranscoder;
import com.amazon.apl.viewhost.primitives.JsonStringDecodable;
import com.amazon.apl.viewhost.request.PrepareDocumentRequest;
import com.amazon.apl.viewhost.request.UpdateDataSourceRequest;
import com.amazon.apl.viewhost.request.UpdateDataSourceRequest.UpdateDataSourceCallback;
import com.amazon.apl.viewhost.utils.CapturingMessageHandler;
import com.amazon.apl.viewhost.utils.ManualExecutor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.shadows.ShadowSystemClock;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class EmbeddedDataSourceContextTest extends AbstractDocUnitTest {
    private static final String SHOPPING_LIST_DOC = "{" +
        "  \"type\": \"APL\"," +
        "  \"version\": \"2023.2\"," +
        "  \"mainTemplate\": {" +
        "    \"parameters\": [" +
        "      \"shoppingListData\"" +
        "    ]," +
        "    \"items\": {" +
        "      \"type\": \"Sequence\"," +
        "      \"width\": \"100%\"," +
        "      \"height\": \"100%\"," +
        "      \"data\": \"${shoppingListData}\"," +
        "      \"items\": {" +
        "        \"type\": \"Text\"," +
        "        \"text\": \"${index + 1}. ${data.text}\"," +
        "        \"color\": \"white\"," +
        "        \"textAlign\": \"center\"," +
        "        \"textAlignVertical\": \"center\"" +
        "      }" +
        "    }" +
        "  }" +
        "}";

    private static final String SHOPPING_LIST_DATA_A_INNER = "{" +
            "  \"type\": \"dynamicIndexList\"," +
            "  \"listId\": \"shoppingListA\"," +
            "  \"startIndex\": 0," +
            "  \"minimumInclusiveIndex\": 0," +
            "  \"maximumExclusiveIndex\": 100," +
            "  \"items\": []" +
            "}";

    private static final String SHOPPING_LIST_DATA_A = "{ \"shoppingListData\": " + SHOPPING_LIST_DATA_A_INNER + "}";

    private static final String SHOPPING_LIST_DATA_B = "{ \"shoppingListData\": {" +
            "  \"type\": \"dynamicIndexList\"," +
            "  \"listId\": \"shoppingListB\"," +
            "  \"startIndex\": 0," +
            "  \"minimumInclusiveIndex\": 0," +
            "  \"maximumExclusiveIndex\": 100," +
            "  \"items\": []" +
            "}}";

    private static final String HOST_DOCUMENT = "{" +
        "  \"type\": \"APL\"," +
        "  \"version\": \"2023.2\"," +
        "  \"mainTemplate\": {" +
        "    \"items\": {" +
        "      \"type\": \"Container\"," +
        "      \"items\": [" +
        "        {" +
        "          \"type\": \"Host\"," +
        "          \"source\": \"documentA\"," +
        "          \"width\": \"100vw\"," +
        "          \"height\": \"50vh\"" +
        "        }," +
        "        {" +
        "          \"type\": \"Host\"," +
        "          \"source\": \"documentB\"," +
        "          \"width\": \"100vw\"," +
        "          \"height\": \"50vh\"" +
        "        }" +
        "      ]" +
        "    }" +
        "  }" +
        "}";


    private static final String DATA_SOURCE_TYPE = "dynamicIndexList";

    @Mock
    private Handler mCoreWorker;
    @Mock
    private IDataSourceContextListener mDataSourceContextListener;
    @Mock
    private UpdateDataSourceRequest mUpdateDataSourceRequest;

    private Viewhost mViewhost;
    private CapturingMessageHandler mMessageHandler;
    private ManualExecutor mRuntimeInteractionWorker;
    HashMap<String, DocumentHandle> mEmbeddedDocuments;
    private APLOptions mAplOptions;
    private DocumentOptions mDocumentOptions;
    @Mock
    private UpdateDataSourceCallback mCallback;
    @Mock
    private IDataSourceErrorCallback mDataSourceErrorCallback;
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
        when(mMetricsRecorder.startTimer(anyString(), any())).thenReturn(mTimer);

        // Fake "core worker" executes everything immediately
        when(mCoreWorker.post(any(Runnable.class))).thenAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        });
        mDocumentOptions = DocumentOptions.builder().build();

        // Create new viewhost for handling embedded documents
        ViewhostConfig config = ViewhostConfig.builder().messageHandler(mMessageHandler).defaultDocumentOptions(mDocumentOptions).build();


        // Create primary document using APLController (legacy) method
        mRootConfig = RootConfig.create("Unit Test", "1.0").registerDataSource(DATA_SOURCE_TYPE);
        mViewhost = new ViewhostImpl(config, mRuntimeInteractionWorker, mCoreWorker);
        EmbeddedDocumentFactory factory = new ShoppingListEmbeddedDocumentFactory(mViewhost);
        mAplOptions = APLOptions.builder()
                .dataSourceContextListener(mDataSourceContextListener)
                .dataSourceFetchCallback(new IDataSourceFetchCallback() {
                    @Override
                    public void onDataSourceFetchRequest(String type, Map<String, Object> payload) {
                        sendShoppingListLegacyPathway(payload);
                    }
                })
                .dataSourceErrorCallback(mDataSourceErrorCallback)
                .embeddedDocumentFactory(factory)
                .viewhost(mViewhost)
                .build();
        // TODO: Needed because AbstractDocUnitTest uses a deprecated version of renderDocument
        mRootConfig.setDocumentManager(factory, mCoreWorker, mTelemetryProvider);
    }

    @Test
    public void testShoppingListLegacyPathway() throws JSONException {
        Content content = null;
        try {
            content = Content.create(SHOPPING_LIST_DOC);
        } catch (Content.ContentException e) {
            Assert.fail(e.getMessage());
        }
        assertNotNull(content);
        content.addData("shoppingListData", SHOPPING_LIST_DATA_A_INNER);
        assertTrue(content.isReady());

        ViewportMetrics metrics = ViewportMetrics.builder()
                .width(1280)
                .height(720)
                .dpi(160)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeHub)
                .build();

        IAPLViewPresenter presenter = mock(IAPLViewPresenter.class);
        when(presenter.getAPLTrace()).thenReturn(mock(APLTrace.class));
        when(presenter.getOrCreateViewportMetrics()).thenReturn(metrics);

        mRootContext = RootContext.create(metrics, content, mRootConfig, mAplOptions, presenter, mMetricsRecorder);
        mRootContext.initTime();
        update(100);

        // Not enough enough time has elapsed yet
        verify(mDataSourceContextListener, times(0)).onDataSourceContextUpdate(any());

        ArgumentCaptor<JSONArray> captor = ArgumentCaptor.forClass(JSONArray.class);

        ShadowSystemClock.advanceBy(Duration.ofMillis(400));
        update(400);
        verify(mDataSourceContextListener).onDataSourceContextUpdate(captor.capture());

        // Legacy data source context update has been called. It has something like this
        // [
        //   {
        //     "type": "dynamicIndexList",
        //     "listId": "shoppingListA",
        //     "listVersion": 0,
        //     "minimumInclusiveIndex": 0,
        //     "maximumExclusiveIndex": 100,
        //     "startIndex": 0
        //   }
        // ]
        assertNotNull(captor.getValue());
        JSONArray jsonArray = captor.getValue();
        assertEquals(1, jsonArray.length());
        assertEquals("shoppingListA", jsonArray.getJSONObject(0).getString("listId"));

        verifyNoMoreInteractions(mDataSourceContextListener);

        //test data source errors in legacy pathway
        testDataSourceErrorsForPrimaryDoc(populateMapWithIncorrectData());
    }

    @Test
    public void testUpdateDataSourceInvalidType() {
        loadDocument(HOST_DOCUMENT, mAplOptions);

        // Expect two document handles, registered by the test factory
        assertEquals(2, mEmbeddedDocuments.size());

        DocumentHandle handleA = mEmbeddedDocuments.get("documentA");
        String payload = "{\n" + "  \"type\" : \"INVALID\"\n" + "}";
        when(mUpdateDataSourceRequest.getData()).thenReturn(new JsonStringDecodable(payload));
        when(mUpdateDataSourceRequest.getCallback()).thenReturn(mCallback);

        handleA.updateDataSource(mUpdateDataSourceRequest);

        assertFalse(mRuntimeInteractionWorker.isEmpty());
        mRuntimeInteractionWorker.flush();

        verify(mCallback).onFailure(anyString());
    }

    @Test
    public void testUpdateDataSourceDocumentInvalid() {
        loadDocument(HOST_DOCUMENT, mAplOptions);

        // Expect two document handles, registered by the test factory
        assertEquals(2, mEmbeddedDocuments.size());

        DocumentHandle handleA = mEmbeddedDocuments.get("documentA");
        //invalidate handle
        ((DocumentHandleImpl)handleA).setDocumentState(DocumentState.FINISHED);
        boolean update = handleA.updateDataSource(mUpdateDataSourceRequest);
        assertFalse(update);
        verifyNoInteractions(mUpdateDataSourceRequest);
    }

    @Test
    public void testEmbeddedLoadingSuccess() throws JSONException {
        loadDocument(HOST_DOCUMENT, mAplOptions);

        // Expect two document handles, registered by the test factory
        assertEquals(2, mEmbeddedDocuments.size());

        DocumentHandle handleA = mEmbeddedDocuments.get("documentA");
        DocumentHandle handleB = mEmbeddedDocuments.get("documentB");

        // Documents are already going to be requesting data
        assertFalse(mRuntimeInteractionWorker.isEmpty());
        mRuntimeInteractionWorker.flush();
        assert(mMessageHandler.queue.size() > 0);

        int fetchCountA = 0;
        int fetchCountB = 0;
        int dataSourceContextChangeNotificationsA = 0;
        int dataSourceContextChangeNotificationsB = 0;
        int loopCount = 0;
        while (!mMessageHandler.queue.isEmpty()) {
            loopCount += 1;
            BaseMessage message = mMessageHandler.queue.poll();
            if (message instanceof FetchDataRequest) {
                if (message.getDocument() == handleA) {
                    fetchCountA += 1;
                    // Respond to request
                    sendShoppingList(handleA, ((FetchDataRequest)message).getParameters());
                } else if (message.getDocument() == handleB) {
                    fetchCountB += 1;
                    // Respond to request
                    sendShoppingList(handleB, ((FetchDataRequest)message).getParameters());
                } else {
                    fail("Unexpected fetch request");
                }
            } else if (message instanceof DataSourceContextChanged) {
                if (message.getDocument() == handleA) {
                    dataSourceContextChangeNotificationsA += 1;
                } else if (message.getDocument() == handleB) {
                    dataSourceContextChangeNotificationsB += 1;
                } else {
                    fail("Unexpected notification");
                }
            }

            // Allow more fetches, and note that this is enough time for data context
            // notifications to be sent
            update(100);
            mRuntimeInteractionWorker.flush();

            if (loopCount > 100) {
                fail("Likely infinite loop (> 100)");
            }
        }

        // There's a total of 6 fetches (3 per document) and 2 context change notifications
        assertEquals(3, fetchCountA);
        assertEquals(3, fetchCountB);
        assertEquals(1, dataSourceContextChangeNotificationsA);
        assertEquals(1, dataSourceContextChangeNotificationsB);

        // Let more time go by, make sure there are no further updates
        update(500);
        assertTrue(mRuntimeInteractionWorker.isEmpty());

        // Let's get the data source context as well
        final int[] successFailCount = {0, 0};
        assertTrue(handleA.requestDataSourceContext(new DocumentHandle.DataSourceContextCallback() {
            @Override
            public void onSuccess(Decodable context) {
                assertTrue(context instanceof JsonDecodable);
                JsonTranscoder transcoder = new JsonTranscoder();
                assertTrue(context.transcode(transcoder));
                assertNull(transcoder.getJsonObject());
                JSONArray jsonArray = transcoder.getJsonArray();
                // The result is something like:
                // [
                //   {
                //     "type": "dynamicIndexList",
                //     "listId": "shoppingListA",
                //     "listVersion": 0,
                //     "minimumInclusiveIndex": 0,
                //     "maximumExclusiveIndex": 100,
                //     "startIndex": 0
                //   }
                // ]
                assertEquals(1, jsonArray.length());
                try {
                    assertEquals("shoppingListA", jsonArray.getJSONObject(0).getString("listId"));
                } catch (JSONException e) {
                    e.printStackTrace();
                    fail("Unexpected problem parsing JSON");
                }
                successFailCount[0]++;
            }

            @Override
            public void onFailure(String reason) {
                successFailCount[1]++;
            }
        }));

        assertFalse(mRuntimeInteractionWorker.isEmpty());
        mRuntimeInteractionWorker.flush();
        assertArrayEquals(new int[]{1, 0}, successFailCount);
    }

    @Test
    public void testRequestingDataSourceContextAfterViewhostIsGone() {
        DocumentHandle handle = new DocumentHandleImpl(null, mCoreWorker, null);

        final int[] successFailCount = {0, 0};
        assertFalse(handle.requestDataSourceContext(new DocumentHandle.DataSourceContextCallback() {
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
    public void testDataSourceErrorsInEmbeddedDoc() {
        loadDocument(HOST_DOCUMENT, mAplOptions);
        assertEquals(2, mEmbeddedDocuments.size());
        DocumentHandleImpl handle = (DocumentHandleImpl)mEmbeddedDocuments.get("documentA");

        Map<String, Object> map = populateMapWithIncorrectData();
        sendShoppingList(handle, map);

        //It will internally call coreFrameUpdate in rootContext
        update(500);

        assertFalse(mRuntimeInteractionWorker.isEmpty());
        mRuntimeInteractionWorker.flush();
        assert(mMessageHandler.queue.size() > 0);
        verifyNoInteractions(mDataSourceErrorCallback);

        int errorCount = 0;
        while (!mMessageHandler.queue.isEmpty()) {
            BaseMessage message = mMessageHandler.queue.poll();
            if (message instanceof ReportRuntimeErrorRequest) {
                errorCount++;
            }
        }

        assertEquals(1, errorCount);
    }

    private Map<String, Object> populateMapWithIncorrectData() {
        Map<String, Object> map = new HashMap<>();
        map.put("startIndex", 0);
        map.put("correlationToken", 101);
        map.put("listId", "wrongListId");
        map.put("count", 1);
        return map;
    }

    private void testDataSourceErrorsForPrimaryDoc(Map<String, Object> map) {
        JSONObject response = createResponse(map);
        assertFalse(mRootContext.updateDataSource(DATA_SOURCE_TYPE, response.toString()));
        //It will internally call coreFrameUpdate in rootContext
        update(500);
        mRuntimeInteractionWorker.flush();

        verify(mDataSourceErrorCallback).onDataSourceError(any());

        //verify no errors in embedded docs
        int errorCount = 0;
        while (!mMessageHandler.queue.isEmpty()) {
            BaseMessage message = mMessageHandler.queue.poll();
            if (message instanceof ReportRuntimeErrorRequest) {
                errorCount++;
            }
        }
        assertEquals(0, errorCount);

    }

    @Test
    public void testRequestingDataSourceContextFailsFastForFinishedDocument() {
        loadDocument(HOST_DOCUMENT, mAplOptions);
        assertEquals(2, mEmbeddedDocuments.size());
        DocumentHandleImpl handle = (DocumentHandleImpl)mEmbeddedDocuments.get("documentA");
        handle.setDocumentState(DocumentState.FINISHED);

        final int[] successFailCount = {0, 0};
        assertFalse(handle.requestDataSourceContext(new DocumentHandle.DataSourceContextCallback() {
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

        assertFalse(((DocumentHandleImpl)handle).getAndClearHasDataSourceContextChanged());
    }

    @Test
    public void testRequestingDataSourceContextBeforeDocumentContextIsAvailable() {
        DocumentHandle handle = new DocumentHandleImpl((ViewhostImpl) mViewhost, mCoreWorker, null);

        final int[] successFailCount = {0, 0};
        assertTrue(handle.requestDataSourceContext(new DocumentHandle.DataSourceContextCallback() {
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

        assertFalse(((DocumentHandleImpl)handle).getAndClearHasDataSourceContextChanged());
    }

    @Test
    public void testRequestingDataSourceContextWithSerializationProblem() {
        ViewhostImpl viewhostImpl = (ViewhostImpl) mViewhost;
        DocumentHandleImpl handle = new DocumentHandleImpl(viewhostImpl, mCoreWorker, null);
        DocumentContext context = mock(DocumentContext.class);
        when(context.getId()).thenReturn((long)123);
        when(context.serializeDataSourceContext()).thenReturn("Invalid JSON");
        handle.setDocumentContext(context);

        final int[] successFailCount = {0, 0};
        assertTrue(handle.requestDataSourceContext(new DocumentHandle.DataSourceContextCallback() {
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

    private void sendShoppingListLegacyPathway(Map<String, Object> request) {
        JSONObject response = createResponse(request);
        assertTrue(mRootContext.updateDataSource(DATA_SOURCE_TYPE, response.toString()));
    }

    private JSONObject createResponse(Map<String, Object> request) {
        try {
            int count = (Integer) request.get("count");
            int startIndex = (Integer) request.get("startIndex");

            JSONArray items = new JSONArray();
            for (int i = startIndex; i < startIndex + count; i++) {
                JSONObject item = new JSONObject();
                item.put("text", "item" + i);
                items.put(item);
            }

            // Response payload structure comes from
            // https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-interface.html#sendindexlistdata-directive
            JSONObject response = new JSONObject();
            response.put("correlationToken", request.get("correlationToken"));
            response.put("listId", request.get("listId"));
            response.put("startIndex", request.get("startIndex"));
            response.put("items", items);
            response.put("minimumInclusiveIndex", 0);
            response.put("maximumExclusiveIndex", 100);
            response.put("type", DATA_SOURCE_TYPE);
            return response;
        } catch (JSONException e) {
            fail("JSON exception " + e);
        }
        return null;
    }
    public void sendShoppingList(DocumentHandle handle, Map<String, Object> request) {
        JSONObject response = createResponse(request);
        UpdateDataSourceRequest updateDataSourceRequest = UpdateDataSourceRequest
                .builder()
                .data(new JsonStringDecodable(response.toString()))
                .callback(mCallback)
                .build() ;
        assertTrue(handle.updateDataSource(updateDataSourceRequest));
    }

    /**
     * Produces the shopping list document in response to every request.
     */
    private class ShoppingListEmbeddedDocumentFactory implements EmbeddedDocumentFactory {
        private final Viewhost mViewhost;
        ShoppingListEmbeddedDocumentFactory(Viewhost viewhost) {
            mViewhost = viewhost;
        }

        @Override
        public void onDocumentRequested(EmbeddedDocumentRequest request) {
            // Need a different list ID per document
            String data = request.getSource().contains("documentA") ?
                    SHOPPING_LIST_DATA_A : SHOPPING_LIST_DATA_B;
            PrepareDocumentRequest prepareDocumentRequest = PrepareDocumentRequest.builder()
                    .document(new JsonStringDecodable(SHOPPING_LIST_DOC))
                    .data(new JsonStringDecodable(data))
                    .documentSession(DocumentSession.create())
                    .build();

            PreparedDocument preparedDocument = mViewhost.prepare(prepareDocumentRequest);
            assertNotNull(preparedDocument.getHandle());

            mEmbeddedDocuments.put(request.getSource(), preparedDocument.getHandle());

            request.resolve(preparedDocument);
        }
    }
}
