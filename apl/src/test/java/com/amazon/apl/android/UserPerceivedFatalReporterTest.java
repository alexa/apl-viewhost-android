/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.amazon.apl.android.dependencies.IUserPerceivedFatalCallback;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class UserPerceivedFatalReporterTest extends ViewhostRobolectricTest {

    @Mock
    private IUserPerceivedFatalCallback userPerceivedFatalCallback;

    private UserPerceivedFatalReporter mUserPerceivedFatalReporter;


    @Before
    public void setup() {
        mUserPerceivedFatalReporter = new UserPerceivedFatalReporter(userPerceivedFatalCallback);
    }

    @Test
    public void testReportUpfOnlyOnceWhenMultipleFatalsReported() {
        // Setup

        // Test
        mUserPerceivedFatalReporter.reportFatal(UserPerceivedFatalReporter.UpfReason.CONTENT_RESOLUTION_FAILURE);
        mUserPerceivedFatalReporter.reportFatal(UserPerceivedFatalReporter.UpfReason.REQUIRED_EXTENSION_LOADING_FAILURE);

        // Verify
        verify(userPerceivedFatalCallback, times(1))
                .onFatalError(eq(UserPerceivedFatalReporter.UpfReason.CONTENT_RESOLUTION_FAILURE.toString()));
        verify(userPerceivedFatalCallback, times(0))
                .onFatalError(eq(UserPerceivedFatalReporter.UpfReason.REQUIRED_EXTENSION_LOADING_FAILURE.toString()));

    }

    @Test
    public void testReportUpfOnlyOnceWhenSuccessIsReportedBeforeFatal() {
        // Setup

        // Test
        mUserPerceivedFatalReporter.reportSuccess();
        mUserPerceivedFatalReporter.reportFatal(UserPerceivedFatalReporter.UpfReason.CONTENT_RESOLUTION_FAILURE);

        // Verify
        verify(userPerceivedFatalCallback, times(1)).onSuccess();
        verify(userPerceivedFatalCallback, times(0))
                .onFatalError(eq(UserPerceivedFatalReporter.UpfReason.CONTENT_RESOLUTION_FAILURE.toString()));

    }

    @Test
    public void testReportUpfOnlyOnceWhenFatalIsReportedBeforeSuccess() {
        // Setup

        // Test
        mUserPerceivedFatalReporter.reportFatal(UserPerceivedFatalReporter.UpfReason.CONTENT_RESOLUTION_FAILURE);
        mUserPerceivedFatalReporter.reportSuccess();

        // Verify
        verify(userPerceivedFatalCallback, times(0)).onSuccess();
        verify(userPerceivedFatalCallback, times(1))
                .onFatalError(eq(UserPerceivedFatalReporter.UpfReason.CONTENT_RESOLUTION_FAILURE.toString()));

    }

    @Test
    public void testReportUpfOnlyOnceWhenMultipleSuccessReported() {
        // Setup

        // Test
        mUserPerceivedFatalReporter.reportSuccess();
        mUserPerceivedFatalReporter.reportSuccess();

        // Verify
        verify(userPerceivedFatalCallback, times(1)).onSuccess();
    }
}