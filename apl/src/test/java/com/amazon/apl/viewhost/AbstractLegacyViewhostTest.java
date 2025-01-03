/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.viewhost;

import static org.junit.Assert.assertArrayEquals;
import static org.robolectric.shadows.ShadowLooper.shadowMainLooper;

import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.amazon.apl.android.APLController;
import com.amazon.apl.android.APLLayout;
import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowChoreographer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BooleanSupplier;

/**
 * Convenience base class for cases testing the legacy rendering. The tenet of these tests is to be
 * as "real" as possible, executing all underlying C++ code and being able to observe side effects
 * of successful and unsuccessful data flows.
 *
 * In style of testing, the document gets INFLATED and the clock ticks. We don't get DISPLAYED
 * because dispatchDraw does not get exercised.
 */
@RunWith(AndroidJUnit4.class)
public abstract class AbstractLegacyViewhostTest extends ViewhostRobolectricTest {
    private static final String TAG = "LegacyTest";

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
     * Basic legacy rendering building blocks
     */
    protected APLOptions.Builder mAplOptionsBuilder = APLOptions.builder();
    protected RootConfig mRootConfig = RootConfig.create();

    protected ConcurrentLinkedQueue<UserEvent> mUserEvents = new ConcurrentLinkedQueue<>();

    /**
     * Setup activity and view.
     * 1. Override the initialize() method and perform custom setup.
     * 2. Call the super class.
     */
    @Before
    public void initialize() {
        // The default delay is 0. We want a non-zero delay so that we can step frame-by-frame.
        ShadowChoreographer.setPostFrameCallbackDelay(CLOCK_FREQUENCY_MILLIS);

        // Create activity, which also creates the APL View
        mActivity = Robolectric.setupActivity(BasicActivity.class);
        mAplLayout = mActivity.getView();

        mAplOptionsBuilder.sendEventCallbackV2((args, components, sources, flags) -> {
            Log.d(TAG, "Received UserEvent: " + Arrays.toString(args));
            mUserEvents.add(new UserEvent(args, components, sources, flags));
        });
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
        runUntil(() -> !mUserEvents.isEmpty());
        assertArrayEquals(arguments, mUserEvents.peek().args);
    }

    public static class UserEvent {
        Object[] args;
        Map<String, Object> components;
        Map<String, Object> sources;
        Map<String, Object> flags;

        public UserEvent(Object[] args, Map<String, Object> components, Map<String, Object> sources, Map<String, Object> flags) {
            this.args = args;
            this.components = components;
            this.sources = sources;
            this.flags = flags;
        }
    }
}
