/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.alexa.android.extension.discovery.test;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Rect;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Surface;

import com.amazon.alexa.android.extension.discovery.AbstractExtensionService;

import java.lang.NullPointerException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import static com.amazon.alexa.android.extension.discovery.ExtensionMultiplexService.ConnectionID;
import static com.amazon.alexa.android.extension.discovery.ExtensionMultiplexService.getInstance;


public final class TestService {

    private static final String TAG = "TestService";

    protected static abstract class AbstractTestExtensionService extends AbstractExtensionService {
        @Override
        public IBinder onBind(final Intent intent) {
            if (mMultiplexService == null) {
                mMultiplexService = getInstance()
                        .connect(Looper.myLooper(), this, testAsync);
            }
            return mMultiplexService;
        }
    }

    /**
     * Basic test service.
     */
    public static final class Simple extends AbstractTestExtensionService {

        @Override
        public String getName() {
            return "Simple";
        }
    }


    /**
     * Basic test service, another one.
     */
    public static final class Simple2 extends AbstractTestExtensionService {

        @Override
        public String getName() {
            return "Simple2";
        }
    }


    /**
     * Test service that supports multiple extension URI.
     * See AndroidManifest
     */
    public static final class Multi extends AbstractTestExtensionService {

        @Override
        public String getName() {
            return "Multi";
        }
    }


    /**
     * Test service with additional capabilities requirements
     * See AndroidManifest
     */
    public static final class Capability extends AbstractTestExtensionService {

        @Override
        public String getName() {
            return "Capability";
        }
    }

    /**
     * Test service with single uri and definition declared.
     */
    public static final class Deferred extends AbstractTestExtensionService {
        @Override
        public String getName() {
            return "Deferred";
        }
    }

    /**
     * Test service with multi uris and definitions declared.
     * Number of uri matches number of definitions.
     */
    public static final class DeferredMulti extends AbstractTestExtensionService {
        @Override
        public String getName() {
            return "DeferredMulti";
        }
    }

    /**
     * Test service with multi uris and definitions declared.
     * Number of uris does not match number of definitions.
     */
    public static final class DeferredMultiMismatch extends AbstractTestExtensionService {
        @Override
        public String getName() {
            return "DeferredMultiMismatch";
        }
    }


    /**
     * Test service that runs in external process.
     * See AndroidManifest
     */
    public static final class Remote extends AbstractTestExtensionService {

        @Override
        public String getName() {
            return "Remote";
        }
    }


    /**
     * Test service that fails binding by returning null.
     */
    public static final class FailCom extends AbstractTestExtensionService {

        @Override
        public String getName() {
            return "FailCom";
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }


    /**
     * Test service to die on receive of message.
     */
    public static final class FailDied extends AbstractTestExtensionService {

        @Override
        public String getName() {
            return "FailDied";
        }

        @Override
        public void onMessage(ConnectionID connectionID, int clientID, String message) {
            throw new RuntimeException("Doom");
        }
    }


    /**
     * Test service with additional security permissions.
     * See AndroidManifest
     */
    public static final class FailSecurity extends AbstractTestExtensionService {

        @Override
        public String getName() {
            return "FailSecurity";
        }
    }


    /**
     * Test service that dies after 100 ms
     */
    public static final class FailDisconnect extends AbstractTestExtensionService {

        @Override
        public String getName() {
            return "FailDisconnect";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public IBinder onBind(Intent intent) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
            }, 100);
            return super.onBind(intent);
        }
    }


    /**
     * Test service that triggers latches for async test monitors.
     */
    public static class Latch extends AbstractTestExtensionService {


        public String mReceived;
        public int mClientID;
        public String mConfiguration;
        public ConnectionID mConnectionID = null;


        public Latch() {
            super();
            latch();
            latchService = this;
        }

        @Override
        public String getName() {
            return "Latch";
        }

        @SuppressLint("Assert")
        @Override
        public void onMessage(ConnectionID connectionID, int clientId, String message) {
            assert (lSvcReceive.getCount() != 0);
            mReceived = message;
            mClientID = clientId;
            assert (connectionID != null);
            assert (mClientID > 0);
            assert (mReceived != null);
            lSvcReceive.countDown();
        }

        @Override
        public void onResourceAvailable(ConnectionID connectionID, int clientID, Surface surface, Rect rect, String resourceID) { 
            mClientID = clientID;
            if (lSvcRscAvailable.getCount() == 0) throw new IllegalArgumentException("lSvcRscAvailable latch count can't be 0");
            if (connectionID == null) throw new NullPointerException("ConnectionID connectionID can't be null");
            if (surface == null) throw new NullPointerException("Surface surface can't be null");
            if (rect == null) throw new NullPointerException("Rect rect can't be null");
            if (resourceID == null) throw new NullPointerException("String resourceId can't be null");
            lSvcRscAvailable.countDown();
        }

        @Override
        public void onResourceUnavailable(ConnectionID connectionID, int clientID, String resourceID) {
            mClientID = clientID;
            if (connectionID == null) throw new NullPointerException("ConnectionID connectionId can't be null");
            if (lSvcRscUnavailable.getCount() == 0) throw new IllegalArgumentException("lSvcRscUnavailable latch count can't be 0");
            if (resourceID == null) throw new NullPointerException("String resourceId can't be null");
            
            lSvcRscUnavailable.countDown();
        }

        @SuppressLint("Assert")
        @Override
        public void onFocusLost(ConnectionID connectionID, int clientID) {
            assert (lSvcOnFocusLost.getCount() != 0);
            lSvcOnFocusLost.countDown();
        }

        @SuppressLint("Assert")
        @Override
        public void onFocusGained(ConnectionID connectionID, int clientID) {
            assert (lSvcOnFocusGain.getCount() != 0);
            lSvcOnFocusGain.countDown();
        }

        @SuppressLint("Assert")
        @Override
        public void onPause(ConnectionID connectionID, int clientID) {
            assert (lSvcOnPause.getCount() != 0);
            lSvcOnPause.countDown();
        }

        @SuppressLint("Assert")
        @Override
        public void onResume(ConnectionID connectionID, int clientID) {
            assert (lSvcOnResume.getCount() != 0);
            lSvcOnResume.countDown();
        }

        @SuppressLint("Assert")
        @Override
        public void onExit(ConnectionID connectionID, int clientID) {
            assert (lSvcOnExit.getCount() != 0);
            lSvcOnExit.countDown();
        }

        @Override
        public void onConnect(ConnectionID connectionID, String configuration) {
            mConnectionID = connectionID;
            mConfiguration = configuration;
            lSvcOnCon.countDown();
        }


        @Override
        public void onConnectionClosed(ConnectionID connectionID, String message) {
            lSvcConClosed.countDown();
        }

        @Override
        public void onConnectionFailure(ConnectionID connectionID, int error, String message) {
            lSvcConFail.countDown();
        }

        public void disconnect(final String message) {
            super.disconnect(message);
            stopSelf();
        }
    }


    /**
     * Test service with multiple URI that triggers latches for async test monitors.
     * See manifest.
     */
    public static final class MultiLatch extends Latch {

        @Override
        public String getName() {
            return "MultiLatch";
        }
    }


    public static boolean testAsync = false;
    public static Latch latchService;
    public static CountDownLatch lSvcReceive;
    public static CountDownLatch lSvcRscAvailable;
    public static CountDownLatch lSvcRscUnavailable;
    public static CountDownLatch lSvcOnCon;
    public static CountDownLatch lSvcConClosed;
    public static CountDownLatch lSvcConFail;
    public static CountDownLatch lSvcOnFocusLost;
    public static CountDownLatch lSvcOnFocusGain;
    public static CountDownLatch lSvcOnPause;
    public static CountDownLatch lSvcOnResume;
    public static CountDownLatch lSvcOnExit;


    public static void latch() {
        latch(1);
    }

    public static void latch(int expectedClients) {
        lSvcOnCon = new CountDownLatch(1);
        lSvcConClosed = new CountDownLatch(1);
        lSvcConFail = new CountDownLatch(1);
        lSvcReceive = new CountDownLatch(expectedClients);
        lSvcRscAvailable = new CountDownLatch(expectedClients);
        lSvcRscUnavailable = new CountDownLatch(expectedClients);
        lSvcOnFocusLost = new CountDownLatch(expectedClients);
        lSvcOnFocusGain = new CountDownLatch(expectedClients);
        lSvcOnPause = new CountDownLatch(expectedClients);
        lSvcOnResume = new CountDownLatch(expectedClients);
        lSvcOnExit = new CountDownLatch(expectedClients);
    }

}