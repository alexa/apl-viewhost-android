/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.touch;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.providers.IDataRetriever;
import com.amazon.apl.android.providers.IDataRetrieverProvider;

public class TouchableVectorGraphicViewTest extends TouchableViewTest {
    static String BATTERY =
            "{" +
            "      \"type\": \"AVG\",\n" +
            "      \"version\": \"1.0\",\n" +
            "      \"height\": 200,\n" +
            "      \"width\": 200,\n" +
            "      \"items\": [{\n" +
            "        \"type\": \"group\",\n" +
            "        \"pivotX\": 12.0,\n" +
            "        \"pivotY\": 12.0,\n" +
            "        \"rotation\": 45.0,\n" +
            "        \"items\": [\n" +
            "          {\n" +
            "            \"type\": \"path\",\n" +
            "            \"fill\": \"blue\",\n" +
            "            \"fillOpacity\": 0.3,\n" +
            "            \"pathData\": \"M15.67,4H14V2h-4v2H8.33C7.6,4 7,4.6 7,5.33V9h4.93L13,7v2h4V5.33C17,4.6 16.4,4 15.67,4z\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"type\": \"path\",\n" +
            "            \"fill\": \"blue\",\n" +
            "            \"pathData\": \"M13,12.5h2L11,20v-5.5H9L11.93,9H7v11.67C7,21.4 7.6,22 8.33,22h7.33c0.74,0 1.34,-0.6 1.34,-1.33V9h-4v3.5z\"\n" +
            "          }\n" +
            "        ]\n" +
            "      }]\n" +
            "    }";

    @Override
    public String getComponentType() {
        return "VectorGraphic";
    }

    @Override
    public String getComponentProps() {
        return "\"source\": \"battery\",\n" +
                "\"scale\": \"best-fit\",\n" +
                "\"align\": \"center\",";
    }

    @Override
    APLOptions getOptions() {
        return APLOptions.builder()
                .sendEventCallbackV2(mSendEventCallback)
                .dataRetrieverProvider(new TouchableVectorGraphicViewTest.TestDataRetrieverProvider())
                .build();
    }

    /**
     * TODO determine why loading the avg from graphics in the document fails in the unit test
     */
    static class TestDataRetrieverProvider implements IDataRetrieverProvider {
        @Override
        public IDataRetriever get() {
            return new TestDataRetriever(BATTERY);
        }
    }

    static class TestDataRetriever implements IDataRetriever {
        String mResult;
        TestDataRetriever(String result) {
            mResult = result;
        }
        @Override
        public void fetch(String source, Callback callback) {
            callback.success(mResult);
        }
        @Override
        public void cancelAll() {

        }
    }
}
