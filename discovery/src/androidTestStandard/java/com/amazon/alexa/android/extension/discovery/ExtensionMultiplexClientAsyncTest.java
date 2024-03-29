/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.alexa.android.extension.discovery;

import android.os.Build;
import android.os.HandlerThread;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amazon.alexa.android.extension.discovery.ExtensionMultiplexClient.ClientConnection;
import com.amazon.alexa.android.extension.discovery.ExtensionMultiplexClient.ConnectionCallback;
import com.amazon.alexa.android.extension.discovery.test.TestService;

import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;


/**
 * This class runs all the tests in {@link ExtensionMultiplexServiceTest}
 * in async mode.
 */
@RunWith(AndroidJUnit4.class)
public class ExtensionMultiplexClientAsyncTest extends ExtensionMultiplexClientTest {

    @Override
    public void doBefore() {
        super.doBefore();

        assumeTrue(
                "Async message processing on a specific " +
                        "Looper is not on Android versions earlier than Pie.",
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
        );

    }

    @Override
    protected ClientConnection createTestConnect(String uri, ConnectionCallback callback) {
        HandlerThread thread = new HandlerThread("AsyncExtensionTestThread");
        thread.start();
        ClientConnection connection = (ClientConnection) mClient.connect(uri, null,
                thread.getLooper(), callback, true);
        assertNotNull(connection);
        return connection;
    }


    @Override
    protected ClientConnection createTestConnect(String uri, String config, ConnectionCallback callback) {
        HandlerThread thread = new HandlerThread("AsyncExtensionTestThread");
        thread.start();
        ClientConnection connection = (ClientConnection) mClient.connect(uri, config,
                thread.getLooper(), callback, true);
        assertNotNull(connection);
        return connection;
    }


}
