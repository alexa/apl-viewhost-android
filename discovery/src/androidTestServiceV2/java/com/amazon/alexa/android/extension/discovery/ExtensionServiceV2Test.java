/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.alexa.android.extension.discovery;

import static com.amazon.alexa.android.extension.discovery.ExtensionMultiplexServiceV2.ConnectionID;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import android.content.Intent;
import android.graphics.Rect;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Pair;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amazon.alexa.android.extension.discovery.L2_IRemoteClient;
import com.amazon.alexaext.ActivityDescriptor;
import com.amazon.alexaext.SessionDescriptor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
public class ExtensionServiceV2Test {
    // Identifier generated client-side
    final static int CLIENT_PROVIDED_ID = 123;

    // Identifier provided by a message-sender to associate responses with requests
    final static int ROUTING_ID = 456;

    private ExampleClient mExampleClient;
    private ExampleService mExampleService;
    private ExtensionMultiplexServiceV2.ServiceConnection mServiceConnection;
    private SessionDescriptor mSession;
    private ActivityDescriptor mActivity;

    @Before
    public void setUp() {
        mExampleClient = new ExampleClient();
        mExampleService = new ExampleService();
        mServiceConnection = (ExtensionMultiplexServiceV2.ServiceConnection)mExampleService.onBind(new Intent());
        mSession = new SessionDescriptor("mySessionId");
        mActivity = new ActivityDescriptor("myUri", mSession, "myActivityId");
    }

    @Test
    public void testClientConnection() throws RemoteException {
        mServiceConnection.L2_connect(mExampleClient, "configuration");
        assertOnLatch(mExampleService.mConnectLatch, "Service notified of client connection");
        assertOnLatch(mExampleClient.mAcceptLatch, "Client notified of server acceptance");
        assertOnLatch(mExampleClient.mLinkToDeathLatch, "Service subscribed to client death");
    }

    @Test
    public void testServiceToClientMessage() throws RemoteException {
        // Client connects to service
        mServiceConnection.L2_connect(mExampleClient, "configuration");
        assertOnLatch(mExampleClient.mAcceptLatch, "Client notified of server acceptance");

        // Client notifies service about registration
        mServiceConnection.L2_onRegistered(CLIENT_PROVIDED_ID, ROUTING_ID, mActivity);
        assertOnLatch(mExampleService.mRegisteredLatch, "Server registered");
        assertEquals(mActivity, mExampleService.mActivity);
        assertEquals(CLIENT_PROVIDED_ID, mExampleService.mOnConnectConnectionID.connectionID);
        assertEquals(CLIENT_PROVIDED_ID, mExampleService.mOnRegisterConnectionID.connectionID);

        // Service sends a message to the client
        mServiceConnection.send(mExampleService.mOnConnectConnectionID, ROUTING_ID, mActivity, "fromService");
        assertOnLatch(mExampleClient.mReceiveLatch, "Got a message");
        assertEquals(1, mExampleClient.mReceivedMessages.size());

        assertEquals(ROUTING_ID, (long)mExampleClient.mReceivedMessages.get(0).first);
        assertEquals("fromService", mExampleClient.mReceivedMessages.get(0).second);
    }

    @Test
    public void testClientToServiceMessage() throws RemoteException {
        // Client connects to service
        mServiceConnection.L2_connect(mExampleClient, "configuration");
        assertOnLatch(mExampleClient.mAcceptLatch, "Client notified of server acceptance");

        // Client notifies service about registration
        mServiceConnection.L2_onRegistered(CLIENT_PROVIDED_ID, ROUTING_ID, mActivity);
        assertOnLatch(mExampleService.mRegisteredLatch, "Server registered");
        assertEquals(mActivity, mExampleService.mActivity);

        // Client sends a message to the server
        mServiceConnection.L2_receive(CLIENT_PROVIDED_ID, ROUTING_ID, mActivity, "fromClient");

        // Service echoes a (modified) message back to the client
        assertOnLatch(mExampleClient.mReceiveLatch, "Got back the echoed message");
        assertEquals(1, mExampleClient.mReceivedMessages.size());
        assertEquals(ROUTING_ID, (long)mExampleClient.mReceivedMessages.get(0).first);
        assertEquals("fromClient:echo", mExampleClient.mReceivedMessages.get(0).second);
    }

    @Test
    public void testServiceNotifiedAboutClientLifecycle() throws TimeoutException, RemoteException {
        // Client connects to service
        mServiceConnection.L2_connect(mExampleClient, "configuration");
        assertOnLatch(mExampleClient.mAcceptLatch, "Client notified of server acceptance");

        // Client notifies service about registration
        mServiceConnection.L2_onRegistered(CLIENT_PROVIDED_ID, ROUTING_ID, mActivity);
        assertOnLatch(mExampleService.mRegisteredLatch, "Server registered");
        assertEquals(mActivity, mExampleService.mActivity);

        // Client sends various lifecycle notification messages
        mServiceConnection.L2_onSessionStarted(CLIENT_PROVIDED_ID, ROUTING_ID, mSession);
        mServiceConnection.L2_onForeground(CLIENT_PROVIDED_ID, ROUTING_ID, mActivity);
        mServiceConnection.L2_onBackground(CLIENT_PROVIDED_ID, ROUTING_ID, mActivity);
        mServiceConnection.L2_onHidden(CLIENT_PROVIDED_ID, ROUTING_ID, mActivity);
        mServiceConnection.L2_onSessionEnded(CLIENT_PROVIDED_ID, ROUTING_ID, mSession);
        mServiceConnection.L2_onUnregistered(CLIENT_PROVIDED_ID, ROUTING_ID, mActivity);

        // Wait for the server to have processed the last message (unregister)
        assertOnLatch(mExampleService.mUnregisteredLatch, "Server unregistered");

        String[] expected = {
                "SESSION_STARTED",
                "FOREGROUND",
                "BACKGROUND",
                "HIDDEN",
                "SESSION_ENDED"
        };
        assertArrayEquals(expected, mExampleService.mLifecycleNotifications.toArray());
    }

    @Test
    public void testDestroyedServiceNotifiesClient() throws TimeoutException, RemoteException {
        mServiceConnection.L2_connect(mExampleClient, "configuration");
        assertOnLatch(mExampleClient.mAcceptLatch, "Client notified of server acceptance");

        mExampleService.onDestroy();
        assertOnLatch(mExampleClient.mConnectionClosedLatch, "Client notified of connection close");
        assertEquals("Service Destroyed: example:extension", mExampleClient.mConnectionClosedMessage);
    }

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

    public class ExampleService extends AbstractExtensionServiceV2 {
        public ConnectionID mOnConnectConnectionID;
        public ConnectionID mOnRegisterConnectionID;
        public SessionDescriptor mSession;
        public ActivityDescriptor mActivity;
        public int mClientId;
        public ArrayList<String> mLifecycleNotifications = new ArrayList<String>();

        @Override
        public IBinder onBind(final Intent intent) {
            return super.onBind(intent);
        }

        @Override
        public boolean onUnbind(final Intent intent) {
            return super.onUnbind(intent);
        }

        @Override
        public void onCreate() {}

        @Override
        public String getName() {
            return "example:extension";
        }

        public CountDownLatch mConnectLatch = new CountDownLatch(1);

        @Override
        public void onConnect(ConnectionID connectionID, String configuration) {
            mOnConnectConnectionID = connectionID;
            mConnectLatch.countDown();
        }

        @Override
        public void onMessage(ConnectionID connectionID, int clientId, ActivityDescriptor activity, String message) {
            // Echo back
            sendMessage(connectionID, clientId, activity, message + ":echo");
        }

        public CountDownLatch mConnectionClosedLatch = new CountDownLatch(1);

        @Override
        public void onConnectionClosed(ConnectionID connectionID, String message) {
            mConnectionClosedLatch.countDown();
        }

        public CountDownLatch mRegisteredLatch = new CountDownLatch(1);;

        @Override
        public void onRegistered(ConnectionID connectionID, int clientId, ActivityDescriptor activity) {
            mOnRegisterConnectionID = connectionID;
            mClientId = clientId;
            mActivity = activity;
            mRegisteredLatch.countDown();
        }

        public CountDownLatch mUnregisteredLatch = new CountDownLatch(1);;

        @Override
        public void onUnregistered(ConnectionID connectionID, int clientId, ActivityDescriptor activity) {
            mActivity = null;
            mUnregisteredLatch.countDown();
        }

        @Override
        public void onSessionStarted(ConnectionID connectionID, int clientId, SessionDescriptor session) {
            mSession = session;
            mLifecycleNotifications.add("SESSION_STARTED");
        }

        @Override
        public void onSessionEnded(ConnectionID connectionID, int clientId, SessionDescriptor session) {
            mLifecycleNotifications.add("SESSION_ENDED");
            mSession = null;
        }

        @Override
        public void onForeground(ConnectionID connectionID, int clientId, ActivityDescriptor activity) {
            mLifecycleNotifications.add("FOREGROUND");
        }

        @Override
        public void onBackground(ConnectionID connectionID, int clientId, ActivityDescriptor activity) {
            mLifecycleNotifications.add("BACKGROUND");
        }

        @Override
        public void onHidden(ConnectionID connectionID, int clientId, ActivityDescriptor activity) {
            mLifecycleNotifications.add("HIDDEN");
        }
    }

    public class ExampleClient implements L2_IRemoteClient, IBinder {
        public int mRoutingID;
        public SessionDescriptor mSession;
        public ActivityDescriptor mActivity;
        public String mConnectionClosedMessage;

        @Override
        public boolean L2_supportsTransactVersion(int expectedVersion) throws RemoteException {
            return true;
        }

        @Override
        public int L2_connectionID() throws RemoteException {
            return CLIENT_PROVIDED_ID;
        }

        public CountDownLatch mAcceptLatch = new CountDownLatch(1);

        @Override
        public void L2_connectionAccept() throws RemoteException {
            mAcceptLatch.countDown();
        }

        public CountDownLatch mRejectLatch = new CountDownLatch(1);

        @Override
        public void L2_connectionReject(int errorCode, String error) throws RemoteException {
            mRejectLatch.countDown();
        }

        public CountDownLatch mConnectionClosedLatch = new CountDownLatch(1);

        @Override
        public void L2_connectionClosed(String message) throws RemoteException {
            mConnectionClosedMessage = message;
            mConnectionClosedLatch.countDown();
        }

        /**
         * BEGIN: Legacy V1 methods
         */
        @Override
        public void L2_receive(int routingID, String message) throws RemoteException {}
        @Override
        public void L2_receiveBroadcast(String message) throws RemoteException {}
        @Override
        public void L2_send(int routingID, String message) throws RemoteException {}
        @Override
        public void L2_setFocusLost(int routingID) throws RemoteException {}
        @Override
        public void L2_setFocusGained(int routingID) throws RemoteException {}
        @Override
        public void L2_pause(int routingID) throws RemoteException {}
        @Override
        public void L2_resume(int routingID) throws RemoteException {}
        @Override
        public void L2_messageFailure(int routingID, int errorCode, String error, String message) throws RemoteException {}
        @Override
        public void L2_onRequestResource(int routingID, String resourceId) throws RemoteException {}
        @Override
        public void L2_resourceAvailable(int routingID, Surface surface, Rect rect, String resourceID) throws RemoteException {}
        @Override
        public void L2_resourceUnavailable(int routingID, String resourceID) throws RemoteException {}
        /**
         * END: Legacy V1 methods
         */

        public ArrayList<Pair<Integer, String>> mReceivedMessages = new ArrayList<Pair<Integer, String>>();
        public CountDownLatch mReceiveLatch = new CountDownLatch(1);

        @Override
        public void L2_receiveV2(int routingID, ActivityDescriptor activity, String message) throws RemoteException {
            mRoutingID = routingID;
            mReceivedMessages.add(new Pair<Integer, String>(routingID, message));
            mReceiveLatch.countDown();
        }

        @Override
        public void L2_sendV2(int routingID, ActivityDescriptor activity, String message) throws RemoteException {
        }

        @Override
        public void L2_messageFailureV2(int routingID, ActivityDescriptor activity, int errorCode, String error, String message) throws RemoteException {
        }

        @Override
        public void L2_resourceAvailableV2(int routingID, ActivityDescriptor activity, Surface surface, Rect rect, String resourceID) throws RemoteException {
        }

        @Override
        public void L2_resourceUnavailableV2(int routingID, ActivityDescriptor activity, String resourceID) throws RemoteException {
        }

        CountDownLatch mRegisteredV2Latch = new CountDownLatch(1);

        @Override
        public void L2_onRegisteredV2(int routingID, ActivityDescriptor activity) throws RemoteException {
            mRoutingID = routingID;
            mActivity = activity;
            mRegisteredV2Latch.countDown();
        }

        @Override
        public void L2_onUnregisteredV2(int routingID, ActivityDescriptor activity) throws RemoteException {
        }

        @Override
        public void L2_onSessionStartedV2(int routingID, SessionDescriptor session) throws RemoteException {
        }

        @Override
        public void L2_onSessionEndedV2(int routingID, SessionDescriptor session) throws RemoteException {
        }

        @Override
        public void L2_onForegroundV2(int routingID, ActivityDescriptor activity) throws RemoteException {
        }

        @Override
        public void L2_onBackgroundV2(int routingID, ActivityDescriptor activity) throws RemoteException {
        }

        @Override
        public void L2_onHiddenV2(int routingID, ActivityDescriptor activity) throws RemoteException {
        }

        /**
         * BEGIN: IBinder methods
         */
        @Override
        public IBinder asBinder() {
            return this;
        }

        @Nullable
        @Override
        public String getInterfaceDescriptor() throws RemoteException {
            return null;
        }

        @Override
        public boolean pingBinder() {
            return false;
        }

        @Override
        public boolean isBinderAlive() {
            return true;
        }

        @Nullable
        @Override
        public IInterface queryLocalInterface(@NonNull String s) {
            return null;
        }

        @Override
        public void dump(@NonNull FileDescriptor fileDescriptor, @Nullable String[] strings) throws RemoteException {}

        @Override
        public void dumpAsync(@NonNull FileDescriptor fileDescriptor, @Nullable String[] strings) throws RemoteException {}

        @Override
        public boolean transact(int i, @NonNull Parcel parcel, @Nullable Parcel parcel1, int i1) throws RemoteException {
            return false;
        }

        public CountDownLatch mLinkToDeathLatch = new CountDownLatch(1);

        @Override
        public void linkToDeath(@NonNull DeathRecipient deathRecipient, int i) throws RemoteException {
            mLinkToDeathLatch.countDown();
        }

        @Override
        public boolean unlinkToDeath(@NonNull DeathRecipient deathRecipient, int i) {
            return false;
        }
        /**
         * END: IBinder methods
         */
    }
}
