/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.document;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.dependencies.ISendEventCallbackV2;
import com.amazon.apl.enums.DisplayState;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

public class DisplayStateTest extends AbstractDocUnitTest {
    private static final String EXAMPLE_DOCUMENT = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.8\"," +
            "  \"mainTemplate\": {" +
            "    \"items\": {" +
            "      \"type\": \"Text\"," +
            "      \"text\": \"Display state is: ${displayState}\"" +
            "    }" +
            "  }," +
            "  \"onMount\": [" +
            "    {" +
            "      \"type\": \"SendEvent\"," +
            "      \"arguments\": [" +
            "        \"mount\"" +
            "      ]," +
            "      \"sequencer\": \"MOUNT_SEQUENCER\"" +
            "    }" +
            "  ]," +
            "  \"onDisplayStateChange\": [" +
            "    {" +
            "      \"type\": \"SendEvent\"," +
            "      \"arguments\": [" +
            "        \"immediate:${displayState}\"" +
            "      ]," +
            "      \"sequencer\": \"DISPLAY_STATE_SEQUENCER_1\"" +
            "    }," +
            "    {" +
            "      \"type\": \"SendEvent\"," +
            "      \"arguments\": [" +
            "        \"delayed:${displayState}\"" +
            "      ]," +
            "      \"delay\": 2," +
            "      \"sequencer\": \"DISPLAY_STATE_SEQUENCER_2\"" +
            "    }" +
            "  ]" +
            "}";

    @Mock
    private ISendEventCallbackV2 mMockSendEventCallback;

    @Before
    public void setup() {
        APLOptions options = APLOptions.builder()
                .sendEventCallbackV2(mMockSendEventCallback)
                .build();
        loadDocument(EXAMPLE_DOCUMENT, options);
    }

    @Test
    public void testDisplayStateChangeNotCalledOnInitialRender() {
        verify(mMockSendEventCallback, times(1)).onSendEvent(any(), any(), any(), any());
        verify(mMockSendEventCallback).onSendEvent(eq(new String[]{"mount"}), any(), any(), any());
    }

    @Test
    public void testBackgroundDisplayState() {
        mRootContext.updateDisplayState(DisplayState.kDisplayStateBackground);

        verify(mMockSendEventCallback, times(2)).onSendEvent(any(), any(), any(), any());

        InOrder inOrder = Mockito.inOrder(mRootContext, mMockSendEventCallback);
        verify(mMockSendEventCallback).onSendEvent(eq(new String[]{"mount"}), any(), any(), any());
        verify(mMockSendEventCallback).onSendEvent(eq(new String[]{"immediate:background"}), any(), any(), any());
    }

    @Test
    public void testHiddenDisplayState() {
        mRootContext.updateDisplayState(DisplayState.kDisplayStateHidden);

        verify(mMockSendEventCallback, times(2)).onSendEvent(any(), any(), any(), any());

        InOrder inOrder = Mockito.inOrder(mRootContext, mMockSendEventCallback);
        verify(mMockSendEventCallback).onSendEvent(eq(new String[]{"mount"}), any(), any(), any());
        verify(mMockSendEventCallback).onSendEvent(eq(new String[]{"immediate:hidden"}), any(), any(), any());
    }

    @Test
    public void testUnchangedForegroundDisplayStateChangeIgnored() {
        mRootContext.updateDisplayState(DisplayState.kDisplayStateForeground);

        verify(mMockSendEventCallback, times(1)).onSendEvent(any(), any(), any(), any());
        verify(mMockSendEventCallback).onSendEvent(eq(new String[]{"mount"}), any(), any(), any());
    }

    @Test
    public void testBackgroundThenHiddenDisplayState() {
        mRootContext.updateDisplayState(DisplayState.kDisplayStateBackground);
        mRootContext.updateDisplayState(DisplayState.kDisplayStateHidden);

        verify(mMockSendEventCallback, times(3)).onSendEvent(any(), any(), any(), any());

        InOrder inOrder = Mockito.inOrder(mRootContext, mMockSendEventCallback);
        verify(mMockSendEventCallback).onSendEvent(eq(new String[]{"mount"}), any(), any(), any());
        verify(mMockSendEventCallback).onSendEvent(eq(new String[]{"immediate:background"}), any(), any(), any());
        verify(mMockSendEventCallback).onSendEvent(eq(new String[]{"immediate:hidden"}), any(), any(), any());
    }

    @Test
    public void testBackgroundThenHiddenThenForegroundDisplayState() {
        mRootContext.updateDisplayState(DisplayState.kDisplayStateBackground);
        mRootContext.updateDisplayState(DisplayState.kDisplayStateHidden);
        mRootContext.updateDisplayState(DisplayState.kDisplayStateForeground);

        verify(mMockSendEventCallback, times(4)).onSendEvent(any(), any(), any(), any());

        InOrder inOrder = Mockito.inOrder(mRootContext, mMockSendEventCallback);
        verify(mMockSendEventCallback).onSendEvent(eq(new String[]{"mount"}), any(), any(), any());
        verify(mMockSendEventCallback).onSendEvent(eq(new String[]{"immediate:background"}), any(), any(), any());
        verify(mMockSendEventCallback).onSendEvent(eq(new String[]{"immediate:hidden"}), any(), any(), any());
        verify(mMockSendEventCallback).onSendEvent(eq(new String[]{"immediate:foreground"}), any(), any(), any());
    }

    @Test
    public void testDoesNotSendDelayedMessagesSendAfterInsufficientTick() {
        mRootContext.updateDisplayState(DisplayState.kDisplayStateBackground);
        // Tick forward 1ms, which isn't enough for the 2ms delay specified in the doc
        update(1);

        verify(mMockSendEventCallback, times(2)).onSendEvent(any(), any(), any(), any());

        InOrder inOrder = Mockito.inOrder(mRootContext, mMockSendEventCallback);
        verify(mMockSendEventCallback).onSendEvent(eq(new String[]{"mount"}), any(), any(), any());
        verify(mMockSendEventCallback).onSendEvent(eq(new String[]{"immediate:background"}), any(), any(), any());
    }

    @Test
    public void testDelayedMessagesSendAfterTick() {
        mRootContext.updateDisplayState(DisplayState.kDisplayStateBackground);
        // Tick forward 2ms, which should be enough to trigger the delayed command
        update(2);

        verify(mMockSendEventCallback, times(3)).onSendEvent(any(), any(), any(), any());

        InOrder inOrder = Mockito.inOrder(mRootContext, mMockSendEventCallback);
        verify(mMockSendEventCallback).onSendEvent(eq(new String[]{"mount"}), any(), any(), any());
        verify(mMockSendEventCallback).onSendEvent(eq(new String[]{"immediate:background"}), any(), any(), any());
        verify(mMockSendEventCallback).onSendEvent(eq(new String[]{"delayed:background"}), any(), any(), any());
    }
}