/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.viewhost.rendering;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import com.amazon.apl.android.DocumentSession;
import com.amazon.apl.viewhost.AbstractUnifiedViewhostTest;
import com.amazon.apl.viewhost.DocumentHandle;
import com.amazon.apl.viewhost.PreparedDocument;
import com.amazon.apl.viewhost.internal.DocumentState;
import com.amazon.apl.viewhost.message.action.SendUserEventRequest;
import com.amazon.apl.viewhost.primitives.JsonStringDecodable;
import com.amazon.apl.viewhost.request.FinishDocumentRequest;
import com.amazon.apl.viewhost.request.PrepareDocumentRequest;
import com.amazon.apl.viewhost.request.RenderDocumentRequest;
import com.amazon.apl.viewhost.request.UpdateDataSourceRequest;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QueuedDynamicDataSourceTest extends AbstractUnifiedViewhostTest {
    private static final String DYNAMIC_DOCUMENT = "{" +
        "  \"type\": \"APL\"," +
        "  \"version\": \"2024.2\"," +
        "  \"onMount\": {" +
        "    \"type\": \"SendEvent\"," +
        "    \"arguments\": [" +
        "      \"Main:onMount\"" +
        "    ]" +
        "  }," +
        "  \"mainTemplate\": {" +
        "    \"parameters\": [" +
        "      \"names\"" +
        "    ]," +
        "    \"item\": {" +
        "      \"type\": \"Container\"," +
        "      \"data\": \"${names}\"," +
        "      \"item\": {" +
        "        \"type\": \"Text\"," +
        "        \"text\": \"Hello, ${data}!\"," +
        "        \"color\": \"white\"," +
        "        \"textAlign\": \"center\"," +
        "        \"textAlignVertical\": \"center\"," +
        "        \"onMount\": {" +
        "          \"type\": \"SendEvent\"," +
        "          \"arguments\": [" +
        "            \"${index}:${data}\"" +
        "          ]" +
        "        }" +
        "      }" +
        "    }" +
        "  }" +
        "}";

    private static final String DYNAMIC_DATA = "{" +
        "  \"names\": {" +
        "    \"type\": \"dynamicIndexList\"," +
        "    \"listId\": \"namesList\"," +
        "    \"startIndex\": 0" +
        "  }" +
        "}";

    private static final String DYNAMIC_UPDATE = "{" +
        "  \"listId\": \"namesList\"," +
        "  \"listVersion\": 1," +
        "  \"operations\": [" +
        "    {" +
        "      \"type\": \"InsertMultipleItems\"," +
        "      \"index\": 0," +
        "      \"items\": [" +
        "        \"Foo\"," +
        "        \"Bar\"" +
        "      ]" +
        "    }" +
        "  ]" +
        "}";

    List<Boolean> mCallbackResults = new ArrayList<>();
    List<String> mCallbackReasons = new ArrayList<>();

    UpdateDataSourceRequest.UpdateDataSourceCallback mCallback;


    @Before
    public void initializeCallback() {
        mCallback = new UpdateDataSourceRequest.UpdateDataSourceCallback() {
            @Override
            public void onSuccess() {
                mCallbackResults.add(true);
            }

            @Override
            public void onFailure(String reason) {
                mCallbackResults.add(false);
                mCallbackReasons.add(reason);
            }
        };
    }

    @Test
    public void testDynamicDataUpdateAfterInflation() {
        RenderDocumentRequest renderDocumentRequest = RenderDocumentRequest.builder()
                .document(new JsonStringDecodable(DYNAMIC_DOCUMENT))
                .data(new JsonStringDecodable(DYNAMIC_DATA))
                .documentSession(DocumentSession.create())
                .build();

        DocumentHandle handle = mViewhost.render(renderDocumentRequest);
        assertSendEvent("Main:onMount");

        List<Boolean> results = new ArrayList<>();
        UpdateDataSourceRequest updateDataSourceRequest = UpdateDataSourceRequest.builder()
                .data(new JsonStringDecodable(DYNAMIC_UPDATE))
                .type("dynamicIndexList")
                .callback(mCallback)
                .build();
        handle.updateDataSource(updateDataSourceRequest);
        runUntil(() -> mMessageHandler.findAll(SendUserEventRequest.class).size() >= 3);
        assertEquals(1, mCallbackResults.size());
        assertTrue(mCallbackResults.get(0));
        assertAllUserEventsReceived();
    }

    @Test
    public void testDynamicDataUpdateBeforeInflation() {
        PrepareDocumentRequest prepareDocumentRequest = PrepareDocumentRequest.builder()
                .document(new JsonStringDecodable(DYNAMIC_DOCUMENT))
                .data(new JsonStringDecodable(DYNAMIC_DATA))
                .documentSession(DocumentSession.create())
                .build();

        PreparedDocument preparedDocument = mViewhost.prepare(prepareDocumentRequest);

        List<Boolean> results = new ArrayList<>();
        UpdateDataSourceRequest updateDataSourceRequest = UpdateDataSourceRequest.builder()
                .data(new JsonStringDecodable(DYNAMIC_UPDATE))
                .type("dynamicIndexList")
                .callback(mCallback)
                .build();

        // The following request will be queued because RootContext does not yet exist
        preparedDocument.getHandle().updateDataSource(updateDataSourceRequest);

        mViewhost.render(preparedDocument);
        runUntil(() -> mMessageHandler.findAll(SendUserEventRequest.class).size() >= 3);
        assertEquals(1, mCallbackResults.size());
        assertTrue(mCallbackResults.get(0));
        assertAllUserEventsReceived();
    }

    private void assertAllUserEventsReceived() {
        List<SendUserEventRequest> userEvents = mMessageHandler.findAll(SendUserEventRequest.class);

        // Sort because order doesn't matter here
        List<String> arguments = new ArrayList<>();
        for (SendUserEventRequest event : userEvents) {
            arguments.add(event.getArguments()[0].toString());
        }
        Collections.sort(arguments);

        // Compare the sorted strings
        assertEquals(3, arguments.size());
        assertEquals("0:Foo", arguments.get(0));
        assertEquals("1:Bar", arguments.get(1));
        assertEquals("Main:onMount", arguments.get(2));
    }

    @Test
    public void testFastFailureForInvalidDocument() {
        PrepareDocumentRequest prepareDocumentRequest = PrepareDocumentRequest.builder()
                .document(new JsonStringDecodable("Invalid"))
                .data(new JsonStringDecodable(DYNAMIC_DATA))
                .documentSession(DocumentSession.create())
                .build();
        PreparedDocument preparedDocument = mViewhost.prepare(prepareDocumentRequest);
        mViewhost.render(preparedDocument);

        assertDocumentStateChanged(DocumentState.ERROR.toString());

        UpdateDataSourceRequest updateDataSourceRequest = UpdateDataSourceRequest.builder()
                .data(new JsonStringDecodable(DYNAMIC_UPDATE))
                .type("dynamicIndexList")
                .build();
        assertFalse(preparedDocument.getHandle().updateDataSource(updateDataSourceRequest));
    }

    @Test
    public void testFailureCallbackForInvalidDataSourceUpdateJson() {
        RenderDocumentRequest renderDocumentRequest = RenderDocumentRequest.builder()
                .document(new JsonStringDecodable(DYNAMIC_DOCUMENT))
                .data(new JsonStringDecodable(DYNAMIC_DATA))
                .documentSession(DocumentSession.create())
                .build();

        DocumentHandle handle = mViewhost.render(renderDocumentRequest);
        assertSendEvent("Main:onMount");

        List<Boolean> results = new ArrayList<>();
        UpdateDataSourceRequest updateDataSourceRequest = UpdateDataSourceRequest.builder()
                .data(new JsonStringDecodable("Invalid"))
                .type("dynamicIndexList")
                .callback(mCallback)
                .build();
        handle.updateDataSource(updateDataSourceRequest);
        runUntil(() -> mCallbackResults.size() > 0);
        assertEquals(1, mCallbackResults.size());
        assertFalse(mCallbackResults.get(0));
        assertEquals(1, mCallbackReasons.size());
        assertTrue(mCallbackReasons.get(0).contains("JSON parsing error"));
    }

    @Test
    public void testFailureCallbackForEmptyDataSourceType() {
        RenderDocumentRequest renderDocumentRequest = RenderDocumentRequest.builder()
                .document(new JsonStringDecodable(DYNAMIC_DOCUMENT))
                .data(new JsonStringDecodable(DYNAMIC_DATA))
                .documentSession(DocumentSession.create())
                .build();

        DocumentHandle handle = mViewhost.render(renderDocumentRequest);
        assertSendEvent("Main:onMount");

        List<Boolean> results = new ArrayList<>();
        UpdateDataSourceRequest updateDataSourceRequest = UpdateDataSourceRequest.builder()
                .data(new JsonStringDecodable(DYNAMIC_DATA))
                .type("")
                .callback(mCallback)
                .build();
        handle.updateDataSource(updateDataSourceRequest);
        runUntil(() -> mCallbackResults.size() > 0);
        assertEquals(1, mCallbackResults.size());
        assertFalse(mCallbackResults.get(0));
        assertEquals(1, mCallbackReasons.size());
        assertTrue(mCallbackReasons.get(0).contains("type not defined"));
    }

    private static final String DYNAMIC_UPDATE_INVALID_VERSION = "{" +
        "  \"listId\": \"namesList\"," +
        "  \"listVersion\": 99," +
        "  \"operations\": [" +
        "    {" +
        "      \"type\": \"InsertMultipleItems\"," +
        "      \"index\": 0," +
        "      \"items\": [" +
        "        \"Foo\"," +
        "        \"Bar\"" +
        "      ]" +
        "    }" +
        "  ]" +
        "}";

    @Test
    public void testFailureCallbackForInvalidListVersion() {
        RenderDocumentRequest renderDocumentRequest = RenderDocumentRequest.builder()
                .document(new JsonStringDecodable(DYNAMIC_DOCUMENT))
                .data(new JsonStringDecodable(DYNAMIC_DATA))
                .documentSession(DocumentSession.create())
                .build();

        DocumentHandle handle = mViewhost.render(renderDocumentRequest);
        assertSendEvent("Main:onMount");

        List<Boolean> results = new ArrayList<>();
        UpdateDataSourceRequest updateDataSourceRequest = UpdateDataSourceRequest.builder()
                .data(new JsonStringDecodable(DYNAMIC_UPDATE_INVALID_VERSION))
                .type("dynamicIndexList")
                .callback(mCallback)
                .build();
        handle.updateDataSource(updateDataSourceRequest);
        runUntil(() -> mCallbackResults.size() > 0);
        assertEquals(1, mCallbackResults.size());
        assertFalse(mCallbackResults.get(0));
        assertEquals(1, mCallbackReasons.size());
        assertTrue(mCallbackReasons.get(0).contains("runtime error"));
    }

    @Test
    public void testFailureCallbackForDocumentBecameInvalid() {
        PrepareDocumentRequest prepareDocumentRequest = PrepareDocumentRequest.builder()
                .document(new JsonStringDecodable(DYNAMIC_DOCUMENT))
                .data(new JsonStringDecodable(DYNAMIC_DATA))
                .documentSession(DocumentSession.create())
                .build();
        PreparedDocument preparedDocument = mViewhost.prepare(prepareDocumentRequest);

        List<Boolean> results = new ArrayList<>();
        List<String> reasons = new ArrayList<>();
        UpdateDataSourceRequest updateDataSourceRequest = UpdateDataSourceRequest.builder()
                .data(new JsonStringDecodable(DYNAMIC_UPDATE))
                .type("dynamicIndexList")
                .callback(mCallback)
                .build();

        // The following request will be queued because RootContext does not yet exist
        preparedDocument.getHandle().updateDataSource(updateDataSourceRequest);

        FinishDocumentRequest finishDocumentRequest = FinishDocumentRequest.builder().build();
        preparedDocument.getHandle().finish(finishDocumentRequest);

        runUntil(() -> mCallbackResults.size() > 0);
        assertEquals(1, mCallbackResults.size());
        assertFalse(mCallbackResults.get(0));
        assertEquals(1, mCallbackReasons.size());
        assertTrue(mCallbackReasons.get(0).contains("became invalid"));
    }
}
