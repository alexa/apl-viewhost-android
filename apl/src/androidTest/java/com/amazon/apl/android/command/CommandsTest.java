/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.command;


import com.amazon.apl.android.Action;
import com.amazon.apl.android.Frame;
import com.amazon.apl.android.document.AbstractDocUnitTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicBoolean;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import static com.amazon.common.test.Asserts.assertNativeHandle;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;


@RunWith(AndroidJUnit4.class)
public class CommandsTest extends AbstractDocUnitTest {

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


    private final String COMMANDS = "[\n" +
            "    {\n" +
            "        \"type\": \"SetValue\",\n" +
            "        \"property\": \"backgroundColor\",\n" +
            "        \"value\": \"purple\",\n" +
            "        \"componentId\": \"frame\"\n" +
            "    },\n" +
            "    {\n" +
            "        \"type\": \"Idle\",\n" +
            "        \"delay\": 1000\n" +
            "    },\n" +
            "    {\n" +
            "        \"type\": \"SetValue\",\n" +
            "        \"property\": \"backgroundColor\",\n" +
            "        \"value\": \"orange\",\n" +
            "        \"componentId\": \"frame\"\n" +
            "    }\n" +
            "]";

    @Before
    public void setup() {
        loadDocument(DOC);
    }

    /**
     * Tests that a command was executed with the then callback called
     */
    @Test
    @SmallTest
    public void testCommands_then() {
        Frame frame = (Frame) mRootContext.getTopComponent();
        int color = frame.getBackgroundColor();
        String hexColor = String.format("#%08X", (color));
        assertEquals("#FF008000", hexColor);
        AtomicBoolean complete = new AtomicBoolean(false);

        update(1000);

        Action action = mRootContext.executeCommands(COMMANDS);
        action.then(() -> {
            complete.set(true);
        });
        assertTrue(action.isPending());
        assertFalse(action.isResolved());
        assertFalse(action.isTerminated());
        color = frame.getBackgroundColor();
        hexColor = String.format("#%08X", (color));
        assertEquals("#FF800080", hexColor);

        update(500);

        assertTrue(action.isPending());
        assertFalse(action.isResolved());
        assertFalse(action.isTerminated());
        color = frame.getBackgroundColor();
        hexColor = String.format("#%08X", (color));
        assertEquals("#FF800080", hexColor);

        //Wait long enough...
        update(1000);

        color = frame.getBackgroundColor();
        hexColor = String.format("#%08X", (color));
        assertEquals("#FFFFA500", hexColor);
        assertTrue(complete.get());
        assertFalse(action.isPending());
        assertTrue(action.isResolved());
        assertFalse(action.isTerminated());
    }

    /**
     * Tests that a command was executed with the then callback called
     */
    @Test
    @SmallTest
    public void testCommands_terminate() {
        AtomicBoolean terminated = new AtomicBoolean(false);
        AtomicBoolean completed = new AtomicBoolean(false);

        update(1000);
        Action action = mRootContext.executeCommands(COMMANDS);
        action.then(() -> {
            completed.set(true);
        });
        action.addTerminateCallback(() -> {
            terminated.set(true);
        });
        assertTrue(action.isPending());
        assertFalse(action.isResolved());
        assertFalse(action.isTerminated());

        update(500);

        assertTrue(action.isPending());
        assertFalse(action.isResolved());
        assertFalse(action.isTerminated());
        mRootContext.cancelExecution();
        assertTrue(action.isTerminated());
        assertFalse(action.isResolved());
        assertFalse(completed.get());
        assertTrue(terminated.get());
    }

    private long getHandle() {
        Action action = mRootContext.executeCommands(COMMANDS);
        mRootContext.cancelExecution();
        return action.getNativeHandle();
    }

    @Test
    @SmallTest
    public void testMemory_binding() {
        long handle = getHandle();

        assertNativeHandle(handle);
    }

}
