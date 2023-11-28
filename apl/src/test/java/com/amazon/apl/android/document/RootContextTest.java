/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.document;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.APLTestContext;
import com.amazon.apl.android.BuildConfig;
import com.amazon.apl.android.NotOnUiThreadError;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.dependencies.IDataSourceContextListener;
import com.amazon.apl.android.dependencies.IVisualContextListener;
import com.amazon.apl.android.media.RuntimeMediaPlayerFactory;
import com.amazon.apl.android.providers.impl.MediaPlayerProvider;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.android.utils.TestClock;
import com.amazon.apl.enums.RootProperty;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import androidx.test.annotation.UiThreadTest;

public class RootContextTest extends ViewhostRobolectricTest {
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

    @Test(expected = IllegalStateException.class)
    public void test_FalseConditions() {
        // create a RootContext
        RootContext rootContext = new APLTestContext()
                .setDocument(DOC_WHEN)
                .buildRootContext();
    }

    @Test
    @UiThreadTest
    public void test_finishDocument_onUiThread() {
        assumeTrue(BuildConfig.DEBUG);
        // create a RootContext
        RootContext rootContext = new APLTestContext()
                .setDocument(DOC)
                .setAplOptions(APLOptions.builder().aplClockProvider(callback -> new TestClock(callback)).build())
                .buildRootContext();

        rootContext.finishDocument();
    }

    @Test(expected = NotOnUiThreadError.class)
    public void test_finishDocument_notOnUiThread() throws Exception, Throwable {
        assumeTrue(BuildConfig.DEBUG);
        // create a RootContext
        final RootContext rootContext = new APLTestContext()
                .setDocument(DOC)
                .setAplOptions(APLOptions.builder().aplClockProvider(callback -> new TestClock(callback)).build())
                .buildRootContext();
        try {
            Executors.newSingleThreadExecutor().submit(() -> {
                rootContext.finishDocument();
                return true;
            }).get();
        } catch (ExecutionException ex) {
            throw ex.getCause();
        }
    }

    @Test
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
    public void test_mediaPlayerProvider() {
        RootConfig rootConfig = RootConfig.create();
        RootContext rootContext = new APLTestContext()
                .setRootConfig(rootConfig)
                .setDocument(DOC_SETTINGS)
                .buildRootContext();
        assertTrue(rootContext.getRenderingContext().getMediaPlayerProvider() instanceof MediaPlayerProvider);
    }

    @Test
    public void test_enabledVideo_enabled_mediaPlayerV2Enabled() {
        MediaPlayerProvider optionsProvider = new MediaPlayerProvider();
        MediaPlayerProvider rootConfigProvider = new MediaPlayerProvider();
        RootConfig rootConfig = RootConfig.create()
                .mediaPlayerFactory(new RuntimeMediaPlayerFactory(rootConfigProvider));
        RootContext rootContext = new APLTestContext()
                .setAplOptions(APLOptions.builder().mediaPlayerProvider(optionsProvider).build())
                .setRootConfig(rootConfig)
                .setDocument(DOC_SETTINGS)
                .buildRootContext();
        assertEquals(rootConfigProvider, rootContext.getRenderingContext().getMediaPlayerProvider());
    }

    @Test
    public void test_notifyContext() {
        MediaPlayerProvider optionsProvider = new MediaPlayerProvider();
        MediaPlayerProvider rootConfigProvider = new MediaPlayerProvider();
        RootConfig rootConfig = RootConfig.create()
                .mediaPlayerFactory(new RuntimeMediaPlayerFactory(rootConfigProvider));
        IDataSourceContextListener dataSourceContextListener = mock(IDataSourceContextListener.class);
        IVisualContextListener visualContextListener = mock(IVisualContextListener.class);
        RootContext rootContext = new APLTestContext()
                .setAplOptions(APLOptions.builder()
                        .dataSourceContextListener(dataSourceContextListener)
                        .visualContextListener(visualContextListener)
                        .build())
                .setRootConfig(rootConfig)
                .setDocument(DOC_SETTINGS)
                .buildRootContext();
        rootContext.notifyContext();
        verify(visualContextListener).onVisualContextUpdate(any());
        verify(dataSourceContextListener).onDataSourceContextUpdate(any());
    }
}
