/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.viewhost.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.amazon.apl.android.DocumentSession;
import com.amazon.apl.viewhost.AbstractUnifiedViewhostTest;
import com.amazon.apl.viewhost.DocumentHandle;
import com.amazon.apl.viewhost.message.action.SendUserEventRequest;
import com.amazon.apl.viewhost.primitives.DisplayState;
import com.amazon.apl.viewhost.primitives.JsonStringDecodable;
import com.amazon.apl.viewhost.request.ExecuteCommandsRequest;
import com.amazon.apl.viewhost.request.RenderDocumentRequest;
import com.amazon.apl.viewhost.request.UpdateViewStateRequest;

import org.junit.Before;
import org.junit.Test;

public class UpdateViewStateTest extends AbstractUnifiedViewhostTest {
    private static final String TAG = UpdateViewStateTest.class.getSimpleName();
    private static final String SIMPLE_DOCUMENT = "{" +
        "  \"type\": \"APL\"," +
        "  \"version\": \"2024.2\"," +
        "  \"onMount\": {" +
        "    \"type\": \"SendEvent\"," +
        "    \"arguments\": [" +
        "      \"mount:${displayState}\"" +
        "    ]," +
        "    \"delay\": 50" +
        "  }," +
        "  \"onDisplayStateChange\": {" +
        "    \"type\": \"SendEvent\"," +
        "    \"sequencer\": \"MY_SEQUENCER\"," +
        "    \"arguments\": [" +
        "      \"${elapsedTime}\"," +
        "      \"${event.displayState}\"" +
        "    ]" +
        "  }," +
        "  \"mainTemplate\": {" +
        "    \"items\": {" +
        "      \"type\": \"Text\"," +
        "      \"text\": \"Hello, World!\"" +
        "    }" +
        "  }" +
        "}";

    private static final String EXECUTE_COMMANDS = "[{" +
        "  \"type\": \"SendEvent\"," +
        "  \"arguments\": [" +
        "    \"command:${displayState}\"" +
        "  ]," +
        "  \"delay\": 50" +
        "}]";

    int mComplete = 0;

    DocumentHandle mDocument;

    UpdateViewStateRequest.Builder mRequestBuilder = UpdateViewStateRequest.builder();

    @Before
    public void setup() {

        mRequestBuilder.callback(new UpdateViewStateRequest.UpdateViewStateCallback() {
            @Override
            public void onComplete() {
                mComplete += 1;
            }
        });
    }

    /**
     * Helper to render a document and assert the expected display state (triggered onMount)
     */
    private void setupDocumentWithExpectedDisplayState(String displayState) {
        RenderDocumentRequest renderDocumentRequest = RenderDocumentRequest.builder()
                .document(new JsonStringDecodable(SIMPLE_DOCUMENT))
                .documentSession(DocumentSession.create())
                .build();

        mDocument = mViewhost.render(renderDocumentRequest);
        assertSendEvent("mount:" + displayState);
        mMessageHandler.queue.clear();
    }

    @Test
    public void testTransitionToBackground() {
        setupDocumentWithExpectedDisplayState("foreground");

        UpdateViewStateRequest request = mRequestBuilder
                .displayState(DisplayState.BACKGROUND)
                .build();
        mViewhost.updateViewState(request);

        runUntil(() -> mComplete > 0);

        assertEquals(1, mComplete);
        SendUserEventRequest message = mMessageHandler.findOne(SendUserEventRequest.class);
        assertNotNull(message);
        assertEquals(2, message.getArguments().length);
        assertEquals("background", message.getArguments()[1]);
        assertEquals(DisplayState.BACKGROUND, mViewhost.getDisplayState());
        assertTrue(mViewhost.getProcessingRate() < 0);
    }

    @Test
    public void testTransitionToHidden() {
        setupDocumentWithExpectedDisplayState("foreground");

        UpdateViewStateRequest request = mRequestBuilder
                .displayState(DisplayState.HIDDEN)
                .build();
        mViewhost.updateViewState(request);

        runUntil(() -> mComplete > 0);

        assertEquals(1, mComplete);
        SendUserEventRequest message = mMessageHandler.findOne(SendUserEventRequest.class);
        assertNotNull(message);
        assertEquals(2, message.getArguments().length);
        assertEquals("hidden", message.getArguments()[1]);
        assertEquals(DisplayState.HIDDEN, mViewhost.getDisplayState());
        assertTrue(mViewhost.getProcessingRate() < 0);
    }

    @Test
    public void testTransitionBackToForeground() {
        setupDocumentWithExpectedDisplayState("foreground");

        UpdateViewStateRequest request = mRequestBuilder
                .displayState(DisplayState.BACKGROUND)
                .build();
        mViewhost.updateViewState(request);

        runUntil(() -> mComplete > 0);
        assertEquals(1, mComplete);

        SendUserEventRequest message = mMessageHandler.findOne(SendUserEventRequest.class);
        assertNotNull(message);
        assertEquals(2, message.getArguments().length);
        assertEquals("background", message.getArguments()[1]);
        assertEquals(DisplayState.BACKGROUND, mViewhost.getDisplayState());
        assertTrue(mViewhost.getProcessingRate() < 0);

        mMessageHandler.queue.clear();

        request = mRequestBuilder
                .displayState(DisplayState.FOREGROUND)
                .build();
        mViewhost.updateViewState(request);

        runUntil(() -> mComplete > 1);
        assertEquals(2, mComplete);

        message = mMessageHandler.findOne(SendUserEventRequest.class);
        assertNotNull(message);
        assertEquals(2, message.getArguments().length);
        assertEquals("foreground", message.getArguments()[1]);
        assertEquals(DisplayState.FOREGROUND, mViewhost.getDisplayState());
        assertTrue(mViewhost.getProcessingRate() < 0);
    }

    @Test
    public void testTransitionToBackgroundAndPause() {
        setupDocumentWithExpectedDisplayState("foreground");

        UpdateViewStateRequest request = mRequestBuilder
                .displayState(DisplayState.BACKGROUND)
                .processingRate(0.0)
                .build();
        mViewhost.updateViewState(request);

        runUntil(() -> mComplete > 0);

        assertEquals(1, mComplete);
        SendUserEventRequest message = mMessageHandler.findOne(SendUserEventRequest.class);
        assertNotNull(message);
        assertEquals(2, message.getArguments().length);
        assertEquals("background", message.getArguments()[1]);
        assertEquals(DisplayState.BACKGROUND, mViewhost.getDisplayState());
        assertEquals(0.0, mViewhost.getProcessingRate(), 0.0);
    }

    @Test
    public void testElapsedTimeStopsWhenPaused() {
        setupDocumentWithExpectedDisplayState("foreground");

        UpdateViewStateRequest request = mRequestBuilder
                .displayState(DisplayState.BACKGROUND)
                .processingRate(0.0)
                .build();
        mViewhost.updateViewState(request);

        runUntil(() -> mComplete > 0);
        assertEquals(1, mComplete);

        SendUserEventRequest message = mMessageHandler.findOne(SendUserEventRequest.class);
        assertNotNull(message);
        assertEquals(2, message.getArguments().length);
        assertEquals("background", message.getArguments()[1]);
        Integer elapsedTimeAtBackground = (Integer)message.getArguments()[0];
        assertEquals(DisplayState.BACKGROUND, mViewhost.getDisplayState());
        assertEquals(0.0, mViewhost.getProcessingRate(), 0.0);

        mMessageHandler.queue.clear();

        request = mRequestBuilder
                .displayState(DisplayState.FOREGROUND)
                .processingRate(-1.0)
                .build();
        mViewhost.updateViewState(request);

        runUntil(() -> mComplete > 1);
        assertEquals(2, mComplete);

        message = mMessageHandler.findOne(SendUserEventRequest.class);
        assertNotNull(message);
        assertEquals(2, message.getArguments().length);
        assertEquals("foreground", message.getArguments()[1]);
        Integer elapsedTimeAtForeground = (Integer)message.getArguments()[0];

        assertEquals(elapsedTimeAtBackground, elapsedTimeAtForeground);
        assertEquals(DisplayState.FOREGROUND, mViewhost.getDisplayState());
        assertTrue(mViewhost.getProcessingRate() < 0);

        mMessageHandler.queue.clear();

        ExecuteCommandsRequest executeCommandsRequest = ExecuteCommandsRequest.builder()
                .commands(new JsonStringDecodable(EXECUTE_COMMANDS))
                .build();

        mDocument.executeCommands(executeCommandsRequest);
        assertSendEvent("command:foreground");
    }

    @Test
    public void testDisplayStateApplied() {
        setupDocumentWithExpectedDisplayState("foreground");

        UpdateViewStateRequest request = mRequestBuilder
                .displayState(DisplayState.BACKGROUND)
                .processingRate(0.0)
                .build();
        mViewhost.updateViewState(request);

        runUntil(() -> mComplete > 0);
        assertEquals(1, mComplete);

        SendUserEventRequest message = mMessageHandler.findOne(SendUserEventRequest.class);
        assertNotNull(message);
        assertEquals(2, message.getArguments().length);
        assertEquals("background", message.getArguments()[1]);
        Integer elapsedTimeAtBackground = (Integer)message.getArguments()[0];

        mMessageHandler.queue.clear();

        request = mRequestBuilder
                .displayState(DisplayState.FOREGROUND)
                .processingRate(-1.0)
                .build();
        mViewhost.updateViewState(request);

        runUntil(() -> mComplete > 1);
        assertEquals(2, mComplete);

        message = mMessageHandler.findOne(SendUserEventRequest.class);
        assertNotNull(message);
        assertEquals(2, message.getArguments().length);
        assertEquals("foreground", message.getArguments()[1]);
        Integer elapsedTimeAtForeground = (Integer)message.getArguments()[0];

        assertEquals(elapsedTimeAtBackground, elapsedTimeAtForeground);

        mMessageHandler.queue.clear();

        ExecuteCommandsRequest executeCommandsRequest = ExecuteCommandsRequest.builder()
                .commands(new JsonStringDecodable(EXECUTE_COMMANDS))
                .build();

        mDocument.executeCommands(executeCommandsRequest);
        assertSendEvent("command:foreground");
    }

    @Test
    public void testViewStateAppliedBeforeRendering() {
        UpdateViewStateRequest request = mRequestBuilder
                .displayState(DisplayState.BACKGROUND)
                .build();
        mViewhost.updateViewState(request);

        runUntil(() -> mComplete > 0);
        assertEquals(1, mComplete);

        mMessageHandler.queue.clear();

        setupDocumentWithExpectedDisplayState("background");
    }

    @Test
    public void testViewStateAppliedWhenReusingDocument() {
        setupDocumentWithExpectedDisplayState("foreground");

        UpdateViewStateRequest request = mRequestBuilder
                .displayState(DisplayState.BACKGROUND)
                .build();
        mViewhost.updateViewState(request);

        runUntil(() -> mComplete > 0);
        assertEquals(1, mComplete);

        SendUserEventRequest message = mMessageHandler.findOne(SendUserEventRequest.class);
        assertNotNull(message);
        assertEquals(2, message.getArguments().length);
        assertEquals("background", message.getArguments()[1]);

        mMessageHandler.queue.clear();

        request = mRequestBuilder
                .displayState(DisplayState.FOREGROUND)
                .build();
        mViewhost.updateViewState(request);

        runUntil(() -> mComplete > 1);
        assertEquals(2, mComplete);

        message = mMessageHandler.findOne(SendUserEventRequest.class);
        assertNotNull(message);
        assertEquals(2, message.getArguments().length);
        assertEquals("foreground", message.getArguments()[1]);
    }

    @Test
    public void testWithSlowBackgroundProcessingRate() {
        setupDocumentWithExpectedDisplayState("foreground");

        // Setup 5Hz background rate
        UpdateViewStateRequest request = mRequestBuilder
                .processingRate(5.0)
                .displayState(DisplayState.BACKGROUND)
                .build();
        mViewhost.updateViewState(request);

        runUntil(() -> mComplete > 0);

        assertEquals(1, mComplete);
        SendUserEventRequest message = mMessageHandler.findOne(SendUserEventRequest.class);
        assertNotNull(message);
        assertEquals(2, message.getArguments().length);
        assertEquals("background", message.getArguments()[1]);

        // TODO: Assert slower pace, perhaps by counting ticks seen by a document tick handler.
    }
}
