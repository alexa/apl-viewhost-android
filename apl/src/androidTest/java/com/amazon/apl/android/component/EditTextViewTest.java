/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import androidx.test.espresso.Espresso;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.dependencies.ISendEventCallbackV2;
import com.amazon.apl.android.document.AbstractDocViewTest;
import com.amazon.apl.enums.RootProperty;

import org.junit.AfterClass;
import org.junit.Test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.amazon.apl.android.espresso.APLViewActions.finish;
import static com.amazon.apl.android.espresso.APLViewAssertions.isFinished;
import static com.amazon.apl.android.utils.KeyboardHelper.isKeyboardOpen;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class EditTextViewTest extends AbstractDocViewTest {
    private static final String DOC =
            "      \"type\": \"Frame\",\n" +
                    "      \"width\": \"100vw\",\n" +
                    "      \"height\": \"100vh\",\n" +
                    "      \"backgroundColor\": \"black\",\n" +
                    "      \"items\": {\n" +
                    "        \"type\": \"Container\",\n" +
                    "        \"width\": \"100vw\",\n" +
                    "        \"height\": \"100vh\",\n" +
                    "        \"items\": [\n" +
                    "          {\n" +
                    "            \"type\": \"EditText\",\n" +
                    "            \"id\": \"myEditText\",\n" +
                    "            \"text\": \"My favourite edit text box\"\n" +
                    "          },\n" +
                    "          {\n" +
                    "            \"type\": \"Text\",\n" +
                    "            \"id\": \"myPlainText\",\n" +
                    "            \"text\": \"Some other text that is plain\"\n" +
                    "          }\n" +
                    "        ]\n" +
                    "      }\n" +
                    "    }";
    private final ISendEventCallbackV2 mSendEventCallback = mock(ISendEventCallbackV2.class);

    @AfterClass
    public static void closeKeyboard() {
        // Try to close the keyboard if open.
        try {
            Espresso.closeSoftKeyboard();
        } catch (RuntimeException ex) {
            // Do nothing.
        }
    }

    @Test
    public void testView_click_on_EditText_opens_keyboard() {
        RootConfig rootConfig = RootConfig.create("EditText Test", "1.0");

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(DOC, "", "", "{}", APLOptions.builder().build(), rootConfig))
                .check(hasRootContext());
        onView(withComponent(mTestContext.getRootContext().findComponentById("myEditText")))
                .perform(click());

        assertTrue(isKeyboardOpen());
    }
}
