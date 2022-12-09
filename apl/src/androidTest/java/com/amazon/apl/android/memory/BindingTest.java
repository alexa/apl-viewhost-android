/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.memory;

import static com.amazon.common.test.Asserts.assertNativeHandle;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.fail;

import androidx.test.annotation.UiThreadTest;
import androidx.test.filters.SmallTest;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.APLTestContext;
import com.amazon.apl.android.Action;
import com.amazon.apl.android.Content;
import com.amazon.apl.android.ExtensionCommandDefinition;
import com.amazon.apl.android.ExtensionEventHandler;
import com.amazon.apl.android.ExtensionFilterDefinition;
import com.amazon.apl.android.LiveArray;
import com.amazon.apl.android.LiveMap;
import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.TextMeasure;
import com.amazon.apl.android.TextMeasureCallback;
import com.amazon.apl.android.document.AbstractDocUnitTest;
import com.amazon.apl.android.utils.TestClock;
import com.amazon.apl.enums.ImageCount;
import com.amazon.common.NativeBinding;

import org.junit.Assert;
import org.junit.Test;

// The tests in this class all involve garbage collection around Java objects that wrap native JNI objects.
// Since the garbage collector in the JVM is different than the Android one,
// these have to stay on Android since that's the only one we actually care about.
public class BindingTest extends AbstractDocUnitTest {

    private long getHandle() {
        Action action = mRootContext.executeCommands("[\n" +
                "    {\n" +
                "        \"type\": \"SetValue\",\n" +
                "        \"property\": \"backgroundColor\",\n" +
                "        \"value\": \"purple\",\n" +
                "        \"componentId\": \"frame\"\n" +
                "    },\n" +
                "    {\n" +
                "        \"type\": \"Idle\",\n" +
                "        \"delay\": 1000\n" +
                "    },\n" +
                "    {\n" +
                "        \"type\": \"SetValue\",\n" +
                "        \"property\": \"backgroundColor\",\n" +
                "        \"value\": \"orange\",\n" +
                "        \"componentId\": \"frame\"\n" +
                "    }\n" +
                "]");
        mRootContext.cancelExecution();
        return action.getNativeHandle();
    }


    @Test
    public void testAction_binding() {
        loadDocument("{\n" +
                "    \"type\": \"APL\",\n" +
                "    \"version\": \"1.0\",\n" +
                "    \"mainTemplate\": {\n" +
                "        \"item\": {\n" +
                "            \"type\": \"Frame\",\n" +
                "            \"id\": \"frame\",\n" +
                "            \"width\": 100,\n" +
                "            \"height\": 100,\n" +
                "            \"backgroundColor\": \"green\"\n" +
                "        }\n" +
                "    }\n" +
                "}");

        long handle = getHandle();

        assertNativeHandle(handle);
    }

    private long createHandle() {
        APLOptions AplOptions = APLOptions.builder()
                .build();

        Content content = null;
        try {
            content = Content.create("{" +
                    "  \"type\": \"APL\"," +
                    "  \"version\": \"1.0\"," +
                    "  \"import\": [" +
                    "    {\n" +
                    "      \"name\": \"test-package\"," +
                    "      \"version\": \"1.0\"" +
                    "    }" +
                    "  ]," +
                    "  \"mainTemplate\": {" +
                    "    \"parameters\": [" +
                    "      \"payload\"" +
                    "    ]," +
                    "    \"item\": {" +
                    "      \"type\": \"Text\"" +
                    "    }" +
                    "  }" +
                    "}", AplOptions);
        } catch (Content.ContentException e) {
            Assert.fail(e.getMessage());
        }

        Content.ImportRequest ir = content.getRequestedPackages().iterator().next();
        try {
            content.addPackage(ir,  "{" +
                    "  \"type\": \"APL\"," +
                    "  \"version\": \"1.0\"" +
                    "}");
        } catch (Content.ContentException e) {
            fail(e.getMessage());
        }

        String param = content.getParameters().iterator().next();
        content.addData(param, "{ " +
                "  \"data\": {" +
                "    \"text\": \"Hello APL World!\"" +
                "  }" +
                "}");

        assertTrue("Content not ready", content.isReady());
        @SuppressWarnings("UnnecessaryLocalVariable") long handle = content.getNativeHandle();
        return handle;
    }

    @Test
    public void testContent_binding() {
        assertNativeHandle(createHandle());
    }

    @Test
    public void testLiveMap_binding() {
        long handle = LiveMap.create().getNativeHandle();

        assertNativeHandle(handle);
    }

    @Test
    public void testLiveArray_binding() {
        long handle = LiveArray.create().getNativeHandle();

        assertNativeHandle(handle);
    }

    private long createTextMeasureCallbackHandle() {
        RenderingContext ctx = RenderingContext.builder().build();
        TextMeasureCallback callback = TextMeasureCallback.factory().create(
                ctx.getMetricsTransform(), new TextMeasure(ctx));

        long handle = callback.getNativeHandle();

        return handle;
    }

    /**
     * Test the allocation and free of an APL RootContext memory..
     */
    @Test
    public void testTextMeasureCallback_binding() {
        // We need to remove the spy for this test so that it gets cleaned up by GC.
        TextMeasureCallback.Factory.inject(null);
        long handle = createTextMeasureCallbackHandle();
        assertNativeHandle(handle);
    }

    @Test
    @UiThreadTest
    public void testRootContext_binding() {
        RootContext rootContext = new APLTestContext()
                .setDocument("{" +
                        "  \"type\": \"APL\"," +
                        "  \"version\": \"1.0\"," +
                        "  \"mainTemplate\": {" +
                        "    \"item\": {" +
                        "      \"type\": \"Frame\"," +
                        "      \"backgroundColor\": \"orange\"" +
                        "    }" +
                        "  }" +
                        "}")
                .setAplOptions(APLOptions.builder().aplClockProvider(callback -> new TestClock(callback)).build())
                .buildRootContext();
        long handle = rootContext.getNativeHandle();
        rootContext.finishDocument();

        // The object has a native handle
        Assert.assertTrue("Expected object to have native handle", handle != 0);
        // The object is registered with the PhantomReference queue
        Assert.assertTrue("Expected bound object to be registered", NativeBinding.testBound(handle));

        // Null out explicit references
        rootContext = null;

        assertNativeHandle(handle);
    }

    @Test
    @SmallTest
    public void testExtensionFilterDefinition_binding() {
        long handle = new ExtensionFilterDefinition("aplext:edgedetectorfilters:11", "Edges", ImageCount.ONE)
                .getNativeHandle();

        assertNativeHandle(handle);
    }

    /**
     * Test the allocation and free of an APL RootContext memory..
     */
    @Test
    @SmallTest
    public void testExtensionEventHandler_binding() {
        long handle = new ExtensionEventHandler("aplext:Test", "Test")
                .getNativeHandle();

        assertNativeHandle(handle);
    }

    @Test
    @SmallTest
    public void testExtensionCommandDefinition_binding() {
        long handle = new ExtensionCommandDefinition("aplext:Test", "MyDef").getNativeHandle();

        assertNativeHandle(handle);
    }

    @Test
    @SmallTest
    public void testRootConfig_binding() {
        long handle = RootConfig.create("Test", "1.0")
                .getNativeHandle();

        assertNativeHandle(handle);
    }
}
