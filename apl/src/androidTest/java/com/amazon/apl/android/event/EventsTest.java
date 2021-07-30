/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.event;


import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.amazon.apl.android.document.AbstractDocUnitTest;
import com.amazon.apl.android.Action;
import com.amazon.apl.android.Event;
import com.amazon.apl.android.Frame;
import com.amazon.apl.android.events.OpenURLEvent;
import com.amazon.apl.enums.EventType;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicBoolean;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;


@RunWith(AndroidJUnit4.class)
public class EventsTest extends AbstractDocUnitTest {

    // Test content
    private final String DOC = "{\n" +
            "    \"type\": \"APL\",\n" +
            "    \"version\": \"1.0\",\n" +
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


    private final String SET_VALUE_COMMANDS = "[\n" +
            "    {\n" +
            "        \"type\": \"SetValue\",\n" +
            "        \"property\": \"backgroundColor\",\n" +
            "        \"value\": \"blue\",\n" +
            "        \"componentId\": \"frame\"\n" +
            "    }\n" +
            "]";

    private final String OPEN_URL_COMMANDS = "[\n" +
            "    {\n" +
            "        \"type\": \"OpenURL\",\n" +
            "        \"source\": \"https://amazon.com\",\n" +
            "        \"onFail\": [\n" +
            "            {\n" +
            "                \"type\": \"SetValue\",\n" +
            "                \"property\": \"backgroundColor\",\n" +
            "                \"value\": \"red\",\n" +
            "                \"componentId\": \"frame\"\n" +
            "            }\n" +
            "        ]\n" +
            "    }\n" +
            "]";

    @Before
    public void setup() {
    }

    /**
     * Tests that an event was properly processed
     */
    @Test
    @SmallTest
    public void testCommands_eventResolve() {
        loadDocument(DOC);
        AtomicBoolean built = new AtomicBoolean(false);
        AtomicBoolean executed = new AtomicBoolean(false);
        AtomicBoolean terminated = new AtomicBoolean(false);
        AtomicBoolean completed = new AtomicBoolean(false);

        doAnswer(answer -> {
            long nativeHandle = answer.getArgument(0);
            int type = answer.getArgument(1);
            assertEquals(type, EventType.kEventTypeOpenURL.getIndex());
            OpenURLEvent event = spy(OpenURLEvent.create(nativeHandle, mRootContext, mOptions.getOpenUrlCallback()));

            doAnswer(executeAnswer -> {
                executed.set(true);
                Event mockedEvent = (Event)executeAnswer.getMock();
                mockedEvent.resolve(0);
                return null;
            }).when(event).execute();

            doAnswer(executeAnswer -> {
                terminated.set(true);
                return null;
            }).when((Event)event).terminate();

            built.set(true);
            return event;

        }).when(mRootContext).buildEvent(anyLong(), anyInt());

        Action action = mRootContext.executeCommands(OPEN_URL_COMMANDS);
        action.then(() -> {
            completed.set(true);
        });
        update(100);
        assertTrue(built.get());
        assertTrue(executed.get());
        assertTrue(completed.get());
        assertFalse(terminated.get());
    }

    /**
     * Tests that an event was properly processed
     */
    @Test
    @SmallTest
    public void testCommands_eventResolveFail() {
        loadDocument(DOC);
        AtomicBoolean completed = new AtomicBoolean(false);

        doAnswer(answer -> {
            long nativeHandle = answer.getArgument(0);
            OpenURLEvent event = spy(OpenURLEvent.create(nativeHandle, mRootContext, mOptions.getOpenUrlCallback()));

            doAnswer(executeAnswer -> {
                Event mockedEvent = (Event)executeAnswer.getMock();
                mockedEvent.resolve(1);
                return null;
            }).when(event).execute();

            doAnswer(executeAnswer -> {
                return null;
            }).when((Event)event).terminate();

            return event;

        }).when(mRootContext).buildEvent(anyLong(), anyInt());

        Action action = mRootContext.executeCommands(OPEN_URL_COMMANDS);
        action.then(() -> {
            completed.set(true);
        });

        mRootContext.doFrame(0);
        mRootContext.doFrame(100000000);

        // check frame color
        Frame frame = (Frame) mRootContext.getTopComponent();
        int color = frame.getBackgroundColor();
        String hexColor = String.format("#%08X", (color));
        assertEquals("#FFFF0000", hexColor);
        assertTrue(completed.get());
    }

    /**
     * Tests that an event was properly processed
     */
    @Test
    @SmallTest
    public void testCommands_eventTerminated() {
        loadDocument(DOC);
        AtomicBoolean urlCompleted = new AtomicBoolean(false);
        AtomicBoolean urlTerminated = new AtomicBoolean(false);
        LambdaWrapper<OpenURLEvent> wrapper = new LambdaWrapper<OpenURLEvent>();
        doAnswer(answer -> {
            long nativeHandle = answer.getArgument(0);
            OpenURLEvent realEvent = OpenURLEvent.create(nativeHandle, mRootContext, mOptions.getOpenUrlCallback());
            OpenURLEvent event = spy(realEvent);
            wrapper.set(realEvent);
            doAnswer(executeAnswer -> {
                // don't resolve
                return null;
            }).when(event).execute();
            return event;

        }).when(mRootContext).buildEvent(anyLong(), anyInt());

        Action urlAction = mRootContext.executeCommands(OPEN_URL_COMMANDS);
        urlAction.then(() -> {
            urlCompleted.set(true);
        });
        urlAction.addTerminateCallback(() -> {
            urlTerminated.set(true);
        });

        mRootContext.doFrame(0);
        mRootContext.doFrame(100000000);
        mRootContext.doFrame(200000000);

        mRootContext.cancelExecution();

        mRootContext.doFrame(300000000);

        // Event and Action should be terminated
        assertTrue(wrapper.get().isTerminated());
        assertTrue(urlTerminated.get());
        assertFalse(urlCompleted.get());
    }

    private final static String SCROLL_COMMANDS = "[{\n" +
            "    \"type\": \"ScrollToIndex\",\n" +
            "    \"componentId\": \"list\",\n" +
            "    \"index\": 5,\n" +
            "    \"align\": \"first\"\n" +
            "}]";

    private final static String SEQUENCE_DOC = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"1.0\",\n" +
            "  \"mainTemplate\": {\n" +
            "    \"item\": {\n" +
            "      \"type\": \"Sequence\",\n" +
            "      \"id\": \"list\",\n" +
            "      \"data\": [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10],\n" +
            "      \"height\": 300,\n" +
            "      \"width\": 300,\n" +
            "      \"item\": {\n" +
            "        \"type\": \"Text\",\n" +
            "        \"text\": \"Hello\"\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}\n" +
            "\n";

    
    @Test
    @SmallTest
    public void testCommands_badCommandDontCrash() {
        loadDocument(SEQUENCE_DOC);

        Action action = mRootContext.executeCommands("[{}]");
        mRootContext.doFrame(0);
        mRootContext.doFrame(100000000);
        mRootContext.doFrame(200000000);
        mRootContext.cancelExecution();
        mRootContext.doFrame(300000000);
    }

}
