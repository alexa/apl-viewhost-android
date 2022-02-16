/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.document;

import android.view.Choreographer;

import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.amazon.apl.android.APLTestContext;
import com.amazon.apl.android.APLViewhostTest;
import com.amazon.apl.android.BuildConfig;
import com.amazon.apl.android.EditText;
import com.amazon.apl.android.NoOpComponent;
import com.amazon.apl.android.NotOnUiThreadError;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.providers.impl.MediaPlayerProvider;
import com.amazon.apl.android.providers.impl.NoOpMediaPlayerProvider;
import com.amazon.apl.enums.RootProperty;
import com.amazon.common.NativeBinding;
import com.amazon.common.test.Asserts;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class RootContextTest extends APLViewhostTest {
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

    private static String DOC_EDIT_TEXT = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.0\"," +
            "  \"mainTemplate\": {" +
            "    \"item\": {" +
            "      \"type\": \"EditText\"" +
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

    @After
    public void resetChoreographer() {
        RootContext.APLChoreographer.setInstance(null);
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
    }

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
        assertTrue("Expected bound object to be registered", NativeBinding.testBound(handle));

        // Null out explicit references
        rootContext = null;

        Asserts.assertNativeHandle(handle);
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

        // test that the methods don't throw with special, and utf-8 4byte characters
        for(String testcase : cases) {
            rootContext.callbackToLowerCase("Üä¨ßÍ\uD83E\uDD86", testcase);
            rootContext.callbackToUpperCase("Üä¨ßÍ\uD83E\uDD86", testcase);
        }
    }

    private static String DOC_LOCALE_OVERFLOW = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"1.5\",\n" +
            "  \"theme\": \"dark\",\n" +
            "  \"mainTemplate\": {\n" +
            "    \"items\": [\n" +
            "      {\n" +
            "        \"type\": \"Container\",\n" +
            "        \"data\": [\n" +
            "          \"${Array.range(0, 512)}\"\n" +
            "        ],\n" +
            "        \"items\": {\n" +
            "          \"type\": \"Frame\",\n" +
            "          \"when\": \"${String.toUpperCase('foo')} == 'FOO'\"\n" +
            "        }\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";

    // There's no Java exception to catch as this just crashes the process in JNI. A passing test should not crash.
    @Test
    public void test_localeMethods_doNotOverflowJNI() {
        RootContext rootContext = new APLTestContext()
                .setDocument(DOC_LOCALE_OVERFLOW)
                .buildRootContext();
    }

    @Test
    @SmallTest
    public void test_enabledVideo() {
        RootConfig rootConfig = RootConfig.create().set(RootProperty.kDisallowVideo, Boolean.FALSE);
        RootContext rootContext = new APLTestContext()
                .setRootConfig(rootConfig)
                .setDocument(DOC_SETTINGS)
                .buildRootContext();
        assertTrue(rootContext.getRenderingContext().getMediaPlayerProvider() instanceof MediaPlayerProvider);
    }

    @Test
    @SmallTest
    public void test_disabledVideo() {
        RootConfig rootConfig = RootConfig.create().set(RootProperty.kDisallowVideo, Boolean.TRUE);
        RootContext rootContext = new APLTestContext()
                .setRootConfig(rootConfig)
                .setDocument(DOC_SETTINGS)
                .buildRootContext();
        assertTrue(rootContext.getRenderingContext().getMediaPlayerProvider() instanceof NoOpMediaPlayerProvider);
    }

    @Test
    @SmallTest
    public void test_disabledEditText() {
        RootConfig rootConfig = RootConfig.create();
        RootContext rootContext = new APLTestContext()
                .setRootConfig(rootConfig)
                .setDocument(DOC_EDIT_TEXT)
                .buildRootContext();

        assertEquals(1, rootContext.getComponentCount());
        assertTrue(rootContext.getTopComponent() instanceof EditText);

        rootConfig.set(RootProperty.kDisallowEditText, Boolean.TRUE);
        rootContext = new APLTestContext()
                .setRootConfig(rootConfig)
                .setDocument(DOC_EDIT_TEXT)
                .buildRootContext();

        assertEquals(1, rootContext.getComponentCount());
        assertTrue(rootContext.getTopComponent() instanceof NoOpComponent);
    }
}
