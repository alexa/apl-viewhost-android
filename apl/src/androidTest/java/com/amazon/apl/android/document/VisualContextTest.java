/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.document;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.APLTestDocs;
import com.amazon.apl.android.Frame;
import com.amazon.apl.android.dependencies.IVisualContextListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.amazon.apl.android.espresso.APLViewActions.executeCommands;
import static com.amazon.apl.android.espresso.APLViewActions.waitFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class VisualContextTest extends AbstractDocViewTest {

    private IVisualContextListener mVisualContextListener;

    @Before
    public void setup() {
        mVisualContextListener = mock(IVisualContextListener.class);
    }

    @Test
    public void testVisualContext_updatesOnlyIfChanged() throws JSONException {
        APLOptions options = APLOptions.builder()
                .visualContextListener(mVisualContextListener)
                .build();
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                 .perform(inflateWithOptions("\"type\": \"Frame\", \"backgroundColor\": \"blue\"", "", options))
                 .check(hasRootContext());
        Frame frame = (Frame) mTestContext.getRootContext().getTopComponent();
        final int initialColor = frame.getBackgroundColor();

        // Check initial context is not empty
        ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
        verify(mVisualContextListener).onVisualContextUpdate(captor.capture());
        JSONObject jsonObject = captor.getValue();
        assertEquals(frame.getComponentId(), jsonObject.getString("uid"));

        // trigger an update to the component
        onView(isRoot())
                .perform(executeCommands(mTestContext.getRootContext(), new JSONArray()
                        .put(new JSONObject()
                                .put("type", "SetValue")
                                .put("componentId", "testcomp")
                                .put("property", "backgroundColor")
                                .put("value", "red")
                        ).toString()))
                .perform(waitFor(100));
        assertNotEquals(initialColor, frame.getBackgroundColor());

        // verify that visual context wasn't updated
        verifyNoMoreInteractions(mVisualContextListener);
    }
}
