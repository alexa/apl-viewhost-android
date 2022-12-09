/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.graphic;

import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.text.TextUtils;

import com.amazon.apl.android.document.AbstractDocUnitTest;
import com.amazon.apl.android.primitive.Gradient;
import com.amazon.apl.android.VectorGraphic;
import com.amazon.apl.enums.GradientSpreadMethod;
import com.amazon.apl.enums.GradientType;
import com.amazon.apl.enums.GraphicElementType;
import com.amazon.apl.enums.GraphicPropertyKey;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GraphicPathElementTest extends AbstractDocUnitTest {
    private static final int OPAQUE = 255;

    final static String BASE_DOC = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.4\"," +
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

    private static final String FULL_PROPS_FOR_AVG_PATH = "{" +
            "  \"type\": \"AVG\"," +
            "  \"version\": \"1.0\"," +
            "  \"height\": 24," +
            "  \"width\": 24," +
            "  \"items\": [" +
            "    {" +
            "          \"type\": \"path\"," +
            "          \"fill\": \"blue\"," +
            "          \"fillOpacity\": 0.3," +
            "          \"pathData\": \"M15.67\"," +
            "          \"stroke\": \"red\"," +
            "          \"strokeDashArray\": [1,3,5,6]," +
            "          \"strokeDashOffset\": 3," +
            "          \"strokeMiterLimit\":2," +
            "          \"strokeLineJoin\": \"round\"," +
            "          \"strokeLineCap\": \"round\"," +
            "          \"pathLength\":20" +
            "    }" +
            "  ]" +
            "}";

    private static final String VALID_ARC_COMMAND_SYNAX_PATH = "{" +
            "  \"type\": \"AVG\"," +
            "  \"version\": \"1.0\"," +
            "  \"height\": 24," +
            "  \"width\": 24," +
            "  \"items\": [" +
            "    {" +
            "          \"type\": \"path\"," +
            "          \"pathData\": \"M0,0 a1 1 0 00-1.05.1 A1 2 3 1,1,5 6 7 8 9 1,1,5 6\"" +
            "    }" +
            "  ]" +
            "}";

    private static final String PATTERNS_FOR_AVG_PATH = "{" +
            "  \"type\": \"AVG\"," +
            "  \"version\": \"1.0\"," +
            "  \"height\": 24," +
            "  \"width\": 24," +
            "  \"resources\": [{" +
            "    \"patterns\": {" +
            "     \"RedCircle\": {" +
            "       \"type\": \"pattern\"," +
            "       \"width\": 8," +
            "       \"height\": 8," +
            "       \"items\": [" +
            "         {" +
            "         \"type\": \"path\"," +
            "         \"pathData\": \"M0,4 a4,4,0,1,1,8,0 a4,4,0,1,1,-8,0\"," +
            "         \"fill\": \"red\"" +
            "         }" +
            "       ]" +
            "     }" +
            "    }" +
            "  }]," +
            "  \"items\": [" +
            "    {" +
            "          \"type\": \"path\"," +
            "          \"stroke\": \"@RedCircle\"," +
            "          \"strokeTransform\": \"rotate(45)\"," +
            "          \"fill\": \"@RedCircle\"," +
            "          \"fillTransform\": \"rotate(45)\"," +
            "          \"pathData\": \"M15.67\"," +
            "          \"stroke\": \"red\"" +
            "    }" +
            "  ]" +
            "}";

    private static final String GRADIENTS_FOR_AVG_PATH = "{" +
            "  \"type\": \"AVG\"," +
            "  \"version\": \"1.0\"," +
            "  \"height\": 24," +
            "  \"width\": 24," +
            "  \"resources\": [{" +
            "       \"gradients\": {" +
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
            "            }," +
            "            \"spreadMethodPad\": {" +
            "              \"inputRange\": [" +
            "                0," +
            "                1" +
            "              ]," +
            "              \"colorRange\": [" +
            "                \"#ff0000ff\"," +
            "                \"#ffffffff\"" +
            "              ]," +
            "              \"spreadMethod\": \"pad\"," +
            "              \"type\": \"linear\"," +
            "              \"x1\": 0.40127709565476446," +
            "              \"y1\": 0.4059174391256225," +
            "              \"x2\": 0.6081258206624885," +
            "              \"y2\": 0.6039892388973439" +
            "            }," +
            "            \"spreadMethodReflect\": {" +
            "              \"inputRange\": [" +
            "                0," +
            "                1" +
            "              ]," +
            "              \"colorRange\": [" +
            "                \"#ff0000ff\"," +
            "                \"#ffffffff\"" +
            "              ]," +
            "              \"spreadMethod\": \"reflect\"," +
            "              \"type\": \"linear\"," +
            "              \"x1\": 0.40127709565476446," +
            "              \"y1\": 0.4059174391256225," +
            "              \"x2\": 0.6081258206624885," +
            "              \"y2\": 0.6039892388973439" +
            "            }," +
            "            \"spreadMethodRepeat\": {" +
            "              \"inputRange\": [" +
            "                0," +
            "                1" +
            "              ]," +
            "              \"colorRange\": [" +
            "                \"#ff0000ff\"," +
            "                \"#ffffffff\"" +
            "              ]," +
            "              \"spreadMethod\": \"repeat\"," +
            "              \"type\": \"linear\"," +
            "              \"x1\": 0.40127709565476446," +
            "              \"y1\": 0.4059174391256225," +
            "              \"x2\": 0.6081258206624885," +
            "              \"y2\": 0.6039892388973439" +
            "            }" +
            "       }" +
            "  }]," +
            "  \"items\": [" +
            "    {" +
            "          \"type\": \"path\"," +
            "          \"stroke\": \"@linearGradient\"," +
            "          \"strokeTransform\": \"rotate(45)\"," +
            "          \"fill\": \"@linearGradient\"," +
            "          \"fillTransform\": \"rotate(45)\"," +
            "          \"pathData\": \"M15.67\"" +
            "    }," +
            "    {" +
            "          \"type\": \"path\"," +
            "          \"stroke\": \"@radialGradient\"," +
            "          \"strokeTransform\": \"rotate(45)\"," +
            "          \"fill\": \"@radialGradient\"," +
            "          \"fillTransform\": \"rotate(45)\"," +
            "          \"pathData\": \"M15.67\"" +
            "    }," +
            "    {" +
            "          \"type\": \"path\"," +
            "          \"stroke\": \"@spreadMethodPad\"," +
            "          \"fill\": \"@spreadMethodPad\"," +
            "          \"pathData\": \"M15.67\"" +
            "    }," +
            "    {" +
            "          \"type\": \"path\"," +
            "          \"stroke\": \"@spreadMethodReflect\"," +
            "          \"fill\": \"@spreadMethodReflect\"," +
            "          \"pathData\": \"M15.67\"" +
            "    }," +
            "    {" +
            "          \"type\": \"path\"," +
            "          \"stroke\": \"@spreadMethodRepeat\"," +
            "          \"fill\": \"@spreadMethodRepeat\"," +
            "          \"pathData\": \"M15.67\"" +
            "    }" +
            "  ]" +
            "}";

    final String AVG_PATH_WITHOUT_MOVE_COMMAND = "{" +
            "  \"type\": \"AVG\"," +
            "  \"version\": \"1.1\"," +
            "  \"height\": 24," +
            "  \"width\": 24," +
            "  \"items\": [" +
            "    {" +
            "      \"type\": \"path\"," +
            "      \"pathData\": \"H0,0\"" +
            "    }" +
            "  ]" +
            "}";


    @Test
    public void testProperties_avgPathInflation_optionalDefaultValues() {
        final String REQUIRED_PROPS_FOR_AVG_PATH = "{" +
                "  \"type\": \"AVG\"," +
                "  \"version\": \"1.0\"," +
                "  \"height\": 24," +
                "  \"width\": 24," +
                "  \"items\": [" +
                "    {" +
                "      \"type\": \"path\"," +
                "      \"pathData\": \"M0,0\"" +
                "    }" +
                "  ]" +
                "}";

        String doc = buildDocument(BASE_DOC, "", OPTIONAL_PROPERTIES_WITH_SOURCE_AS_NAME, String.format(GRAPHICS_SOURCE_TEMPLATE, REQUIRED_PROPS_FOR_AVG_PATH));
        loadDocument(doc);

        GraphicContainerElement containerElement = getRoot();
        assertEquals(1, containerElement.getChildren().size());

        GraphicElement element = containerElement.getChildren().get(0);
        assertEquals(GraphicElementType.kGraphicElementTypePath, element.getType());

        GraphicPathElement avgPathElement = (GraphicPathElement) element;
        Assert.assertNotNull(avgPathElement.getPathNodes());

        Paint fillPaint = avgPathElement.getFillPaint();
        assertEquals(Color.TRANSPARENT, fillPaint.getColor());
        assertEquals("M0,0", avgPathElement.getPathData());
        Paint strokePaint = avgPathElement.getStrokePaint();
        assertEquals(Color.TRANSPARENT, strokePaint.getColor());

        assertEquals(1, avgPathElement.getStrokeWidth(), 0.1);

        float[] strokeDashArrayDefault = avgPathElement.getStrokeDashArray();
        assertEquals(0, strokeDashArrayDefault.length);
        assertEquals(0, avgPathElement.getStrokeDashOffset(), 0);
        assertEquals(4, avgPathElement.getStrokeMiterLimit(), 0);
        assertEquals(Paint.Join.MITER, avgPathElement.getPaintJoin());
        assertEquals(Paint.Cap.BUTT, avgPathElement.getPaintCap());
        assertEquals(0, avgPathElement.getPathLength(), 0);
    }

    @Test
    public void testProperties_avgPathInflationWithPatterns_fillAndStrokePatternsSet() {
        String doc = buildDocument(BASE_DOC, "", OPTIONAL_PROPERTIES_WITH_SOURCE_AS_NAME, String.format(GRAPHICS_SOURCE_TEMPLATE, PATTERNS_FOR_AVG_PATH));
        loadDocument(doc);

        GraphicContainerElement containerElement = getRoot();
        assertEquals(1, containerElement.getChildren().size());

        GraphicElement element = containerElement.getChildren().get(0);
        assertEquals(GraphicElementType.kGraphicElementTypePath, element.getType());

        GraphicPathElement avgPathElement = (GraphicPathElement) element;

        assertTrue(avgPathElement.getProperties().isGraphicPattern(GraphicPropertyKey.kGraphicPropertyStroke));
        assertTrue(avgPathElement.getProperties().isGraphicPattern(GraphicPropertyKey.kGraphicPropertyFill));
        Matrix rot45 = new Matrix();
        rot45.postRotate(45);
        assertEquals(rot45, avgPathElement.getStrokeTransform());
        assertEquals(rot45, avgPathElement.getFillTransform());

        GraphicPathElement avgPatternPathElement =
                (GraphicPathElement) avgPathElement.getFillGraphicPattern().getItems().get(0);
        Paint fillPaint = avgPatternPathElement.getFillPaint();
        assertEquals(Color.RED, fillPaint.getColor());
    }

    @Test
    public void testProperties_avgPathInflationWithGradient_linearGradient() {
        String doc = buildDocument(BASE_DOC, "", OPTIONAL_PROPERTIES_WITH_SOURCE_AS_NAME, String.format(GRAPHICS_SOURCE_TEMPLATE, GRADIENTS_FOR_AVG_PATH));
        loadDocument(doc);

        GraphicContainerElement containerElement = getRoot();
        verifyAllChildrenInflated(containerElement);

        GraphicElement element = containerElement.getChildren().get(0);
        assertEquals(GraphicElementType.kGraphicElementTypePath, element.getType());

        GraphicPathElement avgPathElement = (GraphicPathElement) element;

        assertTrue(avgPathElement.getProperties().isGradient(GraphicPropertyKey.kGraphicPropertyStroke));
        assertTrue(avgPathElement.getProperties().isGradient(GraphicPropertyKey.kGraphicPropertyFill));
        Matrix rot45 = new Matrix();
        rot45.postRotate(45);
        assertEquals(rot45, avgPathElement.getStrokeTransform());
        assertEquals(rot45, avgPathElement.getFillTransform());

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
        assertEquals(GradientSpreadMethod.PAD, fillGradient.getSpreadMethod()); // Default when spreadMethod is not specified.

        Gradient strokeGradient = element.getGradient(GraphicPropertyKey.kGraphicPropertyStroke);
        assertEquals(GradientType.LINEAR, strokeGradient.getType());
        assertArrayEquals(expectedColorRange, strokeGradient.getColorRange());
        assertArrayEquals(expectedInputRange, strokeGradient.getInputRange(), 0.01f);
        assertEquals(0.3, strokeGradient.getX1(), 0.01);
        assertEquals(0.4, strokeGradient.getY1(), 0.01);
        assertEquals(0.7, strokeGradient.getX2(), 0.01);
        assertEquals(0.5, strokeGradient.getY2(), 0.01);
        assertEquals(GradientSpreadMethod.PAD, strokeGradient.getSpreadMethod());
    }

    @Test
    public void testProperties_avgPathInflationWithGradient_radialGradient() {
        String doc = buildDocument(BASE_DOC, "", OPTIONAL_PROPERTIES_WITH_SOURCE_AS_NAME, String.format(GRAPHICS_SOURCE_TEMPLATE, GRADIENTS_FOR_AVG_PATH));
        loadDocument(doc);

        GraphicContainerElement containerElement = getRoot();
        verifyAllChildrenInflated(containerElement);

        GraphicElement element = containerElement.getChildren().get(1);
        assertEquals(GraphicElementType.kGraphicElementTypePath, element.getType());

        GraphicPathElement avgPathElement = (GraphicPathElement) element;

        assertTrue(avgPathElement.getProperties().isGradient(GraphicPropertyKey.kGraphicPropertyStroke));
        assertTrue(avgPathElement.getProperties().isGradient(GraphicPropertyKey.kGraphicPropertyFill));
        Matrix rot45 = new Matrix();
        rot45.postRotate(45);
        assertEquals(rot45, avgPathElement.getStrokeTransform());
        assertEquals(rot45, avgPathElement.getFillTransform());

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
    public void testProperties_avgPathInflationWithGradient_spreadMethod_pad() {
        String doc = buildDocument(BASE_DOC, "", OPTIONAL_PROPERTIES_WITH_SOURCE_AS_NAME, String.format(GRAPHICS_SOURCE_TEMPLATE, GRADIENTS_FOR_AVG_PATH));
        loadDocument(doc);

        GraphicContainerElement containerElement = getRoot();
        verifyAllChildrenInflated(containerElement);

        GraphicElement element = containerElement.getChildren().get(2);
        assertEquals(GraphicElementType.kGraphicElementTypePath, element.getType());

        GraphicPathElement avgPathElement = (GraphicPathElement) element;

        assertTrue(avgPathElement.getProperties().isGradient(GraphicPropertyKey.kGraphicPropertyStroke));
        assertTrue(avgPathElement.getProperties().isGradient(GraphicPropertyKey.kGraphicPropertyFill));

        Gradient fillGradient = element.getGradient(GraphicPropertyKey.kGraphicPropertyFill);
        assertEquals(GradientSpreadMethod.PAD, fillGradient.getSpreadMethod());

        Gradient strokeGradient = element.getGradient(GraphicPropertyKey.kGraphicPropertyStroke);
        assertEquals(GradientSpreadMethod.PAD, strokeGradient.getSpreadMethod());
    }

    @Test
    public void testProperties_avgPathInflationWithGradient_spreadMethod_reflect() {
        String doc = buildDocument(BASE_DOC, "", OPTIONAL_PROPERTIES_WITH_SOURCE_AS_NAME, String.format(GRAPHICS_SOURCE_TEMPLATE, GRADIENTS_FOR_AVG_PATH));
        loadDocument(doc);
        GraphicContainerElement containerElement = getRoot();

        verifyAllChildrenInflated(containerElement);

        GraphicElement element = containerElement.getChildren().get(3);
        assertEquals(GraphicElementType.kGraphicElementTypePath, element.getType());

        GraphicPathElement avgPathElement = (GraphicPathElement) element;

        assertTrue(avgPathElement.getProperties().isGradient(GraphicPropertyKey.kGraphicPropertyStroke));
        assertTrue(avgPathElement.getProperties().isGradient(GraphicPropertyKey.kGraphicPropertyFill));

        Gradient fillGradient = element.getGradient(GraphicPropertyKey.kGraphicPropertyFill);
        assertEquals(GradientSpreadMethod.REFLECT, fillGradient.getSpreadMethod());

        Gradient strokeGradient = element.getGradient(GraphicPropertyKey.kGraphicPropertyStroke);
        assertEquals(GradientSpreadMethod.REFLECT, strokeGradient.getSpreadMethod());
    }

    @Test
    public void testProperties_avgPathInflationWithGradient_spreadMethod_repeat() {
        String doc = buildDocument(BASE_DOC, "", OPTIONAL_PROPERTIES_WITH_SOURCE_AS_NAME, String.format(GRAPHICS_SOURCE_TEMPLATE, GRADIENTS_FOR_AVG_PATH));
        loadDocument(doc);

        GraphicContainerElement containerElement = getRoot();
        verifyAllChildrenInflated(containerElement);

        GraphicElement element = containerElement.getChildren().get(4);
        assertEquals(GraphicElementType.kGraphicElementTypePath, element.getType());

        GraphicPathElement avgPathElement = (GraphicPathElement) element;

        assertTrue(avgPathElement.getProperties().isGradient(GraphicPropertyKey.kGraphicPropertyStroke));
        assertTrue(avgPathElement.getProperties().isGradient(GraphicPropertyKey.kGraphicPropertyFill));

        Gradient fillGradient = element.getGradient(GraphicPropertyKey.kGraphicPropertyFill);
        assertEquals(GradientSpreadMethod.REPEAT, fillGradient.getSpreadMethod());

        Gradient strokeGradient = element.getGradient(GraphicPropertyKey.kGraphicPropertyStroke);
        assertEquals(GradientSpreadMethod.REPEAT, strokeGradient.getSpreadMethod());
    }

    @Test
    public void aplVersion14_pathDataWithoutLeadingMoveCommand_noPathNodesReturnedForPath() {
        String DOC_14 = "{" +
                "  \"type\": \"APL\"," +
                "  \"version\": \"1.4\"," +
                "  \"mainTemplate\": {" +
                "    \"item\":" +
                "    {" +
                "      \"id\": \"testcomp\", " +
                "      \"type\": \"VectorGraphic\" %s" +
                "    }" +
                "  }" +
                "%s" +
                "}";

        String doc = buildDocument(DOC_14, "", OPTIONAL_PROPERTIES_WITH_SOURCE_AS_NAME, String.format(GRAPHICS_SOURCE_TEMPLATE, AVG_PATH_WITHOUT_MOVE_COMMAND));
        loadDocument(doc);

        GraphicContainerElement containerElement = getRoot();
        assertEquals(1, containerElement.getChildren().size());

        GraphicElement element = containerElement.getChildren().get(0);
        assertEquals(GraphicElementType.kGraphicElementTypePath, element.getType());

        GraphicPathElement avgPathElement = (GraphicPathElement) element;
        Assert.assertNull(avgPathElement.getPathNodes());
    }

    @Test
    public void aplVersion13_pathDataWithoutLeadingMoveCommand_pathNodesAreReturned() {
        String DOC_13 = "{" +
                "  \"type\": \"APL\"," +
                "  \"version\": \"1.3\"," +
                "  \"mainTemplate\": {" +
                "    \"item\":" +
                "    {" +
                "      \"id\": \"testcomp\", " +
                "      \"type\": \"VectorGraphic\" %s" +
                "    }" +
                "  }" +
                "%s" +
                "}";

        String doc = buildDocument(DOC_13, "", OPTIONAL_PROPERTIES_WITH_SOURCE_AS_NAME, String.format(GRAPHICS_SOURCE_TEMPLATE, AVG_PATH_WITHOUT_MOVE_COMMAND));
        loadDocument(doc);

        GraphicContainerElement containerElement = getRoot();
        assertEquals(1, containerElement.getChildren().size());

        GraphicElement element = containerElement.getChildren().get(0);
        assertEquals(GraphicElementType.kGraphicElementTypePath, element.getType());

        GraphicPathElement avgPathElement = (GraphicPathElement) element;
        Assert.assertTrue(avgPathElement.getPathNodes().length == 1);
    }

    @Test
    public void testProperties_avgPathInflationWithArcCommands_arcArgsParsedCorrected() {
        String doc = buildDocument(BASE_DOC, "", OPTIONAL_PROPERTIES_WITH_SOURCE_AS_NAME, String.format(GRAPHICS_SOURCE_TEMPLATE, VALID_ARC_COMMAND_SYNAX_PATH));
        loadDocument(doc);

        GraphicElement element = getRoot().getChildren().get(0);
        assertEquals(GraphicElementType.kGraphicElementTypePath, element.getType());
        GraphicPathElement avgPathElement = (GraphicPathElement) element;
        PathParser.PathDataNode[] pathDataNodes = avgPathElement.getPathNodes();

        // a1 1 0 00-1.05.1
        assertEquals('a', pathDataNodes[1].mType);
        assertEquals(1f, pathDataNodes[1].mParams[0], .1);
        assertEquals(1f, pathDataNodes[1].mParams[1], .1);
        assertEquals(0f, pathDataNodes[1].mParams[2], .1);
        assertEquals(0f, pathDataNodes[1].mParams[3], .1);
        assertEquals(0f, pathDataNodes[1].mParams[4], .1);
        assertEquals(-1.05f, pathDataNodes[1].mParams[5], .01);
        assertEquals(.1f, pathDataNodes[1].mParams[6], .1);

        // A1 2 3 1,1,5 6 7 8 9 1,1,5 6
        assertEquals('A', pathDataNodes[2].mType);
        assertEquals(1f, pathDataNodes[2].mParams[0], .1);
        assertEquals(2f, pathDataNodes[2].mParams[1], .1);
        assertEquals(3f, pathDataNodes[2].mParams[2], .1);
        assertEquals(1f, pathDataNodes[2].mParams[3], .1);
        assertEquals(1f, pathDataNodes[2].mParams[4], .1);
        assertEquals(5f, pathDataNodes[2].mParams[5], .1);
        assertEquals(6f, pathDataNodes[2].mParams[6], .1);

        // second set of arc command arguments, i.e. ..7 8 9 1,1,5 6
        assertEquals(7f, pathDataNodes[2].mParams[7], .1);
        assertEquals(8f, pathDataNodes[2].mParams[8], .1);
        assertEquals(9f, pathDataNodes[2].mParams[9], .1);
        assertEquals(1f, pathDataNodes[2].mParams[10], .1);
        assertEquals(1f, pathDataNodes[2].mParams[11], .1);
        assertEquals(5f, pathDataNodes[2].mParams[12], .1);
        assertEquals(6f, pathDataNodes[2].mParams[13], .1);
    }


    private void verifyAllChildrenInflated(final GraphicContainerElement containerElement) {
        assertEquals(5, containerElement.getChildren().size());
    }

    VectorGraphic getTestComponent() {
        return (VectorGraphic) mRootContext.findComponentById("testcomp");
    }

    GraphicContainerElement getRoot() {
        return getTestComponent().getOrCreateGraphicContainerElement();
    }

    @Override
    protected void loadDocument(String doc) {
        super.loadDocument(doc);
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

    private boolean compareColors(final int colorOne, final int colorTwo) {
        int colorOneWithoutAlpha = colorOne | 0xFF000000;
        int colorTwoWithoutAlpha = colorTwo | 0xFF000000;
        return colorOneWithoutAlpha == colorTwoWithoutAlpha;
    }
}
