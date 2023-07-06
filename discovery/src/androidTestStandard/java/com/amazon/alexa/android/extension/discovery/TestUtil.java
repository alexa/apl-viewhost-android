/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.alexa.android.extension.discovery;

import android.app.ActivityManager;
import android.content.Context;
import androidx.test.InstrumentationRegistry;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

class TestUtil {

    /**
     * Connection callback that springs a latch when connection events happen.  This allows
     * for waiting on the event.
     */
    static class ClientTestCallback implements ExtensionMultiplexClient.ConnectionCallback {

        int uuid = ExtensionMultiplexClient.randomConnectionID();
        String mExtensionURI;
        int mErrorCode;
        CountDownLatch lOnCon;
        CountDownLatch lConClose;
        CountDownLatch lConFail;
        CountDownLatch lReceive;
        CountDownLatch lMsgFail;
        String mReceived;

        ClientTestCallback() {
            latch();
        }

        @Override
        public int getID() {
            return uuid;
        }

        @Override
        public void onConnect(String extensionURI) {
            mExtensionURI = extensionURI;
            lOnCon.countDown();
        }

        @Override
        public void onConnectionClosed(String extensionURI, String message) {
            mExtensionURI = extensionURI;
            lConClose.countDown();
        }

        @Override
        public void onConnectionFailure(String extensionURI, int errorCode, String message) {
            mExtensionURI = extensionURI;
            mErrorCode = errorCode;
            lConFail.countDown();
        }

        @Override
        public void onMessage(String extensionURI, String payload) {
            if (lReceive.getCount() == 0)
                fail("Unexpected OnMessage");
            mReceived = payload;
            lReceive.countDown();
        }

        @Override
        public void onMessageFailure(String extensionURI, int errorCode, String message, String failed) {
            mExtensionURI = extensionURI;
            mErrorCode = errorCode;
            lMsgFail.countDown();
        }

        public void latch() {
            lOnCon = new CountDownLatch(1);
            lConClose = new CountDownLatch(1);
            lConFail = new CountDownLatch(1);
            lReceive = new CountDownLatch(1);
            lMsgFail = new CountDownLatch(1);
        }
    }


    /**
     * Helper method to identify if service is running.
     */
    static boolean isRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) InstrumentationRegistry.getTargetContext()
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> services = manager.getRunningServices(Integer.MAX_VALUE);
        for (ActivityManager.RunningServiceInfo service : services) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    /**
     * Helper method to block on a given sLatch for the duration of the set timeout with the
     * expectation the latch will trigger within the given time.
     */
    static void assertOnLatch(CountDownLatch latch, String actionName) {
        try {
            if (!latch.await(3L, TimeUnit.SECONDS)) {
                fail(actionName);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(actionName);
        }
    }


    /**
     * Helper method to block on a given sLatch for the duration with the expectation the latch
     * will never be triggered.
     */
    static void assertNoLatch(CountDownLatch latch, String actionName) {
        try {
            if (latch.await(2L, TimeUnit.SECONDS)) {
                fail(actionName);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(actionName);
        }
    }


}
