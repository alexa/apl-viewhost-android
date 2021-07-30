/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import com.amazon.apl.android.views.APLAbsoluteLayout;
import com.amazon.apl.android.MultiChildComponent;

import org.junit.Before;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
public class ContainerViewTest extends AbstractComponentViewTest<APLAbsoluteLayout, MultiChildComponent> {


    @Before
    public void buildView() {
        REQUIRED_PROPERTIES = ""; // no required properties in Container Component.
        OPTIONAL_PROPERTIES =
                "\"height\": \"100vh\",\n" +
                        "      \"width\": \"100vw\",\n" +
                        "      \"numbered\": true,\n" +
                        "      \"alignItems\": \"baseline\",\n" +
                        "      \"direction\": \"row\",\n" +
                        "      \"justifyContent\": \"center\",\n" +
                        "      \"firstItem\": {\n" +
                        "        \"type\": \"Text\",\n" +
                        "        \"text\": \"First\",\n" +
                        "        \"color\": \"red\"\n" +
                        "      },\n" +
                        "      \"items\": [\n" +
                        "        {\n" +
                        "          \"type\": \"Text\",\n" +
                        "          \"text\": \"Turtle\",\n" +
                        "          \"color\": \"white\"\n" +
                        "        }\n" +
                        "      ],\n" +
                        "      \"lastItem\": {\n" +
                        "        \"type\": \"Text\",\n" +
                        "        \"color\": \"blue\",\n" +
                        "        \"text\": \"Last\"\n" +
                        "      }";
        CHILD_LAYOUT_PROPERTIES =
                "      \"alignItems\": \"stretch\",\n" +
                        "      \"direction\": \"column\",\n" +
                        "      \"justifyContent\": \"spaceBetween\",\n" +
                        "      \"firstItem\": {\n" +
                        "        \"type\": \"Text\",\n" +
                        "        \"text\": \"Red\",\n" +
                        "        \"color\": \"red\"\n" +
                        "      },\n" +
                        "      \"items\": [\n" +
                        "        {\n" +
                        "          \"type\": \"Text\",\n" +
                        "          \"text\": \"Green\",\n" +
                        "          \"color\": \"green\"\n" +
                        "        }\n" +
                        "      ],\n" +
                        "      \"lastItem\": {\n" +
                        "        \"type\": \"Text\",\n" +
                        "        \"color\": \"blue\",\n" +
                        "        \"text\": \"Blue\"\n" +
                        "      }";
    }

    @Override
    Class<APLAbsoluteLayout> getViewClass() {
        return APLAbsoluteLayout.class;
    }

    /**
     * @return The string representation used in the APL document for this component.  For example
     * when testing the Text Component this method should return "Text".
     */
    @Override
    String getComponentType() {
        return "Container";
    }

    /**
     * Test the view after properties have been assigned.
     *
     * @param view The Component View for testing.
     **/
    @Override
    void testView_applyProperties(APLAbsoluteLayout view) {
        // Container has no properties applied to the view.
    }
}
