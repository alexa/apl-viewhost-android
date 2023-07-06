/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.alexa.android.extension.discovery;

import android.os.RemoteException;
import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amazon.alexa.android.extension.discovery.ExtensionMultiplexClient.ClientConnection;
import com.amazon.alexa.android.extension.discovery.ExtensionMultiplexService.ServiceConnection;
import com.amazon.alexa.android.extension.discovery.test.TestService.Latch;
import com.amazon.alexaext.ActivityDescriptor;
import com.amazon.alexaext.SessionDescriptor;
import com.amazon.common.test.LeakRulesBaseClass;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static com.amazon.alexa.android.extension.discovery.TestUtil.assertNoLatch;
import static com.amazon.alexa.android.extension.discovery.TestUtil.assertOnLatch;
import static com.amazon.alexa.android.extension.discovery.TestUtil.isRunning;
import static com.amazon.alexa.android.extension.discovery.test.TestService.MultiLatch;
import static com.amazon.alexa.android.extension.discovery.test.TestService.lSvcConClosed;
import static com.amazon.alexa.android.extension.discovery.test.TestService.lSvcOnCon;
import static com.amazon.alexa.android.extension.discovery.test.TestService.lSvcOnExit;
import static com.amazon.alexa.android.extension.discovery.test.TestService.lSvcOnFocusGain;
import static com.amazon.alexa.android.extension.discovery.test.TestService.lSvcOnFocusLost;
import static com.amazon.alexa.android.extension.discovery.test.TestService.lSvcReceive;
import static com.amazon.alexa.android.extension.discovery.test.TestService.latch;
import static com.amazon.alexa.android.extension.discovery.test.TestService.latchService;
import static com.amazon.alexa.android.extension.discovery.test.TestService.testAsync;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExtensionMultiplexServiceTest extends LeakRulesBaseClass {

    private ExtensionMultiplexClient mClient;
    private ConnectionResult mCallback;

    /**
     * Connection callback that springs a latch when connection events happen.  This allows
     * for waiting on the event.
     */
    private static class ConnectionResult implements ExtensionMultiplexClient.ConnectionCallback {

        int uuid = ExtensionMultiplexClient.randomConnectionID();
        String mExtensionURI;
        int mErrorCode;
        CountDownLatch lOnCon;
        CountDownLatch lConClose;
        CountDownLatch lConFail;
        CountDownLatch lReceive;
        CountDownLatch lMsgFail;
        String mReceived;

        ConnectionResult() {
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
        public void onMessageFailure(String extensionURI, int errorCode, String message,
                                     String failed) {
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


    // Allocate common test resources
    @Before
    public void doBefore() {
        // would have preferred to mock this.  unfortunately it's difficult to intercept
        // the Service creation via intent, and Mokito doesn't like mocking/spying on
        // the singletons or abstract classes.
        testAsync = false;
        latch();
        mClient = new ExtensionMultiplexClient(InstrumentationRegistry::getTargetContext);
        mCallback = new ConnectionResult();
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
        assertEquals(0, conCount);
        assertEquals(0, clientCount);
        // clean up handles
        mClient = null;
        mCallback = null;
    }


    /**
     * The test code didn't escape to a release package.
     * The application declared in the manifest is testOnly='true'
     */
    @Test
    public void testAppContext() {

        assertEquals("The application for this Android module should only " +
                        "be exposed for testing.",
                "com.amazon.alexa.android.extension.discovery.test",
                InstrumentationRegistry.getTargetContext().getPackageName()
        );
    }


    /**
     * Basic connect/disconnect
     */
    @Test
    public void testConnect_simple() {

        final String uri = "alexatest:latch:10";
        ClientConnection connection = (ClientConnection) mClient.connect(uri, mCallback);
        assertNotNull(connection);

        // verify successful connection
        assertOnLatch(mCallback.lOnCon, "Service success expected");
        assertOnLatch(lSvcOnCon, "Service connect expected");
        assertTrue(isRunning(Latch.class));
        assertNotNull(latchService);
        ServiceConnection mSvcConnection = (ServiceConnection) latchService.mMultiplexService;
        assertNotNull(mSvcConnection);
        assertEquals(1, mSvcConnection.getClientCount());

        // The connection should be cleaned up if it cannot handshake
        mClient.disconnect(uri, mCallback, "disconnect");
        assertOnLatch(lSvcOnExit, "Expected on exit");
        assertOnLatch(lSvcConClosed, "Expected close connection");
        assertEquals(0, mSvcConnection.getClientCount());
        assertFalse(isRunning(Latch.class));
    }


    /**
     * Multiple attempts to connect to the same service has no effect
     */
    @Test
    public void testConnect_repeat() {
        // connect
        final String uri = "alexatest:latch:10";
        ClientConnection connection = (ClientConnection) mClient.connect(uri, mCallback);
        assertNotNull(connection);

        // verify success
        assertOnLatch(mCallback.lOnCon, "Service success expected");
        assertOnLatch(lSvcOnCon, "Service connect expected");
        assertTrue(isRunning(Latch.class));
        assertNotNull(latchService);
        ServiceConnection mSvcConnection = (ServiceConnection) latchService.mMultiplexService;
        assertNotNull(mSvcConnection);
        assertEquals(1, mSvcConnection.getClientCount());

        // repeat connect
        mClient.connect(uri, mCallback);
        assertEquals(1, mSvcConnection.getClientCount());

        // disconnect
        mClient.disconnect(uri, mCallback, "disconnect");
        assertOnLatch(lSvcOnExit, "Expected on exit");
        assertOnLatch(lSvcConClosed, "Expected close connection");
        assertEquals(0, mSvcConnection.getClientCount());
        assertFalse(isRunning(Latch.class));
    }


    /**
     * Multiple attempts to connect to the same service has no effect
     */
    @Test
    public void testConnect_connectionClosed() {
        // connect
        final String uri = "alexatest:latch:10";
        ClientConnection connection = (ClientConnection) mClient.connect(uri, mCallback);
        assertNotNull(connection);

        // verify success
        assertOnLatch(mCallback.lOnCon, "Service success expected");
        assertOnLatch(lSvcOnCon, "Service connect expected");
        assertTrue(isRunning(Latch.class));
        assertNotNull(latchService);
        ServiceConnection mSvcConnection = (ServiceConnection) latchService.mMultiplexService;
        assertNotNull(mSvcConnection);
        assertEquals(1, mSvcConnection.getClientCount());

        // disconnect
        mClient.disconnect(uri, mCallback, "disconnect");
        assertOnLatch(lSvcOnExit, "Expected on exit");
        assertOnLatch(lSvcConClosed, "Expected close connection");
        assertEquals(0, mSvcConnection.getClientCount());
        assertFalse(isRunning(Latch.class));
    }


    /**
     * Multiple callbacks succeed for single connection same multiple URI.
     * Multi extension services share the same connection, but unique callback per URI.
     * It should appear as though they were different services. The service should only shutdown
     * after all URI are disconnected.
     */
    @Test
    @Ignore
    public void testConnect_multiExtensionService() {

        final String uri10 = "alexatest:multilatch:10";
        ClientConnection connection = (ClientConnection) mClient.connect(uri10, mCallback);
        assertNotNull(connection);

        // verify successful connection
        assertOnLatch(mCallback.lOnCon, "Service success expected");
        assertOnLatch(lSvcOnCon, "Service connect expected");
        assertTrue(isRunning(MultiLatch.class));
        assertNotNull(latchService);
        ServiceConnection mSvcConnection = (ServiceConnection) latchService.mMultiplexService;
        assertNotNull(mSvcConnection);
        assertEquals(1, mSvcConnection.getClientCount());

        // repeat connect with alternate URI
        latch();
        final String uri20 = "alexatest:multilatch:20";
        ClientConnection connection20 = (ClientConnection) mClient.connect(uri20, mCallback);
        assertNotNull(connection20);

        // verify successful connection
        assertOnLatch(mCallback.lOnCon, "Service success expected");
        assertOnLatch(lSvcOnCon, "Service connect expected");
        assertTrue(isRunning(MultiLatch.class));
        assertEquals(2, mSvcConnection.getClientCount());

        // disconnect the first client
        mClient.disconnect(uri10, mCallback, "disconnect");
        assertOnLatch(lSvcOnExit, "Expected on exit");
        assertOnLatch(lSvcConClosed, "Expected close connection");
        assertEquals(1, mSvcConnection.getClientCount());
        assertTrue(isRunning(MultiLatch.class));

        // disconnect the second client
        mClient.disconnect(uri20, mCallback, "disconnect");
        assertOnLatch(lSvcOnExit, "Expected on exit");
        assertOnLatch(lSvcConClosed, "Expected close connection");
        assertEquals(0, mSvcConnection.getClientCount());
        assertFalse(isRunning(MultiLatch.class));
    }


    /**
     * Multiple callbacks succeed for single connection same URI.
     * The service should only shutdown after all URI are disconnected.
     */
    @Test
    @Ignore
    public void testConnect_multiplexConnection() {

        // connect
        String uri = "alexatest:latch:10";
        ClientConnection connection10 = (ClientConnection) mClient.connect(uri, mCallback);
        assertNotNull(connection10);
        assertEquals(1, mClient.getConnectionCount());
        assertEquals(1, connection10.getRegisteredCallbackCount());

        // verify success
        assertOnLatch(mCallback.lOnCon, "Service never connected");
        assertEquals(uri, mCallback.mExtensionURI);
        assertTrue(isRunning(Latch.class));
        assertEquals(1, mClient.getConnectionCount());
        assertEquals(1, connection10.getRegisteredCallbackCount());
        assertNotNull(latchService);
        ServiceConnection mSvcConnection = (ServiceConnection) latchService.mMultiplexService;
        assertNotNull(mSvcConnection);
        assertEquals(1, mSvcConnection.getClientCount());

        // repeat connect
        ConnectionResult callback2 = new ConnectionResult();
        ClientConnection connection2 = (ClientConnection) mClient.connect(uri, callback2);
        assertNotNull(connection2);
        assertEquals(connection10, connection2);

        // verify multiple callbacks on the same connection
        assertOnLatch(mCallback.lOnCon, "Service never connected");
        assertEquals(uri, mCallback.mExtensionURI);
        assertTrue(isRunning(Latch.class));
        assertEquals(1, mClient.getConnectionCount());
        assertEquals(2, connection2.getRegisteredCallbackCount());
        assertEquals(1, mSvcConnection.getClientCount());

        // disconnect both connections, service stays running until both are disconnected
        mClient.disconnect(uri, mCallback, "done");
        assertTrue(isRunning(Latch.class));
        assertEquals(1, mClient.getConnectionCount());
        assertEquals(1, connection10.getRegisteredCallbackCount());
        assertEquals(1, mSvcConnection.getClientCount());

        mClient.disconnect(uri, callback2, "done");
        assertOnLatch(lSvcOnExit, "Expected on exit");
        assertOnLatch(lSvcConClosed, "Expected close connection");
        assertFalse(isRunning(Latch.class));
        assertEquals(0, mClient.getConnectionCount());
        assertEquals(0, connection2.getRegisteredCallbackCount());
        assertEquals(0, mSvcConnection.getClientCount());
    }


    @Test
    @Ignore
    public void testReceive_simple() {
        final String uri = "alexatest:latch:10";
        ActivityDescriptor activity = new ActivityDescriptor(uri, new SessionDescriptor("session"), "activity");
        ClientConnection connection = (ClientConnection) mClient.connect(uri, mCallback);
        assertNotNull(connection);

        // verify successful connection
        assertOnLatch(mCallback.lOnCon, "Service success expected");
        assertOnLatch(lSvcOnCon, "Service connect expected");
        assertTrue(isRunning(Latch.class));
        assertNotNull(latchService);
        ServiceConnection mSvcConnection = (ServiceConnection) latchService.mMultiplexService;
        assertNotNull(mSvcConnection);
        assertEquals(1, mSvcConnection.getClientCount());

        // Send a message from client
        try {
            connection.send(mCallback, activity, "APL");
        } catch (RemoteException e) {
            fail(e.getMessage());
        }

        // verify receipt
        assertOnLatch(lSvcReceive, "Expected Receive");
        assertNotNull(latchService);
        assertNotNull(latchService.mReceived);
        assertEquals("APL", latchService.mReceived);

        // The connection should be cleaned up if it cannot handshake
        mClient.disconnect(uri, mCallback, "disconnect");
        assertOnLatch(lSvcOnExit, "Expected on exit");
        assertOnLatch(lSvcConClosed, "Expected close connection");
        assertEquals(0, mSvcConnection.getClientCount());
        assertFalse(isRunning(Latch.class));
    }

    @Test
    public void testFocus_lost() {
        final String uri = "alexatest:latch:10";
        ClientConnection connection = (ClientConnection) mClient.connect(uri, mCallback);
        assertNotNull(connection);

        // verify successful connection
        assertOnLatch(mCallback.lOnCon, "Service success expected");
        assertOnLatch(lSvcOnCon, "Service connect expected");
        assertTrue(isRunning(Latch.class));
        assertNotNull(latchService);
        ServiceConnection mSvcConnection = (ServiceConnection) latchService.mMultiplexService;
        assertNotNull(mSvcConnection);
        assertEquals(1, mSvcConnection.getClientCount());

        // Send a message from client
        try {
            connection.setFocusLost(mCallback);
        } catch (RemoteException e) {
            fail(e.getMessage());
        }

        // verify receipt
        assertOnLatch(lSvcOnFocusLost, "Expected Focus Lost");
        assertNotNull(latchService);


        // The connection should be cleaned up if it cannot handshake
        mClient.disconnect(uri, mCallback, "disconnect");
        assertOnLatch(lSvcOnExit, "Expected on exit");
        assertOnLatch(lSvcConClosed, "Expected close connection");
        assertEquals(0, mSvcConnection.getClientCount());
        assertFalse(isRunning(Latch.class));
    }


    @Test
    public void testFocus_gain() {
        final String uri = "alexatest:latch:10";
        ClientConnection connection = (ClientConnection) mClient.connect(uri, mCallback);
        assertNotNull(connection);

        // verify successful connection
        assertOnLatch(mCallback.lOnCon, "Service success expected");
        assertOnLatch(lSvcOnCon, "Service connect expected");
        assertTrue(isRunning(Latch.class));
        assertNotNull(latchService);
        ServiceConnection mSvcConnection = (ServiceConnection) latchService.mMultiplexService;
        assertNotNull(mSvcConnection);
        assertEquals(1, mSvcConnection.getClientCount());

        // Send a message from client
        try {
            connection.setFocusGain(mCallback);
        } catch (RemoteException e) {
            fail(e.getMessage());
        }

        // verify receipt
        assertOnLatch(lSvcOnFocusGain, "Expected Receive");
        assertNotNull(latchService);

        // The connection should be cleaned up if it cannot handshake
        mClient.disconnect(uri, mCallback, "disconnect");
        assertOnLatch(lSvcOnExit, "Expected on exit");
        assertOnLatch(lSvcConClosed, "Expected close connection");
        assertEquals(0, mSvcConnection.getClientCount());
        assertFalse(isRunning(Latch.class));
    }


    @Test
    public void testSend_simple() {
        final String uri = "alexatest:latch:10";
        ClientConnection connection = (ClientConnection) mClient.connect(uri, mCallback);
        assertNotNull(connection);

        // verify successful connection
        assertOnLatch(mCallback.lOnCon, "Service success expected");
        assertOnLatch(lSvcOnCon, "Service connect expected");
        assertTrue(isRunning(Latch.class));
        assertNotNull(latchService);
        ServiceConnection mSvcConnection = (ServiceConnection) latchService.mMultiplexService;
        assertNotNull(mSvcConnection);
        assertEquals(1, mSvcConnection.getClientCount());

        // Send a message from the service
        latchService.sendMessage(latchService.mConnectionID, mCallback.uuid, "APL");

        assertOnLatch(mCallback.lReceive, "Expected Receive");
        assertNotNull(mCallback.mReceived);
        assertEquals("APL", mCallback.mReceived);

        // The connection should be cleaned up if it cannot handshake
        mClient.disconnect(uri, mCallback, "disconnect");
        assertOnLatch(lSvcOnExit, "Expected on exit");
        assertOnLatch(lSvcConClosed, "Expected close connection");
        assertEquals(0, mSvcConnection.getClientCount());
        assertFalse(isRunning(Latch.class));
    }


    @Test
    public void testSend_broadcast() {
        final String uri = "alexatest:latch:10";
        ClientConnection connection = (ClientConnection) mClient.connect(uri, mCallback);
        assertNotNull(connection);

        // verify successful connection
        assertOnLatch(mCallback.lOnCon, "Service success expected");
        assertOnLatch(lSvcOnCon, "Service connect expected");
        assertTrue(isRunning(Latch.class));
        assertNotNull(latchService);
        ServiceConnection mSvcConnection = (ServiceConnection) latchService.mMultiplexService;
        assertNotNull(mSvcConnection);
        assertEquals(1, mSvcConnection.getClientCount());

        // Send a message from the service
        latchService.sendBroadcast("APL");

        assertOnLatch(mCallback.lReceive, "Expected Receive");
        assertNotNull(mCallback.mReceived);
        assertEquals("APL", mCallback.mReceived);

        // The connection should be cleaned up if it cannot handshake
        mClient.disconnect(uri, mCallback, "disconnect");
        assertOnLatch(lSvcOnExit, "Expected on exit");
        assertOnLatch(lSvcConClosed, "Expected close connection");
        assertEquals(0, mSvcConnection.getClientCount());
        assertFalse(isRunning(Latch.class));
    }


    @Test
    public void testSend_messageFailure() {
        final String uri = "alexatest:latch:10";
        ClientConnection connection = (ClientConnection) mClient.connect(uri, mCallback);
        assertNotNull(connection);

        // verify successful connection
        assertOnLatch(mCallback.lOnCon, "Service success expected");
        assertOnLatch(lSvcOnCon, "Service connect expected");
        assertTrue(isRunning(Latch.class));
        assertNotNull(latchService);
        ServiceConnection mSvcConnection = (ServiceConnection) latchService.mMultiplexService;
        assertNotNull(mSvcConnection);
        assertEquals(1, mSvcConnection.getClientCount());

        // Send a message from the service
        latchService.sendFailure(latchService.mConnectionID, mCallback.uuid,
                -200, "oops", "failedPayload");

        assertOnLatch(mCallback.lMsgFail, "Expected Receive Failure");
        assertEquals(uri, mCallback.mExtensionURI);
        assertEquals(-200, mCallback.mErrorCode);

        // The connection should be cleaned up if it cannot handshake
        mClient.disconnect(uri, mCallback, "disconnect");
        assertOnLatch(lSvcOnExit, "Expected on exit");
        assertOnLatch(lSvcConClosed, "Expected close connection");
        assertEquals(0, mSvcConnection.getClientCount());
        assertFalse(isRunning(Latch.class));
    }


    /**
     * Multiple attempts to connect to the same service has no effect
     */
    @Test
    public void testDisconnect_simple() {
        // connect
        final String uri = "alexatest:latch:10";
        ClientConnection connection = (ClientConnection) mClient.connect(uri, mCallback);
        assertNotNull(connection);

        // verify success
        assertOnLatch(mCallback.lOnCon, "Service success expected");
        assertOnLatch(lSvcOnCon, "Service connect expected");
        assertTrue(isRunning(Latch.class));
        assertNotNull(latchService);
        ServiceConnection mSvcConnection = (ServiceConnection) latchService.mMultiplexService;
        assertNotNull(mSvcConnection);
        assertEquals(1, mSvcConnection.getClientCount());

        // disconnect
        latchService.disconnect("done");
        assertOnLatch(mCallback.lConClose, "Expected close connection");
        assertEquals(0, mSvcConnection.getClientCount());
        assertFalse(isRunning(Latch.class));
    }

    /**
     * No receive of messages after disconnect.
     */
    @Test
    public void testDisconnect_noReceiveAfterDisconnect() {

        final String uri = "alexatest:latch:10";
        ClientConnection connection = (ClientConnection) mClient.connect(uri, mCallback);
        assertNotNull(connection);

        // verify success
        assertOnLatch(mCallback.lOnCon, "Service success expected");
        assertOnLatch(lSvcOnCon, "Service connect expected");
        assertTrue(isRunning(Latch.class));
        assertNotNull(latchService);
        ServiceConnection mSvcConnection = (ServiceConnection) latchService.mMultiplexService;
        assertNotNull(mSvcConnection);
        assertEquals(1, mSvcConnection.getClientCount());

        // disconnect
        latchService.disconnect("done");
        assertOnLatch(mCallback.lConClose, "Expected close connection");
        assertEquals(0, mSvcConnection.getClientCount());
        assertFalse(isRunning(Latch.class));

        // Check that if disconnect is called before these methods we don't get an uncaught exception
        latchService.sendMessage(latchService.mConnectionID, mCallback.uuid, "Test for crash");
        latchService.sendFailure(latchService.mConnectionID, mCallback.uuid,
                -200, "oops", "Test for crash");
        latchService.sendBroadcast("Test for crash");

        // reset
        latch();

        // Now send all possible client messages and verify no receive
        // because the connection is closed messages are sent directly to the seerver via the
        // aidl interface. (bypassing ipc)
        try {
            mSvcConnection.L2_connect(connection, "teest");
            mSvcConnection.L2_connectionClosed(connection, "test");
            mSvcConnection.L2_receive(connection.connectionID(), mCallback.uuid, "test");
            mSvcConnection.L2_onFocusLost(connection.connectionID(), mCallback.uuid);
            mSvcConnection.L2_onFocusGained(connection.connectionID(), mCallback.uuid);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        assertNoLatch(lSvcOnCon, "expected NO connect");
        assertNoLatch(lSvcOnExit, "expected no exit");
        assertNoLatch(lSvcConClosed, "expected NO con fail");
        assertNoLatch(lSvcReceive, "expected NO receive");
        assertNoLatch(lSvcOnFocusGain, "expected NO message fail");
        assertNoLatch(lSvcOnFocusLost, "expected NO message fail");


    }


}
