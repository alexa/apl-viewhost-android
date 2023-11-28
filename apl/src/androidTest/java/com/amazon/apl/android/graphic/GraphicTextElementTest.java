/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.graphic;

import android.graphics.Color;
import android.graphics.Matrix;
import android.text.TextUtils;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amazon.apl.android.document.AbstractDocUnitTest;
import com.amazon.apl.android.primitive.Gradient;
import com.amazon.apl.android.VectorGraphic;
import com.amazon.apl.enums.FontStyle;
import com.amazon.apl.enums.GradientType;
import com.amazon.apl.enums.GraphicLayoutDirection;
import com.amazon.apl.enums.GraphicPropertyKey;
import com.amazon.apl.enums.GraphicTextAnchor;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class GraphicTextElementTest extends AbstractDocUnitTest {
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

    private static final String OPTIONAL_PROPERTIES_WITH_SOURCE_AS_NAME =
            " \"source\": \"box\"," +
                    " \"width\": \"100\"," +
                    " \"height\": \"100\"";

    private static final String GRAPHICS_SOURCE_TEMPLATE =
            "  \"graphics\": {" +
                    "    \"box\": %s" +
                    "  }";

    private static final String AVG_TEXT_LAYOUTDIRECTION_ANCHOR = "{" +
            "      \"type\": \"AVG\",\n" +
            "      \"version\": \"1.0\",\n" +
            "      \"height\": 24,\n" +
            "      \"width\": 24,\n" +
            "      \"lang\": \"en-US\",\n" +
            "      \"layoutDirection\": \"%s\",\n" +
            "      \"viewportWidth\": 24,\n" +
            "      \"viewportHeight\": 24,\n" +
            "      \"items\": [\n" +
            "            {\n" +
            "              \"type\": \"text\",\n" +
            "              \"text\": \"message\",\n" +
            "              \"textAnchor\": \"%s\",\n" +
            "              \"x\": 50,\n" +
            "              \"y\": 50\n" +
            "            }\n" +
            "      ]\n" +
            "    }";

    @Test
    public void testProperties_avgTextInflation_bounds_layoutDirection_LTR() {
        String doc = buildDocument(BASE_DOC, "", OPTIONAL_PROPERTIES_WITH_SOURCE_AS_NAME, String.format(GRAPHICS_SOURCE_TEMPLATE, String.format(AVG_TEXT_LAYOUTDIRECTION_ANCHOR, "LTR", "start")));
        loadDocument(doc);
        GraphicTextElement textElement = getTextElement();
        int initialXCoordinate = textElement.getX();
        doc = buildDocument(BASE_DOC, "", OPTIONAL_PROPERTIES_WITH_SOURCE_AS_NAME, String.format(GRAPHICS_SOURCE_TEMPLATE, String.format(AVG_TEXT_LAYOUTDIRECTION_ANCHOR, "LTR", "end")));
        loadDocument(doc);
        textElement = getTextElement();
        int finalXCoordinate = textElement.getX();
        // Spec: https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-avg-format.html#textanchor
        // start The text starts at the starting point (x,y). For AVG layoutDirection specified as LTR, the text extends to the right.
        // end 	 The text ends at the starting point (x,y). For AVG layoutDirection specified as LTR, the text extends to the left.
        // Translating the above text to a shift, the text moves from right to left when layoutDirection is LTR and textAnchor changes from start to end.
        assertTrue(finalXCoordinate < initialXCoordinate);
    }

    @Test
    public void testProperties_avgTextInflation_bounds_layoutDirection_RTL() {
        String doc = buildDocument(BASE_DOC, "", OPTIONAL_PROPERTIES_WITH_SOURCE_AS_NAME, String.format(GRAPHICS_SOURCE_TEMPLATE, String.format(AVG_TEXT_LAYOUTDIRECTION_ANCHOR, "RTL", "start")));
        loadDocument(doc);
        GraphicTextElement textElement = getTextElement();
        int initialXCoordinate = textElement.getX();
        doc = buildDocument(BASE_DOC, "", OPTIONAL_PROPERTIES_WITH_SOURCE_AS_NAME, String.format(GRAPHICS_SOURCE_TEMPLATE, String.format(AVG_TEXT_LAYOUTDIRECTION_ANCHOR, "RTL", "end")));
        loadDocument(doc);
        textElement = getTextElement();
        int finalXCoordinate = textElement.getX();
        // Spec: https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-avg-format.html#textanchor
        // start The text starts at the starting point (x,y). For AVG layoutDirection specified as RTL, the text extends to the left.
        // end   For AVG layoutDirection specified as RTL, the text extends to the right.
        // Translating the above text to a shift, the text moves from left to right when layoutDirection is RTL and textAnchor changes from start to end.
        assertTrue(finalXCoordinate > initialXCoordinate);
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

    private GraphicTextElement getTextElement() {
        VectorGraphic component = getTestComponent();
        GraphicContainerElement graphicContainerElement = component.getOrCreateGraphicContainerElement();
        return (GraphicTextElement)
                graphicContainerElement.getChildren().get(0);

    }
}
