/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.focus;

import static org.junit.Assert.assertEquals;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.graphics.Color;
import android.text.StaticLayout;
import android.view.KeyEvent;

import com.amazon.apl.android.APLLayout;
import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.dependencies.ISendEventCallbackV2;
import com.amazon.apl.android.document.AbstractDocViewTest;
import com.amazon.apl.android.views.APLTextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.mockito.Mock;

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
        inflateWithOptions(componentProps(), DOC_PROPS, APLOptions.builder().sendEventCallbackV2(mSendEventCallback));
    }

    void pressKey(int keyCode) {
        activityRule.getScenario().onActivity(activity -> {
            APLLayout aplLayout = activity.findViewById(com.amazon.apl.android.test.R.id.apl);

            aplLayout.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
            aplLayout.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
        });

        testClock.doFrameUpdate(100);
    }

    void checkFocusedComponent(int focusedComponentIndex) {
        Component gridSequence = mTestContext.getRootContext().findComponentById("comp");
        activityRule.getScenario().onActivity(activity -> {
            for (int i = 0; i < gridSequence.getChildCount(); i++) {
                Component child = gridSequence.getChildAt(i);
                APLTextView view = activity.findViewById(child.getChildAt(0).getComponentId().hashCode());
                StaticLayout layout = (StaticLayout) view.getLayout();
                if (i == focusedComponentIndex) {
                    assertEquals(Color.RED, layout.getPaint().getColor());
                } else {
                    assertEquals(Color.BLACK, layout.getPaint().getColor());
                }
            }
        });

        if (focusedComponentIndex != -1)
            testKeyEvents(focusedComponentIndex);
    }

    private void testKeyEvents(int focusedComponentIndex) {
        pressKey(KeyEvent.KEYCODE_W);
        verify(mSendEventCallback, timeout(1000)).onSendEvent(eq(new String[]{data[focusedComponentIndex]}), any(), any(), any());
        clearInvocations(mSendEventCallback);

        pressKey(KeyEvent.KEYCODE_S);
        verify(mSendEventCallback, timeout(1000)).onSendEvent(eq(new String[] {"parent"}), any(), any(), any());
        clearInvocations(mSendEventCallback);
    }
}
