/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.extension;

import androidx.test.platform.app.InstrumentationRegistry;

import com.amazon.apl.android.APLController;
import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.APLTestContext;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.ExtensionClient;
import com.amazon.apl.android.ExtensionEventHandler;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.RuntimeConfig;
import com.amazon.apl.android.Text;
import com.amazon.apl.android.font.CompatFontResolver;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.android.utils.TestClock;
import com.amazon.apl.enums.ComponentType;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

public class ExtensionLiveDataTest extends ViewhostRobolectricTest {
    private static final String NESTED_DYNAMIC_DATA_DOC = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.7\"," +
            "  \"theme\": \"dark\"," +
            "  \"extensions\": [" +
            "    {" +
            "      \"uri\": \"alexaext:test:10\"," +
            "      \"name\": \"FakeDataStore\"" +
            "    }" +
            "  ]," +
            "  \"mainTemplate\": {" +
            "    \"items\": {" +
            "      \"type\": \"Container\"," +
            "      \"items\": [" +
            "        {" +
            "          \"type\": \"Text\"," +
            "          \"id\": \"MyText\"," +
            "          \"text\": \"${environment.extension.FakeDataStore ? 'Present' : 'Missing'}\"" +
            "        }," +
            "        {" +
            "          \"type\": \"Container\"," +
            "          \"id\": \"TopLevelContainer\"," +
            "          \"data\": \"${outerItems}\"," +
            "          \"items\": {" +
            "            \"type\": \"Container\"," +
            "            \"id\": \"OuterContainer\"," +
            "            \"items\": [" +
            "              {" +
            "                \"type\": \"Text\"," +
            "                \"id\": \"OuterText\"," +
            "                \"text\": \"* ${data.name}\"" +
            "              }," +
            "              {" +
            "                \"type\": \"Container\"," +
            "                \"id\": \"InnerContainer\"," +
            "                \"data\": \"${innerItems}\"," +
            "                \"items\": [" +
            "                  {" +
            "                    \"type\": \"Text\"," +
            "                    \"id\": \"InnerText\"," +
            "                    \"text\": \" - ${data.name}\"" +
            "                  }" +
            "                ]" +
            "              }" +
            "            ]" +
            "          }" +
            "        }" +
            "      ]" +
            "    }" +
            "  }" +
            "}";

    private static final String REGISTRATION_SUCCESS_MESSAGE = "{" +
            "  \"method\": \"RegisterSuccess\"," +
            "  \"version\": \"1.0\"," +
            "  \"token\": \"fc982c79-8644-4fd0-9b02-501ded9d9f44\"," +
            "  \"environment\": {" +
            "    \"version\": \"alexaext-datastore-1.0\"" +
            "  }," +
            "  \"schema\": {" +
            "    \"type\": \"Schema\"," +
            "    \"version\": \"1.0\"," +
            "    \"uri\": \"alexaext:test:10\"," +
            "    \"types\": [" +
            "      {" +
            "        \"name\": \"ItemData\"," +
            "        \"properties\": {" +
            "          \"dataModel\": {" +
            "            \"type\": \"object\"," +
            "            \"required\": true," +
            "            \"default\": \"{}\"" +
            "          }" +
            "        }" +
            "      }" +
            "    ]," +
            "    \"events\": []," +
            "    \"commands\": []," +
            "    \"liveData\": [" +
            "      {" +
            "        \"name\": \"outerItems\"," +
            "        \"type\": \"ItemData[]\"" +
            "      }," +
            "      {" +
            "        \"name\": \"innerItems\"," +
            "        \"type\": \"ItemData[]\"" +
            "      }" +
            "    ]" +
            "  }" +
            "}";

    private static final String OUTER_LIVE_DATA_UPDATE = "{" +
            "  \"version\": \"1.0\"," +
            "  \"method\": \"LiveDataUpdate\"," +
            "  \"name\": \"outerItems\"," +
            "  \"target\": \"alexaext:test:10\"," +
            "  \"operations\": [" +
            "    {" +
            "      \"type\": \"Clear\"" +
            "    }," +
            "    {" +
            "      \"type\": \"Insert\"," +
            "      \"index\": 0," +
            "      \"item\": [" +
            "        {" +
            "          \"dataModel\": {" +
            "            \"name\": \"Outer1\"" +
            "          }," +
            "          \"name\": \"Outer1\"" +
            "        }," +
            "        {" +
            "          \"dataModel\": {" +
            "            \"name\": \"Outer2\"" +
            "          }," +
            "          \"name\": \"Outer2\"" +
            "        }" +
            "      ]" +
            "    }" +
            "  ]" +
            "}";

    private static final String INNER_LIVE_DATA_UPDATE = "{" +
            "  \"version\": \"1.0\"," +
            "  \"method\": \"LiveDataUpdate\"," +
            "  \"name\": \"innerItems\"," +
            "  \"target\": \"alexaext:test:10\"," +
            "  \"operations\": [" +
            "    {" +
            "      \"type\": \"Clear\"" +
            "    }," +
            "    {" +
            "      \"type\": \"Insert\"," +
            "      \"index\": 0," +
            "      \"item\": [" +
            "        {" +
            "          \"dataModel\": {" +
            "            \"name\": \"Inner1\"" +
            "          }," +
            "          \"name\": \"Inner1\"" +
            "        }," +
            "        {" +
            "          \"dataModel\": {" +
            "            \"name\": \"Inner2\"" +
            "          }," +
            "          \"name\": \"Inner2\"" +
            "        }" +
            "      ]" +
            "    }" +
            "  ]" +
            "}";

    private static final String OUTER_LIVE_DATA_UPDATE2 = "{" +
            "  \"version\": \"1.0\"," +
            "  \"method\": \"LiveDataUpdate\"," +
            "  \"name\": \"outerItems\"," +
            "  \"target\": \"alexaext:test:10\"," +
            "  \"operations\": [" +
            "    {" +
            "      \"type\": \"Clear\"" +
            "    }," +
            "    {" +
            "      \"type\": \"Insert\"," +
            "      \"index\": 0," +
            "      \"item\": [" +
            "        {" +
            "          \"dataModel\": {" +
            "            \"name\": \"Outer3\"" +
            "          }," +
            "          \"name\": \"Outer3\"" +
            "        }," +
            "        {" +
            "          \"dataModel\": {" +
            "            \"name\": \"Outer4\"" +
            "          }," +
            "          \"name\": \"Outer4\"" +
            "        }" +
            "      ]" +
            "    }" +
            "  ]" +
            "}";

    private static final String INNER_LIVE_DATA_UPDATE2 = "{" +
            "  \"version\": \"1.0\"," +
            "  \"method\": \"LiveDataUpdate\"," +
            "  \"name\": \"innerItems\"," +
            "  \"target\": \"alexaext:test:10\"," +
            "  \"operations\": [" +
            "    {" +
            "      \"type\": \"Clear\"" +
            "    }," +
            "    {" +
            "      \"type\": \"Insert\"," +
            "      \"index\": 0," +
            "      \"item\": [" +
            "        {" +
            "          \"dataModel\": {" +
            "            \"name\": \"Inner3\"" +
            "          }," +
            "          \"name\": \"Inner3\"" +
            "        }," +
            "        {" +
            "          \"dataModel\": {" +
            "            \"name\": \"Inner4\"" +
            "          }," +
            "          \"name\": \"Inner4\"" +
            "        }" +
            "      ]" +
            "    }" +
            "  ]" +
            "}";

    static {
        RuntimeConfig runtimeConfig = RuntimeConfig.builder().fontResolver(new CompatFontResolver()).build();
        APLController.initializeAPL(InstrumentationRegistry.getInstrumentation().getContext(), runtimeConfig);
    }

    private APLTestContext mTestContext;
    private APLOptions.Builder mOptionsBuilder;
    private RootConfig mRootConfig;
    private RootContext mRootContext;
    private int mTicks;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mTestContext = new APLTestContext();
        mOptionsBuilder = APLOptions.builder().aplClockProvider(callback -> new TestClock(callback));
        mRootConfig = RootConfig.create("Test", "1.3");
        mTicks = 0;
    }

    private void tick() {
        mTicks += 1;
        mRootContext.onTick(mTicks * 1_000_000);
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
    public void testExtensionFailingToLoad() {
        loadDocument(NESTED_DYNAMIC_DATA_DOC);

        Component component = mRootContext.findComponentById("MyText");
        assertNotNull(component);
        assertEquals(ComponentType.kComponentTypeText, component.getComponentType());

        Text myText = (Text) component;
        assertEquals("Missing", myText.getText());
    }

    @Test
    public void testExtensionSuccessfullyLoads() {
        mRootConfig.registerExtension("alexaext:test:10");
        loadDocument(NESTED_DYNAMIC_DATA_DOC);

        Component component = mRootContext.findComponentById("MyText");
        assertNotNull(component);
        assertEquals(ComponentType.kComponentTypeText, component.getComponentType());

        Text myText = (Text) component;
        assertEquals("Present", myText.getText());
    }

    @Test
    public void testExtensionProcessesRegistrationSuccess() {
        mRootConfig.registerExtension("alexaext:test:10");
        ExtensionClient client = ExtensionClient.create(mRootConfig, "alexaext:test:10");
        client.processMessage(mRootContext, REGISTRATION_SUCCESS_MESSAGE);

        loadDocument(NESTED_DYNAMIC_DATA_DOC);

        Component component = mRootContext.findComponentById("MyText");
        assertNotNull(component);
        assertEquals(ComponentType.kComponentTypeText, component.getComponentType());

        Text myText = (Text) component;
        assertEquals("Present", myText.getText());

        Component topLevelContainer = mRootContext.findComponentById("TopLevelContainer");
        assertNotNull(topLevelContainer);
        assertEquals(ComponentType.kComponentTypeContainer, topLevelContainer.getComponentType());
        assertEquals(0, topLevelContainer.getChildCount());
    }

    private void assertDynamicContent(String outerA, String outerB, String innerA, String innerB) {
        Component topLevelContainer = mRootContext.findComponentById("TopLevelContainer");
        assertNotNull(topLevelContainer);
        assertEquals(ComponentType.kComponentTypeContainer, topLevelContainer.getComponentType());
        assertEquals(2, topLevelContainer.getChildCount());

        Component outerContainerA = topLevelContainer.getChildAt(0);
        Component outerContainerB = topLevelContainer.getChildAt(1);
        assertEquals(2, outerContainerA.getChildCount());
        assertEquals(2, outerContainerB.getChildCount());

        Component innerContainerA = outerContainerA.getChildAt(1);
        Component innerContainerB = outerContainerB.getChildAt(1);
        assertEquals(2, innerContainerA.getChildCount());
        assertEquals(2, innerContainerB.getChildCount());

        String text = ((Text) innerContainerA.getChildAt(0)).getText();
        text = ((Text) innerContainerA.getChildAt(1)).getText();
        text = ((Text) innerContainerB.getChildAt(0)).getText();
        text = ((Text) innerContainerB.getChildAt(1)).getText();
        assertEquals("* " + outerA, ((Text) outerContainerA.getChildAt(0)).getText());
        assertEquals("* " + outerB, ((Text) outerContainerB.getChildAt(0)).getText());
        assertEquals("- " + innerA, ((Text) innerContainerA.getChildAt(0)).getText());
        assertEquals("- " + innerB, ((Text) innerContainerA.getChildAt(1)).getText());
        assertEquals("- " + innerA, ((Text) innerContainerB.getChildAt(0)).getText());
        assertEquals("- " + innerB, ((Text) innerContainerB.getChildAt(1)).getText());
    }

    @Test
    public void testExtensionPopulatesLiveData() {
        mRootConfig.registerExtension("alexaext:test:10");
        ExtensionClient client = ExtensionClient.create(mRootConfig, "alexaext:test:10");
        client.processMessage(mRootContext, REGISTRATION_SUCCESS_MESSAGE);

        loadDocument(NESTED_DYNAMIC_DATA_DOC);

        Text myText = (Text) mRootContext.findComponentById("MyText");
        assertEquals("Present", myText.getText());

        Component topLevelContainer = mRootContext.findComponentById("TopLevelContainer");
        assertNotNull(topLevelContainer);
        assertEquals(ComponentType.kComponentTypeContainer, topLevelContainer.getComponentType());
        assertEquals(0, topLevelContainer.getChildCount());

        tick();
        assertEquals(0, topLevelContainer.getChildCount());

        client.processMessage(mRootContext, OUTER_LIVE_DATA_UPDATE);
        client.processMessage(mRootContext, INNER_LIVE_DATA_UPDATE);
        assertEquals(0, topLevelContainer.getChildCount());

        tick();
        assertEquals(2, topLevelContainer.getChildCount());

        assertDynamicContent("Outer1", "Outer2", "Inner1", "Inner2");
    }

    @Test
    public void testChangesToInnerDataOnly() {
        mRootConfig.registerExtension("alexaext:test:10");
        ExtensionClient client = ExtensionClient.create(mRootConfig, "alexaext:test:10");
        client.processMessage(mRootContext, REGISTRATION_SUCCESS_MESSAGE);

        // Step 1: Doc is inflated with empty live data
        loadDocument(NESTED_DYNAMIC_DATA_DOC);

        client.processMessage(mRootContext, OUTER_LIVE_DATA_UPDATE);
        client.processMessage(mRootContext, INNER_LIVE_DATA_UPDATE);
        tick();
        assertDynamicContent("Outer1", "Outer2", "Inner1", "Inner2");

        client.processMessage(mRootContext, INNER_LIVE_DATA_UPDATE2);
        tick();
        assertDynamicContent("Outer1", "Outer2", "Inner3", "Inner4");

        client.processMessage(mRootContext, INNER_LIVE_DATA_UPDATE);
        tick();
        assertDynamicContent("Outer1", "Outer2", "Inner1", "Inner2");

        client.processMessage(mRootContext, INNER_LIVE_DATA_UPDATE2);
        tick();
        assertDynamicContent("Outer1", "Outer2", "Inner3", "Inner4");

        client.processMessage(mRootContext, INNER_LIVE_DATA_UPDATE);
        tick();
        assertDynamicContent("Outer1", "Outer2", "Inner1", "Inner2");
    }

    @Test
    public void testChangesToOuterDataOnly() {
        mRootConfig.registerExtension("alexaext:test:10");
        ExtensionClient client = ExtensionClient.create(mRootConfig, "alexaext:test:10");
        client.processMessage(mRootContext, REGISTRATION_SUCCESS_MESSAGE);

        loadDocument(NESTED_DYNAMIC_DATA_DOC);

        client.processMessage(mRootContext, OUTER_LIVE_DATA_UPDATE);
        client.processMessage(mRootContext, INNER_LIVE_DATA_UPDATE);
        tick();
        assertDynamicContent("Outer1", "Outer2", "Inner1", "Inner2");

        client.processMessage(mRootContext, OUTER_LIVE_DATA_UPDATE2);
        tick();
        assertDynamicContent("Outer3", "Outer4", "Inner1", "Inner2");

        client.processMessage(mRootContext, OUTER_LIVE_DATA_UPDATE);
        tick();
        assertDynamicContent("Outer1", "Outer2", "Inner1", "Inner2");

        client.processMessage(mRootContext, OUTER_LIVE_DATA_UPDATE2);
        tick();
        assertDynamicContent("Outer3", "Outer4", "Inner1", "Inner2");

        client.processMessage(mRootContext, OUTER_LIVE_DATA_UPDATE);
        tick();
        assertDynamicContent("Outer1", "Outer2", "Inner1", "Inner2");
    }

    @Test
    public void testChangesToBothInnerAndOuterData() {
        mRootConfig.registerExtension("alexaext:test:10");
        ExtensionClient client = ExtensionClient.create(mRootConfig, "alexaext:test:10");
        client.processMessage(mRootContext, REGISTRATION_SUCCESS_MESSAGE);

        loadDocument(NESTED_DYNAMIC_DATA_DOC);

        client.processMessage(mRootContext, OUTER_LIVE_DATA_UPDATE);
        client.processMessage(mRootContext, INNER_LIVE_DATA_UPDATE);
        tick();
        assertDynamicContent("Outer1", "Outer2", "Inner1", "Inner2");

        client.processMessage(mRootContext, OUTER_LIVE_DATA_UPDATE2);
        client.processMessage(mRootContext, INNER_LIVE_DATA_UPDATE2);
        tick();
        assertDynamicContent("Outer3", "Outer4", "Inner3", "Inner4");

        client.processMessage(mRootContext, OUTER_LIVE_DATA_UPDATE);
        client.processMessage(mRootContext, INNER_LIVE_DATA_UPDATE);
        tick();
        assertDynamicContent("Outer1", "Outer2", "Inner1", "Inner2");

        client.processMessage(mRootContext, OUTER_LIVE_DATA_UPDATE2);
        client.processMessage(mRootContext, INNER_LIVE_DATA_UPDATE2);
        tick();
        assertDynamicContent("Outer3", "Outer4", "Inner3", "Inner4");

        client.processMessage(mRootContext, OUTER_LIVE_DATA_UPDATE);
        tick();
        assertDynamicContent("Outer1", "Outer2", "Inner3", "Inner4");

        client.processMessage(mRootContext, INNER_LIVE_DATA_UPDATE);
        client.processMessage(mRootContext, OUTER_LIVE_DATA_UPDATE2);
        client.processMessage(mRootContext, INNER_LIVE_DATA_UPDATE2);
        tick();
        assertDynamicContent("Outer3", "Outer4", "Inner3", "Inner4");

        client.processMessage(mRootContext, INNER_LIVE_DATA_UPDATE);
        tick();
        assertDynamicContent("Outer3", "Outer4", "Inner1", "Inner2");

        client.processMessage(mRootContext, OUTER_LIVE_DATA_UPDATE);
        client.processMessage(mRootContext, OUTER_LIVE_DATA_UPDATE2);
        client.processMessage(mRootContext, INNER_LIVE_DATA_UPDATE2);
        tick();
        assertDynamicContent("Outer3", "Outer4", "Inner3", "Inner4");
    }
}
