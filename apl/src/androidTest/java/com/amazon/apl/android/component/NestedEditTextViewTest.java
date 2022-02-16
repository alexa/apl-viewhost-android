/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.graphics.Color;
import android.view.inputmethod.InputMethodManager;

import androidx.test.espresso.Espresso;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.dependencies.ISendEventCallbackV2;
import com.amazon.apl.android.document.AbstractDocViewTest;

import org.junit.AfterClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.amazon.apl.android.espresso.APLViewAssertions.hasBackgroundColor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static com.amazon.apl.android.utils.KeyboardHelper.isKeyboardOpen;
import static com.amazon.apl.android.utils.KeyboardHelper.getInputMethodManager;

public class NestedEditTextViewTest extends AbstractDocViewTest {
    private static final String INITIAL_FRAME_COLOR = "blue";
    private static final String FINAL_FRAME_COLOR = "white";
    private ISendEventCallbackV2 mSendEventCallback = mock(ISendEventCallbackV2.class);
    private static final String DOC =
            "      \"type\": \"Frame\",\n" +
            "      \"width\": \"100vw\",\n" +
            "      \"height\": \"100vh\",\n" +
            "      \"backgroundColor\": \"blue\",\n" +
            "      \"items\": {\n" +
            "        \"type\": \"Container\",\n" +
            "        \"width\": \"100vw\",\n" +
            "        \"height\": \"100vh\",\n" +
            "        \"data\": [ 1, 2],\n" +
            "        \"items\": {\n" +
            "          \"type\": \"TouchWrapper\",\n" +
            "          \"id\": \"touchwrapper\"," +
            "          \"width\": \"100vw\",\n" +
            "          \"height\": \"100dp\",\n" +
            "          \"onPress\": {\n" +
            "            \"type\": \"SendEvent\",\n" +
            "            \"arguments\": [\n" +
            "              \"pressed\"\n" +
            "            ]\n" +
            "          },\n" +
            "          \"onDown\": {\n" +
            "            \"type\": \"SetValue\",\n" +
            "            \"componentId\": \"testcomp\",\n" +
            "            \"property\": \"backgroundColor\",\n" +
            "            \"value\": \"yellow\"\n" +
            "          },\n" +
            "          \"onMove\": {\n" +
            "            \"type\": \"SetValue\",\n" +
            "            \"componentId\": \"testcomp\",\n" +
            "            \"property\": \"backgroundColor\",\n" +
            "            \"value\": \"red\"\n" +
            "          },\n" +
            "          \"onUp\": {\n" +
            "            \"type\": \"SetValue\",\n" +
            "            \"componentId\": \"testcomp\",\n" +
            "            \"property\": \"backgroundColor\",\n" +
            "            \"value\": \"white\"\n" +
            "          },\n" +
            "          \"onCancel\": {\n" +
            "            \"type\": \"SetValue\",\n" +
            "            \"componentId\": \"testcomp\",\n" +
            "            \"property\": \"backgroundColor\",\n" +
            "            \"value\": \"green\"\n" +
            "          },\n" +
            "          \"items\": [\n" +
            "            {\n" +
            "              \"type\": \"EditText\",\n" +
            "              \"width\":  \"200dp\",\n" +
            "              \"height\": \"100dp\",\n" +
            "              \"id\": \"editText-${data}\",\n" +
            "              \"fontColor\": \"black\",\n" +
            "              \"text\": \"${data}\",\n" +
            "              \"hintColor\": \"gray\",\n" +
            "              \"submitKeyType\": \"next\"" +
            "            }\n" +
            "          ]\n" +
            "        }\n" +
            "      }\n" +
            "    }";

    @AfterClass
    public static void closeKeyboard() {
        // Try to close the keyboard if open.
        try {
            Espresso.closeSoftKeyboard();
        } catch (RuntimeException ex) {
            // Do nothing.
        }
    }

    /**
     * Test the keyboard opens when EditText inside a TouchWrapper is clicked.
     */
    @Test
    public void testView_click_on_EditText_opens_keyboard() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflateWithOptions(DOC, "", getOptions()))
                .check(hasRootContext());
        onView(withComponent(mTestContext.getRootContext().findComponentById("editText-1")))
                .perform(click());

        assertTrue(isKeyboardOpen());
        ArgumentCaptor<Object[]> argumentCaptor = ArgumentCaptor.forClass(Object[].class);
        ArgumentCaptor<Map<String, Object>> sourcesArgumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mSendEventCallback).onSendEvent(argumentCaptor.capture(), any(), sourcesArgumentCaptor.capture(), any());
        verifyEventArguments(argumentCaptor.getValue(), sourcesArgumentCaptor.getValue());
    }

    /**
     * Tests the keyboard does not open when TouchWrapper containing EditText is clicked.
     */
    @Test
    public void testView_click_on_TouchWrapper_does_not_open_keyboard() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflateWithOptions(DOC, "", getOptions()))
                .check(hasRootContext());
        // Verify initial state
        onView(withComponent(mTestContext.getRootContext().findComponentById("testcomp")))
                .check(hasBackgroundColor(Color.parseColor(INITIAL_FRAME_COLOR)));

        onView(withComponent(mTestContext.getRootContext().findComponentById("touchwrapper")))
                .perform(click());

        // Verify final state
        onView(withComponent(mTestContext.getRootContext().findComponentById("testcomp")))
                .check(hasBackgroundColor(Color.parseColor(FINAL_FRAME_COLOR)));
        assertFalse(isKeyboardOpen());
        verify(mSendEventCallback).onSendEvent(any(), any(), any(), any());
    }
    
    @Test
    public void test_nextSubmitKey_focusesNextEditTextView() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflateWithOptions(DOC, "", getOptions()))
                .check(hasRootContext());
        onView(withComponent(mTestContext.getRootContext().findComponentById("editText-1")))
                .perform(click());
        verify(mSendEventCallback).onSendEvent(any(), any(), any(), any());

        InputMethodManager inputMethodManager = getInputMethodManager();

        assertTrue(isKeyboardOpen());
        onView(withComponent(mTestContext.getRootContext().findComponentById("editText-1")))
                .check((view, noViewFoundException) -> inputMethodManager.isActive(view))
                .perform(pressImeActionButton());

        onView(withComponent(mTestContext.getRootContext().findComponentById("editText-2")))
                .check((view, noViewFoundException) -> inputMethodManager.isActive(view));
    }

    private APLOptions getOptions() {
        return APLOptions.builder().sendEventCallbackV2(mSendEventCallback).build();
    }

    private void verifyEventArguments(Object[] args, Map<String, Object> sources) {
        assertEquals(1, args.length);
        assertEquals("pressed", args[0].toString());
        assertEquals("TouchWrapper", sources.get("type"));
    }

}
