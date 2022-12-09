/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.extension;

import androidx.test.filters.SmallTest;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.APLTestContext;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.ExtensionEventHandler;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.Text;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.android.utils.TestClock;
import com.amazon.apl.enums.ComponentType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;


public class ExtensionEventHandlerTest extends ViewhostRobolectricTest {
    private APLTestContext mTestContext;
    private APLOptions.Builder mOptionsBuilder;
    private RootConfig mRootConfig;
    private RootContext mRootContext;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mTestContext = new APLTestContext();
        mOptionsBuilder = APLOptions.builder()
                .aplClockProvider(callback -> new TestClock(callback));
        mRootConfig = RootConfig.create("Test", "1.3");
    }

    private void setExtensionEventHandler(String uri, String name) {
        mRootConfig.registerExtensionEventHandler(new ExtensionEventHandler(uri, name));
    }

    private void loadDocument(String document) {
        mRootContext = mTestContext
                .setDocument(document)
                .setAplOptions(mOptionsBuilder.build())
                .setRootConfig(mRootConfig)
                .buildRootContext();
    }

    @Test
    @SmallTest
    public void testExtension_Create() {

        ExtensionEventHandler extHandler = new ExtensionEventHandler("aplext:TEST", "MyFooCommand");

        assertEquals("MyFooCommand", extHandler.getName());
        assertEquals("aplext:TEST", extHandler.getURI());
    }


    private static final String BASIC =
            "{" +
                    "  \"type\": \"APL\"," +
                    "  \"version\": \"1.2\"," +
                    "  \"extensions\": [" +
                    "    {" +
                    "      \"name\": \"T\"," +
                    "      \"uri\": \"aplext:Test\"" +
                    "    }" +
                    "  ]," +
                    "  \"mainTemplate\": {" +
                    "    \"item\": {" +
                    "      \"type\": \"Text\"," +
                    "      \"id\": \"MyText\"" +
                    "    }" +
                    "  }," +
                    "  \"T:onSetArguments\": {" +
                    "    \"type\": \"SetValue\"," +
                    "    \"componentId\": \"MyText\"," +
                    "    \"property\": \"text\"," +
                    "    \"value\": \"Hello \"" +
                    "  }" +
                    "}";

    /**
     * Don't register for the custom handler.  When the system tries to invoke it,
     * that should generate a error message on the log (not the console)
     */
    @Test
    @SmallTest
    public void testExtension_BasicMissingHandler() {
        loadDocument(BASIC);

        // verify the document inflated
        Component component = mRootContext.getTopComponent();
        assertNotNull(component);
        assertEquals(ComponentType.kComponentTypeText, component.getComponentType());
        Text text = (Text) component;

        mRootContext.invokeExtensionEventHandler("aplext:Test", "onSetArguments", null, false);

        // Advance Time
        mRootContext.onTick(1);

        assertEquals("", text.getProxy().getStyledText().getUnprocessedText());

    }

    /**
     * Register for the custom handler and invoke it.
     */
    @Test
    @SmallTest
    public void testExtension_BasicWithHandler() {
        setExtensionEventHandler("aplext:Test", "onSetArguments");
        loadDocument(BASIC);

        // verify the document inflated
        Component component = mRootContext.getTopComponent();
        assertEquals(ComponentType.kComponentTypeText, component.getComponentType());
        Text text = (Text) component;

        // Invoke a handler event
        mRootContext.invokeExtensionEventHandler("aplext:Test", "onSetArguments", null, false);

        // Advance Time
        mRootContext.onTick(1);

        // Check the handler event fired
        assertEquals("Hello", text.getProxy().getStyledText().getUnprocessedText());
    }


    private static final String WITH_ARGUMENTS =
            "{" +
                    "  \"type\": \"APL\"," +
                    "  \"version\": \"1.2\"," +
                    "  \"extensions\": [" +
                    "    {" +
                    "      \"name\": \"T\"," +
                    "      \"uri\": \"aplext:Test\"" +
                    "    }" +
                    "  ]," +
                    "  \"mainTemplate\": {" +
                    "    \"item\": {" +
                    "      \"type\": \"Text\"," +
                    "      \"id\": \"MyText\"" +
                    "    }" +
                    "  }," +
                    "  \"T:onSetArguments\": {" +
                    "    \"type\": \"SetValue\"," +
                    "    \"componentId\": \"MyText\"," +
                    "    \"property\": \"text\"," +
                    "    \"value\": \"Hello ${a} ${b}\"" +
                    "  }" +
                    "}";

    /**
     * Provide arguments when invoking the custom handler and verify that those arguments
     * are passed through.
     */
    @Test
    @SmallTest
    public void testExtension_WithArguments() {
        setExtensionEventHandler("aplext:Test", "onSetArguments");

        loadDocument(WITH_ARGUMENTS);

        // verify the document inflated
        Component component = mRootContext.getTopComponent();
        assertNotNull(component);
        assertEquals(ComponentType.kComponentTypeText, component.getComponentType());
        Text text = (Text) component;

        //{{"a", 2}, {"b", "Hello"}};
        Map<String, Object> map = new HashMap<>();
        map.put("a", 2);
        map.put("b", "Hello");

        // Invoke a handler event
        mRootContext.invokeExtensionEventHandler("aplext:Test", "onSetArguments",
                map, false);

        // Advance Time
        mRootContext.onTick(1);

        // Check the handler event fired
        assertEquals("Hello 2 Hello", text.getProxy().getStyledText().getUnprocessedText());
    }


    private static final String IMPORT_TEST =
            "{" +
                    "  \"type\": \"APL\"," +
                    "  \"version\": \"1.2\"," +
                    "  \"import\": [" +
                    "    {" +
                    "      \"name\": \"simple\"," +
                    "      \"version\": \"1.0\"" +
                    "    }" +
                    "  ]," +
                    "  \"mainTemplate\": {" +
                    "    \"item\": {" +
                    "      \"type\": \"Text\"," +
                    "      \"id\": \"MyText\"" +
                    "    }" +
                    "  }" +
                    "}";
    private static final String IMPORT_TEST_PACKAGE =
            "{" +
                    "  \"type\": \"APL\"," +
                    "  \"version\": \"1.2\"," +
                    "  \"extensions\": [" +
                    "    {" +
                    "      \"name\": \"T\"," +
                    "      \"uri\": \"aplext:Test\"" +
                    "    }" +
                    "  ]," +
                    "  \"T:onSetArguments\": {" +
                    "    \"type\": \"SetValue\"," +
                    "    \"componentId\": \"MyText\"," +
                    "    \"property\": \"text\"," +
                    "    \"value\": \"FromImport\"" +
                    "  }" +
                    "}";

    /**
     * Define a custom handler in an imported package
     */
    @Test
    @SmallTest
    public void testExtension_ImportTest() {
        setExtensionEventHandler("aplext:Test", "onSetArguments");
        mTestContext.setDocumentImport("simple", IMPORT_TEST_PACKAGE);
        loadDocument(IMPORT_TEST);

        // verify the document inflated
        Component component = mRootContext.getTopComponent();
        assertNotNull(component);
        assertEquals(ComponentType.kComponentTypeText, component.getComponentType());
        Text text = (Text) component;

        // Invoke a handler event
        mRootContext.invokeExtensionEventHandler("aplext:Test", "onSetArguments", null, false);

        // Advance Time
        mRootContext.onTick(1);

        // Check the handler event fired
        assertEquals("FromImport", text.getProxy().getStyledText().getUnprocessedText());
    }


    private static final String IMPORT_TEST_OVERRIDE =
            "{" +
                    "  \"type\": \"APL\"," +
                    "  \"version\": \"1.2\"," +
                    "  \"import\": [" +
                    "    {" +
                    "      \"name\": \"simple\"," +
                    "      \"version\": \"1.0\"" +
                    "    }" +
                    "  ]," +
                    "  \"extensions\": [" +
                    "    {" +
                    "      \"name\": \"T\"," +
                    "      \"uri\": \"aplext:Test\"" +
                    "    }" +
                    "  ]," +
                    "  \"mainTemplate\": {" +
                    "    \"item\": {" +
                    "      \"type\": \"Text\"," +
                    "      \"id\": \"MyText\"" +
                    "    }" +
                    "  }," +
                    "  \"T:onSetArguments\": {" +
                    "    \"type\": \"SetValue\"," +
                    "    \"componentId\": \"MyText\"," +
                    "    \"property\": \"text\"," +
                    "    \"value\": \"FromMain\"" +
                    "  }" +
                    "}";

    /**
     * Override the imported package handler with a document handler
     */
    @Test
    @SmallTest
    public void testExtension_ImportTestOverride() {
        setExtensionEventHandler("aplext:Test", "onSetArguments");
        mTestContext.setDocumentImport("simple", IMPORT_TEST_PACKAGE);
        loadDocument(IMPORT_TEST_OVERRIDE);

        // verify the document inflated
        Component component = mRootContext.getTopComponent();
        assertNotNull(component);
        assertEquals(ComponentType.kComponentTypeText, component.getComponentType());
        Text text = (Text) component;

        // Invoke a handler event
        mRootContext.invokeExtensionEventHandler("aplext:Test", "onSetArguments", null, false);

        // Advance Time
        mRootContext.onTick(1);

        // Check the handler event fired
        assertEquals("FromMain", text.getProxy().getStyledText().getUnprocessedText());
    }


    private static final String FAST_MODE =
            "{" +
                    "  \"type\": \"APL\"," +
                    "  \"version\": \"1.2\"," +
                    "  \"extensions\": [" +
                    "    {" +
                    "      \"name\": \"T\"," +
                    "      \"uri\": \"aplext:Test\"" +
                    "    }" +
                    "  ]," +
                    "  \"mainTemplate\": {" +
                    "    \"item\": {" +
                    "      \"type\": \"Text\"," +
                    "      \"id\": \"MyText\"" +
                    "    }" +
                    "  }," +
                    "  \"T:onSetArguments\": [" +
                    "    {" +
                    "      \"type\": \"SendEvent\"" +
                    "    }," +
                    "    {" +
                    "      \"type\": \"SetValue\"," +
                    "      \"componentId\": \"MyText\"," +
                    "      \"property\": \"text\"," +
                    "      \"value\": \"FromMain\"" +
                    "    }" +
                    "  ]" +
                    "}";

    /**
     * Run the custom handler in fast mode
     */
    @Test
    @SmallTest
    public void testExtension_FastMode() {
        boolean[] sendResult = new boolean[1];
        mOptionsBuilder.sendEventCallbackV2((args, components, sources, flags) -> {
            sendResult[0] = true; // the callback ran
        });

        setExtensionEventHandler("aplext:Test", "onSetArguments");

        loadDocument(FAST_MODE);

        // verify the document inflated
        Component component = mRootContext.getTopComponent();
        assertNotNull(component);
        assertEquals(ComponentType.kComponentTypeText, component.getComponentType());
        Text text = (Text) component;

        // Invoke a handler event
        mRootContext.invokeExtensionEventHandler("aplext:Test", "onSetArguments", null, true);

        // Advance Time
        mRootContext.onTick(1);


        // Check the handler event fired in fast-mode
        assertFalse(sendResult[0]);
        assertEquals("FromMain", text.getProxy().getStyledText().getUnprocessedText());
    }

    private static final String DUPLICATE_EXTENSION_NAME =
            "{" +
                    "  \"type\": \"APL\"," +
                    "  \"version\": \"1.2\"," +
                    "  \"extensions\": [" +
                    "    {" +
                    "      \"name\": \"A\"," +
                    "      \"uri\": \"test\"" +
                    "    }," +
                    "    {" +
                    "      \"name\": \"B\"," +
                    "      \"uri\": \"test\"" +
                    "    }" +
                    "  ]," +
                    "  \"mainTemplate\": {" +
                    "    \"item\": {" +
                    "      \"type\": \"Text\"," +
                    "      \"id\": \"MyText\"" +
                    "    }" +
                    "  }," +
                    "  \"A:onExecute\": {" +
                    "    \"type\": \"SetValue\"," +
                    "    \"componentId\": \"MyText\"," +
                    "    \"property\": \"text\"," +
                    "    \"value\": \"FromA\"" +
                    "  }," +
                    "  \"B:onExecute\": {" +
                    "    \"type\": \"SetValue\"," +
                    "    \"componentId\": \"MyText\"," +
                    "    \"property\": \"text\"," +
                    "    \"value\": \"FromB\"" +
                    "  }" +
                    "}";

    /**
     * Register the same extension twice under different names.  Only the last handler should execute.
     */
    @Test
    @SmallTest
    public void testExtension_DuplicateExtensionName() {
        setExtensionEventHandler("test", "onExecute");
        loadDocument(DUPLICATE_EXTENSION_NAME);

        // verify the document inflated
        Component component = mRootContext.getTopComponent();
        assertNotNull(component);
        assertEquals(ComponentType.kComponentTypeText, component.getComponentType());
        Text text = (Text) component;

        // Invoke a handler event
        mRootContext.invokeExtensionEventHandler("test", "onExecute", null, true);

        // Advance Time
        mRootContext.onTick(1);

        // Check the handler event fired
        assertEquals("FromB", text.getProxy().getStyledText().getUnprocessedText());
    }

    private static final String EXTENSION_ACCESSING_PAYLOAD =
            "{" +
                    "  \"type\": \"APL\"," +
                    "  \"version\": \"1.3\"," +
                    "  \"extensions\": [" +
                    "    {" +
                    "      \"name\": \"A\"," +
                    "      \"uri\": \"URI_A\"" +
                    "    }" +
                    "  ]," +
                    "  \"mainTemplate\": {" +
                    "    \"parameters\": [" +
                    "      \"payload\"" +
                    "    ]," +
                    "    \"item\": {" +
                    "      \"type\": \"Text\"," +
                    "      \"id\": \"MyText\"," +
                    "      \"text\": \"${payload.start}\"" +
                    "    }" +
                    "  }," +
                    "  \"A:onExecute\": {" +
                    "    \"type\": \"SetValue\"," +
                    "    \"componentId\": \"MyText\"," +
                    "    \"property\": \"text\"," +
                    "    \"value\": \"${payload.end}\"" +
                    "  }" +
                    "}";

    private static final String EXTENSION_PAYLOAD = "{\"start\":\"START\",\"end\":\"END\"}";

    /**
     * Verify that the extension handler can access the payload.
     */
    @Test
    @SmallTest
    public void testExtension_AccessingPayload() {
        setExtensionEventHandler("URI_A", "onExecute");
        mTestContext.setDocumentPayload("payload", EXTENSION_PAYLOAD);
        loadDocument(EXTENSION_ACCESSING_PAYLOAD);

        // verify the document inflated
        Component component = mRootContext.getTopComponent();
        assertNotNull(component);
        assertEquals(ComponentType.kComponentTypeText, component.getComponentType());
        Text text = (Text) component;

        // Invoke a handler event
        mRootContext.invokeExtensionEventHandler("URI_A", "onExecute", null, true);

        // Advance Time
        mRootContext.onTick(1);

        // Check the handler event fired
        assertEquals("END", text.getProxy().getStyledText().getUnprocessedText());
    }


}





