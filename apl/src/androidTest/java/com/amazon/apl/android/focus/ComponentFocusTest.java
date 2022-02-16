/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.focus;

import android.graphics.Color;
import android.view.KeyEvent;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.action.ViewActions;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.Text;
import com.amazon.apl.android.dependencies.ISendEventCallbackV2;
import com.amazon.apl.android.document.AbstractDocViewTest;
import com.amazon.apl.android.espresso.APLMatchers;
import com.amazon.apl.android.espresso.APLViewIdlingResource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.actionWithAssertions;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.amazon.apl.android.espresso.APLViewActions.waitFor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;

/**
 * Tests focus navigation and key handling for Actionable components
 */
public abstract class ComponentFocusTest extends AbstractDocViewTest {
    @Mock
    ISendEventCallbackV2 mSendEventCallback;

    static final String DOC_PROPS =
            "\"styles\": {\n" +
                    "    \"textStylePressable\": {\n" +
                    "      \"values\": [\n" +
                    "        {\n" +
                    "          \"color\": \"black\"\n" +
                    "        },\n" +
                    "        {\n" +
                    "          \"when\": \"${state.focused}\",\n" +
                    "          \"color\": \"red\"\n" +
                    "        }\n" +
                    "      ]\n" +
                    "    }\n" +
                    "  }";

    abstract String componentProps();

    static String[] data = new String[] {
            "First",
            "Second",
            "Third",
            "Fourth",
            "Fifth",
            "Sixth"
    };

    static String dataFor6Items() {
        try {
            JSONArray jsonArray = new JSONArray();
            for (int i = 0; i < data.length; i++) {
                jsonArray.put(new JSONObject()
                        .put("text", data[i]));
            }
            return jsonArray.toString();
        } catch (JSONException e) {
            return "";
        }
    }

    @Before
    public void setupView() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(actionWithAssertions(inflateWithOptions(componentProps(), DOC_PROPS, APLOptions.builder().sendEventCallbackV2(mSendEventCallback).build())))
                .check(hasRootContext());


        mIdlingResource = new APLViewIdlingResource(mTestContext.getTestView());
        IdlingRegistry.getInstance().register(mIdlingResource);
    }

    void pressKey(int keyCode) {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(ViewActions.pressKey(keyCode));
        onView(isRoot()).perform(waitFor(100));
    }

    void checkFocusedComponent(int focusedComponentIndex) {
        Component gridSequence = mTestContext.getRootContext().findComponentById("comp");
        for (int i = 0; i < gridSequence.getChildCount(); i++) {
            Component child = gridSequence.getChildAt(i);
            String textString = ((Text) child.getChildAt(0)).getText();
            if (i == focusedComponentIndex) {
                onView(APLMatchers.withText(textString))
                        .check(matches(APLMatchers.withTextColor(Color.RED)))
                        .check(matches(isDisplayed()));
            } else {
                onView(APLMatchers.withText(textString))
                        .check(matches(APLMatchers.withTextColor(Color.BLACK)))
                        .check(matches(isDisplayed()));
            }
        }

        if (focusedComponentIndex != -1)
            testKeyEvents(focusedComponentIndex);
    }

    private void testKeyEvents(int focusedComponentIndex) {
        pressKey(KeyEvent.KEYCODE_W);
        verify(mSendEventCallback).onSendEvent(eq(new String[]{data[focusedComponentIndex]}), any(), any(), any());
        clearInvocations(mSendEventCallback);

        pressKey(KeyEvent.KEYCODE_S);
        verify(mSendEventCallback).onSendEvent(eq(new String[] {"parent"}), any(), any(), any());
        clearInvocations(mSendEventCallback);
    }
}
