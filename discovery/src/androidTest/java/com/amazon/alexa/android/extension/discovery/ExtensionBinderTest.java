/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.alexa.android.extension.discovery;

import android.app.ActivityManager;
import android.content.Context;
import android.os.IBinder;

import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amazon.alexa.android.extension.discovery.ExtensionBinder.ConnectionCallback;
import com.amazon.alexa.android.extension.discovery.test.TestService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.amazon.alexa.android.extension.discovery.ExtensionBinder.ConnectionCallback.FAIL_DISCONNECTED;
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
@SuppressWarnings({"ConstantConditions", "CheckStyle"})
@RunWith(AndroidJUnit4.class)
public class ExtensionBinderTest {

    private ExtensionBinder mDiscover = null;
    private DiscoveryCallback mCallback = null;
    private Context mAppContext;

    // Store results of binding callback
    @SuppressWarnings("CheckStyle")
    private static class DiscoveryCallback implements ConnectionCallback {

        private L2_IRemoteService mService;
        private int mErrorCode;
        private String mError;
        CountDownLatch lBindingSuccess;
        CountDownLatch lBindingFail;

        DiscoveryCallback() {
            latch();
        }

        @Override
        public void bindingSuccess(IBinder service) {
            mService = L2_IRemoteService.Stub.asInterface(service);
            lBindingSuccess.countDown();
        }

        @Override
        public void bindingFailure(int errorCode, String error) {
            mErrorCode = errorCode;
            mError = error;
            lBindingFail.countDown();
        }

        void latch() {
            lBindingSuccess = new CountDownLatch(1);
            lBindingFail = new CountDownLatch(1);
        }
    }


    // Allocate common test resources
    @Before
    public void doBefore() {
        mAppContext = InstrumentationRegistry.getTargetContext();
        mDiscover = new ExtensionBinder();
        mCallback = new DiscoveryCallback();
        assertNotNull(mDiscover);
    }

    // Free common test resources
    @After
    public void doAfter() {
        mDiscover.unbindAll(mAppContext);
        mAppContext = null;
        mDiscover = null;
        mCallback = null;
    }

    /**
     * Sanity test that test code didn't move to release.
     */
    @Test
    public void testAppContext() {

        // The application declared in the manifest is testOnly='true'
        assertEquals("The application for this Android module should only be exposed for testing.",
                "com.amazon.alexa.android.extension.discovery.test",
                mAppContext.getPackageName()
        );
    }


    /**
     * Invalid context parameter handled gracefully.
     */
    @Test
    public void testBind_badContext() {
        boolean attempted = mDiscover.bind(null, "alexaext:fake:10", mCallback);
        assertFalse("unhandled bad parameter", attempted);
    }

    /**
     * Invalid uri parameter handled gracefully.
     */
    @Test
    public void testBind_badURI() {
        boolean attempted = mDiscover.bind(mAppContext, null, mCallback);
        assertFalse("unhandled bad parameter", attempted);
    }

    /**
     * Invalid callback parameter handled gracefully.
     */
    @Test
    public void testBind_badCallback() {
        boolean attempted = mDiscover.bind(mAppContext, "alexaext:fake:10", null);
        assertFalse("unhandled bad parameter", attempted);
    }

    /**
     * Discover, bind, unbind from a simple service.
     */
    @Test
    public void testBind_simple() {
        boolean attempted = mDiscover.bind(mAppContext, "alexatest:simple:10", mCallback);
        assertTrue("Service bind failed attempt", attempted);
        assertOnLatch(mCallback.lBindingSuccess, "Expect Connect");
        assertNotNull(mCallback.mService);
        assertTrue(isRunning(TestService.Simple.class));
        assertEquals(1, mDiscover.getBindingCount());

        boolean unbind = mDiscover.unbind(mAppContext, "alexatest:simple:10");
        assertTrue(unbind);
        assertFalse(isRunning(TestService.Simple.class));
        assertEquals(0, mDiscover.getBindingCount());
    }

    /**
     * Discover, bind, unbind from a service that supports multiple extensions.
     */
    @Test
    public void testBind_multi() {
        boolean attempted = mDiscover.bind(mAppContext, "alexatest:multi:10", mCallback);
        assertTrue("Service bind failed attempt", attempted);
        assertOnLatch(mCallback.lBindingSuccess, "Expect Connect");
        assertNotNull(mCallback.mService);
        assertTrue(isRunning(TestService.Multi.class));
        assertEquals(1, mDiscover.getBindingCount());

        // Connect to same service using different URI
        mCallback.latch();
        assertEquals(1, mCallback.lBindingSuccess.getCount());
        boolean reattempted = mDiscover.bind(mAppContext, "alexatest:multi:20", mCallback);
        assertTrue("Service should have already been attempted", reattempted);
        assertOnLatch(mCallback.lBindingSuccess, "Expect Connect");
        assertNotNull(mCallback.mService);
        assertTrue(isRunning(TestService.Multi.class));
        assertEquals(2, mDiscover.getBindingCount());

        // unbind the first extension
        boolean unbind = mDiscover.unbind(mAppContext, "alexatest:multi:10");
        assertTrue(unbind);
        assertTrue(isRunning(TestService.Multi.class));
        assertEquals(1, mDiscover.getBindingCount());

        // unbind the second extension
        boolean reUnbind = mDiscover.unbind(mAppContext, "alexatest:multi:20");
        assertTrue(reUnbind);
        assertFalse(isRunning(TestService.Multi.class));
        assertEquals(0, mDiscover.getBindingCount());
    }


    /**
     * Discover, bind, unbind from a service in a remote process.
     */
    @Test
    public void testBind_remote() {
        boolean attempted = mDiscover.bind(mAppContext, "alexatest:remote:10", mCallback);
        assertTrue("Service bind failed attempt", attempted);
        assertOnLatch(mCallback.lBindingSuccess, "Expect Connect");
        assertNotNull(mCallback.mService);
        assertTrue(isRunning(TestService.Remote.class));
        assertEquals(1, mDiscover.getBindingCount());

        mDiscover.unbind(mAppContext, "alexatest:remote:10");
        assertFalse(isRunning(TestService.Remote.class));
        assertEquals(0, mDiscover.getBindingCount());
    }

    /**
     * Discover, bind, unbind from a service that crashes. Result is FAIL_DISCONNECTED
     */
    @Test
    public void testBind_failDisconnect() {
        boolean attempted = mDiscover.bind(mAppContext, "alexatest:faildisconnect:10", mCallback);
        assertTrue("Service bind failed attempt", attempted);
        // expect bind success, then fail after process kill
        assertOnLatch(mCallback.lBindingSuccess, "Expect Connect");
        assertOnLatch(mCallback.lBindingFail, "Expect Fail");
        assertEquals(FAIL_DISCONNECTED, mCallback.mErrorCode);
        assertNotNull(mCallback.mError);

        // connections persist and must be unbound
        assertEquals(1, mDiscover.getBindingCount());
        mDiscover.unbind(mAppContext, "alexatest:faildisconnect:10");
        assertFalse(isRunning(TestService.Remote.class));
        assertEquals(0, mDiscover.getBindingCount());
    }


    /**
     * Discover, bind, unbind from a service.  Second attempts to bind result in FAIL_INVALID.
     */
    @Test
    public void testBind_failDuplicate() {

        boolean attempted = mDiscover.bind(mAppContext, "alexatest:simple:10", mCallback);
        assertTrue("Service bind failed attempt", attempted);
        assertOnLatch(mCallback.lBindingSuccess, "Expect Connect");
        assertNotNull(mCallback.mService);
        assertTrue(isRunning(TestService.Simple.class));
        assertEquals(1, mDiscover.getBindingCount());

        // Duplicate the effort
        boolean illegal = false;
        try {
            mDiscover.bind(mAppContext, "alexatest:simple:10", mCallback);
        } catch (IllegalStateException e) {
            illegal = true;
        }
        assertTrue(illegal);

        // connections persist and must be unbound
        mDiscover.unbind(mAppContext, "alexatest:simple:10");
        assertFalse(isRunning(TestService.Remote.class));
        assertEquals(0, mDiscover.getBindingCount());
    }


    /**
     * Discover, bind, unbind from a service that is then uninstalled.  Result is
     */
    //@Test
    @SuppressWarnings("EmptyMethod")
    public void testBind_failDied() {
        // died is not testable because it happens when the apk is uninstalled during use
        // this can't be done in the test environment
    }


    /**
     * The Service return a null binder and cannot complete the handshake.
     */
    @Test
    public void testBind_failCom() {
        boolean attempted = mDiscover.bind(mAppContext, "alexatest:failcom:10", mCallback);
        assertTrue("Service bind failed attempt", attempted);
        assertOnLatch(mCallback.lBindingFail, "Expect Fail");
        assertEquals(ConnectionCallback.FAIL_COM, mCallback.mErrorCode);

        mDiscover.unbind(mAppContext, "alexatest:failcom:10");
        assertFalse(isRunning(TestService.FailCom.class));
        assertEquals(0, mDiscover.getBindingCount());
    }


    /**
     * The runtime does not have permissions to use the extension.  This is uncommon,
     * and would only be the case if the runtime wants a build time dependency on a
     * discoverable extension.
     */
    //@Test
    @SuppressWarnings("EmptyMethod")
    public void testBind_failSecurity() {
        // security permission is untestable because the test environment is the same package
        // as the running service.
    }


    /**
     * Helper method to identify if service is running.
     *
     * @return {@code true} if the service is running.
     */
    private boolean isRunning(final Class<?> serviceClass) {
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
     * Helper method to block on a given sLatch for the duration of the set timeout
     */
    private void assertOnLatch(final CountDownLatch latch, final String actionName) {
        try {
            if (!latch.await(3L, TimeUnit.SECONDS)) {
                fail(actionName);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(actionName);
        }
    }
}
