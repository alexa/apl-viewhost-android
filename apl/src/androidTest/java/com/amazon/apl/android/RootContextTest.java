/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amazon.apl.android.dependencies.IVisualContextListener;
import com.amazon.apl.android.document.AbstractDocUnitTest;
import com.amazon.apl.android.scaling.ViewportMetrics;
import com.amazon.apl.enums.ScreenShape;
import com.amazon.apl.enums.ViewportMode;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(AndroidJUnit4.class)
public class RootContextTest extends AbstractDocUnitTest {

    private final String DOC = "{\n" +
            "    \"type\": \"APL\",\n" +
            "    \"version\": \"1.0\",\n" +
            "    \"mainTemplate\": {\n" +
            "        \"item\": {\n" +
            "            \"type\": \"Frame\",\n" +
            "            \"id\": \"frame\",\n" +
            "            \"width\": 100,\n" +
            "            \"height\": 100,\n" +
            "            \"backgroundColor\": \"green\"\n" +
            "        }\n" +
            "    }\n" +
            "}";

    @Test
    public void test_InflateDoesNotUpdateContext() {
        IVisualContextListener contextListener = mock(IVisualContextListener.class);

        loadDocument(DOC, APLOptions
                .builder()
                .visualContextListener(contextListener)
                .build());

        verify(contextListener, never()).onVisualContextUpdate(any(JSONObject.class));
    }

    @Test
    public void test_pauseAndResume() {
        ViewportMetrics metrics = ViewportMetrics.builder()
                .width(1280)
                .height(720)
                .dpi(160)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeMobile)
                .build();

        APLOptions aplOptions = APLOptions.builder().build();

        loadDocument(DOC, aplOptions, metrics);

        InOrder inOrder = Mockito.inOrder(mRootContext, mAPLPresenter);

        // Document is running, so it can be paused now
        mRootContext.pauseDocument();
        inOrder.verify(mAPLPresenter).onDocumentPaused();

        // Repeating the pause has no effect
        mRootContext.pauseDocument();
        inOrder.verify(mAPLPresenter, never()).onDocumentPaused();

        // But you can resume it
        mRootContext.resumeDocument();
        inOrder.verify(mAPLPresenter).onDocumentResumed();

        // Repeating the resume has no effect
        mRootContext.resumeDocument();
        inOrder.verify(mAPLPresenter, never()).onDocumentResumed();
    }
}
