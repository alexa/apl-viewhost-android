/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.document;

import androidx.annotation.NonNull;

import com.amazon.apl.android.APLController;
import com.amazon.apl.android.APLLayout;
import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.Frame;
import com.amazon.apl.android.IAPLController;
import com.amazon.apl.android.IDocumentLifecycleListener;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.views.APLAbsoluteLayout;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

// This test requires a view to be displayed thus must be done via androidTest.
public class APLControllerTest extends AbstractDocViewTest {

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
                    .aplOptions(APLOptions.builder().build())
                    .aplLayout(aplLayout)
                    .rootConfig(RootConfig.create())
                    .render();

            assertNotNull(aplController);
        });

        assertTrue(renderLatch.await(1, TimeUnit.SECONDS));
        assertTrue(displayedLatch.await(5, TimeUnit.SECONDS));
    }
}
