/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.viewhost.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import android.os.Handler;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amazon.apl.android.Action;
import com.amazon.apl.android.Content;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.viewhost.DocumentHandle;
import com.amazon.apl.viewhost.primitives.JsonStringDecodable;
import com.amazon.apl.viewhost.request.ExecuteCommandsRequest;
import com.amazon.apl.viewhost.request.ExecuteCommandsRequest.ExecuteCommandsCallback;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;


@RunWith(AndroidJUnit4.class)
public class DocumentHandleTest extends ViewhostRobolectricTest {

    @Mock
    private ViewhostImpl mViewhost;

    @Mock
    private Handler mHandler;

    @Mock
    private DocumentContext mDocumentContext;

    @Mock
    private Action mAction;

    private DocumentHandle mDocumentHandle;

    private ExecuteCommandsRequest executeCommandsRequest;

    private ExecuteCommandsCallback executeCommandsCallback;

    private static final String COMMANDS = "commands";

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        executeCommandsCallback = new ExecuteCommandsCallback() {
            @Override
            public void onComplete() {

            }

            @Override
            public void onTerminated() {

            }
        };
        executeCommandsRequest = ExecuteCommandsRequest.builder()
                .commands(new JsonStringDecodable(COMMANDS))
                .callback(executeCommandsCallback)
                .build();
        mDocumentHandle = new DocumentHandleImpl(mViewhost, mHandler);
    }

    @Test
    public void testExecuteCommandSuccess() {
        DocumentHandleImpl impl = (DocumentHandleImpl) mDocumentHandle;
        impl.setDocumentContext(mDocumentContext);
        when(mDocumentContext.executeCommands(COMMANDS)).thenReturn(mAction);
        when(mHandler.post(any(Runnable.class))).thenAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        });
        boolean result = mDocumentHandle.executeCommands(executeCommandsRequest);
        assertTrue(result);
    }

    @Test
    public void testExecuteCommandDocumentContextNullReturnsFalse() {
        boolean result = mDocumentHandle.executeCommands(executeCommandsRequest);
        assertFalse(result);
    }

    @Test
    public void testUserDataHolder() {
        String userData = "UserData";
        assertTrue(mDocumentHandle.setUserData(userData));
        assertTrue(mDocumentHandle.getUserData() instanceof String);
        assertEquals("UserData", mDocumentHandle.getUserData());

        ((DocumentHandleImpl)mDocumentHandle).setDocumentState(DocumentState.ERROR);
        assertFalse(mDocumentHandle.setUserData(userData));
    }

    @Test
    public void testGetContentSetting_nullContent_returnsDefaultValue() {
        DocumentHandleImpl handle = (DocumentHandleImpl) mDocumentHandle;
        handle.setContent(null);

        String ret = handle.getDocumentSetting("my-property", "fallback");
        assertEquals("fallback", ret);
    }

    private static String DOC_SETTINGS = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.0\"," +
            "  \"mainTemplate\": {" +
            "    \"item\": {" +
            "      \"type\": \"Frame\"," +
            "      \"backgroundColor\": \"orange\"" +
            "    }" +
            "  }," +
            "  \"settings\": {" +
            "    \"propertyA\": true," +
            "    \"propertyB\": 60000," +
            "    \"propertyC\": \"abc\"," +
            "    \"subSetting\": {" +
            "      \"propertyD\": 12.34" +
            "    }" +
            "  }" +
            "}";
    @Test
    public void testGetContentSetting_content_returnsExpectedValue() {
        DocumentHandleImpl handle = (DocumentHandleImpl) mDocumentHandle;
        try {
            Content mContent = Content.create(DOC_SETTINGS);
            handle.setContent(mContent);
        } catch (Content.ContentException e) {
            fail(e.getMessage());
        }

        // Properties existing in Content return expected values
        boolean propertyA = handle.getDocumentSetting("propertyA", false);
        int propertyB = handle.getDocumentSetting("propertyB", 3000);
        String propertyC = handle.getDocumentSetting("propertyC", "def");
        Map<String, Double> subSetting = handle.getDocumentSetting("subSetting", new HashMap<>());
        double propertyD = subSetting.get("propertyD");

        assertEquals(true, propertyA);
        assertEquals(60000, propertyB);
        assertEquals( "abc", propertyC);
        assertEquals(12.34, propertyD, 0.001);

        // Properties not existing return default value
        String propertyF = handle.getDocumentSetting("propertyF", "fallback");

        assertEquals("fallback", propertyF);
    }
}
