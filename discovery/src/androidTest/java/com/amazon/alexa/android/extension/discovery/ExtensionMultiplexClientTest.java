/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.alexa.android.extension.discovery;

import static com.amazon.alexa.android.extension.discovery.TestUtil.assertNoLatch;
import static com.amazon.alexa.android.extension.discovery.TestUtil.assertOnLatch;
import static com.amazon.alexa.android.extension.discovery.TestUtil.isRunning;
import static com.amazon.alexa.android.extension.discovery.test.TestService.Latch;
import static com.amazon.alexa.android.extension.discovery.test.TestService.Multi;
import static com.amazon.alexa.android.extension.discovery.test.TestService.Simple;
import static com.amazon.alexa.android.extension.discovery.test.TestService.lSvcOnCon;
import static com.amazon.alexa.android.extension.discovery.test.TestService.lSvcOnFocusGain;
import static com.amazon.alexa.android.extension.discovery.test.TestService.lSvcOnFocusLost;
import static com.amazon.alexa.android.extension.discovery.test.TestService.lSvcOnPause;
import static com.amazon.alexa.android.extension.discovery.test.TestService.lSvcOnResume;
import static com.amazon.alexa.android.extension.discovery.test.TestService.lSvcReceive;
import static com.amazon.alexa.android.extension.discovery.test.TestService.lSvcRscAvailable;
import static com.amazon.alexa.android.extension.discovery.test.TestService.lSvcRscUnavailable;
import static com.amazon.alexa.android.extension.discovery.test.TestService.latch;
import static com.amazon.alexa.android.extension.discovery.test.TestService.latchService;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.RemoteException;
import android.view.Surface;

import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amazon.alexa.android.extension.discovery.ExtensionMultiplexClient.ClientConnection;
import com.amazon.alexa.android.extension.discovery.ExtensionMultiplexClient.ConnectionCallback;
import com.amazon.alexa.android.extension.discovery.ExtensionMultiplexClient.IMultiplexClientConnection;
import com.amazon.alexa.android.extension.discovery.TestUtil.ClientTestCallback;
import com.amazon.alexa.android.extension.discovery.test.TestService;
import com.amazon.alexa.android.extension.discovery.test.TestService.FailDied;
import com.amazon.alexa.android.extension.discovery.test.TestService.Remote;
import com.amazon.alexaext.ActivityDescriptor;
import com.amazon.alexaext.SessionDescriptor;
import com.amazon.common.test.LeakRulesBaseClass;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@SuppressWarnings("ConstantConditions")
@RunWith(AndroidJUnit4.class)
public class ExtensionMultiplexClientTest extends LeakRulesBaseClass  {

    protected ExtensionMultiplexClient mClient;
    private ClientTestCallback mCallback;
    protected boolean testAsync;


    // Allocate common test resources
    @Before
    public void doBefore() {
        testAsync = false;
        latch();
        mClient = new ExtensionMultiplexClient(InstrumentationRegistry::getTargetContext);
        mCallback = new ClientTestCallback();
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
        assertEquals("Test did not cleanup, check test for failure", 0, clientCount);
        assertEquals("Test did not cleanup, check test for failure", 0, conCount);
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

        assertEquals("The application for this Android module should only be exposed for testing.",
                "com.amazon.alexa.android.extension.discovery.test",
                InstrumentationRegistry.getTargetContext().getPackageName()
        );
    }


    /**
     * Invalid URI is handled gracefully.
     */
    @Test
    public void testConnect_badURI() {
        IMultiplexClientConnection connection = mClient.connect(null, mCallback);
        assertNull(connection);
    }


    /**
     * Invalid callback is handled gracefully.
     */
    @Test
    public void testConnect_badCallback() {
        IMultiplexClientConnection connection = mClient.connect("alexatest:simple:10", null);
        assertNull(connection);
    }

    /**
     * Non existent extension returns null.
     */
    @Test
    public void testConnect_doesNotExist() {
        IMultiplexClientConnection connection = mClient.connect("alexatest:dne:10", mCallback);
        assertNull(connection);
        assertEquals(0, mClient.getConnectionCount());
    }

    @Test
    public void testHasExtension_doesNotExist() {
        final ExtensionDiscovery.ExtensionPresence presence = mClient.hasExtension("alexatest:dne:10");
        assertEquals(ExtensionDiscovery.ExtensionPresence.NOT_PRESENT, presence);
    }

    protected ClientConnection createTestConnect(String uri, ConnectionCallback callback) {
        ClientConnection connection = (ClientConnection) mClient.connect(uri, callback);
        assertNotNull(connection);
        return connection;
    }

    protected ClientConnection createTestConnect(String uri, String config, ConnectionCallback callback) {
        ClientConnection connection = (ClientConnection) mClient.connect(uri, config, callback);
        assertNotNull(connection);
        return connection;
    }


    /**
     * Basic connect/disconnect
     */
    @Test
    public void testConnect_simple() {
        // connect
        final String uri = "alexatest:simple:10";
        ClientConnection connection = createTestConnect(uri, mCallback);
        assertNotNull(connection);
        assertEquals(1, mClient.getConnectionCount());
        assertEquals(1, connection.getRegisteredCallbackCount());

        // verify success
        assertOnLatch(mCallback.lOnCon, "Service success expected");
        assertEquals(uri, mCallback.mExtensionURI);
        assertTrue(isRunning(Simple.class));

        // disconnect
        mClient.disconnect(uri, mCallback, "done");
        assertFalse(isRunning(Simple.class));
        assertEquals(0, mClient.getConnectionCount());
        assertEquals(0, connection.getRegisteredCallbackCount());
    }


    /**
     * Basic connect/disconnect
     */
    @Test
    public void testConnect_remote() {
        // connect
        final String uri = "alexatest:remote:10";
        ClientConnection connection = createTestConnect(uri, mCallback);
        assertNotNull(connection);
        assertEquals(1, mClient.getConnectionCount());
        assertEquals(1, connection.getRegisteredCallbackCount());

        // verify success
        assertOnLatch(mCallback.lOnCon, "Service success expected");
        assertEquals(uri, mCallback.mExtensionURI);
        assertTrue(isRunning(Remote.class));

        // disconnect
        mClient.disconnect(uri, mCallback, "done");
        assertFalse(isRunning(Remote.class));
        assertEquals(0, mClient.getConnectionCount());
        assertEquals(0, connection.getRegisteredCallbackCount());
    }


    /**
     * Multiple attempts to connect to the same service has no effect
     */
    @Test
    public void testConnect_repeat() {
        // connect
        final String uri = "alexatest:simple:10";
        ClientConnection connection = createTestConnect(uri, mCallback);
        assertEquals(1, mClient.getConnectionCount());
        assertEquals(1, connection.getRegisteredCallbackCount());

        // verify success
        assertOnLatch(mCallback.lOnCon, "Service never connected");
        assertEquals(uri, mCallback.mExtensionURI);
        assertTrue(isRunning(Simple.class));
        int hash = connection.hashCode();

        // repeat connect
        connection = (ClientConnection) mClient.connect(uri, mCallback);
        assertEquals(hash, connection.hashCode());
        assertTrue(isRunning(Simple.class));
        assertEquals(1, mClient.getConnectionCount());
        assertEquals(1, connection.getRegisteredCallbackCount());

        // disconnect
        mClient.disconnect(uri, mCallback, "done");
        assertFalse(isRunning(Simple.class));
        assertEquals(0, mClient.getConnectionCount());
        assertEquals(0, connection.getRegisteredCallbackCount());
    }


    /**
     * Multiple attempts to connect to the same service has no effect
     */
    @Test
    public void testConnect_multipleContext() {
        // connect
        final String uri = "alexatest:simple:10";
        ClientConnection connection = createTestConnect(uri, mCallback);
        assertEquals(1, mClient.getConnectionCount());
        assertEquals(1, connection.getRegisteredCallbackCount());

        // verify success
        assertOnLatch(mCallback.lOnCon, "Service never connected");
        assertEquals(uri, mCallback.mExtensionURI);
        assertTrue(isRunning(Simple.class));

        // repeat connect with different context
        ExtensionMultiplexClient client2 = new ExtensionMultiplexClient(InstrumentationRegistry::getContext);
        ClientConnection connection2 = (ClientConnection) client2.connect(uri, mCallback);
        assertTrue(isRunning(Simple.class));
        assertEquals(1, client2.getConnectionCount());
        assertEquals(1, connection2.getRegisteredCallbackCount());

        // disconnect
        mClient.disconnect(uri, mCallback, "done");
        client2.disconnect(uri, mCallback, "done");
        assertFalse(isRunning(Simple.class));
        assertEquals(0, mClient.getConnectionCount());
        assertEquals(0, client2.getConnectionCount());
        assertEquals(0, connection.getRegisteredCallbackCount());
        assertEquals(0, connection2.getRegisteredCallbackCount());
        assertEquals(0, mClient.getBinder().getBindingCount());
        assertEquals(0, client2.getBinder().getBindingCount());
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

        // connect
        String uri10 = "alexatest:multi:10";
        ClientConnection connection10 = createTestConnect(uri10, mCallback);
        assertEquals(1, mClient.getConnectionCount());
        assertEquals(1, connection10.getRegisteredCallbackCount());

        // verify success
        assertOnLatch(mCallback.lOnCon, "Service never connected");
        assertEquals(uri10, mCallback.mExtensionURI);
        assertTrue(isRunning(Multi.class));
        assertEquals(1, mClient.getConnectionCount());
        assertEquals(1, connection10.getRegisteredCallbackCount());

        // repeat connect with alternate URI
        mCallback.latch();
        String uri20 = "alexatest:multi:20";
        ClientConnection connection20 = createTestConnect(uri20, mCallback);
        assertNotNull(connection20);

        // verify success
        assertOnLatch(mCallback.lOnCon, "Service never connected");
        assertEquals(uri20, mCallback.mExtensionURI);
        assertTrue(isRunning(Multi.class));
        assertEquals(2, mClient.getConnectionCount());
        assertEquals(1, connection20.getRegisteredCallbackCount());

        // disconnect both connections, service stays running until both are disconnected
        mClient.disconnect(uri10, mCallback, "done");
        assertTrue(isRunning(Multi.class));
        assertEquals(1, mClient.getConnectionCount());

        mClient.disconnect(uri20, mCallback, "done");
        assertFalse(isRunning(Multi.class));
        assertEquals(0, mClient.getConnectionCount());
    }


    /**
     * Multiple callbacks succeed for single connection same URI.
     * The service should only shutdown after all URI are disconnected.
     */
    @Test
    public void testConnect_multiplexConnection() {

        // connect
        String uri = "alexatest:simple:10";
        ClientConnection connection10 = createTestConnect(uri, mCallback);
        assertEquals(1, mClient.getConnectionCount());
        assertEquals(1, connection10.getRegisteredCallbackCount());

        // verify success
        assertOnLatch(mCallback.lOnCon, "Service never connected");
        assertEquals(uri, mCallback.mExtensionURI);
        assertTrue(isRunning(Simple.class));
        assertEquals(1, mClient.getConnectionCount());
        assertEquals(1, connection10.getRegisteredCallbackCount());

        // repeat connect
        ClientTestCallback callback2 = new ClientTestCallback();
        ClientConnection connection20 = createTestConnect(uri, callback2);

        // verify multiple callbacks on the same connection
        assertOnLatch(mCallback.lOnCon, "Service never connected");
        assertEquals(uri, mCallback.mExtensionURI);
        assertTrue(isRunning(Simple.class));
        assertEquals(1, mClient.getConnectionCount());
        assertEquals(2, connection20.getRegisteredCallbackCount());

        // disconnect both connections, service stays running until both are disconnected
        mClient.disconnect(uri, mCallback, "done");
        assertTrue(isRunning(Simple.class));
        assertEquals(1, mClient.getConnectionCount());
        assertEquals(1, connection10.getRegisteredCallbackCount());

        mClient.disconnect(uri, callback2, "done");
        assertFalse(isRunning(Simple.class));
        assertEquals(0, mClient.getConnectionCount());
        assertEquals(0, connection20.getRegisteredCallbackCount());
    }


    /**
     * The IPC handshake cannot be completed due to a communication error with the service.
     * (Service returns a null binder)
     */
    @Test
    public void testConnect_failHandshakeCom() {
        final String uri = "alexatest:failcom:10";
        ClientConnection connection = createTestConnect(uri, mCallback);

        // verify fail
        assertOnLatch(mCallback.lConFail, "Service fail expected");
        assertEquals(uri, mCallback.mExtensionURI);
        assertEquals(mCallback.FAIL_HANDSHAKE, mCallback.mErrorCode);

        // The connection should be cleaned up if it cannot handshake
        assertEquals(0, connection.getRegisteredCallbackCount());
        assertEquals(0, mClient.getConnectionCount());
        assertFalse(isRunning(Latch.class));
    }


    /**
     * The IPC handshake cannot be completed due to a communication error with the service.
     * (Service returns a null binder)
     */
    @Test
    @Ignore
    public void testConnect_failHandshakeDied() {
        final String uri = "alexatest:faildied:10";
        ActivityDescriptor activity = new ActivityDescriptor(uri, new SessionDescriptor("session"), "activity");
        ClientConnection connection = createTestConnect(uri, mCallback);

        // verify successful connection
        assertOnLatch(mCallback.lOnCon, "Service fail expected");
        assertEquals(uri, mCallback.mExtensionURI);
        assertEquals(1, mClient.getConnectionCount());
        assertEquals(1, connection.getRegisteredCallbackCount());
        assertTrue(isRunning(FailDied.class));

        // Send a message
        try {
            connection.send(mCallback, activity, "APL");
        } catch (RemoteException e) {
            fail(e.getMessage());
        }
        assertOnLatch(connection.mBindingFailTest, "expected closed");
        assertOnLatch(mCallback.lConFail, "expected disconnect");
        assertEquals(mCallback.FAIL_SERVER_DIED, mCallback.mErrorCode);

        // cleanup
        mClient.disconnect(uri, mCallback, "disconnect");
        assertEquals(0, mClient.getConnectionCount());
        assertEquals(0, connection.getRegisteredCallbackCount());
        assertFalse(isRunning(FailDied.class));
    }


    /**
     * The IPC handshake cannot be completed due the service rejecting the connection.
     */
//    @Test
    public void testConnect_failHandshakeReject() {
        // TODO Permissions
//        final String uri = "alexatest:mock:10";
//        ClientConnection connection = (ClientConnection) mClient.connect(uri, mCallback);
//        assertNotNull(connection);
//
//        // verify fail
//        assertOnLatch(mCallback.fLatch, "Service fail expected");
//        assertEquals(uri, mCallback.mExtensionURI);
//        assertEquals(mCallback.FAIL_HANDSHAKE, mCallback.mErrorCode);
//
//        // The connection should be cleaned up if it cannot handshake
//        assertEquals(0, mClient.getConnectionCount());
//        assertEquals(0, connection.getRegisteredCallbackCount());
//
//        assertFalse(isRunning(TestService.Latch.class));
//        fail();
    }


    @Test
    @Ignore
    public void testSend() {
        final String uri = "alexatest:latch:10";
        ActivityDescriptor activity = new ActivityDescriptor(uri, new SessionDescriptor("session"), "activity");
        ClientConnection connection = createTestConnect(uri, mCallback);

        // verify successful connection
        assertOnLatch(mCallback.lOnCon, "Service fail expected");
        assertOnLatch(lSvcOnCon, "Service connect expected");
        assertEquals(uri, mCallback.mExtensionURI);
        assertEquals(1, mClient.getConnectionCount());
        assertEquals(1, connection.getRegisteredCallbackCount());
        assertTrue(isRunning(Latch.class));
        assertNotNull(latchService);

        // Send a message
        try {
            connection.send(mCallback, activity, "APL");
        } catch (RemoteException e) {
            fail(e.getMessage());
        }
        assertOnLatch(lSvcReceive, "Expected Receive");
        assertNotNull(latchService);
        assertNotNull(latchService.mReceived);
        assertEquals("APL", latchService.mReceived);

        // cleanup
        mClient.disconnect(uri, mCallback, "disconnect");
        assertEquals(0, mClient.getConnectionCount());
        assertEquals(0, connection.getRegisteredCallbackCount());
        assertFalse(isRunning(Latch.class));
    }


    @Test
    public void testSend_null() {
        final String uri = "alexatest:simple:10";
        ActivityDescriptor activity = new ActivityDescriptor(uri, new SessionDescriptor("session"), "activity");
        ClientConnection connection = createTestConnect(uri, mCallback);

        // verify successful connection
        assertOnLatch(mCallback.lOnCon, "connect expected");

        // Send a message
        boolean failed = false;
        try {
            connection.send(mCallback, activity, null);
        } catch (RemoteException e) {
            failed = true;
        }
        assert (failed);

        // cleanup
        mClient.disconnect(uri, mCallback, "disconnect");
        assertEquals(0, mClient.getConnectionCount());
        assertEquals(0, connection.getRegisteredCallbackCount());
        assertFalse(isRunning(Latch.class));
    }


    @Test
    public void testLegacyFocusLost() {
        final String uri = "alexatest:latch:10";
        ClientConnection connection = createTestConnect(uri, mCallback);

        // verify successful connection
        assertOnLatch(mCallback.lOnCon, "Service fail expected");
        assertOnLatch(lSvcOnCon, "Service connect expected");
        assertEquals(uri, mCallback.mExtensionURI);
        assertEquals(1, mClient.getConnectionCount());
        assertEquals(1, connection.getRegisteredCallbackCount());
        assertTrue(isRunning(Latch.class));
        assertNotNull(latchService);

        // Send a message
        try {
            connection.setFocusLost(mCallback);
        } catch (RemoteException e) {
            fail(e.getMessage());
        }
        assertOnLatch(lSvcOnFocusLost, "Expected focus lost");
        assertNotNull(latchService);

        // cleanup
        mClient.disconnect(uri, mCallback, "disconnect");
        assertEquals(0, mClient.getConnectionCount());
        assertEquals(0, connection.getRegisteredCallbackCount());
        assertFalse(isRunning(Latch.class));
    }


    @Test
    public void testLegacyFocusGained() {
        final String uri = "alexatest:latch:10";
        ClientConnection connection = createTestConnect(uri, mCallback);

        // verify successful connection
        assertOnLatch(mCallback.lOnCon, "Service fail expected");
        assertOnLatch(lSvcOnCon, "Service connect expected");
        assertEquals(uri, mCallback.mExtensionURI);
        assertEquals(1, mClient.getConnectionCount());
        assertEquals(1, connection.getRegisteredCallbackCount());
        assertTrue(isRunning(Latch.class));
        assertNotNull(latchService);

        // Send a message
        try {
            connection.setFocusGain(mCallback);
        } catch (RemoteException e) {
            fail(e.getMessage());
        }
        assertOnLatch(lSvcOnFocusGain, "Expected focus gain");
        assertNotNull(latchService);

        // cleanup
        mClient.disconnect(uri, mCallback, "disconnect");
        assertEquals(0, mClient.getConnectionCount());
        assertEquals(0, connection.getRegisteredCallbackCount());
        assertFalse(isRunning(Latch.class));
    }


    @Test
    public void testPause() {
        final String uri = "alexatest:latch:10";
        ClientConnection connection = createTestConnect(uri, mCallback);

        // verify successful connection
        assertOnLatch(mCallback.lOnCon, "Service fail expected");
        assertOnLatch(lSvcOnCon, "Service connect expected");
        assertEquals(uri, mCallback.mExtensionURI);
        assertEquals(1, mClient.getConnectionCount());
        assertEquals(1, connection.getRegisteredCallbackCount());
        assertTrue(isRunning(Latch.class));
        assertNotNull(latchService);

        // Send a message
        try {
            connection.pause(mCallback);
        } catch (RemoteException e) {
            fail(e.getMessage());
        }
        assertOnLatch(lSvcOnPause, "Expected Receive");
        assertNotNull(latchService);

        // cleanup
        mClient.disconnect(uri, mCallback, "disconnect");
        assertEquals(0, mClient.getConnectionCount());
        assertEquals(0, connection.getRegisteredCallbackCount());
        assertFalse(isRunning(Latch.class));
    }


    @Test
    public void testResume() {
        final String uri = "alexatest:latch:10";
        ClientConnection connection = createTestConnect(uri, mCallback);

        // verify successful connection
        assertOnLatch(mCallback.lOnCon, "Service fail expected");
        assertOnLatch(lSvcOnCon, "Service connect expected");
        assertEquals(uri, mCallback.mExtensionURI);
        assertEquals(1, mClient.getConnectionCount());
        assertEquals(1, connection.getRegisteredCallbackCount());
        assertTrue(isRunning(Latch.class));
        assertNotNull(latchService);

        // Send a message
        try {
            connection.resume(mCallback);
        } catch (RemoteException e) {
            fail(e.getMessage());
        }
        assertOnLatch(lSvcOnResume, "Expected Receive");
        assertNotNull(latchService);

        // cleanup
        mClient.disconnect(uri, mCallback, "disconnect");
        assertEquals(0, mClient.getConnectionCount());
        assertEquals(0, connection.getRegisteredCallbackCount());
        assertFalse(isRunning(Latch.class));
    }


    @Test
    public void testReceive_simple() {
        final String uri = "alexatest:latch:10";
        ClientConnection connection = createTestConnect(uri, mCallback);

        // verify successful connection
        assertOnLatch(mCallback.lOnCon, "Service success expected");
        assertOnLatch(lSvcOnCon, "Service connect expected");
        assertEquals(uri, mCallback.mExtensionURI);
        assertEquals(1, mClient.getConnectionCount());
        assertEquals(1, connection.getRegisteredCallbackCount());
        assertTrue(isRunning(Latch.class));
        assertNotNull(latchService);

        // Send a message from the service
        latchService.sendMessage(latchService.mConnectionID, mCallback.uuid, "APL");

        assertOnLatch(mCallback.lReceive, "Expected Receive");
        assertNotNull(mCallback.mReceived);
        assertEquals("APL", mCallback.mReceived);

        // cleanup
        mClient.disconnect(uri, mCallback, "disconnect");
        assertEquals(0, mClient.getConnectionCount());
        assertEquals(0, connection.getRegisteredCallbackCount());
        assertFalse(isRunning(Latch.class));
    }


    @Test
    public void testReceive_broadcast() {
        final String uri = "alexatest:latch:10";
        ClientConnection connection = createTestConnect(uri, mCallback);

        // verify successful connection
        assertOnLatch(mCallback.lOnCon, "Service success expected");
        assertOnLatch(lSvcOnCon, "Service connect expected");
        assertEquals(uri, mCallback.mExtensionURI);
        assertEquals(1, mClient.getConnectionCount());
        assertEquals(1, connection.getRegisteredCallbackCount());
        assertTrue(isRunning(Latch.class));
        assertNotNull(latchService);

        // Send a message from the service
        latchService.sendBroadcast("APL");

        assertOnLatch(mCallback.lReceive, "Expected Receive");
        assertNotNull(mCallback.mReceived);
        assertEquals("APL", mCallback.mReceived);

        // The connection should be cleaned up if it cannot handshake
        mClient.disconnect(uri, mCallback, "disconnect");
        assertEquals(0, mClient.getConnectionCount());
        assertEquals(0, connection.getRegisteredCallbackCount());
        assertFalse(isRunning(Latch.class));
    }


    @Test
    public void testReceive_messageFailure() {
        final String uri = "alexatest:latch:10";
        ClientConnection connection = createTestConnect(uri, mCallback);

        // verify successful connection
        assertOnLatch(mCallback.lOnCon, "Service success expected");
        assertOnLatch(lSvcOnCon, "Service connect expected");
        assertEquals(uri, mCallback.mExtensionURI);
        assertEquals(1, mClient.getConnectionCount());
        assertEquals(1, connection.getRegisteredCallbackCount());
        assertTrue(isRunning(Latch.class));
        assertNotNull(latchService);

        // Send a message from the service
        latchService.sendFailure(latchService.mConnectionID, mCallback.uuid,
                -200, "oops", "failedPayload");

        assertOnLatch(mCallback.lMsgFail, "Expected Receive Failure");
        assertEquals(uri, mCallback.mExtensionURI);
        assertEquals(-200, mCallback.mErrorCode);

        // The connection should be cleaned up if it cannot handshake
        mClient.disconnect(uri, mCallback, "disconnect");
        assertEquals(0, mClient.getConnectionCount());
        assertEquals(0, connection.getRegisteredCallbackCount());
        assertFalse(isRunning(Latch.class));
    }


    /**
     * Basic connect/disconnect
     */
    @Test
    public void testConnect_withConfig() {
        final String uri = "alexatest:latch:10";
        final String config = "config";
        ClientConnection connection = createTestConnect(uri, config, mCallback);

        // verify successful connection
        assertOnLatch(mCallback.lOnCon, "Service fail expected");
        assertOnLatch(lSvcOnCon, "Service connect expected");
        assertEquals(uri, mCallback.mExtensionURI);
        assertEquals(1, mClient.getConnectionCount());
        assertEquals(1, connection.getRegisteredCallbackCount());
        assertTrue(isRunning(Latch.class));
        assertNotNull(latchService);
        assertEquals(config, latchService.mConfiguration);

        // cleanup
        mClient.disconnect(uri, mCallback, "disconnect");
        assertEquals(0, mClient.getConnectionCount());
        assertEquals(0, connection.getRegisteredCallbackCount());
        assertFalse(isRunning(Latch.class));
    }


    /**
     * Disconnect notifies service
     */
    public void testDisconnect_onExit() {

        final String uri = "alexatest:latch:10";
        ClientConnection connection = createTestConnect(uri, mCallback);

        // verify successful connection
        assertOnLatch(mCallback.lOnCon, "Service success expected");
        assertOnLatch(lSvcOnCon, "Service connect expected");
        assertEquals(uri, mCallback.mExtensionURI);
        assertEquals(1, mClient.getConnectionCount());
        assertEquals(1, connection.getRegisteredCallbackCount());
        assertTrue(isRunning(Latch.class));
        assertNotNull(latchService);

        // disconnect, verifying that the service gets a client exit and
        // a connection close
        mClient.disconnect(uri, mCallback, "disconnect");
        assertOnLatch(TestService.lSvcOnExit, "Expected Client Exit");
        assertOnLatch(TestService.lSvcConClosed, "Expected Server Close");
    }

    /**
     * No receive of messages after disconnect.
     */
    @Test
    public void testDisconnect_noReceiveAfterDisconnect() {

        final String uri = "alexatest:latch:10";
        ClientConnection connection = createTestConnect(uri, mCallback);

        // verify successful connection
        assertOnLatch(mCallback.lOnCon, "Service success expected");
        assertOnLatch(lSvcOnCon, "Service connect expected");
        assertEquals(uri, mCallback.mExtensionURI);
        assertEquals(1, mClient.getConnectionCount());
        assertEquals(1, connection.getRegisteredCallbackCount());
        assertTrue(isRunning(Latch.class));
        assertNotNull(latchService);

        // disconnect, verifying that the service gets a connection close indicates
        // the client should not receive messages
        mClient.disconnect(uri, mCallback, "disconnect");
        assertOnLatch(TestService.lSvcConClosed, "Expected Server Close");

        // reset
        mCallback.latch();

        // Now send all possible server messages and verify no receive
        // because the connection is closed messages are sent directly to the client via the
        // aidl interface. (bypassing ipc_
        try {
            connection.L2_connectionAccept();
            connection.L2_connectionClosed("test");
            connection.L2_connectionReject(100, "test");
            connection.L2_receive(mCallback.getID(), "test");
            connection.L2_messageFailure(mCallback.getID(), 100, "test", "test");
            connection.L2_receiveBroadcast("test");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        assertNoLatch(mCallback.lOnCon, "expected NO connect");
        assertNoLatch(mCallback.lConClose, "expected NO close");
        assertNoLatch(mCallback.lConFail, "expected NO con fail");
        assertNoLatch(mCallback.lReceive, "expected NO receive");
        assertNoLatch(mCallback.lMsgFail, "expected NO message fail");

        // cleanup
        assertEquals(0, mClient.getConnectionCount());
        assertEquals(0, connection.getRegisteredCallbackCount());
        assertFalse(isRunning(Latch.class));
    }

    /**
     * Test that kill correctly cleans up connections.
     */
    @Test
    public void testKill_cleanUp() {
        final String uri = "alexatest:simple:10";

        ClientTestCallback[] connectionResults
                = new ClientTestCallback[10];

        for (int i = 0; i < connectionResults.length; i++) {
            connectionResults[i] = new ClientTestCallback();
            ClientConnection con = (ClientConnection) mClient.connect(uri, connectionResults[i]);
            assertNotNull(con);
            assertEquals(i + 1, con.getRegisteredCallbackCount());
        }
        assertEquals(1, mClient.getConnectionCount());

        // now kill the connection
        mClient.kill();
        assertTrue(mClient.isKilled());

        assertEquals(0, mClient.getBinder().getBindingCount());
        assertEquals(0, mClient.getConnectionCount());
    }


    /**
     * Test that kill prevents the connection from reuse.  This is required because the death
     * monitor for the connections is disabled after kill.
     */
    @Test
    public void testKill_noReuse() {
        final String uri = "alexatest:simple:10";

        ClientTestCallback connectionResults = new ClientTestCallback();
        mClient.connect(uri, connectionResults);
        assertEquals(1, mClient.getBinder().getBindingCount());
        assertEquals(1, mClient.getConnectionCount());

        // now kill the connection
        mClient.kill();
        assertTrue(mClient.isKilled());

        assertEquals(0, mClient.getBinder().getBindingCount());
        assertEquals(0, mClient.getConnectionCount());

        // try to reuse and expect illegal state
        boolean ise = false;
        try {
            mClient.connect(uri, connectionResults);
        } catch (IllegalStateException e) {
            ise = true;
        }
        assertTrue(ise);

        assertEquals(0, mClient.getBinder().getBindingCount());
        assertEquals(0, mClient.getConnectionCount());
    }

    @Test
    @Ignore
    public void testLegacySend() {
        final String uri = "alexatest:latch:10";
        ClientConnection connection = createTestConnect(uri, mCallback);

        // verify successful connection
        assertOnLatch(mCallback.lOnCon, "Service fail expected");
        assertOnLatch(lSvcOnCon, "Service connect expected");
        assertEquals(uri, mCallback.mExtensionURI);
        assertEquals(1, mClient.getConnectionCount());
        assertEquals(1, connection.getRegisteredCallbackCount());
        assertTrue(isRunning(Latch.class));
        assertNotNull(latchService);

        // Send a message
        try {
            connection.send(mCallback, "APL");
        } catch (RemoteException e) {
            fail(e.getMessage());
        }
        assertOnLatch(lSvcReceive, "Expected Receive");
        assertNotNull(latchService);
        assertNotNull(latchService.mReceived);
        assertEquals("APL", latchService.mReceived);

        // cleanup
        mClient.disconnect(uri, mCallback, "disconnect");
        assertEquals(0, mClient.getConnectionCount());
        assertEquals(0, connection.getRegisteredCallbackCount());
        assertFalse(isRunning(Latch.class));
    }

    @Test
    public void testLegacyResourceAvailable() {
        final String uri = "alexatest:latch:10";
        ClientConnection connection = createTestConnect(uri, mCallback);

        // verify successful connection
        assertOnLatch(mCallback.lOnCon, "Service fail expected");
        assertOnLatch(lSvcOnCon, "Service connect expected");
        assertEquals(uri, mCallback.mExtensionURI);
        assertEquals(1, mClient.getConnectionCount());
        assertEquals(1, connection.getRegisteredCallbackCount());
        assertTrue(isRunning(Latch.class));
        assertNotNull(latchService);

        // Send a message
        try {
            connection.resourceAvailable(
                    mCallback,
                    new Surface(new SurfaceTexture(true)),
                    new Rect(0, 0, 100, 100),
                    "resourceId");

        } catch (RemoteException e) {
            fail(e.getMessage());
        }
        assertOnLatch(lSvcRscAvailable, "Expected resource available");
        assertNotNull(latchService);

        // cleanup
        mClient.disconnect(uri, mCallback, "disconnect");
        assertEquals(0, mClient.getConnectionCount());
        assertEquals(0, connection.getRegisteredCallbackCount());
        assertFalse(isRunning(Latch.class));
    }

    @Test
    public void testLegacyResourceUnavailable() {
        final String uri = "alexatest:latch:10";
        ClientConnection connection = createTestConnect(uri, mCallback);

        // verify successful connection
        assertOnLatch(mCallback.lOnCon, "Service fail expected");
        assertOnLatch(lSvcOnCon, "Service connect expected");
        assertEquals(uri, mCallback.mExtensionURI);
        assertEquals(1, mClient.getConnectionCount());
        assertEquals(1, connection.getRegisteredCallbackCount());
        assertTrue(isRunning(Latch.class));
        assertNotNull(latchService);

        // Send a message
        try {
            connection.resourceUnavailable(
                    mCallback,
                    "resourceId");

        } catch (RemoteException e) {
            fail(e.getMessage());
        }
        assertOnLatch(lSvcRscUnavailable, "Expected resource unavailable");
        assertNotNull(latchService);

        // cleanup
        mClient.disconnect(uri, mCallback, "disconnect");
        assertEquals(0, mClient.getConnectionCount());
        assertEquals(0, connection.getRegisteredCallbackCount());
        assertFalse(isRunning(Latch.class));
    }
}
