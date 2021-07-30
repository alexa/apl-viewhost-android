/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;


import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amazon.apl.android.VectorGraphic;
import com.amazon.apl.android.graphic.APLVectorGraphicView;
import com.amazon.apl.android.providers.IDataRetriever;
import com.amazon.apl.android.providers.IDataRetrieverProvider;

import org.junit.Before;
import org.junit.runner.RunWith;

import static org.junit.Assert.fail;


@RunWith(AndroidJUnit4.class)
public class VectorGraphicViewTest extends AbstractComponentViewTest<APLVectorGraphicView, VectorGraphic> {

    private static final String DUMMY_GRAPHIC = "icon-battery";
    private static final String DUMMY_URL = "http://example.xyz";
    //
    private static final String  OPTION_HTTP_GRAPHIC = "{\n" +
            "  \"type\": \"AVG\",\n" +
            "  \"version\": \"1.0\",\n" +
            "  \"height\": 24,\n" +
            "  \"width\": 24,\n" +
            "  \"viewportWidth\": 24,\n" +
            "  \"viewportHeight\": 24,\n" +
            "  \"items\": [\n" +
            "    {\n" +
            "      \"type\": \"group\",\n" +
            "      \"pivotX\": 12.0,\n" +
            "      \"pivotY\": 12.0,\n" +
            "      \"rotation\": 45.0,\n" +
            "      \"items\": [\n" +
            "        {\n" +
            "          \"type\": \"path\",\n" +
            "          \"fill\": \"blue\",\n" +
            "          \"fillOpacity\": 0.3,\n" +
            "          \"pathData\": \"M15.67,4H14V2h-4v2H8.33C7.6,4 7,4.6 7,5.33V9h4.93L13,7v2h4V5.33C17,4.6 16.4,4 15.67,4z\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"type\": \"path\",\n" +
            "          \"fill\": \"blue\",\n" +
            "          \"pathData\": \"M13,12.5h2L11,20v-5.5H9L11.93,9H7v11.67C7,21.4 7.6,22 8.33,22h7.33c0.74,0 1.34,-0.6 1.34,-1.33V9h-4v3.5z\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    private String OPTIONAL_TEMPLATE_PROPERTIES;

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
                "              \"pathData\": \"M15.67,4H14V2h-4v2H8.33C7.6,4 7,4.6 7,5.33V9h4.93L13,7v2h4V5.33C17,4.6 16.4,4 15.67,4z\"\n" +
                "            },\n" +
                "            {\n" +
                "              \"type\": \"path\",\n" +
                "              \"fill\": \"black\",\n" +
                "              \"pathData\": \"M13,12.5h2L11,20v-5.5H9L11.93,9H7v11.67C7,21.4 7.6,22 8.33,22h7.33c0.74,0 1.34,-0.6 1.34,-1.33V9h-4v3.5z\"\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  }";
    }

    @Override
    Class<APLVectorGraphicView> getViewClass() {
        return APLVectorGraphicView.class;
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


    /**
     * @return The string representation used in the APL document for this component.  For example
     * when testing the Text Component this method should return "Text".
     */
    @Override
    String getComponentType() {
        return "VectorGraphic";
    }

    @Override
    void testView_applyProperties(APLVectorGraphicView view) {
        // TODO implement test
    }

}

