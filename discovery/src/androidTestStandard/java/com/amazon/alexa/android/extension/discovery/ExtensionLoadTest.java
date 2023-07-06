/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.alexa.android.extension.discovery;

import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import com.amazon.alexa.android.extension.discovery.ExtensionMultiplexClient.ClientConnection;
import com.amazon.alexa.android.extension.discovery.TestUtil.ClientTestCallback;
import com.amazon.alexa.android.extension.discovery.test.TestService;
import com.amazon.common.test.LeakRulesBaseClass;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static com.amazon.alexa.android.extension.discovery.TestUtil.assertOnLatch;
import static com.amazon.alexa.android.extension.discovery.test.TestService.lSvcConClosed;
import static com.amazon.alexa.android.extension.discovery.test.TestService.lSvcOnExit;
import static com.amazon.alexa.android.extension.discovery.test.TestService.latch;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ExtensionLoadTest extends LeakRulesBaseClass  {

    protected ExtensionMultiplexClient mClient;
    protected boolean testAsync;
    private static final int LOAD = 100;


    // Allocate common test resources
    @Before
    public void doBefore() {
        testAsync = false;
        latch(LOAD);
        mClient = new ExtensionMultiplexClient(InstrumentationRegistry::getTargetContext);
        assertNotNull(mClient);
    }

    // Free common test resources
    @After
    public void doAfter() {
        // count all connections that should be closed
        int conCount = mClient.getBinder().getBindingCount();
        int clientCount = mClient.getConnectionCount();
        // fail safe to ensure service clean-up and continued test run
        mClient.kill();
        // verify connections were closed
        assertEquals(0, clientCount);
        assertEquals(0, conCount);
        // clean up handles
        mClient = null;
    }


    @MediumTest
    @Test
    public void testMany_connectThenDisconnect() {
        final String uri = "alexatest:latch:10";

        ClientTestCallback[] connectionResults = new ClientTestCallback[LOAD];
        ClientConnection connection = null;

        for (int i = 0; i < connectionResults.length; i++) {
            connectionResults[i] = new ClientTestCallback();
            ClientConnection con = (ClientConnection) mClient.connect(uri, connectionResults[i]);
            assertNotNull(con);
            if (connection != null)  // same service same connection
                assertEquals(connection, con);
            connection = con;
        }

        assertEquals(connectionResults.length, connection.getRegisteredCallbackCount());
        assertEquals(1, mClient.getConnectionCount());

        // verify all disconnected
        for (int i = 0; i < connectionResults.length; i++) {
            ClientTestCallback connectionResult = connectionResults[i];
            assertOnLatch(connectionResult.lOnCon, "expected connect:" + i);
            mClient.disconnect(uri, connectionResult, "bye");
        }

        // verify connections were closed
        assertOnLatch(lSvcOnExit, "Expected Exit");
        assertOnLatch(lSvcConClosed, "Expected connection close");
        assertEquals(0, mClient.getConnectionCount());
        assertEquals(0, mClient.getBinder().getBindingCount());

    }


    @MediumTest
    @Test
    public void testMany_connectDisconnectImmediate() {
        final String uri = "alexatest:latch:10";
        ClientConnection connection;

        for (int i = 0; i < LOAD; i++) {
            ClientTestCallback ctc = new ClientTestCallback();
            connection = (ClientConnection) mClient.connect(uri, ctc);
            assertOnLatch(ctc.lOnCon, "expected connect");
            assertNotNull(connection);
            assertEquals(1, connection.getRegisteredCallbackCount());
            assertEquals(1, mClient.getConnectionCount());
            mClient.disconnect(uri, ctc, "bye");
            assertEquals(0, connection.getRegisteredCallbackCount());
            assertEquals(0, mClient.getConnectionCount());
        }

        // Check the service for exit counts
        assertOnLatch(lSvcConClosed, "Expected connection close");
        assertEquals(0, mClient.getConnectionCount());
        assertEquals(0, mClient.getBinder().getBindingCount());
    }


    @MediumTest
    @Test
    public void testMany_connectOrder() {
        final String uri = "alexatest:latch:10";

        OrderedClientCallback[] connectionResults = new OrderedClientCallback[LOAD];
        ClientConnection connection = null;

        // connect all
        OrderedClientCallback.latch();
        for (int i = 0; i < connectionResults.length; i++) {
            connectionResults[i] = new OrderedClientCallback();
            connection = (ClientConnection) mClient.connect(uri, connectionResults[i]);
        }
        // verify registration
        assertEquals(connectionResults.length, connection.getRegisteredCallbackCount());

        // verify all connected, and were notified in order
        assertOnLatch(OrderedClientCallback.latch, "expected connect");
        assertTrue(OrderedClientCallback.pass);

        // send a message to all
        OrderedClientCallback.latch();
        TestService.latchService.sendBroadcast("Hello");
        assertOnLatch(OrderedClientCallback.latch, "expected message");
        assertTrue(OrderedClientCallback.pass);

        // disconnect all
        OrderedClientCallback.latch();
        for (OrderedClientCallback connectionResult : connectionResults) {
            mClient.disconnect(uri, connectionResult, "bye");
        }
        // verify unregister
        assertEquals(0, connection.getRegisteredCallbackCount());

        // verify all disconnect, and were notified in order
        assertTrue(OrderedClientCallback.pass);

        // verify connections were closed
        assertOnLatch(lSvcOnExit, "Expected Exit");
        assertOnLatch(lSvcConClosed, "Expected connection close");
        assertEquals(0, mClient.getConnectionCount());
        assertEquals(0, mClient.getBinder().getBindingCount());

    }


    private static class OrderedClientCallback extends ExtensionClientCallback {

        // ordered identifier
        static AtomicInteger order = new AtomicInteger(0);
        final int testId;
        // values under test
        static boolean pass;
        static int previousId;
        static CountDownLatch latch;

        static void latch() {
            pass = true;
            previousId = 0;
            latch = new CountDownLatch(LOAD);
        }

        OrderedClientCallback() {
            testId = order.incrementAndGet();
        }

        void testOrder() {
            if (pass) {
                pass = (previousId + 1 == testId);
            }
            previousId = testId;
            latch.countDown();
        }

        @Override
        public void onConnect(String extensionURI) {
            testOrder();
        }


        @Override
        public void onMessage(String extensionURI, String message) {
            testOrder();
        }

    }
}
