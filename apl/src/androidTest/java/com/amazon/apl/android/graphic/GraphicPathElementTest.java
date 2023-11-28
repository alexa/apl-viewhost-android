/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.graphic;

import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.text.TextUtils;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amazon.apl.android.document.AbstractDocUnitTest;
import com.amazon.apl.android.primitive.Gradient;
import com.amazon.apl.android.VectorGraphic;
import com.amazon.apl.enums.GradientSpreadMethod;
import com.amazon.apl.enums.GradientType;
import com.amazon.apl.enums.GraphicElementType;
import com.amazon.apl.enums.GraphicPropertyKey;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
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
            "          \"pathData\": \"M15,67\"," +
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

    @Test
    public void testProperties_avgPathInflation_optionalExplicitValues() {
        String doc = buildDocument(BASE_DOC, "", OPTIONAL_PROPERTIES_WITH_SOURCE_AS_NAME, String.format(GRAPHICS_SOURCE_TEMPLATE, FULL_PROPS_FOR_AVG_PATH));
        loadDocument(doc);

        GraphicContainerElement containerElement = getRoot();
        assertEquals(1, containerElement.getChildren().size());

        GraphicElement element = containerElement.getChildren().get(0);
        assertEquals(GraphicElementType.kGraphicElementTypePath, element.getType());

        GraphicPathElement avgPathElement = (GraphicPathElement) element;

        Paint fillPaint = avgPathElement.getFillPaint(1f);
        assertTrue(compareColors(Color.BLUE, fillPaint.getColor()));
        assertEquals((int) (OPAQUE * .3), fillPaint.getAlpha());
        assertEquals("M15,67", avgPathElement.getPathData());
        Paint strokePaint = avgPathElement.getStrokePaint(1f);
        assertTrue(compareColors(Color.RED, strokePaint.getColor()));
        assertEquals(OPAQUE, strokePaint.getAlpha());
        assertEquals(1, strokePaint.getStrokeWidth(), 0.1);

        float[] strokeDashArray = avgPathElement.getStrokeDashArray();
        assertEquals(1f, strokeDashArray[0], 0);
        assertEquals(3f, strokeDashArray[1], 0);
        assertEquals(5f, strokeDashArray[2], 0);
        assertEquals(3, avgPathElement.getStrokeDashOffset(), 0);
        assertEquals(2, avgPathElement.getStrokeMiterLimit(), 0);
        assertEquals(Paint.Join.ROUND, avgPathElement.getPaintJoin());
        assertEquals(Paint.Cap.ROUND, avgPathElement.getPaintCap());
        assertEquals(20, avgPathElement.getPathLength(),0);
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
