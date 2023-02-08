/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.alexa.android.extension.discovery;

import static org.junit.Assume.assumeTrue;

import android.os.Build;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amazon.alexa.android.extension.discovery.test.TestService;

import org.junit.runner.RunWith;


/**
 * This class runs all the tests in {@link ExtensionMultiplexServiceTest}
 * in async mode.
 */
@RunWith(AndroidJUnit4.class)
public class ExtensionMultiplexServiceAsyncTest extends ExtensionMultiplexServiceTest {


    @Override
    public void doBefore() {
        super.doBefore();
        // would have preferred to mock this.  unfortunately it's difficult to intercept
        // the Service creation via intent, and Mokito doesn't like mocking/spyinng on
        // the singletons or abstract classes.
        TestService.testAsync = true;

        assumeTrue(
                "Async message processing on a specific " +
                        "Looper is not on Android versions earlier than Pie.",
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        );

    }
}
