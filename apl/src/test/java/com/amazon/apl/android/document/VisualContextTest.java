/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.document;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.Frame;
import com.amazon.apl.android.dependencies.IVisualContextListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

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
        APLOptions.Builder optionsBuilder = APLOptions.builder()
                .visualContextListener(mVisualContextListener);
        inflateWithOptions("\"type\": \"Frame\", \"backgroundColor\": \"blue\"", "", optionsBuilder);

        Frame frame = (Frame) mTestContext.getRootContext().getTopComponent();
        final int initialColor = frame.getBackgroundColor();

        // Check initial context is not empty
        ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
        verify(mVisualContextListener).onVisualContextUpdate(captor.capture());
        JSONObject jsonObject = captor.getValue();
        assertEquals(frame.getComponentId(), jsonObject.getString("uid"));

        // trigger an update to the component
        executeCommands(new JSONArray()
                        .put(new JSONObject()
                                .put("type", "SetValue")
                                .put("componentId", "testcomp")
                                .put("property", "backgroundColor")
                                .put("value", "red")
                        ).toString());

        assertNotEquals(initialColor, frame.getBackgroundColor());

        // verify that visual context wasn't updated
        verifyNoMoreInteractions(mVisualContextListener);
    }
}
