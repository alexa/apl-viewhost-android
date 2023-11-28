/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import static com.amazon.apl.android.providers.ITelemetryProvider.APL_DOMAIN;
import static com.amazon.apl.android.providers.ITelemetryProvider.Type.TIMER;
import static com.amazon.apl.enums.ComponentType.kComponentTypeText;

import com.amazon.apl.android.dependencies.IVisualContextListener;
import com.amazon.apl.android.document.AbstractDocUnitTest;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.scaling.ViewportMetrics;
import com.amazon.apl.enums.ScreenShape;
import com.amazon.apl.enums.ViewportMode;

import org.json.JSONObject;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Calendar;
import java.util.Date;

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
    public void test_timersInitializedDuringRestore() {
        ITelemetryProvider telemetryMock = Mockito.mock(ITelemetryProvider.class);
        APLOptions aplOptions = APLOptions.builder().telemetryProvider(telemetryMock).build();
        ViewportMetrics metrics = ViewportMetrics.builder()
                .width(1280)
                .height(720)
                .dpi(160)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeMobile)
                .build();

        loadDocument(DOC, aplOptions, metrics);

        DocumentState documentState = new DocumentState(mRootContext, mContent);
        reset(telemetryMock);

        RootContext.createFromCachedDocumentState(documentState, mAPLPresenter);
        verify(telemetryMock, times(1)).createMetricId(APL_DOMAIN, RootContext.METRIC_REINFLATE, TIMER);
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

    private final String TIME_DOC = "{\n" +
            "    \"type\": \"APL\",\n" +
            "    \"version\": \"1.0\",\n" +
            "    \"mainTemplate\": {\n" +
            "        \"item\": {\n" +
            "            \"type\": \"Text\",\n" +
            "            \"id\": \"text\",\n" +
            "            \"text\": \"%s\"\n" +
            "        }\n" +
            "    }\n" +
            "}";

    @Test
    public void test_updateTime_localTime() {
        Date before = new Date();
        loadDocument(String.format(TIME_DOC, "${localTime}"));
        Date after = new Date();
        // Local time should be the current time in the local timezone
        Component component = mRootContext.findComponentById("text");
        assertEquals(kComponentTypeText, component.getComponentType());
        Calendar now = Calendar.getInstance();
        long offset = now.get(Calendar.ZONE_OFFSET) + now.get(Calendar.DST_OFFSET);
        assertTrue(before.getTime() <= Long.parseLong(((Text) component).getText()) - offset);
        assertTrue(Long.parseLong(((Text) component).getText()) - offset <= after.getTime());
    }

    @Test
    public void test_updateTime_utcTime() {
        // UTC time should be the utc time
        Date before = new Date();
        loadDocument(String.format(TIME_DOC, "${utcTime}"));
        Date after = new Date();
        Component component = mRootContext.findComponentById("text");
        assertEquals(kComponentTypeText, component.getComponentType());
        assertTrue(before.getTime() <= Long.parseLong(((Text) component).getText()));
        assertTrue(Long.parseLong(((Text) component).getText()) <= after.getTime());
    }

    @Test
    public void test_updateTime_localTime_elapsedTime() {
        // Elapsed time should be 0 initially
        loadDocument(String.format(TIME_DOC, "${elapsedTime}"));
        Component component = mRootContext.findComponentById("text");
        assertEquals(kComponentTypeText, component.getComponentType());
        assertEquals("0", ((Text)component).getText());
        update(500);
        // Elapsed time should be updated to 500 now
        component = mRootContext.findComponentById("text");
        assertEquals(kComponentTypeText, component.getComponentType());
        assertEquals("500", ((Text)component).getText());
    }
}
