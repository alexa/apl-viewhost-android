/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.os.Looper;

import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.enums.DisplayState;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

public class APLControllerTest extends ViewhostRobolectricTest {

    @Mock
    private RootContext mRootContext;
    @Mock
    private Content mContent;
    @Mock
    private Action mAction;
    @Mock
    private APLLayout mAplLayout;
    @Mock
    private APLOptions mOptions;
    @Mock
    private IAPLViewPresenter mViewPresenter;
    @Mock
    private RootConfig mRootConfig;

    private APLController mController;

    @Before
    public void setup() {
        mController = new APLController(mRootContext, mContent);
        when(mRootContext.executeCommands(any())).thenReturn(mAction);
        when(mRootContext.getOptions()).thenReturn(mOptions);
        when(mAplLayout.getPresenter()).thenReturn(mViewPresenter);
    }

    @Test
    public void testRunnablesQueuedAreInvokedOnRender() {
        mController = new APLController(null, mContent);

        mController.executeCommands("commands", null);
        mController.updateDataSource("type", "data", null);
        mController.invokeExtensionEventHandler("uri", "name", new HashMap<>(), false, null);
        mController.cancelExecution();

        mController.onDocumentRender(mRootContext);
        mController.onDocumentDisplayed();
        InOrder inOrder = inOrder(mRootContext);
        inOrder.verify(mRootContext).getOptions();
        inOrder.verify(mRootContext).executeCommands(eq("commands"));
        inOrder.verify(mRootContext).updateDataSource(eq("type"), eq("data"));
        inOrder.verify(mRootContext).invokeExtensionEventHandler(eq("uri"), eq("name"), any(), eq(false));
        inOrder.verify(mRootContext).cancelExecution();
        verifyNoMoreInteractions(mRootContext);

        mController.finishDocument();
        verify(mRootContext).finishDocument();
    }

    @Test
    public void testRunnablesNotInvokedAfterFinish() {
        mController = new APLController(mRootContext, mContent);
        mController.onDocumentFinish();

        mController.executeCommands("commands", null);
        mController.updateDataSource("type", "data", null);
        mController.invokeExtensionEventHandler("uri", "name", new HashMap<>(), false, null);
        mController.cancelExecution();
        mController.finishDocument();

        verifyZeroInteractions(mRootContext);
    }

    @Test
    public void testMultipleCommandsInvokedInOrder() {
        mController = new APLController(null, mContent);

        mController.executeCommands("a", null);
        mController.executeCommands("b", null);

        mController.onDocumentRender(mRootContext);
        mController.onDocumentDisplayed();

        InOrder inOrder = inOrder(mRootContext);
        inOrder.verify(mRootContext).executeCommands(eq("a"));
        inOrder.verify(mRootContext).executeCommands(eq("b"));
    }

    @Test
    public void testInvokedOnOtherThread() throws InterruptedException {
        mController = new APLController(mRootContext, mContent);

        CountDownLatch inner = new CountDownLatch(1);
        CountDownLatch outer = new CountDownLatch(1);
        Thread thread = new Thread(() -> {
            mController.executeCommands("commands", action -> {
                assertEquals(mAction, action);
                inner.countDown();
            });
            outer.countDown();
        });
        thread.start();
        verifyZeroInteractions(mRootContext);

        mController.onDocumentDisplayed();
        outer.await();
        shadowOf(Looper.getMainLooper()).idle();
        verify(mRootContext).executeCommands(eq("commands"));
        inner.await();
    }

    @Test
    public void testActionCallbackInvoked() throws InterruptedException {
        mController.onDocumentDisplayed();
        CountDownLatch latch = new CountDownLatch(1);
        mController.executeCommands("commands", action -> {
            assertEquals(mAction, action);
            latch.countDown();
        });
        latch.await();
    }

    @Test
    public void testLifecycleEvents() {
        // Verify first resumeDocument() does not do anything
        mController.resumeDocument();
        verify(mRootContext, never()).resumeDocument();
        // Document is displayed. After this all RootContext runnables should execute.
        mController.onDocumentDisplayed();
        mController.resumeDocument();
        verify(mRootContext, times(2)).resumeDocument();

        mController.pauseDocument();
        verify(mRootContext).pauseDocument();

        mController.finishDocument();
        verify(mRootContext).finishDocument();
    }

    @Test
    public void testLifecycleEventsIgnoredAfterFinish() {
        mController.finishDocument();
        verify(mRootContext).finishDocument();

        mController.resumeDocument();


        mController.pauseDocument();

        verifyNoMoreInteractions(mRootContext);
    }

    @Test
    public void testDisplayStateChanges() {
        mController.onDocumentDisplayed();
        mController.updateDisplayState(DisplayState.kDisplayStateBackground);
        verify(mRootContext).updateDisplayState(eq(DisplayState.kDisplayStateBackground));

        mController.updateDisplayState(DisplayState.kDisplayStateForeground);
        verify(mRootContext).updateDisplayState(eq(DisplayState.kDisplayStateForeground));
    }

    @Test
    public void testDisplayStateChangesIgnoredAfterFinish() {
        mController.finishDocument();
        verify(mRootContext).finishDocument();

        mController.updateDisplayState(DisplayState.kDisplayStateHidden);

        verifyNoMoreInteractions(mRootContext);
    }

    @Test
    public void testExposesUnderlyingDocVersion() {
        when(mContent.getAPLVersion()).thenReturn("1.7");

        assertEquals(APLVersionCodes.APL_1_7, mController.getDocVersion());

        mController.finishDocument();
        verify(mRootContext).finishDocument();

        mController.getDocVersion();

        verifyNoMoreInteractions(mRootContext);
    }

    @Test
    public void testRenderV2_addsDocumentLifecycleListeners() {
        ITelemetryProvider mockTelemetry = mock(ITelemetryProvider.class);
        APLOptions aplOptions = APLOptions.builder()
                .telemetryProvider(mockTelemetry).build();

        APLController.builder()
                .aplLayout(mAplLayout)
                .rootConfig(mRootConfig)
                .aplDocument("{}")
                .aplOptions(aplOptions)
                .contentCreator(((aplDocument, options, callbackV2, session) -> mContent))
                .render();

        verify(mViewPresenter).addDocumentLifecycleListener(mockTelemetry);
    }
}
