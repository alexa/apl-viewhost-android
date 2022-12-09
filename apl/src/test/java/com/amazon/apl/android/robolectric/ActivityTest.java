/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.robolectric;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import com.amazon.apl.android.APLController;
import com.amazon.apl.android.APLLayout;
import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.APLTestContext;
import com.amazon.apl.android.Content;
import com.amazon.apl.android.IAPLController;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.TestActivity;
import com.amazon.apl.android.utils.TestClock;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.mockito.MockitoAnnotations;
import org.robolectric.shadows.ShadowLooper;

/**
 * This base class serves as initial setup for Espresso Tests.
 *
 * In order to inflate the document, you should invoke it:
 *
 *         onView(withId(com.amazon.apl.android.test.R.id.apl))
 *                 .perform(inflate(myDocument))
 *                 .check(hasRootContext());
 *
 * where it will populate `APLTestContext` where it contains a reference
 * of the real RootContext of the application.
 */
public abstract class ActivityTest extends ViewhostRobolectricTest {

    protected TestClock testClock = new TestClock();
    protected APLTestContext mTestContext;
    protected IAPLController mAPLController;

    @Rule
    public ActivityScenarioRule<TestActivity> activityRule = new ActivityScenarioRule<>(TestActivity.class);

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void teardown() {
        if (mTestContext != null) {
            activityRule.getScenario().onActivity(activity -> {
                mTestContext.getRootContext().finishDocument();
            });
        }
    }

    protected void executeCommands(String commands) {
        mTestContext.getRootContext().executeCommands(commands);
        testClock.doFrameUpdate(100);
    }

    protected void inflate(String document, String payloadId, String data, APLOptions.Builder optionsBuilder, RootConfig rootConfig) {
        mTestContext = new APLTestContext()
                .setDocument(document)
                .setAplOptions(optionsBuilder
                        .aplClockProvider(callback -> {
                            testClock.registerCallback(callback);
                            return testClock;
                        })
                        .build());

        if (payloadId != null && data != null) {
            mTestContext.setDocumentPayload(payloadId, data);
        }

        mTestContext.buildRootContextDependencies();

        if (rootConfig != null) {
            mTestContext.setRootConfig(rootConfig);
        }

        Content content = mTestContext.getContent();
        Assert.assertTrue("Failed to create Content", content.isReady());

        activityRule.getScenario().onActivity(activity -> {
            APLLayout aplLayout = activity.findViewById(com.amazon.apl.android.test.R.id.apl);
            try {
                mAPLController = APLController.renderDocument(content, mTestContext.getAplOptions(), mTestContext.getRootConfig(), aplLayout.getPresenter());

                RootContext rc = aplLayout.getAPLContext();
                // Store the RC configuration made by this test
                mTestContext.setRootContext(rc);
                mTestContext.setPresenter(rc.getViewPresenter());
            } catch (APLController.APLException e) {
                Assert.fail(e.getMessage());
            }
        });

        // Flush the main looper to finish creating views.
        ShadowLooper.idleMainLooper();
    }
}