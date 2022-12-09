/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.graphic;

import android.text.TextUtils;

import com.amazon.apl.android.VectorGraphic;
import com.amazon.apl.android.document.AbstractDocUnitTest;
import com.amazon.apl.android.scaling.ViewportMetrics;
import com.amazon.apl.enums.ScreenShape;
import com.amazon.apl.enums.ViewportMode;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

public class GraphicContainerElementTest extends AbstractDocUnitTest {

    final static String BASE_DOC = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.0\"," +
            "  \"mainTemplate\": {" +
            "    \"item\":" +
            "    {" +
            "      \"id\": \"testcomp\", " +
            "      \"type\": \"VectorGraphic\" %s" +
            "    }" +
            "  }" +
            "%s" +
            "}";

    private static final String GRAPHICS_SOURCE_TEMPLATE =
            "  \"graphics\": {" +
                    "    \"box\": %s" +
                    "  }";


    @Test
    public void testProperties_avgContainer_scaling() {
        ViewportMetrics metrics = ViewportMetrics.builder()
                .width(1920)
                .height(1200)
                .dpi(240)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeHub)
                .build();

        String optionalProps  =
                " \"source\": \"box\"";

        String avgProps =
                "    {\n" +
                        "      \"type\": \"AVG\",\n" +
                        "      \"version\": \"1.1\",\n" +
                        "      \"width\": \"100vw\",\n" +
                        "      \"height\": \"100vh\",\n" +
                        "      \"items\": [\n" +
                        "        {\n" +
                        "          \"type\": \"path\",\n" +
                        "          \"fill\": \"rgba(255, 255, 255, 1)\",\n" +
                        "          \"pathData\": \"M 0,0 H ${width} V ${height}\"" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n";

        String doc = buildDocument(BASE_DOC, "", optionalProps, String.format(GRAPHICS_SOURCE_TEMPLATE, avgProps));
        loadDocument(doc, metrics);
        VectorGraphic component = getTestComponent();

        GraphicContainerElement containerElement = component.getOrCreateGraphicContainerElement();

        // Viewhost units are pixels which is 1920x1200
        assertEquals(1920, containerElement.getWidthActual(), 0.1);
        assertEquals(1200, containerElement.getHeightActual(), 0.1);
        // Core units are dp which is 1280x800
        assertEquals(1280, containerElement.getViewportWidthActual(), 0.1);
        assertEquals(800, containerElement.getViewportHeightActual(), 0.1);
    }

    VectorGraphic getTestComponent() {
        return (VectorGraphic) mRootContext.findComponentById("testcomp");
    }

    final String buildDocument(String baseDocument,
                               String requiredProperties,
                               String optionalProperties,
                               String optionalTemplateProperties) {
        StringBuilder fullProps = new StringBuilder();
        if (requiredProperties != null && requiredProperties.length() > 0) {
            fullProps.append(",");
            fullProps.append(requiredProperties);
        }
        if (optionalProperties != null && optionalProperties.length() > 0) {
            fullProps.append(",");
            fullProps.append(optionalProperties);
        }
        if (!TextUtils.isEmpty(optionalTemplateProperties)) {
            optionalTemplateProperties = "," + optionalTemplateProperties;
        }
        return String.format(baseDocument,
                fullProps.toString(), optionalTemplateProperties);
    }
}
