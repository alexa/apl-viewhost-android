/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.event;

import android.view.View;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.configuration.ConfigurationChange;
import com.amazon.apl.android.dependencies.ISendEventCallbackV2;
import com.amazon.apl.android.document.AbstractDocUnitTest;
import com.amazon.apl.android.providers.AbstractMediaPlayerProvider;
import com.amazon.apl.android.scaling.ViewportMetrics;
import com.amazon.apl.enums.ScreenShape;
import com.amazon.apl.enums.ViewportMode;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@RunWith(AndroidJUnit4.class)
public class ReinflateEventTest extends AbstractDocUnitTest {
    @Mock
    private ISendEventCallbackV2 mMockSendEventCallback;
    @Mock
    private AbstractMediaPlayerProvider<View> mMockMediaPlayerProvider;

    private static final String REINFLATE_DOC = "{\n" +
            "    \"type\": \"APL\",\n" +
            "    \"version\": \"1.5\",\n" +
            "    \"mainTemplate\": {\n" +
            "        \"item\": {\n" +
            "            \"type\": \"Frame\",\n" +
            "            \"id\": \"frame\",\n" +
            "            \"width\": 100,\n" +
            "            \"height\": 100,\n" +
            "            \"backgroundColor\": \"green\"\n" +
            "        }\n" +
            "    }\n," +
            "    \"onConfigChange\": {\n" +
            "        \"type\": \"Reinflate\"\n" +
            "    }\n" +
            "}";

    @Before
    public void setUp() {
    }

    @Test
    public void testReinflate_rootContextReinflate_invoked() {
        ViewportMetrics metrics = ViewportMetrics.builder()
                .width(1280)
                .height(720)
                .dpi(160)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeMobile)
                .build();


        APLOptions aplOptions = APLOptions.builder()
                .mediaPlayerProvider(mMockMediaPlayerProvider)
                .build();

        loadDocument(REINFLATE_DOC, aplOptions, metrics);

        // Dummy config
        ConfigurationChange configChange = mRootContext.createConfigurationChange()
                .build();

        mRootContext.handleConfigurationChange(configChange);

        update(100);

        // Verify that the document is paused and document lifecycle onDocumentPaused() is executed.
        InOrder inOrder = Mockito.inOrder(mRootContext, mAPLPresenter, mMockMediaPlayerProvider);
        inOrder.verify(mRootContext).reinflate();
        inOrder.verify(mMockMediaPlayerProvider).releasePlayers();
        inOrder.verify(mRootContext).pauseDocument();
        inOrder.verify(mAPLPresenter).onDocumentPaused();
        inOrder.verify(mAPLPresenter).reinflate();
        // Verify that repeated calls to pauseDocument() and resumeDocument() are ignored until initTime() is called.
        reset(mAPLPresenter);
        verifyPauseAndResumeIgnored();
        // Verify that a doFrame() call does not post the callback again.
        mRootContext.doFrame(1);
        verifyPauseAndResumeIgnored();
        // Verify that after initTime() is called, resumeDocument() and pauseDocument() are processed normally.
        mRootContext.initTime();
        inOrder.verify(mRootContext).resumeDocument();
        inOrder.verify(mAPLPresenter).onDocumentResumed();
        mRootContext.pauseDocument();
        verify(mAPLPresenter).onDocumentPaused();
    }

    private void verifyPauseAndResumeIgnored() {
        mRootContext.pauseDocument();
        verify(mAPLPresenter, never()).onDocumentPaused();
        mRootContext.resumeDocument();
        verify(mAPLPresenter, never()).onDocumentResumed();
    }

    // TODO: Add integration tests corresponding to Scaling cases.
}
