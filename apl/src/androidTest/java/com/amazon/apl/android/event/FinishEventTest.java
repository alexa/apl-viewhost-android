/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.event;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.document.AbstractDocUnitTest;
import com.amazon.apl.android.dependencies.ISendEventCallback;
import com.amazon.apl.android.dependencies.IOnAplFinishCallback;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@RunWith(AndroidJUnit4.class)
public class FinishEventTest extends AbstractDocUnitTest {
    @Mock
    private IOnAplFinishCallback mMockOnAplFinishListener;
    @Mock
    private ISendEventCallback mMockSendEventCallback;

    private static final String DOC = "{\n" +
            "    \"type\": \"APL\",\n" +
            "    \"version\": \"1.3\",\n" +
            "    \"mainTemplate\": {\n" +
            "        \"item\": {\n" +
            "            \"type\": \"Frame\",\n" +
            "            \"id\": \"frame\",\n" +
            "            \"width\": 100,\n" +
            "            \"height\": 100,\n" +
            "            \"backgroundColor\": \"green\"\n" +
            "        }\n" +
            "    }\n" +
            "}";

    private final String FINISH_COMMAND =
            "[\n" +
                    "    {\n" +
                    "        \"type\": \"Finish\"\n" +
                    "    }\n" +
                    "]";

    private final String FINISH_COMMAND_MULTIPLE_FINISH_FIRST =
            "[\n" +
                    "    {\n"    +
                    "        \"type\": \"Finish\",\n" +
                    "        \"reason\": \"exit\"\n" +
                    "    },\n" +
                    "    {\n"    +
                    "        \"type\": \"SendEvent\",\n" +
                    "        \"arguments\": [ \"exiting\" ]\n" +
                    "    }\n" +
                    "]";

    private final String FINISH_COMMAND_MULTIPLE_FINISH_LAST =
            "[\n" +
                    "    {\n"    +
                    "        \"type\": \"SendEvent\",\n" +
                    "        \"arguments\": [ \"exiting\" ]\n" +
                    "    },\n" +
                    "    {\n"    +
                    "        \"type\": \"Finish\",\n" +
                    "        \"reason\": \"exit\"\n" +
                    "    }\n" +
                    "]";

    @Before
    public void setUp() {
        APLOptions options = APLOptions.builder()
                .onAplFinishCallback(mMockOnAplFinishListener)
                .sendEventCallback(mMockSendEventCallback)
                .build();

        loadDocument(DOC, options);
    }

    @Test
    public void testFinishEvent_noReason() {
        testEvent(FINISH_COMMAND);
    }

    @Test
    public void testFinishEvent_multipleEventsFinishFirst() {
        testEvent(FINISH_COMMAND_MULTIPLE_FINISH_FIRST);
        // Verify that send event is not sent
        verifyZeroInteractions(mMockSendEventCallback);
    }

    @Test
    public void testFinishEvent_multipleEventsFinishLast() {
        testEvent(FINISH_COMMAND_MULTIPLE_FINISH_LAST);
        // Verify that send event is sent
        verify(mMockSendEventCallback).onSendEvent(any(), anyMap(), anyMap());
    }

    private void testEvent(String expected) {
        // Execute commands
        mRootContext.executeCommands(expected);
        update(100);

        // Verify interaction with expected reason
        verify(mMockOnAplFinishListener).onAplFinish();
    }
}
