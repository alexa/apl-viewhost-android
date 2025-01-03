/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.viewhost.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import android.os.Handler;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amazon.apl.android.Action;
import com.amazon.apl.android.Content;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.metrics.MetricsOptions;
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


@RunWith(AndroidJUnit4.class)
public class DocumentHandleTest extends ViewhostRobolectricTest {


    @Mock
    private Content mContent;

    @Mock
    private ViewhostImpl mViewhost;

    @Mock
    private Handler mHandler;

    @Mock
    private MetricsOptions mMetricsOptions;

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
        mDocumentHandle = new DocumentHandleImpl(mViewhost, mHandler, mMetricsOptions);
        ((DocumentHandleImpl)mDocumentHandle).setContent(mContent);
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
    public void testCancelExecution() {
        ((DocumentHandleImpl)mDocumentHandle).setPrimary(mock(RootContext.class), null);
        mDocumentHandle.cancelExecution();
        verify(((DocumentHandleImpl)mDocumentHandle).getRootContext()).cancelExecution();
    }

}
