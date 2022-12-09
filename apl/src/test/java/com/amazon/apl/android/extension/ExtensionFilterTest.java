/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.extension;

import android.graphics.Bitmap;
import androidx.annotation.Nullable;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.APLTestContext;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.ExtensionFilterDefinition;
import com.amazon.apl.android.Image;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.dependencies.ExtensionFilterParameters;
import com.amazon.apl.android.dependencies.IExtensionImageFilterCallback;
import com.amazon.apl.android.primitive.Filters;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.android.utils.TestClock;
import com.amazon.apl.enums.ComponentType;
import com.amazon.apl.enums.FilterType;
import com.amazon.apl.enums.ImageCount;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

public class ExtensionFilterTest extends ViewhostRobolectricTest {

    private APLTestContext mTestContext;
    private APLOptions mOptions;
    private RootConfig mRootConfig;
    private RootContext mRootContext;
    private LatchCallback mLatchCallback;

    private static class LatchCallback implements IExtensionImageFilterCallback {
        CountDownLatch latch = new CountDownLatch(1);

        @Override
        public Bitmap processImage(@Nullable Bitmap sourceBitmap, @Nullable Bitmap destinationBitmap, ExtensionFilterParameters params) {
            latch.countDown();
            return sourceBitmap;
        }
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mTestContext = new APLTestContext();
        mLatchCallback = new LatchCallback();
        mOptions = APLOptions.builder()
                .extensionImageFilterCallback(mLatchCallback)
                .aplClockProvider(callback -> new TestClock(callback))
                .build();
        mRootConfig = RootConfig.create("Test", "1.3");
    }

    private void loadDocument(String document) {
        mRootContext = mTestContext
                .setContext(InstrumentationRegistry.getTargetContext())
                .setDocument(document)
                .setAplOptions(mOptions)
                .setRootConfig(mRootConfig)
                .buildRootContext();
    }

    @Test
    @SmallTest
    public void test_filterDefSimple() {
        ExtensionFilterDefinition def = new ExtensionFilterDefinition("aplext:edgedetectorfilters:11", "Edges", ImageCount.ONE)
                .property("lower", 0.1)
                .property("upper", 0.9);

        assertEquals("Edges", def.getName());
        assertEquals("aplext:edgedetectorfilters:11", def.getURI());
        assertEquals(2, def.getPropertyCount());
        double lower = def.<Double>getPropertyValue("lower");
        assertEquals(0.1, lower, 0.0d);
        double upper = def.<Double>getPropertyValue("upper");
        assertEquals(0.9, upper, 0.0d);
    }

    @Test
    @SmallTest
    public void test_filterDefIllegal() {
        ExtensionFilterDefinition def = new ExtensionFilterDefinition("aplext:edgedetectorfilters:11", "Edges", ImageCount.TWO)
                .property("type", 100)
                .property("when", false)
                .property("source", -1);

        assertEquals(0, def.getPropertyCount());

        //TODO ASSERT_TRUE(LogMessage());
    }

    @Test
    @SmallTest
    public void test_commandMissingProperty() {
        ExtensionFilterDefinition def = new ExtensionFilterDefinition("aplext:edgedetectorfilters:11", "Edges", ImageCount.ZERO);

        assertEquals("Edges", def.getName());
        assertEquals("aplext:edgedetectorfilters:11", def.getURI());
        assertEquals(0, def.getPropertyCount());
        assertNull(def.<Integer>getPropertyValue("foo"));
        assertNull(def.<Integer>getPropertyValue("bar"));
    }


    static final String EXTENSION_FILTER = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"1.4\",\n" +
            "  \"extensions\": {\n" +
            "    \"name\": \"Canny\",\n" +
            "    \"uri\": \"aplext:CannyEdgeFilters:10\"\n" +
            "  },\n" +
            "  \"mainTemplate\": {\n" +
            "    \"items\": {\n" +
            "      \"type\": \"Image\",\n" +
            "      \"filters\": {\n" +
            "        \"type\": \"Canny:FindEdges\",\n" +
            "        \"min\": 0.2,\n" +
            "        \"max\": 0.8\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

    /**
     * Test an extension that operates on a single image from the source array.
     * The kFilterPropertySource property will be generated with a default value of -1.
     */
    @Test
    @SmallTest
    public void test_ExtensionWithSource() {
        ExtensionFilterDefinition def = new ExtensionFilterDefinition("aplext:CannyEdgeFilters:10",
                "FindEdges", ImageCount.ONE)
                .property("min", 0.1)
                .property("max", 0.9);
        mRootConfig.registerExtensionFilter(def);

        loadDocument(EXTENSION_FILTER);

        Component component = mRootContext.getTopComponent();
        assertNotNull(component);
        assertEquals(ComponentType.kComponentTypeImage, component.getComponentType());
        Image image = (Image) component;

        Filters filters = image.getFilters();
        assertNotNull(filters);
        assertEquals(1, filters.size());

        Filters.Filter filter = filters.at(0);
        assertEquals(FilterType.kFilterTypeExtension, filter.filterType());
        assertEquals("aplext:CannyEdgeFilters:10", filter.extensionURI());
        assertEquals("FindEdges", filter.name());
        assertEquals(Integer.valueOf(-1), filter.source());
        assertNull(filter.destination());
        Map<String, Object> bag = filter.extensionParams();
        assertNotNull(bag);
        assertEquals(2, bag.size());
        assertEquals(.2, bag.get("min"));
        assertEquals(.8, bag.get("max"));

        // TODO Inject the async task and check the parameters
//        assertEquals(0, mLatchCallback.latch.getCount());

    }

    final static String EXTENSION_TWO_IMAGES_FILTER = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"1.4\",\n" +
            "  \"extensions\": {\n" +
            "    \"name\": \"Morph\",\n" +
            "    \"uri\": \"aplext:MorphingFilters:10\"\n" +
            "  },\n" +
            "  \"mainTemplate\": {\n" +
            "    \"items\": {\n" +
            "      \"type\": \"Image\",\n" +
            "      \"filters\": {\n" +
            "        \"type\": \"Morph:MergeTwo\",\n" +
            "        \"amount\": 0.25,\n" +
            "        \"source\": 1\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";


    /**
     * Test an extension that combines two images from the source array.
     * The kFilterPropertySource property will be generated with a default value of -1.
     * The kFilterPropertyDestination property will be generated with a default value of -2.
     */
    @Test
    @SmallTest
    public void test_ExtensionWithSourceAndDestination() {
        ExtensionFilterDefinition def = new ExtensionFilterDefinition("aplext:MorphingFilters:10",
                "MergeTwo", ImageCount.TWO)
                .property("amount", 0.5);
        mRootConfig.registerExtensionFilter(def);

        loadDocument(EXTENSION_TWO_IMAGES_FILTER);

        Component component = mRootContext.getTopComponent();
        assertNotNull(component);
        assertEquals(ComponentType.kComponentTypeImage, component.getComponentType());
        Image image = (Image) component;

        Filters filters = image.getFilters();
        assertNotNull(filters);
        assertEquals(1, filters.size());

        Filters.Filter filter = filters.at(0);
        assertEquals(FilterType.kFilterTypeExtension, filter.filterType());
        assertEquals("aplext:MorphingFilters:10", filter.extensionURI());
        assertEquals("MergeTwo", filter.name());
        assertEquals(Integer.valueOf(1), filter.source());
        assertEquals(Integer.valueOf(-2), filter.destination());
        Map<String, Object> bag = filter.extensionParams();
        assertNotNull(bag);
        assertEquals(1, bag.size());
        assertEquals(.25, bag.get("amount"));

        // TODO Inject the async task and check the parameters
//        assertEquals(0, mLatchCallback.latch.getCount());
    }


    static final String EXTENSION_ZERO_IMAGES_FILTER = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"1.4\",\n" +
            "  \"extensions\": {\n" +
            "    \"name\": \"Foo\",\n" +
            "    \"uri\": \"aplext:NoiseGeneration:10\"\n" +
            "  },\n" +
            "  \"mainTemplate\": {\n" +
            "    \"items\": {\n" +
            "      \"type\": \"Image\",\n" +
            "      \"filters\": {\n" +
            "        \"type\": \"Foo:Perlin\",\n" +
            "        \"width\": 256,\n" +
            "        \"height\": 256,\n" +
            "        \"cellSize\": 12.2,\n" +
            "        \"attenuation\": 3.2,\n" +
            "        \"color\": true\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

    /**
     * This extension does not take any input images; it only generates an output image.
     */
    @Test
    @SmallTest
    public void test_ExtensionNoInputImages() {
        ExtensionFilterDefinition definition = new ExtensionFilterDefinition("aplext:NoiseGeneration:10",
                "Perlin", ImageCount.ZERO)
                .property("width", 128)
                .property("height", 128)
                .property("cellSize", 8)
                .property("attenuation", 0.4)
                .property("color", false);
        mRootConfig.registerExtensionFilter(definition);

        loadDocument(EXTENSION_ZERO_IMAGES_FILTER);

        Component component = mRootContext.getTopComponent();
        assertNotNull(component);
        assertEquals(ComponentType.kComponentTypeImage, component.getComponentType());
        Image image = (Image) component;

        Filters filters = image.getFilters();
        assertNotNull(filters);
        assertEquals(1, filters.size());

        Filters.Filter filter = filters.at(0);
        assertEquals(FilterType.kFilterTypeExtension, filter.filterType());
        assertEquals("aplext:NoiseGeneration:10", filter.extensionURI());
        assertEquals("Perlin", filter.name());
        assertNull(filter.source());
        assertNull(filter.destination());
        Map<String, Object> bag = filter.extensionParams();
        assertNotNull(bag);
        assertEquals(5, bag.size());
        assertEquals(256, bag.get("width"));
        assertEquals(256, bag.get("height"));
        assertEquals(12.2, bag.get("cellSize"));
        assertEquals(3.2, bag.get("attenuation"));
        assertEquals(true, bag.get("color"));

        // TODO Inject the async task and check the parameters
//        assertEquals(0, mLatchCallback.latch.getCount());
    }


}
