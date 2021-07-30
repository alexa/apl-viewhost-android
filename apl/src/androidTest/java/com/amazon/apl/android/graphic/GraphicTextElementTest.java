/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.graphic;

import android.graphics.Color;
import android.graphics.Matrix;
import android.text.TextUtils;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amazon.apl.android.APLVersionCodes;
import com.amazon.apl.android.document.AbstractDocUnitTest;
import com.amazon.apl.android.primitive.Gradient;
import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.android.VectorGraphic;
import com.amazon.apl.android.providers.impl.NoOpTelemetryProvider;
import com.amazon.apl.enums.FontStyle;
import com.amazon.apl.enums.GradientType;
import com.amazon.apl.enums.GraphicLayoutDirection;
import com.amazon.apl.enums.GraphicPropertyKey;
import com.amazon.apl.enums.GraphicTextAnchor;
import com.amazon.apl.enums.ObjectType;
import com.amazon.apl.enums.PropertyKey;

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

    private static final String FULL_PROPS_FOR_AVG_TEXT = "{" +
            "      \"type\": \"AVG\",\n" +
            "      \"version\": \"1.0\",\n" +
            "      \"height\": 24,\n" +
            "      \"width\": 24,\n" +
            "      \"lang\": \"en-US\",\n" +
            "      \"layoutDirection\": \"RTL\",\n" +
            "      \"viewportWidth\": 24,\n" +
            "      \"viewportHeight\": 24,\n" +
            "      \"items\": [\n" +
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
            "      ]\n" +
            "    }";

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

    private static final String PATTERNS_FOR_AVG_TEXT = "{" +
            "      \"type\": \"AVG\",\n" +
            "      \"version\": \"1.0\",\n" +
            "      \"height\": 24,\n" +
            "      \"width\": 24,\n" +
            "      \"viewportWidth\": 24,\n" +
            "      \"viewportHeight\": 24,\n" +
            "      \"resources\": [{" +
            "        \"patterns\": {" +
            "          \"RedCircle\": {" +
            "            \"type\": \"pattern\"," +
            "            \"width\": 8," +
            "            \"height\": 8," +
            "            \"items\": [" +
            "              {" +
            "              \"type\": \"path\"," +
            "              \"pathData\": \"M0,4 a4,4,0,1,1,8,0 a4,4,0,1,1,-8,0\"," +
            "              \"fill\": \"red\"" +
            "              }" +
            "            ]" +
            "          }" +
            "        }" +
            "      }]," +
            "      \"items\": [\n" +
            "            {\n" +
            "              \"type\": \"text\",\n" +
            "              \"fill\": \"@RedCircle\",\n" +
            "              \"fillTransform\": \"rotate(45)\"," +
            "              \"fontSize\": 20,\n" +
            "              \"stroke\": \"@RedCircle\",\n" +
            "              \"strokeTransform\": \"rotate(45)\"," +
            "              \"text\": \"message\",\n" +
            "              \"x\": 50,\n" +
            "              \"y\": 50\n" +
            "            }\n" +
            "      ]\n" +
            "    }";

    private static final String GRADIENTS_FOR_AVG_TEXT = "{" +
            "      \"type\": \"AVG\",\n" +
            "      \"version\": \"1.0\",\n" +
            "      \"height\": 24,\n" +
            "      \"width\": 24,\n" +
            "      \"viewportWidth\": 24,\n" +
            "      \"viewportHeight\": 24,\n" +
            "      \"resources\": [{" +
            "         \"gradients\": {" +
            "            \"linearGradient\": {" +
            "              \"inputRange\": [" +
            "                0," +
            "                0.5492504222972973," +
            "                1" +
            "              ]," +
            "              \"colorRange\": [" +
            "                \"#ffffffff\"," +
            "                \"#ff0000ff\"," +
            "                \"#000000ff\"" +
            "              ]," +
            "              \"type\": \"linear\"," +
            "              \"x1\": 0.3," +
            "              \"y1\": 0.4," +
            "              \"x2\": 0.7," +
            "              \"y2\": 0.5" +
            "            }," +
            "            \"radialGradient\": {" +
            "              \"inputRange\": [" +
            "                0," +
            "                1" +
            "              ]," +
            "              \"colorRange\": [" +
            "                \"black\"," +
            "                \"white\"" +
            "              ]," +
            "              \"type\": \"radial\"," +
            "              \"centerX\": 0.6," +
            "              \"centerY\": 0.4," +
            "              \"radius\": 1" +
            "            }" +
            "         }" +
            "      }]," +
            "      \"items\": [\n" +
            "            {\n" +
            "              \"type\": \"text\",\n" +
            "              \"fill\": \"@linearGradient\",\n" +
            "              \"fillTransform\": \"rotate(45)\"," +
            "              \"fontSize\": 20,\n" +
            "              \"stroke\": \"@linearGradient\",\n" +
            "              \"strokeTransform\": \"rotate(45)\"," +
            "              \"text\": \"message\",\n" +
            "              \"x\": 50,\n" +
            "              \"y\": 50\n" +
            "            },\n" +
            "            {\n" +
            "              \"type\": \"text\",\n" +
            "              \"fill\": \"@radialGradient\",\n" +
            "              \"fillTransform\": \"rotate(45)\"," +
            "              \"fontSize\": 20,\n" +
            "              \"stroke\": \"@radialGradient\",\n" +
            "              \"strokeTransform\": \"rotate(45)\"," +
            "              \"text\": \"message\",\n" +
            "              \"x\": 100,\n" +
            "              \"y\": 100\n" +
            "            }\n" +
            "      ]\n" +
            "    }";

    @Test
    public void testProperties_avgTextInflation_optionalDefaultValues() {
        // TODO Add default text properties
    }

    @Test
    public void testProperties_avgTextInflation_optionalExplicitValues() {
        String doc = buildDocument(BASE_DOC, "", OPTIONAL_PROPERTIES_WITH_SOURCE_AS_NAME, String.format(GRAPHICS_SOURCE_TEMPLATE, FULL_PROPS_FOR_AVG_TEXT));
        loadDocument(doc);
        GraphicTextElement textElement = getTextElement();

        assertEquals(0.6f, textElement.getFillOpacity(), 0.01);
        assertEquals("amazon-ember", textElement.getFontFamily());
        assertEquals(20, textElement.getFontSize());
        assertEquals(FontStyle.kFontStyleItalic.getIndex(), textElement.getFontStyle());
        assertEquals(800, textElement.getFontWeight());
        assertEquals(1.0f, textElement.getLetterSpacing(), 0.01);
        assertEquals(0.4f, textElement.getStrokeOpacity(), 0.01);
        assertEquals(3, textElement.getStrokeWidth(), 0.01);
        assertEquals("message", textElement.getText());
        assertEquals(GraphicTextAnchor.kGraphicTextAnchorMiddle.getIndex(), textElement.getTextAnchor());
        assertEquals(50, textElement.getCoordinateX());
        assertEquals(50, textElement.getCoordinateY());
        assertEquals(GraphicLayoutDirection.kGraphicLayoutDirectionRTL, textElement.getRootContainer().getLayoutDirection());
        assertEquals("en-US", textElement.getRootContainer().getFontLanguage());
    }

    @Test
    public void testProperties_avgTextInflationWithPatterns_fillAndStrokePatternsSet() {
        String doc = buildDocument(BASE_DOC, "", OPTIONAL_PROPERTIES_WITH_SOURCE_AS_NAME, String.format(GRAPHICS_SOURCE_TEMPLATE, PATTERNS_FOR_AVG_TEXT));
        loadDocument(doc);
        VectorGraphic component = getTestComponent();

        GraphicContainerElement containerElement = component.getOrCreateGraphicContainerElement();
        assertEquals(1, containerElement.getChildren().size());

        GraphicElement element = containerElement.getChildren().get(0);
        GraphicTextElement avgTextElement = (GraphicTextElement) element;

        assertEquals(ObjectType.kGraphicPatternType, avgTextElement.getStrokeType());
        assertEquals(ObjectType.kGraphicPatternType, avgTextElement.getFillType());
        Matrix rot45 = new Matrix();
        rot45.postRotate(45);
        assertEquals(rot45, avgTextElement.getStrokeTransform());
        assertEquals(rot45, avgTextElement.getFillTransform());

        GraphicPathElement avgPatternPathElement =
                (GraphicPathElement) avgTextElement.getFillGraphicPattern().getItems().get(0);
        assertEquals(Color.RED, avgPatternPathElement.getFillColor());
    }

    @Test
    public void testProperties_avgTextInflationWithGradient_fillAndStrokeGradientsSet_linear() {
        String doc = buildDocument(BASE_DOC, "", OPTIONAL_PROPERTIES_WITH_SOURCE_AS_NAME, String.format(GRAPHICS_SOURCE_TEMPLATE, GRADIENTS_FOR_AVG_TEXT));
        loadDocument(doc);
        VectorGraphic component = getTestComponent();

        GraphicContainerElement containerElement = component.getOrCreateGraphicContainerElement();
        assertEquals(2, containerElement.getChildren().size());

        GraphicElement element = containerElement.getChildren().get(0);
        GraphicTextElement avgTextElement = (GraphicTextElement) element;

        Matrix rot45 = new Matrix();
        rot45.postRotate(45);
        assertEquals(rot45, avgTextElement.getFillTransform());
        assertEquals(rot45, avgTextElement.getStrokeTransform());
        assertEquals(ObjectType.kGradientType, avgTextElement.getStrokeType());
        assertEquals(ObjectType.kGradientType, avgTextElement.getFillType());

        Gradient fillGradient = element.getGradient(GraphicPropertyKey.kGraphicPropertyFill);
        assertEquals(GradientType.LINEAR, fillGradient.getType());
        int[] expectedColorRange = {-1, -65536, -16777216};
        assertArrayEquals(expectedColorRange, fillGradient.getColorRange());
        float[] expectedInputRange = {0, 0.5492504222972973f, 1};
        assertArrayEquals(expectedInputRange, fillGradient.getInputRange(), 0.01f);
        assertEquals(0.3, fillGradient.getX1(), 0.01);
        assertEquals(0.4, fillGradient.getY1(), 0.01);
        assertEquals(0.7, fillGradient.getX2(), 0.01);
        assertEquals(0.5, fillGradient.getY2(), 0.01);

        Gradient strokeGradient = element.getGradient(GraphicPropertyKey.kGraphicPropertyStroke);
        assertEquals(GradientType.LINEAR, strokeGradient.getType());
        assertArrayEquals(expectedColorRange, strokeGradient.getColorRange());
        assertArrayEquals(expectedInputRange, strokeGradient.getInputRange(), 0.01f);
        assertEquals(0.3, strokeGradient.getX1(), 0.01);
        assertEquals(0.4, strokeGradient.getY1(), 0.01);
        assertEquals(0.7, strokeGradient.getX2(), 0.01);
        assertEquals(0.5, strokeGradient.getY2(), 0.01);
    }

    @Test
    public void testProperties_avgTextInflationWithGradient_fillAndStrokeGradientsSet_radial() {
        String doc = buildDocument(BASE_DOC, "", OPTIONAL_PROPERTIES_WITH_SOURCE_AS_NAME, String.format(GRAPHICS_SOURCE_TEMPLATE, GRADIENTS_FOR_AVG_TEXT));
        loadDocument(doc);
        VectorGraphic component = getTestComponent();

        GraphicContainerElement containerElement = component.getOrCreateGraphicContainerElement();
        assertEquals(2, containerElement.getChildren().size());

        GraphicElement element = containerElement.getChildren().get(1);
        GraphicTextElement avgTextElement = (GraphicTextElement) element;

        Matrix rot45 = new Matrix();
        rot45.postRotate(45);
        assertEquals(rot45, avgTextElement.getFillTransform());
        assertEquals(rot45, avgTextElement.getStrokeTransform());
        assertEquals(ObjectType.kGradientType, avgTextElement.getStrokeType());
        assertEquals(ObjectType.kGradientType, avgTextElement.getFillType());

        Gradient fillGradient = element.getGradient(GraphicPropertyKey.kGraphicPropertyFill);
        assertEquals(GradientType.RADIAL, fillGradient.getType());
        int[] expectedColorRange = {-16777216, -1};
        assertArrayEquals(expectedColorRange, fillGradient.getColorRange());
        float[] expectedInputRange = {0, 1};
        assertArrayEquals(expectedInputRange, fillGradient.getInputRange(), 0.01f);
        assertEquals(0.6, fillGradient.getCenterX(), 0.01);
        assertEquals(0.4, fillGradient.getCenterY(), 0.01);
        assertEquals(1, fillGradient.getRadius(), 0.01);

        Gradient strokeGradient = element.getGradient(GraphicPropertyKey.kGraphicPropertyStroke);
        assertEquals(GradientType.RADIAL, strokeGradient.getType());
        assertArrayEquals(expectedColorRange, strokeGradient.getColorRange());
        assertArrayEquals(expectedInputRange, strokeGradient.getInputRange(), 0.01f);
        assertEquals(0.6, strokeGradient.getCenterX(), 0.01);
        assertEquals(0.4, strokeGradient.getCenterY(), 0.01);
        assertEquals(1, strokeGradient.getRadius(), 0.01);
    }

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
