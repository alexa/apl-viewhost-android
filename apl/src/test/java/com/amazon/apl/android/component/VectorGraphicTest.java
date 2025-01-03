/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.Content;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.VectorGraphic;
import com.amazon.apl.android.graphic.APLVectorGraphicView;
import com.amazon.apl.android.graphic.GraphicContainerElement;
import com.amazon.apl.android.primitive.UrlRequests;
import com.amazon.apl.android.providers.IDataRetriever;
import com.amazon.apl.android.providers.IDataRetrieverProvider;
import com.amazon.apl.android.scaling.ViewportMetrics;
import com.amazon.apl.enums.ComponentType;
import com.amazon.apl.enums.ScreenShape;
import com.amazon.apl.enums.VectorGraphicAlign;
import com.amazon.apl.enums.VectorGraphicScale;
import com.amazon.apl.enums.ViewportMode;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;


public class VectorGraphicTest extends AbstractComponentUnitTest<APLVectorGraphicView, VectorGraphic> {

    private static final String DUMMY_GRAPHIC = "icon-battery";
    private static final String DUMMY_URL = "http://example.xyz";

    private static final String OPTION_HTTP_GRAPHIC = "{" +
            "  \"type\": \"AVG\"," +
            "  \"version\": \"1.0\"," +
            "  \"height\": 24," +
            "  \"width\": 24," +
            "  \"viewportWidth\": 24," +
            "  \"viewportHeight\": 24," +
            "  \"items\": [" +
            "    {" +
            "      \"type\": \"group\"," +
            "      \"pivotX\": 12.0," +
            "      \"pivotY\": 12.0," +
            "      \"rotation\": 45.0," +
            "      \"items\": [" +
            "        {" +
            "          \"type\": \"path\"," +
            "          \"fill\": \"blue\"," +
            "          \"fillOpacity\": 0.3," +
            "          \"pathData\": \"M15.67,4H14V2h-4v2H8.33C7.6,4 7,4.6 7,5.33V9h4.93L13,7v2h4V5.33C17,4.6 16.4,4 15.67,4z\"" +
            "        }," +
            "        {" +
            "          \"type\": \"path\"," +
            "          \"fill\": \"blue\"," +
            "          \"pathData\": \"M13,12.5h2L11,20v-5.5H9L11.93,9H7v11.67C7,21.4 7.6,22 8.33,22h7.33c0.74,0 1.34,-0.6 1.34,-1.33V9h-4v3.5z\"" +
            "        }" +
            "      ]" +
            "    }" +
            "  ]" +
            "}";

    @Before
    public void doBefore() {
        REQUIRED_PROPERTIES = ""; // no required properties in Vector Graphic Component.
        OPTIONAL_PROPERTIES =
                " \"source\": \"" + DUMMY_GRAPHIC + "\"," +
                        " \"width\": \"100\"," +
                        " \"height\": \"100\"," +
                        " \"scale\": \"best-fit\"";

        //the graphics property.
        OPTIONAL_TEMPLATE_PROPERTIES = "  \"graphics\": {\n" +
                "    \"icon-battery\": {\n" +
                "      \"type\": \"AVG\",\n" +
                "      \"version\": \"1.0\",\n" +
                "      \"height\": 24,\n" +
                "      \"width\": 24,\n" +
                "      \"viewportWidth\": 24,\n" +
                "      \"viewportHeight\": 24,\n" +
                "      \"items\": [\n" +
                "        {\n" +
                "          \"type\": \"group\",\n" +
                "          \"pivotX\": 12.0,\n" +
                "          \"pivotY\": 12.0,\n" +
                "          \"rotation\": 45.0,\n" +
                "          \"items\": [\n" +
                "            {\n" +
                "              \"type\": \"path\",\n" +
                "              \"fill\": \"blue\",\n" +
                "              \"fillOpacity\": 0.3,\n" +
                "              \"pathData\": \"M15.67,4H14V2h-4v2H8.33C7.6,4 7,4.6 7,5.33V9h4.93L13,7v2h4V5.33C17,4.6 16.4,4 15.67,4z\",\n" +
                "              \"strokeDashArray\": [1,3,5,6],\n" +
                "              \"strokeDashOffset\": 3,\n" +
                "              \"strokeMiterLimit\":2,\n" +
                "              \"strokeLineJoin\": \"round\",\n" +
                "              \"strokeLineCap\": \"round\",\n" +
                "              \"pathLength\":20\n" +
                "            },\n" +
                "            {\n" +
                "              \"type\": \"path\",\n" +
                "              \"fill\": \"black\",\n" +
                "              \"pathData\": \"M13,12.5h2L11,20v-5.5H9L11.93,9H7v11.67C7,21.4 7.6,22 8.33,22h7.33c0.74,0 1.34,-0.6 1.34,-1.33V9h-4v3.5z\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"type\": \"text\",\n" +
                "              \"fill\": \"#ffffff\",\n" +
                "              \"fillOpacity\": 0.6,\n" +
                "              \"fontFamily\": \"amazon-ember\",\n" +
                "              \"fontSize\": 20,\n" +
                "              \"fontStyle\": \"italic\",\n" +
                "              \"fontWeight\": \"800\",\n" +
                "              \"letterSpacing\": 1,\n" +
                "              \"stroke\": \"#ffffff\",\n" +
                "              \"strokeOpacity\": 0.4,\n" +
                "              \"strokeWidth\": 3,\n" +
                "              \"text\": \"message\",\n" +
                "              \"textAnchor\": \"middle\",\n" +
                "              \"x\": 50,\n" +
                "              \"y\": 50\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  }";
    }

    /**
     * Test Data Retriever Provider.
     */
    private static class TestDataRetrieverProvider implements IDataRetrieverProvider {
        @Override
        public IDataRetriever get() {
            return new TestDataRetriever(OPTION_HTTP_GRAPHIC, false);
        }
    }

    private static class TestDataRetriever implements IDataRetriever {
        boolean mIsError;
        String mResult;

        TestDataRetriever(String result, boolean isError) {
            mResult = result;
            mIsError = isError;
        }

        @Override
        public void fetch(String source, Callback callback) {
            if (mIsError)
                callback.error();
            else
                callback.success(mResult);
        }

        @Override
        public void cancelAll() {

        }
    }


    @Override
    String getComponentType() {
        return "VectorGraphic";
    }

    @Override
    void testProperties_required(VectorGraphic component) {
        assertEquals(ComponentType.kComponentTypeVectorGraphic, component.getComponentType());
    }

    @Override
    void testProperties_optionalDefaultValues(VectorGraphic component) {
        assertEquals("", component.getSourceRequest().url());
        assertEquals(VectorGraphicAlign.kVectorGraphicAlignCenter, component.getAlign());
        assertEquals(VectorGraphicScale.kVectorGraphicScaleNone, component.getScale());
    }

    @Override
    void testProperties_optionalExplicitValues(VectorGraphic component) {
        assertEquals(DUMMY_GRAPHIC, component.getSourceRequest().url());
        assertEquals(VectorGraphicAlign.kVectorGraphicAlignCenter, component.getAlign());
        assertEquals(VectorGraphicScale.kVectorGraphicScaleBestFit, component.getScale());
        GraphicContainerElement root = component.getOrCreateGraphicContainerElement();
        assertNotNull(root);
        assertEquals(1, root.getChildren().size());
    }

    @Test
    public void testSourceProperty() {
        OPTIONAL_PROPERTIES =
                " \"source\": \"" + DUMMY_URL + "\"," +
                        " \"width\": \"100\"," +
                        " \"height\": \"100\"," +
                        " \"scale\": \"best-fit\"";

        String doc = buildDocument(BASE_DOC, REQUIRED_PROPERTIES, OPTIONAL_PROPERTIES, "");
        inflateDocument(doc);
        VectorGraphic component = getTestComponent();
        assertNotNull(component);
        assertEquals(ComponentType.kComponentTypeVectorGraphic, component.getComponentType());
        assertEquals(VectorGraphicAlign.kVectorGraphicAlignCenter, component.getAlign());
        assertEquals(VectorGraphicScale.kVectorGraphicScaleBestFit, component.getScale());
        assertNull(component.getOrCreateGraphicContainerElement());
    }

    @Test
    public void testProperties_sourceWithHeaders() {
        String headerKey = "headerKey";
        String headerValue = "headerValue";
        OPTIONAL_PROPERTIES =
                " \"source\": {\"url\": \"" + DUMMY_URL + "\", \"headers\": [\"" + headerKey + ": " + headerValue + "\"]}," +
                        " \"width\": \"100\"," +
                        " \"height\": \"100\"," +
                        " \"scale\": \"best-fit\"";

        String doc = buildDocument(BASE_DOC, REQUIRED_PROPERTIES, OPTIONAL_PROPERTIES, "");
        inflateDocument(doc);
        VectorGraphic component = getTestComponent();
        assertNotNull(component);
        UrlRequests.UrlRequest sourceRequest = component.getSourceRequest();
        assertEquals(DUMMY_URL, sourceRequest.url());
        assertTrue(sourceRequest.headers().containsKey(headerKey));
        assertEquals(headerValue, sourceRequest.headers().get(headerKey));
    }

    @Override
    protected void inflateDocument(String document) {
        Content content = null;
        try {
            content = Content.create(String.format(document, getComponentType()));
        } catch (Content.ContentException e) {
            Assert.fail(e.getMessage());
        }
        // create a RootContext
        // TODO metrics init from device for instrumented tests
        APLOptions options = APLOptions.builder()
                .dataRetrieverProvider(new TestDataRetrieverProvider())
                .build();
        mMetrics = ViewportMetrics.builder()
                .width(2048)
                .height(1024)
                .dpi(160)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeHub)
                .build();

        RootConfig rootConfig = RootConfig.create("Unit Test", "1.0");
        when(mMetricsRecorder.createCounter(anyString())).thenReturn(mCounter);
        when(mMetricsRecorder.startTimer(anyString(), any())).thenReturn(mTimer);
        mRootContext = RootContext.create(mMetrics, content, rootConfig, options, mAPLPresenter, mMetricsRecorder, mFluidityIncidentReporter);

        if (mRootContext.getNativeHandle() == 0) {
            Assert.fail("The document failed to load.");
        }
    }
}

