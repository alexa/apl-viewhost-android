/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.graphic;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.text.TextUtils;

import com.amazon.apl.android.document.AbstractDocUnitTest;
import com.amazon.apl.android.VectorGraphic;
import com.amazon.apl.enums.GraphicElementType;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class GraphicGroupElementTest extends AbstractDocUnitTest {

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

    private static final String FULL_PROPS_FOR_AVG_GROUP = "{" +
            "  \"type\": \"AVG\"," +
            "  \"version\": \"1.1\"," +
            "  \"height\": 24," +
            "  \"width\": 24," +
            "  \"items\": [" +
            "    {" +
            "      \"type\": \"group\"," +
            "      \"opacity\": 0.8," +
            "      \"scaleX\": 0.5," +
            "      \"scaleY\": 0.5," +
            "      \"pivotX\": 50," +
            "      \"pivotY\": 50," +
            "      \"rotation\": 90.0," +
            "      \"translateX\": 10," +
            "      \"translateY\": 10," +
            "      \"clipPath\": \"M0,0\", " +
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


    private static final String OPACITIES_AVG_GROUP = "{" +
            "  \"type\": \"AVG\"," +
            "  \"version\": \"1.1\"," +
            "  \"resources\": [" +
            "    {" +
            "      \"patterns\": {" +
            "        \"RedCircle\": {" +
            "          \"width\": \"18\"," +
            "          \"height\": \"18\"," +
            "          \"item\": {" +
            "            \"type\": \"group\"," +
            "            \"opacity\": 0.5," +
            "            \"items\": [" +
            "              {" +
            "                \"type\": \"path\"," +
            "                \"pathData\": \"M0,9 a9,9 0 1 1 18,0 a9,9 0 1 1 -18,0\"," +
            "                \"fill\": \"red\"," +
            "                \"fillOpacity\": 0.5," +
            "                \"stroke\": \"pink\"," +
            "                \"strokeOpacity\": 0.25" +
            "              }" +
            "            ]" +
            "          }" +
            "        }" +
            "      }," +
            "      \"gradients\": {" +
            "         \"LinearGradient\": {" +
            "           \"inputRange\": [ 0, 1]," +
            "           \"colorRange\": [ \"#ffffffff\", \"#000000ff\" ]," +
            "           \"type\": \"linear\"," +
            "           \"x1\": 0.3," +
            "           \"y1\": 0.4," +
            "           \"x2\": 0.7," +
            "           \"y2\": 0.5" +
            "         }" +
            "      }" +
            "    }" +
            "  ]," +
            "  \"height\": 24," +
            "  \"width\": 24," +
            "  \"items\": [" +
            "    {" +
            "      \"type\": \"group\"," +
            "      \"opacity\": 0.8," +
            "      \"items\": [" +
            "        {" +
            "          \"type\": \"path\"," +
            "          \"fill\": \"blue\"," +
            "          \"fillOpacity\": 0.3," +
            "          \"stroke\": \"@RedCircle\"," +
            "          \"strokeOpacity\": 0.1," +
            "          \"pathData\": \"M15.67,4H14V2h-4v2H8.33C7.6,4 7,4.6 7,5.33V9h4.93L13,7v2h4V5.33C17,4.6 16.4,4 15.67,4z\"" +
            "        }," +
            "        {" +
            "          \"type\": \"path\"," +
            "          \"fill\": \"white\"," +
            "          \"pathData\": \"M13,12.5h2L11,20v-5.5H9L11.93,9H7v11.67C7,21.4 7.6,22 8.33,22h7.33c0.74,0 1.34,-0.6 1.34,-1.33V9h-4v3.5z\"" +
            "        }," +
            "        {" +
            "          \"type\": \"path\"," +
            "          \"stroke\": \"blue\"," +
            "          \"pathData\": \"M13,12.5h2L11,20v-5.5H9L11.93,9H7v11.67C7,21.4 7.6,22 8.33,22h7.33c0.74,0 1.34,-0.6 1.34,-1.33V9h-4v3.5z\"" +
            "        }," +
            "        {" +
            "          \"type\": \"text\"," +
            "          \"fill\": \"@LinearGradient\"," +
            "          \"fillOpacity\": 0.6," +
            "          \"fontFamily\": \"amazon-ember\"," +
            "          \"stroke\": \"orange\"," +
            "          \"strokeOpacity\": 0.4," +
            "          \"strokeWidth\": 3," +
            "          \"text\": \"message\"" +
            "        }" +
            "      ]" +
            "    }" +
            "  ]" +
            "}";

    @Test
    public void testProperties_avgGroupInflation_optionalDefaultValues() {

        final String REQUIRED_PROPS_FOR_AVG_GROUP = "{" +
                "  \"type\": \"AVG\"," +
                "  \"version\": \"1.0\"," +
                "  \"height\": 24," +
                "  \"width\": 24," +
                "  \"items\": [{" +
                "      \"type\": \"group\"" +
                "    }" +
                "  ]" +
                "}";

        String doc = buildDocument(BASE_DOC, "", OPTIONAL_PROPERTIES_WITH_SOURCE_AS_NAME, String.format(GRAPHICS_SOURCE_TEMPLATE, REQUIRED_PROPS_FOR_AVG_GROUP));
        loadDocument(doc);
        VectorGraphic component = getTestComponent();

        GraphicContainerElement containerElement = component.getOrCreateGraphicContainerElement();
        assertEquals(1, containerElement.getChildren().size());

        GraphicElement element = containerElement.getChildren().get(0);
        assertEquals(GraphicElementType.kGraphicElementTypeGroup, element.getType());

        GraphicGroupElement avgGroupElement = (GraphicGroupElement)element;

        assertEquals(1, avgGroupElement.getOpacity(), 0.1);

        Matrix expectedTransformation = getTransformationCalculation(1, 1, 0, 0, 0, 0, 0);
        assertArrayEquals(getMatrixArray(expectedTransformation), getMatrixArray(avgGroupElement.getTransform()), 0.0001f);
        assertEquals(0, avgGroupElement.getChildren().size());
        assertEquals("", avgGroupElement.getClipPath());
    }

    @Test
    public void testProperties_avgGroupInflation_optionalExplicitValues() {
        String doc = buildDocument(BASE_DOC, "", OPTIONAL_PROPERTIES_WITH_SOURCE_AS_NAME, String.format(GRAPHICS_SOURCE_TEMPLATE, FULL_PROPS_FOR_AVG_GROUP));
        loadDocument(doc);
        VectorGraphic component = getTestComponent();

        GraphicContainerElement containerElement = component.getOrCreateGraphicContainerElement();
        assertEquals(1, containerElement.getChildren().size());

        GraphicElement element = containerElement.getChildren().get(0);
        assertEquals(GraphicElementType.kGraphicElementTypeGroup, element.getType());

        GraphicGroupElement avgGroupElement = (GraphicGroupElement)element;

        assertEquals(0.8, avgGroupElement.getOpacity(), 0.1);
        Matrix expectedTransformation = getTransformationCalculation(0.5f, 0.5f, 90, 50, 50, 10, 10);
        assertArrayEquals(getMatrixArray(expectedTransformation), getMatrixArray(avgGroupElement.getTransform()), 0.0001f);
        assertEquals(2, avgGroupElement.getChildren().size());
        assertEquals("M0,0", avgGroupElement.getClipPath());
    }

    private Matrix getTransformationCalculation(float scaleX, float scaleY, int rotation,
                                                int pivotX, int pivotY, int translateX, int translateY) {
        Matrix matrix = new Matrix();
        matrix.postTranslate(-pivotX, -pivotY);
        matrix.postScale(scaleX, scaleY);
        matrix.postRotate(rotation, 0, 0);
        matrix.postTranslate(translateX + pivotX, translateY + pivotY);
        return matrix;
    }

    private float[] getMatrixArray(Matrix m) {
        float[] arr = new float[9];
        m.getValues(arr);
        return arr;
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
