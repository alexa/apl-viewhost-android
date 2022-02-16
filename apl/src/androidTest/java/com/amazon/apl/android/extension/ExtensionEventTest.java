/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.extension;

import android.graphics.Color;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.APLTestContext;
import com.amazon.apl.android.APLViewhostTest;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.Content;
import com.amazon.apl.android.ExtensionCommandDefinition;
import com.amazon.apl.android.Frame;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.Text;
import com.amazon.apl.android.dependencies.IExtensionEventCallback.IExtensionEventCallbackResult;
import com.amazon.apl.android.touch.Pointer;
import com.amazon.apl.enums.PointerEventType;
import com.amazon.apl.enums.PointerType;
import com.amazon.apl.enums.UpdateType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@RunWith(AndroidJUnit4.class)
public class ExtensionEventTest extends APLViewhostTest {
    private EventResult[] eventResults = new EventResult[2];
    private int[] count = {0};
    private APLTestContext mTestContext;
    private RootContext mRootContext;
    private RootConfig mRootConfig;

    private static class EventResult {
        String name;
        String uri;
        Map<String, Object> custom;
        Map<String, Object> source;
        IExtensionEventCallbackResult successCB;
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        RootContext.APLChoreographer choreographer = mock(RootContext.APLChoreographer.class);
        RootContext.APLChoreographer.setInstance(choreographer);
        mTestContext = new APLTestContext()
                .setAplOptions(APLOptions.builder()
                        .extensionEventCallback((name, uri, source, custom, resultCB) -> {
                            int i = count[0];
                            eventResults[i] = new EventResult();
                            eventResults[i].name = name;
                            eventResults[i].uri = uri;
                            eventResults[i].source = source;
                            eventResults[i].custom = custom;
                            eventResults[i].successCB = resultCB;
                            count[0]++;
                        }).build());
        mRootConfig = mTestContext.buildRootConfig();
    }

    @After
    public void cleanup() {
        // Remove the mock
        RootContext.APLChoreographer.setInstance(null);
    }

    private void loadDocument(String document) {
        mRootContext = mTestContext
                .setDocument(document)
                .buildRootContext();
    }

    private static final String BASIC = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.2\"," +
            "  \"extensions\": {" +
            "    \"name\": \"T\"," +
            "    \"uri\": \"aplext:Test\"" +
            "  }," +
            "  \"mainTemplate\": {" +
            "    \"item\": {" +
            "      \"type\": \"TouchWrapper\"," +
            "      \"onPress\": [" +
            "        {" +
            "          \"type\": \"T:MyCommand\"," +
            "          \"value\": 7" +
            "        }," +
            "        {" +
            "          \"type\": \"SetValue\"," +
            "          \"componentId\": \"MyFrame\"," +
            "          \"property\": \"backgroundColor\"," +
            "          \"value\": \"black\"" +
            "        }" +
            "      ]," +
            "      \"items\": {" +
            "        \"type\": \"Frame\"," +
            "        \"id\": \"MyFrame\"," +
            "        \"backgroundColor\": \"white\"" +
            "      }" +
            "    }" +
            "  }" +
            "}";

    /**
     * Invoking a custom command when it has not been set up in the RootConfig.
     * The custom command should be ignored and the following command should run normally.
     */
    @Test
    @SmallTest
    public void testCommand_BasicMissing() {
        mRootContext = mTestContext.setDocument(BASIC)
                .buildRootContext();

        assertNotNull(mRootContext.getTopComponent());
        Frame frame = (Frame) mRootContext.findComponentById("MyFrame");
        assertNotNull(frame);
        assertEquals(Color.WHITE, frame.getBackgroundColor());

        // press the TouchWrapper and advance time
        tap(1, 1);
        mRootContext.doFrame(1);

        // No event was generated
        assertNull(eventResults[0]);

        // Since the command wasn't registered, we expect no change
        assertEquals(Color.BLACK, frame.getBackgroundColor());
    }


    /**
     * Invoke a custom command when it HAS been set up correctly in the RootConfig.
     * We expect to get an event with the command and correctly set property values.
     */
    @Test
    @SmallTest
    public void testCommand_Basic() {
        // Set up the extension
        ExtensionCommandDefinition def = new ExtensionCommandDefinition("aplext:Test", "MyCommand")
                .property("value", -1, false);
        mRootConfig.registerExtensionCommand(def);
        loadDocument(BASIC);

        // verify the document inflated
        Component component = mRootContext.getTopComponent();
        assertNotNull(component);
        assertEquals(1, component.getChildCount());
        Frame frame = (Frame) mRootContext.findComponentById("MyFrame");
        assertNotNull(frame);
        assertEquals(Color.WHITE, frame.getBackgroundColor());

        // press the TouchWrapper and advance time
        tap(1, 1);
        mRootContext.doFrame(1);

        // Verify the callback happened
        assertNotNull(eventResults[0]);
        assertEquals("MyCommand", eventResults[0].name);
        assertEquals("aplext:Test", eventResults[0].uri);
        assertEquals("TouchWrapper", eventResults[0].source.get("type"));
        assertEquals(7, eventResults[0].custom.get("value"));

        // The SetValue command should also have run by now
        assertEquals(Color.BLACK, frame.getBackgroundColor());
    }


    /**
     * Invoke a custom command that requires resolution.  The next command in the sequence will
     * be pended until the first command is resolved.
     */
    @Test
    @SmallTest
    public void testCommand_BasicWithActionRef() {
        // Set up the extension
        ExtensionCommandDefinition def = new ExtensionCommandDefinition("aplext:Test", "MyCommand")
                .property("value", -1, false)
                .requireResolution(true);
        mRootConfig.registerExtensionCommand(def);
        loadDocument(BASIC);


        // verify the document inflated
        Component component = mRootContext.getTopComponent();
        assertNotNull(component);
        assertEquals(1, component.getChildCount());
        Frame frame = (Frame) mRootContext.findComponentById("MyFrame");
        assertNotNull(frame);
        assertEquals(Color.WHITE, frame.getBackgroundColor());

        tap(1,1);
        mRootContext.doFrame(1);

        // Verify the callback happened
        assertNotNull(eventResults[0]);
        assertEquals("MyCommand", eventResults[0].name);
        assertEquals("aplext:Test", eventResults[0].uri);
        assertEquals("TouchWrapper", eventResults[0].source.get("type"));
        assertEquals(7, eventResults[0].custom.get("value"));

        // The SetValue command should NOT have run
        assertEquals(Color.WHITE, frame.getBackgroundColor());

        // Resolve Event and Advance Time
        eventResults[0].successCB.onResult(true);
        mRootContext.doFrame(100);

        // The SetValue command should have now run
        assertEquals(Color.BLACK, frame.getBackgroundColor());
    }


    private static final String RICH_ARGUMENTS = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.3\"," +
            "  \"extensions\": [" +
            "    {" +
            "      \"name\": \"A\"," +
            "      \"uri\": \"URI_A\"" +
            "    }" +
            "  ]," +
            "  \"mainTemplate\": {" +
            "    \"items\": {" +
            "      \"type\": \"TouchWrapper\"," +
            "      \"id\": \"MyTouchWrapper\"," +
            "      \"onPress\": {" +
            "        \"type\": \"A:doIt\"," +
            "        \"value\": [" +
            "          \"${event.source.id}\"," +
            "          \"${event.source.value}\"" +
            "        ]" +
            "      }" +
            "    }" +
            "  }" +
            "}";

    /**
     * Verify that data-binding evaluation occurs inside of an array
     */
    @Test
    @SmallTest
    public void testRichArguments_Basic() {
        // Set up the extension
        ExtensionCommandDefinition def = new ExtensionCommandDefinition("URI_A", "doIt")
                .property("value", new ArrayList<>(), false);
        mRootConfig.registerExtensionCommand(def);
        loadDocument(RICH_ARGUMENTS);

        // verify the document inflated
        Component component = mRootContext.getTopComponent();
        assertNotNull(component);

        // press the TouchWrapper and advance time
        tap(1, 1);
        mRootContext.doFrame(1);

        // Verify the callback happened
        assertNotNull(eventResults[0]);
        assertEquals("doIt", eventResults[0].name);
        assertEquals("URI_A", eventResults[0].uri);
        assertEquals("TouchWrapper", eventResults[0].source.get("type"));
        assertEquals("MyTouchWrapper", eventResults[0].source.get("id"));
        Object[] richValue = (Object[]) eventResults[0].custom.get("value");
        assertNotNull(richValue);
        assertTrue(eventResults[0].custom.get("value") instanceof Object[]);
        assertEquals(2, richValue.length);
        assertEquals("MyTouchWrapper", richValue[0]);
        assertEquals(false, richValue[1]);
    }


    private static final String RICH_ARGUMENTS_WITH_PAYLOAD = "{" +
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
            "    \"items\": {" +
            "      \"type\": \"TouchWrapper\"," +
            "      \"id\": \"MyTouchWrapper\"," +
            "      \"onPress\": {" +
            "        \"type\": \"A:doIt\"," +
            "        \"positions\": [" +
            "          \"${payload.subarray}\"" +
            "        ]," +
            "        \"map\": {" +
            "          \"key\": \"${payload.key}\"," +
            "          \"timeStamp\": \"${payload.theTime}\"," +
            "          \"value\": [" +
            "            \"${payload.basePosition}\"," +
            "            \"${payload.basePosition + 10}\"" +
            "          ]" +
            "        }" +
            "      }" +
            "    }" +
            "  }" +
            "}";

    private static final String RICH_ARGUMENT_PAYLOAD = "{\"subarray\": [1,2,\"foo\"], \"key\": \"TheKey\", \"theTime\": 3147483647, \"basePosition\": 20})";

    /**
     * Verify that data-binding evaluation is occurring inside of a map and an array-ified array
     */
    @Test
    @SmallTest
    public void testRichArguments_Array() {
        // Set up the extension
        ExtensionCommandDefinition def = new ExtensionCommandDefinition("URI_A", "doIt")
                .arrayProperty("positions", false)
                .arrayProperty("missing", false)
                .property("map", null, false);

        mRootConfig.registerExtensionCommand(def);

        mTestContext.setDocumentPayload("payload", RICH_ARGUMENT_PAYLOAD);
        loadDocument(RICH_ARGUMENTS_WITH_PAYLOAD);


        // verify the document inflated
        Component component = mRootContext.getTopComponent();
        assertNotNull(component);

        // press the TouchWrapper and advance time
        tap(1, 1);

        mRootContext.doFrame(1);

        // Verify the callback happened
        assertNotNull(eventResults[0]);
        assertEquals("doIt", eventResults[0].name);
        assertEquals("URI_A", eventResults[0].uri);
        assertEquals("TouchWrapper", eventResults[0].source.get("type"));
        assertEquals("MyTouchWrapper", eventResults[0].source.get("id"));

        assertTrue(eventResults[0].custom.get("positions") instanceof Object[]);
        Object[] positions = (Object[]) eventResults[0].custom.get("positions");
        assertNotNull(positions);
        assertEquals(3, positions.length);
        assertEquals(1, positions[0]);
        assertEquals(2, positions[1]);
        assertEquals("foo", positions[2]);

        assertTrue(eventResults[0].custom.get("missing") instanceof Object[]);
        Object[] missing = (Object[]) eventResults[0].custom.get("missing");
        assertNotNull(missing);
        assertEquals(0, missing.length);

        assertTrue(eventResults[0].custom.get("map") instanceof Map);
        Map map = (Map) eventResults[0].custom.get("map");
        assertNotNull(map);
        assertEquals(3, map.size());
        assertEquals("TheKey", map.get("key"));
        assertEquals(3147483647L, map.get("timeStamp"));

        assertTrue(map.get("value") instanceof Object[]);
        Object[] subarray = (Object[]) map.get("value");
        assertNotNull(subarray);
        assertEquals(20, subarray[0]);
        assertEquals(30, subarray[1]);

    }


    private static final String SCROLL_VIEW = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.2\"," +
            "  \"extensions\": {" +
            "    \"name\": \"T\"," +
            "    \"uri\": \"aplext:Test\"" +
            "  }," +
            "  \"mainTemplate\": {" +
            "    \"item\": {" +
            "      \"type\": \"ScrollView\"," +
            "      \"id\": \"MyScrollView\"," +
            "      \"height\": 100," +
            "      \"onScroll\": [" +
            "        {" +
            "          \"type\": \"T:MyCommand\"," +
            "          \"id\": \"${event.source.id}\"," +
            "          \"value\": \"${event.source.value}\"" +
            "        }," +
            "        {" +
            "          \"type\": \"SetValue\"," +
            "          \"componentId\": \"MyFrame\"," +
            "          \"property\": \"backgroundColor\"," +
            "          \"value\": \"red\"" +
            "        }" +
            "      ]," +
            "      \"items\": {" +
            "        \"type\": \"Frame\"," +
            "        \"id\": \"MyFrame\"," +
            "        \"height\": \"200\"," +
            "        \"backgroundColor\": \"green\"" +
            "      }" +
            "    }" +
            "  }" +
            "}";

    /**
     * Run a custom command in fast mode.  Mark the custom command as NOT runnable in fast mode.
     * The command should be skipped and the following command should be executed.
     */
    @Test
    @SmallTest
    public void testFastMode_NotAllowed() {
        // Set up the extension
        ExtensionCommandDefinition def = new ExtensionCommandDefinition("aplext:Test", "MyCommand")
                .property("id", "NO_ID", true)
                .property("value", 0, false)
                .allowFastMode(false)
                .requireResolution(true);
        mRootConfig = mTestContext.buildRootConfig()
                .registerExtensionCommand(def);
        loadDocument(SCROLL_VIEW);

        // verify the document inflated
        Component component = mRootContext.getTopComponent();
        assertNotNull(component);
        assertEquals(1, component.getChildCount());
        Frame frame = (Frame) mRootContext.findComponentById("MyFrame");
        assertNotNull(frame);
        assertEquals(0xFF008000, frame.getBackgroundColor()); //green

        // Scroll and Advance Time
        component.update(UpdateType.kUpdateScrollPosition, 50);
        mRootContext.doFrame(1);

        // No event was generated
        assertNull(eventResults[0]);

        // The SetValue command will have run
        assertEquals(Color.RED, frame.getBackgroundColor());
    }


    /**
     * Run a custom command in fast mode.  Mark the custom command as runnable in fast mode,
     * but also mark it as requiring resolution.  Because it is fast mode, the command should
     * run and NOT require resolution.
     */
    @Test
    @SmallTest
    public void testFastMode_Allowed() {
        // Set up the extension
        ExtensionCommandDefinition def = new ExtensionCommandDefinition("aplext:Test", "MyCommand")
                .property("id", "NO_ID", true)
                .property("value", 0, false)
                .allowFastMode(true)
                .requireResolution(true);
        mRootConfig = mTestContext.buildRootConfig()
                .registerExtensionCommand(def);
        loadDocument(SCROLL_VIEW);

        // verify the document inflated
        Component component = mRootContext.getTopComponent();
        assertNotNull(component);
        assertEquals(1, component.getChildCount());
        Frame frame = (Frame) mRootContext.findComponentById("MyFrame");
        assertNotNull(frame);
        assertEquals(0xFF008000, frame.getBackgroundColor()); //green

        // Scroll and Advance Time
        component.update(UpdateType.kUpdateScrollPosition, 50);
        mRootContext.doFrame(1);

        // Verify the callback happened
        assertNotNull(eventResults[0]);
        assertEquals("MyCommand", eventResults[0].name);
        assertEquals("aplext:Test", eventResults[0].uri);
        assertEquals("ScrollView", eventResults[0].source.get("type"));
        assertEquals("MyScrollView", eventResults[0].source.get("id"));

        assertEquals(.5, eventResults[0].custom.get("value")); // scroll position of 50%

        // The SetValue command should have run
        assertEquals(Color.RED, frame.getBackgroundColor());
    }


    private static final String SCROLL_VIEW_BAD_COMMAND = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.2\"," +
            "  \"extensions\": {" +
            "    \"name\": \"T\"," +
            "    \"uri\": \"aplext:Test\"" +
            "  }," +
            "  \"mainTemplate\": {" +
            "    \"item\": {" +
            "      \"type\": \"ScrollView\"," +
            "      \"id\": \"MyScrollView\"," +
            "      \"height\": 100," +
            "      \"onScroll\": [" +
            "        {" +
            "          \"type\": \"T:MyCommand\"" +
            "        }," +
            "        {" +
            "          \"type\": \"SetValue\"," +
            "          \"componentId\": \"MyFrame\"," +
            "          \"property\": \"backgroundColor\"," +
            "          \"value\": \"red\"" +
            "        }" +
            "      ]," +
            "      \"items\": {" +
            "        \"type\": \"Frame\"," +
            "        \"id\": \"MyFrame\"," +
            "        \"height\": \"200\"," +
            "        \"backgroundColor\": \"green\"" +
            "      }" +
            "    }" +
            "  }" +
            "}";


    /**
     * Try to run a command that is missing a required property. A command which does not
     * follow properly the command definition will be skipped from the command execution queue.
     */
    @Test
    @SmallTest
    public void testProperty_MissingRequired() {
        // Set up the extension
        ExtensionCommandDefinition def = new ExtensionCommandDefinition("aplext:Test", "MyCommand")
                .property("id", "NO_ID", true)
                .property("value", 0, false)
                .allowFastMode(true)
                .requireResolution(true); // Resolution isn't required in fast mode even if set
        mRootConfig = mTestContext.buildRootConfig()
                .registerExtensionCommand(def);
        loadDocument(SCROLL_VIEW_BAD_COMMAND);

        // verify the document inflated
        Component component = mRootContext.getTopComponent();
        assertNotNull(component);
        assertEquals(1, component.getChildCount());
        Frame frame = (Frame) mRootContext.findComponentById("MyFrame");
        assertNotNull(frame);
        assertEquals(0xFF008000, frame.getBackgroundColor()); //green

        // Scroll and Advance Time
        component.update(UpdateType.kUpdateScrollPosition, 50);
        mRootContext.doFrame(1);

        // No event was generated
        assertNull(eventResults[0]);

        // The SetValue command should have run
        assertEquals(Color.RED, frame.getBackgroundColor());
    }


    /**
     * Run a command with missing properties, where those properties are not required.
     * Verify that the properties get assigned default values.
     */
    @Test
    @SmallTest
    public void testProperties_Optional() {
        // Set up the extension
        ExtensionCommandDefinition def = new ExtensionCommandDefinition("aplext:Test", "MyCommand")
                .property("id", "NO_ID", false)
                .property("value", -1001, false)
                .allowFastMode(true)
                .requireResolution(true); // Resolution isn't required in fast mode even if set
        mRootConfig = mTestContext.buildRootConfig()
                .registerExtensionCommand(def);
        loadDocument(SCROLL_VIEW_BAD_COMMAND);

        // verify the document inflated
        Component component = mRootContext.getTopComponent();
        assertNotNull(component);
        assertEquals(1, component.getChildCount());
        Frame frame = (Frame) mRootContext.findComponentById("MyFrame");
        assertNotNull(frame);
        assertEquals(0xFF008000, frame.getBackgroundColor()); //green

        // Scroll and Advance Time
        component.update(UpdateType.kUpdateScrollPosition, 50);
        mRootContext.doFrame(1);

        // Verify the callback happened
        assertNotNull(eventResults[0]);
        assertEquals("MyCommand", eventResults[0].name);
        assertEquals("aplext:Test", eventResults[0].uri);
        assertEquals("ScrollView", eventResults[0].source.get("type"));
        assertEquals("MyScrollView", eventResults[0].source.get("id"));

        assertEquals(-1001, eventResults[0].custom.get("value"));// Expect the default value
        assertEquals("NO_ID", eventResults[0].custom.get("id"));// Expect the default value

        // The SetValue command should have run
        assertEquals(Color.RED, frame.getBackgroundColor()); //green
    }


    private static final String MULTIPLE_NAMES_FOR_SAME_COMMAND = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.3\"," +
            "  \"extensions\": [" +
            "    {" +
            "      \"name\": \"A\"," +
            "      \"uri\": \"URI1\"" +
            "    }," +
            "    {" +
            "      \"name\": \"B\"," +
            "      \"uri\": \"URI1\"" +
            "    }" +
            "  ]," +
            "  \"mainTemplate\": {" +
            "    \"items\": {" +
            "      \"type\": \"TouchWrapper\"," +
            "      \"onPress\": [" +
            "        {" +
            "          \"type\": \"A:doIt\"," +
            "          \"value\": \"A\"" +
            "        }," +
            "        {" +
            "          \"type\": \"B:doIt\"," +
            "          \"value\": \"B\"" +
            "        }" +
            "      ]" +
            "    }" +
            "  }" +
            "}";

    @Test
    @SmallTest
    public void testExtension_MultipleNames() {
        // Set up the extension
        ExtensionCommandDefinition def = new ExtensionCommandDefinition("URI1", "doIt")
                .property("value", "none", true);
        mRootConfig = mTestContext.buildRootConfig()
                .registerExtensionCommand(def);
        loadDocument(MULTIPLE_NAMES_FOR_SAME_COMMAND);

        // verify the document inflated
        Component component = mRootContext.getTopComponent();
        assertNotNull(component);

        // press the TouchWrapper and advance time
        tap(1, 1);
        mRootContext.doFrame(1);

        // The callback was called twice
        assertEquals(2, count[0]);

        // Verify the A callback happened
        assertNotNull(eventResults[0]);
        assertEquals("doIt", eventResults[0].name);
        assertEquals("URI1", eventResults[0].uri);
        assertEquals("TouchWrapper", eventResults[0].source.get("type"));
        assertEquals("A", eventResults[0].custom.get("value"));

        // Verify the B callback happened
        assertNotNull(eventResults[1]);
        assertEquals("doIt", eventResults[1].name);
        assertEquals("URI1", eventResults[1].uri);
        assertEquals("TouchWrapper", eventResults[1].source.get("type"));
        assertEquals("B", eventResults[1].custom.get("value"));
    }


    final static String SETTINGS = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.2\"," +
            "  \"extension\": {" +
            "    \"uri\": \"aplext:Test\"," +
            "    \"name\": \"T\"" +
            "  }," +
            "  \"settings\": {" +
            "    \"T\": {" +
            "      \"keyA\": \"valueA\"," +
            "      \"keyB\": \"valueB\"" +
            "    }" +
            "  }," +
            "  \"mainTemplate\": {" +
            "    \"item\": {" +
            "      \"type\": \"Text\"" +
            "    }" +
            "  }" +
            "}";


    /**
     * Document does not provide extension settings
     */
    @Test
    @SmallTest
    public void testExtension_DocWithoutSettings() {
        loadDocument(BASIC);

        Content content = mTestContext.getContent();
        assertTrue(content.isReady());

        Set<String> requested = content.getExtensionRequests();
        assertNotNull(requested);
        assertEquals(1, requested.size());
        assertTrue(requested.contains("aplext:Test"));

        Map<String, Object> settings = content.getExtensionSettings("aplext:Test");
        assertNull(settings);
    }

    /**
     * Document provides extension settings
     */
    @Test
    @SmallTest
    public void testExtension_DocWithSettings() {

        loadDocument(SETTINGS);

        Content content = mTestContext.getContent();
        assertTrue(content.isReady());

        Set<String> requested = content.getExtensionRequests();
        assertNotNull(requested);
        assertEquals(1, requested.size());
        assertTrue(requested.contains("aplext:Test"));

        Map<String, Object> settings = content.getExtensionSettings("aplext:Test");
        assertNotNull(settings);
        assertEquals(2, settings.size());

        assertTrue(settings.containsKey("keyB"));
        assertEquals("valueA", settings.get("keyA"));
        assertTrue(settings.containsKey("keyB"));
        assertEquals("valueB", settings.get("keyB"));

    }


    static private final String WITH_CONFIG = "{ \n" +
            "  \"type\":\"APL\", \n" +
            "  \"version\":\"1.4\", \n" +
            "  \"extensions\":[ \n" +
            "    { \n" +
            "      \"uri\":\"_URIXdefault\", \n" +
            "      \"name\":\"Xdefault\" \n" +
            "    }, \n" +
            "    { \n" +
            "      \"uri\":\"_URIXbool\", \n" +
            "      \"name\":\"Xbool\" \n" +
            "    }, \n" +
            "    { \n" +
            "      \"uri\":\"_URIXstring\", \n" +
            "      \"name\":\"Xstring\" \n" +
            "    }, \n" +
            "    { \n" +
            "      \"uri\":\"_URIXnumber\", \n" +
            "      \"name\":\"Xnumber\" \n" +
            "    }\n" +
            "  ], \n" +
            "  \"mainTemplate\":{ \n" +
            "    \"item\":{ \n" +
            "      \"type\":\"Text\", \n" +
            "      \"when\": \"${environment.extension.Xdefault}\", \n" +
            "      \"text\":\"${environment.extension.Xbool}|${environment.extension.Xstring}|${environment.extension.Xnumber + 1}\" \n" +
            "    } \n" +
            "  } \n" +
            "}";

    /**
     * Register and extension with simple Object configuration.
     */
    @Test
    @SmallTest
    public void testExtension_WithSimpleConfig() {

        mRootConfig = mTestContext.buildRootConfig()
                .registerExtension("_URIXdefault")
                .registerExtensionEnvironment("_URIXbool", true)
                .registerExtensionEnvironment("_URIXstring", "dog")
                .registerExtensionEnvironment("_URIXnumber", 64);

        loadDocument(WITH_CONFIG);

        Text text = (Text) mRootContext.getTopComponent();
        assertEquals("true|dog|65", text.getProxy().getStyledText().getUnprocessedText());
    }

    /**
     * Register an extension with simple Object configuration and pass opaque data using flags.
     */
    @Test
    @SmallTest
    public void testExtension_WithSimpleConfigAndFlags() {
        testFlagsOfString();
        testFlagsOfMap();
    }

    private void testFlagsOfString() {
        mRootConfig = mTestContext.buildRootConfig()
                .registerExtensionFlags("_URIXdefault", "simpleFlagString");

        loadDocument(WITH_CONFIG);
        assertTrue(mRootConfig.getExtensionFlags("_URIXdefault") instanceof String);
        assertEquals("simpleFlagString", mRootConfig.getExtensionFlags("_URIXdefault"));
    }


    private void testFlagsOfMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("key1", 1);
        map.put("key2", true);
        map.put("key3", "three");

        mRootConfig = mTestContext.buildRootConfig()
                .registerExtensionFlags("_URIXdefault", map);

        loadDocument(WITH_CONFIG);
        assertTrue(mRootConfig.getExtensionFlags("_URIXdefault") instanceof HashMap);
        assertEquals(map, mRootConfig.getExtensionFlags("_URIXdefault"));
    }

    static private final String WITH_CONFIG_MAP = "{ \n" +
            "  \"type\":\"APL\", \n" +
            "  \"version\":\"1.4\", \n" +
            "  \"extensions\":[ \n" +
            "    { \n" +
            "      \"uri\":\"_URIXmap\", \n" +
            "      \"name\":\"Xmap\" \n" +
            "    } \n" +
            "  ], \n" +
            "  \"mainTemplate\":{ \n" +
            "    \"item\":{ \n" +
            "      \"type\":\"Text\", \n" +
            "      \"when\": \"${environment.extension.Xmap.cfg3}\", \n" +
            "      \"text\":\"${environment.extension.Xmap.cfg3}|${environment.extension.Xmap.cfg1}|${environment.extension.Xmap.cfg2 + 1}\" \n" +
            "    } \n" +
            "  } \n" +
            "}";

    /**
     * Register an extension with map configuration values.
     */
    @Test
    @SmallTest
    public void testExtension_WithConfigMap() {

        Map<String, Object> map = new HashMap<>();
        map.put("cfg1", "dog");
        map.put("cfg2", 64);
        map.put("cfg3", true);

        mRootConfig = mTestContext.buildRootConfig()
                .registerExtension("_URIXmap").registerExtensionEnvironment("_URIXmap", map);

        loadDocument(WITH_CONFIG_MAP);

        Text text = (Text) mRootContext.getTopComponent();
        // verify the environment has configuration values for the extension name
        assertEquals("true|dog|65", text.getProxy().getStyledText().getUnprocessedText());
    }

    void tap(float x, float y) {
        mTestContext.getRootContext().handlePointer(Pointer.create(0, PointerType.kTouchPointer, PointerEventType.kPointerDown, x, y));
        mTestContext.getRootContext().handlePointer(Pointer.create(0, PointerType.kTouchPointer, PointerEventType.kPointerUp, x, y));
    }

}





