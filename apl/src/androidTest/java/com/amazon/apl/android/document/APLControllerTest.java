/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.document;

import static com.amazon.apl.android.APLController.LIBRARY_INITIALIZATION_TIME;

import androidx.annotation.NonNull;
import androidx.test.annotation.UiThreadTest;

import com.amazon.apl.android.APLController;
import com.amazon.apl.android.APLLayout;
import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.Frame;
import com.amazon.apl.android.IAPLController;
import com.amazon.apl.android.IDocumentLifecycleListener;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.configuration.ConfigurationChange;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.views.APLAbsoluteLayout;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// This test requires a view to be displayed thus must be done via androidTest.
public class APLControllerTest extends AbstractDocViewTest {

    @Mock
    private ITelemetryProvider mockTelemetry;

    private static final String SIMPLE_DOC = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.0\"," +
            "  \"mainTemplate\": {" +
            "    \"item\":" +
            "    {" +
            "      \"type\": \"Frame\"" +
            "    }" +
            "  }" +
            "}";

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(mockTelemetry.createMetricId(ITelemetryProvider.APL_DOMAIN,
                LIBRARY_INITIALIZATION_TIME, ITelemetryProvider.Type.TIMER)).thenReturn(1);
    }

    @Test
    public void testAPLController_renderDefaults() throws InterruptedException {
        final CountDownLatch renderLatch = new CountDownLatch(1);
        final CountDownLatch displayedLatch = new CountDownLatch(1);

        activityRule.getScenario().onActivity(activity -> {
            APLLayout aplLayout = activity.findViewById(com.amazon.apl.android.test.R.id.apl);
            aplLayout.getPresenter().addDocumentLifecycleListener(new IDocumentLifecycleListener() {
                @Override
                public void onDocumentRender(@NonNull RootContext rootContext) {
                    assertTrue(rootContext.getTopComponent() instanceof Frame);
                    renderLatch.countDown();
                }

                @Override
                public void onDocumentDisplayed() {
                    assertTrue(aplLayout.getChildAt(0) instanceof APLAbsoluteLayout);
                    displayedLatch.countDown();
                }
            });

            IAPLController aplController = new APLController.Builder()
                    .aplDocument(SIMPLE_DOC)
                    .aplOptions(APLOptions.builder().telemetryProvider(mockTelemetry).build())
                    .aplLayout(aplLayout)
                    .rootConfig(RootConfig.create())
                    .render();

            assertNotNull(aplController);
            verify(mockTelemetry).createMetricId(ITelemetryProvider.APL_DOMAIN, LIBRARY_INITIALIZATION_TIME, ITelemetryProvider.Type.TIMER);
            ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
            verify(mockTelemetry).reportTimer(eq(1), eq(TimeUnit.MILLISECONDS), captor.capture());
            assertTrue(captor.getValue() > 0);
        });

        assertTrue(renderLatch.await(1, TimeUnit.SECONDS));
        assertTrue(displayedLatch.await(5, TimeUnit.SECONDS));
    }

    @UiThreadTest
    @Test
    public void testAPLController_renderWithAutoSize() throws InterruptedException {
        final CountDownLatch renderLatch = new CountDownLatch(1);

        activityRule.getScenario().onActivity(activity -> {
            APLLayout aplLayout = activity.findViewById(com.amazon.apl.android.test.R.id.apl);
            aplLayout.setMinMaxHeight(100, 3000);
            aplLayout.setMinMaxWidth(50, 3000);
            aplLayout.getPresenter().addDocumentLifecycleListener(new IDocumentLifecycleListener() {
                @Override
                public void onDocumentRender(@NonNull RootContext rootContext) {
                    assertTrue(rootContext.getTopComponent() instanceof Frame);
                    assertTrue(rootContext.isAutoSizeLayoutPending());
                    assertEquals(aplLayout.getLayoutParams().width, rootContext.getAutoSizedWidth());
                    assertEquals(aplLayout.getLayoutParams().height, rootContext.getAutoSizedHeight());
                    renderLatch.countDown();
                }
            });

            IAPLController aplController = new APLController.Builder()
                    .aplDocument(SIMPLE_DOC)
                    .aplOptions(APLOptions.builder().telemetryProvider(mockTelemetry).build())
                    .aplLayout(aplLayout)
                    .rootConfig(RootConfig.create())
                    .render();

            assertNotNull(aplController);
            verify(mockTelemetry).createMetricId(ITelemetryProvider.APL_DOMAIN, LIBRARY_INITIALIZATION_TIME, ITelemetryProvider.Type.TIMER);
            ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
            verify(mockTelemetry).reportTimer(eq(1), eq(TimeUnit.MILLISECONDS), captor.capture());
            assertTrue(captor.getValue() > 0);
        });

        assertTrue(renderLatch.await(5, TimeUnit.SECONDS));
    }

    @UiThreadTest
    @Test
    public void testAPLController_renderWithAutoSizeConfigurationChange() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        activityRule.getScenario().onActivity(activity -> {
            APLLayout aplLayout = activity.findViewById(com.amazon.apl.android.test.R.id.apl);
            aplLayout.getPresenter().addDocumentLifecycleListener(new IDocumentLifecycleListener() {
                @Override
                public void onDocumentRender(@NonNull RootContext rootContext) {
                    assertTrue(rootContext.getTopComponent() instanceof Frame);
                    assertFalse(rootContext.isAutoSizeLayoutPending());

                    ConfigurationChange configurationChange = aplLayout.createConfigurationChange().
                            minWidth(300).maxWidth(2000).minHeight(300).maxHeight(4000).
                            build();
                    try {
                        aplLayout.handleConfigurationChange(configurationChange);
                        //layoutmanager.layout() is trigered after pending components are cleared, hence we call on tick
                        rootContext.onTick(400);
                        assertTrue(rootContext.isAutoSizeLayoutPending());
                        assertEquals(aplLayout.getLayoutParams().width, rootContext.getAutoSizedWidth());
                        assertEquals(aplLayout.getLayoutParams().height, rootContext.getAutoSizedHeight());
                        latch.countDown();
                    } catch (APLController.APLException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            IAPLController aplController = new APLController.Builder()
                    .aplDocument(SIMPLE_DOC)
                    .aplOptions(APLOptions.builder().telemetryProvider(mockTelemetry).build())
                    .aplLayout(aplLayout)
                    .rootConfig(RootConfig.create())
                    .render();

            assertNotNull(aplController);
            verify(mockTelemetry).createMetricId(ITelemetryProvider.APL_DOMAIN, LIBRARY_INITIALIZATION_TIME, ITelemetryProvider.Type.TIMER);
            ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
            verify(mockTelemetry).reportTimer(eq(1), eq(TimeUnit.MILLISECONDS), captor.capture());
            assertTrue(captor.getValue() > 0);
        });
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }
}
