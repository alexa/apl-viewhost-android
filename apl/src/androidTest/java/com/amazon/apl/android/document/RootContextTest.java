/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.document;

import android.view.Choreographer;

import com.amazon.apl.android.APLBinding;
import com.amazon.apl.android.APLTestContext;
import com.amazon.apl.android.BuildConfig;
import com.amazon.apl.android.NotOnUiThreadError;
import com.amazon.apl.android.RootContext;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class RootContextTest implements BoundObjectDefaultTest {

    // Load the APL library.
    static {
        System.loadLibrary("apl-jni");
    }

    private static String DOC = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.0\"," +
            "  \"mainTemplate\": {" +
            "    \"item\": {" +
            "      \"type\": \"Frame\"," +
            "      \"backgroundColor\": \"orange\"" +
            "    }" +
            "  }" +
            "}";

    private static String DOC_WHEN = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.0\"," +
            "  \"mainTemplate\": {" +
            "    \"item\": {" +
            "      \"type\": \"Frame\"," +
            "   \"when\": false," +
            "      \"backgroundColor\": \"orange\"" +
            "    }" +
            "  }" +
            "}";

    private static String DOC_SETTINGS = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.0\"," +
            "  \"mainTemplate\": {" +
            "    \"item\": {" +
            "      \"type\": \"Frame\"," +
            "      \"backgroundColor\": \"orange\"" +
            "    }" +
            "  }," +
            "  \"settings\": {" +
            "    \"propertyA\": true," +
            "    \"-propertyB\": 60000," +
            "    \"-propertyC\": \"abc\"," +
            "    \"subSetting\": {" +
            "      \"propertyD\": 12.34" +
            "    }" +
            "  }" +
            "}";

    static class MockChoreographer extends RootContext.APLChoreographer {
        @Override
        protected void postFrameCallback(Choreographer.FrameCallback callback) {
            // Do nothing
        }

        @Override
        protected void removeFrameCallback(Choreographer.FrameCallback callback) {
            // Do nothing
        }
    }

    /**
     * Create a handle to the bound object under test.  The tests should not hold
     * a reference to the bound object to allow for gc and unbinding tests.
     * Recommended pattern for this method:
     * <p>
     * Foo foo =  Foo.create();
     * long handle = foo.getNativeHandle();
     * return handle;
     *
     * @return The handle BoundObject under test.
     */
    @Override
    public long createBoundObjectHandle() {
        // create a RootContext
        RootContext rootContext = new APLTestContext()
                .setDocument(DOC)
                .buildRootContext();

        long handle = rootContext.getNativeHandle();
        return handle;
    }


    @Test(expected = IllegalStateException.class)
    @SmallTest
    public void test_FalseConditions() {
        // create a RootContext
        RootContext rootContext = new APLTestContext()
                .setDocument(DOC_WHEN)
                .buildRootContext();
    }

    @Test
    @UiThreadTest
    public void test_finishDocument_onUiThread() {
        assertTrue(BuildConfig.DEBUG);
        // create a RootContext
        RootContext.APLChoreographer.setInstance(new MockChoreographer());
        RootContext rootContext = new APLTestContext()
                .setDocument(DOC)
                .buildRootContext();

        rootContext.finishDocument();

        RootContext.APLChoreographer.setInstance(null);
    }

    @Test(expected = NotOnUiThreadError.class)
    public void test_finishDocument_notOnUiThread() {
        assertTrue(BuildConfig.DEBUG);
        // create a RootContext
        RootContext.APLChoreographer.setInstance(new MockChoreographer());
        RootContext rootContext = new APLTestContext()
                .setDocument(DOC)
                .buildRootContext();

        rootContext.finishDocument();

        RootContext.APLChoreographer.setInstance(null);
    }

    @Override
    @Test
    @UiThreadTest
    public void testMemory_binding() {
        RootContext.APLChoreographer.setInstance(new MockChoreographer());
        RootContext rootContext = new APLTestContext()
                .setDocument(DOC)
                .buildRootContext();
        long handle = rootContext.getNativeHandle();
        rootContext.finishDocument();

        // The object has a native handle
        assertTrue("Expected object to have native handle", handle != 0);
        // The object is registered with the PhantomReference queue
        assertTrue("Expected bound object to be registered", APLBinding.testBound(handle));

        // Null out explicit references
        rootContext = null;

        System.runFinalization();
        System.gc();
        try {
            // sleep to allow gc to keep pace with short lived test cases
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        APLBinding.doDeletes();

        assertFalse("expected unbound object", APLBinding.testBound(handle));

        // unset mocked variable
        RootContext.APLChoreographer.setInstance(null);
    }

    @Test
    @SmallTest
    public void test_settings() {
        // create a RootContext
        RootContext rootContext = new APLTestContext()
                .setDocument(DOC_SETTINGS)
                .buildRootContext();

        boolean a = rootContext.optSetting("propertyA", false);
        int b = rootContext.optSetting("-propertyB", 30000);
        String c = rootContext.optSetting("-propertyC", "foo");
        Map<String, Double> subSetting = rootContext.optSetting("subSetting", new HashMap<>());
        double d = subSetting.get("propertyD");
        Object e = rootContext.optSetting("-notExistingProperty", null);

        assertTrue(rootContext.hasSetting("propertyA"));
        assertTrue(rootContext.hasSetting("-propertyB"));
        assertTrue(rootContext.hasSetting("-propertyC"));
        assertTrue(rootContext.hasSetting("subSetting"));
        assertFalse(rootContext.hasSetting("-notExistingProperty"));

        assertTrue(a);
        assertEquals(60000, b);
        assertEquals("abc", c);
        assertEquals(12.34, d, 0.001);
        assertNull(e);
    }

    @Test
    public void test_LocaleCallbacksDontBreakWithBogusLocale() {
        RootContext rootContext = new APLTestContext()
                .setDocument(DOC_SETTINGS)
                .buildRootContext();

        String[] cases = {"test-abc", "?????" , "", null};

        for(String testcase : cases) {
            rootContext.callbackToLowerCase("Üä¨ßÍ", testcase);
            rootContext.callbackToUpperCase("Üä¨ßÍ", testcase);
        }
    }
}