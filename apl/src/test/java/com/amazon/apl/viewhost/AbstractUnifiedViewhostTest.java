/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.viewhost;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.robolectric.shadows.ShadowLooper.shadowMainLooper;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.amazon.apl.android.APLController;
import com.amazon.apl.android.APLLayout;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.viewhost.config.ViewhostConfig;
import com.amazon.apl.viewhost.message.action.SendUserEventRequest;
import com.amazon.apl.viewhost.message.notification.DocumentStateChanged;
import com.amazon.apl.viewhost.utils.CapturingMessageHandler;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowChoreographer;

import java.time.Duration;
import java.util.function.BooleanSupplier;

/**
 * Convenience base class for cases testing the newer "unified" rendering flow. The tenet of these
 * tests is to be as "real" as possible, executing all underlying C++ code and being able to observe
 * side effects of successful and unsuccessful data flows.
 *
 * This test gets the document as far as INFLATED. Getting to DISPLAYED would require the "draw"
 * calls, which don't get exercised in this test framework.
 */
@RunWith(AndroidJUnit4.class)
public abstract class AbstractUnifiedViewhostTest extends ViewhostRobolectricTest {
    /**
     * Load the APL JNI files
     */
    static {
        APLController.initializeAPL(InstrumentationRegistry.getInstrumentation().getContext());
    }

    /**
     * Simulated clock frequency (vsync rate).
     */
    private static final int CLOCK_FREQUENCY_MILLIS = 16;

    /**
     * Basic activity to hold the APL view and a reference to that view.
     */
    protected BasicActivity mActivity;
    protected APLLayout mAplLayout;

    /**
     * Basic viewhost building blocks
     */
    protected CapturingMessageHandler mMessageHandler = new CapturingMessageHandler();
    protected ViewhostConfig.Builder mViewhostConfigBuilder = ViewhostConfig.builder();
    protected Viewhost mViewhost;

    /**
     * Setup fully-functional view host. A test class that wants different behavior can:
     * 1. Override the initialize() method and perform custom setup.
     * 2. Call the super class.
     */
    @Before
    public void initialize() {
        // The default delay is 0. We want a non-zero delay so that we can step frame-by-frame.
        ShadowChoreographer.setPostFrameCallbackDelay(CLOCK_FREQUENCY_MILLIS);

        // Create view host
        mViewhostConfigBuilder.messageHandler(mMessageHandler);
        mViewhost = Viewhost.create(mViewhostConfigBuilder.build());

        // Create activity, which also creates the APL View
        mActivity = Robolectric.setupActivity(BasicActivity.class);
        mAplLayout = mActivity.getView();

        // Bind the viewhost to the view
        mViewhost.bind(mAplLayout);
    }

    /**
     * Maximum time to execute before giving up (see runUntil).
     */
    private static final long MAX_ELAPSED_MILLIS = 5_000;

    /**
     * Runs the ShadowLooper until the given predicate is true, or the maximum elapsed time is exceeded.
     *
     * @param predicate the predicate to check for loop termination
     * @throws IllegalStateException if the predicate is not true after the maximum elapsed time
     */
    public void runUntil(BooleanSupplier predicate) {
        long elapsed = 0;
        while (!predicate.getAsBoolean()) {
            shadowMainLooper().idleFor(Duration.ofMillis(CLOCK_FREQUENCY_MILLIS));
            try {
                // Actually sleep a little because ShadowLooper.idleFor() only simulates the
                // passage of time on the main thread. Sometime things take actual time to occur.
                Thread.sleep(CLOCK_FREQUENCY_MILLIS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            elapsed += CLOCK_FREQUENCY_MILLIS;
            if (elapsed > MAX_ELAPSED_MILLIS) {
                throw new IllegalStateException("Predicate did not become true within the maximum elapsed time");
            }
        }
    }

    /**
     * Convenience method that waits until a single SendEvent is emitted and then asserts the
     * arguments of that SendEvent.
     */
    public void assertSendEvent(Object... arguments) {
        runUntil(() -> mMessageHandler.has(SendUserEventRequest.class));
        SendUserEventRequest message = mMessageHandler.findOne(SendUserEventRequest.class);
        assertNotNull(message);
        assertArrayEquals(arguments, message.getArguments());
    }

    /**
     * Convenience method that waits until the desired state change notification is emitted.
     */
    public void assertDocumentStateChanged(String state) {
        runUntil(() -> mMessageHandler.has(DocumentStateChanged.class));
        DocumentStateChanged notification = mMessageHandler.findOne(DocumentStateChanged.class);
        assertEquals(state, notification.getState());
    }
}
