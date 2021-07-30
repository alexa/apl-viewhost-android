/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.content.Context;
import android.graphics.Color;
import android.view.inputmethod.InputMethodManager;

import androidx.test.espresso.Espresso;
import androidx.test.platform.app.InstrumentationRegistry;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.dependencies.ISendEventCallback;
import com.amazon.apl.android.document.AbstractDocViewTest;

import org.junit.AfterClass;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.amazon.apl.android.espresso.APLViewAssertions.hasBackgroundColor;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class NestedEditTextViewTest extends AbstractDocViewTest {
    private static final String INITIAL_FRAME_COLOR = "blue";
    private static final String FINAL_FRAME_COLOR = "white";
    private ISendEventCallback mSendEventCallback = mock(ISendEventCallback.class);
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
        Espresso.closeSoftKeyboard();
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
        assertFalse(isKeyboardOpened());
        verify(mSendEventCallback).onSendEvent(any(), any(), any());
    }
    
    @Test
    public void test_nextSubmitKey_focusesNextEditTextView() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflateWithOptions(DOC, "", getOptions()))
                .check(hasRootContext());
        onView(withComponent(mTestContext.getRootContext().findComponentById("editText-1")))
                .perform(click());

        InputMethodManager inputMethodManager = getInputMethodManager();

        assertTrue(isKeyboardOpened());
        onView(withComponent(mTestContext.getRootContext().findComponentById("editText-1")))
                .check((view, noViewFoundException) -> inputMethodManager.isActive(view))
                .perform(pressImeActionButton());

        onView(withComponent(mTestContext.getRootContext().findComponentById("editText-2")))
                .check((view, noViewFoundException) -> inputMethodManager.isActive(view));
    }

    /**
     * Reference: https://stackoverflow.com/questions/33970956/test-if-soft-keyboard-is-visible-using-espresso
     * @return true if soft keyboard is open, else false.
     */
    private boolean isKeyboardOpened() {
        return getInputMethodManager().isAcceptingText();
    }

    private APLOptions getOptions() {
        return APLOptions.builder().sendEventCallback(mSendEventCallback).build();
    }

    private InputMethodManager getInputMethodManager() {
        return (InputMethodManager)InstrumentationRegistry.getInstrumentation().getTargetContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    }

}
