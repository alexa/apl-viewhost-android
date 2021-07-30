/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;


import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.Text;
import com.amazon.apl.android.views.APLTextView;
import com.amazon.apl.enums.ComponentType;
import com.amazon.apl.enums.FontStyle;
import com.amazon.apl.enums.TextAlign;
import com.amazon.apl.enums.TextAlignVertical;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static com.amazon.apl.enums.PropertyKey.kPropertyText;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;


@RunWith(AndroidJUnit4.class)
public class TextTest extends AbstractComponentUnitTest<APLTextView, Text> {


    @Override
    String getComponentType() {
        return "Text";
    }

    @Mock
    APLTextView mView;

    @Before
    public void doBefore() {
        REQUIRED_PROPERTIES = ""; // no required properties in Text Component.
        OPTIONAL_PROPERTIES =
                " \"color\": \"blue\"," +
                        " \"fontFamily\": \"Sans Serif\"," +
                        " \"fontSize\": \"100dp\"," +
                        " \"fontStyle\": \"italic\"," +
                        " \"fontWeight\": 700," +
                        " \"letterSpacing\": 0.25," +
                        " \"lineHeight\": 1.5," +
                        " \"maxLines\": 2," +
                        " \"text\": \"Hello APL!\"," +
                        " \"textAlign\":\"center\"," +
                        " \"textAlignVertical\": \"center\"";
        when(mView.getDensity()).thenReturn(160.0f);
    }


    /**
     * Test the required properties of the Component.
     *
     * @param component The Component for testing.
     */
    @Override
    void testProperties_required(Text component) {
        assertEquals(ComponentType.kComponentTypeText, component.getComponentType());
    }

    /**
     * Test the optional properties of the Component.  This test should check for default value
     * and values. {@link #OPTIONAL_PROPERTIES} should be set prior to this test to ensure a valid
     * Component is created.
     *
     * @param component The Component for testing.
     */
    @Override
    void testProperties_optionalDefaultValues(Text component) {
        assertEquals(0xFFFAFAFA, component.getColor());
        assertEquals("sans-serif", component.getFontFamily());
        assertEquals(40, component.getFontSize().intValue());
        assertEquals(FontStyle.kFontStyleNormal, component.getFontStyle());
        assertEquals(400, component.getFontWeight());
        assertEquals(0.0f, component.getLetterSpacing().value(), 0);
        assertEquals(1.25f, component.getLineHeight(), 0);
        assertEquals(0, component.getMaxLines());
        assertEquals("", component.getText(component.mProperties.getStyledText(kPropertyText)).toString());
        assertEquals(TextAlign.kTextAlignAuto, component.getTextAlign());
        assertEquals(TextAlignVertical.kTextAlignVerticalAuto, component.getTextAlignVertical());
    }

    /**
     * Test the optional properties of the Component.  This test should check values when the property
     * is set explicitly, and should use values other than the default.  Set the {@link #OPTIONAL_PROPERTIES}
     * value before this test.
     *
     * @param component The Component for testing.
     */
    @Override
    void testProperties_optionalExplicitValues(Text component) {
        assertEquals(Color.BLUE, component.getColor());
        assertEquals("Sans Serif", component.getFontFamily());
        assertEquals(100, component.getFontSize().intValue());
        assertEquals(FontStyle.kFontStyleItalic, component.getFontStyle());
        assertEquals(700, component.getFontWeight());
        assertEquals(.25f, component.getLetterSpacing().value(), 0);
        assertEquals(1.5f, component.getLineHeight(), 0);
        assertEquals(2, component.getMaxLines());
        assertEquals("Hello APL!", component.getText(component.mProperties.getStyledText(kPropertyText)).toString());
        assertEquals(TextAlign.kTextAlignCenter, component.getTextAlign());
        assertEquals(TextAlignVertical.kTextAlignVerticalCenter, component.getTextAlignVertical());
    }

    @Test
    @SmallTest
    public void testMeasure_modeAtMostExciting() {

        // Use a document where text has a parent, otherwise measurement isn't needed

        // MeasureMode.AT_MOST
        inflateDocument(buildDocument(PARENT_DOC, "",
                " \"text\": \"77°F\"", ""));
        Text text = getTestComponent();
        text.measureTextContent(mView.getDensity(), text.getInnerBounds().getWidth(), RootContext.MeasureMode.AtMost, text.getInnerBounds().getHeight(), RootContext.MeasureMode.AtMost);
        assertEquals(82, text.getMeasuredWidthPx());
        assertEquals(51, text.getMeasuredHeightPx());
    }


    @Test
    @SmallTest
    public void testMeasure_modeUndefined() {

        String scrollDoc = "{" +
                "  \"type\": \"APL\"," +
                "  \"version\": \"1.0\"," +
                "  \"mainTemplate\": {" +
                "    \"item\": {\n" +
                "      \"type\": \"Text\"," +
                "      \"height\": 200," +
                "      \"item\": {" +
                "      \"name\": \"testcomp\", " +
                "        \"type\": \"%s\" %s" +
                "      }" +
                "    }" +
                "  }" +
                "}";

        // MeasureMode.Undefined
        // height is undefined when in a ScrollView
        inflateDocument(buildDocument(scrollDoc, "", "", ""));
        Text text = getTestComponent();
        text.measureTextContent(mView.getDensity(), text.getInnerBounds().getWidth(), RootContext.MeasureMode.Exactly, text.getInnerBounds().getHeight(), RootContext.MeasureMode.Exactly);
        assertEquals(mRootContext.getPixelWidth(), text.getMeasuredWidthPx());
        assertEquals(200, text.getMeasuredHeightPx());
    }

    @Test
    @SmallTest
    public void testMeasure_modeUndefinedBoring() {
        String PARENT = "{" +
                "  \"type\": \"APL\"," +
                "  \"version\": \"1.0\"," +
                "  \"mainTemplate\": {" +
                "    \"item\": {" +
                "      \"type\": \"Container\"," +
                "      \"direction\": \"row\"," +
                "      \"item\": {" +
                "      \"type\": \"Frame\"," +
                "      \"name\": \"parentcomp\"," +
                "        \"item\": { " +
                "          \"id\": \"testcomp\", " +
                "          \"width\": \"100%%%%\", " +
                "          \"type\": \"%s\" %s" +
                "         }" +
                "       }" +
                "    }" +
                "  }" +
                "}";
        // MeasureMode.Undefined
        inflateDocument(buildDocument(PARENT, "",
                " \"text\": \"Boring\"", ""));
        Text text = getTestComponent();
        text.measureTextContent(mView.getDensity(), text.getInnerBounds().getWidth(), RootContext.MeasureMode.Undefined, text.getInnerBounds().getHeight(), RootContext.MeasureMode.Undefined);
        assertEquals(120, text.getMeasuredWidthPx());
        assertEquals(51, text.getMeasuredHeightPx());
    }


    @Test
    @SmallTest
    public void testMeasure_modeExactly() {

        // MeasureMode.Exactly happens when the text has a width/height explicitly set,
        // in this case measureTextContent isn't called until the view is created

        // buildRootContextDependencies the default document which has an empty text an set explicit width and height
        inflateDocument(buildDocument("\"width\": 1011,\"height\": 621"));
        Text text = getTestComponent();
        text.measureTextContent(mView.getDensity(), text.getInnerBounds().getWidth(), RootContext.MeasureMode.Exactly, text.getInnerBounds().getHeight(), RootContext.MeasureMode.Exactly);
        assertEquals(1011, text.getMeasuredWidthPx());
        assertEquals(621, text.getMeasuredHeightPx());
    }


    @Test
    @SmallTest
    public void testFont_lineHeight() {
        inflateDocument(buildDocument(REQUIRED_PROPERTIES, OPTIONAL_PROPERTIES));
        Text text = getTestComponent();
        int fontSize = text.getFontSize().intValue();
        assertEquals(100, fontSize);

        TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(fontSize);
        textPaint.setTypeface(Typeface.SANS_SERIF);
        textPaint.density = 1.0f;

        float ls = text.getLineHeight();
        assertEquals(1.5, ls, 0.0f);
    }


    @Test
    @SmallTest
    public void testMeasureContent_WhenDisplayIsNone_MakesMeasuredWidthAndHeightEqualToInputWidthPxAndHeightPx() {
        final String optionalProperties = OPTIONAL_PROPERTIES + ", \"display\": \"none\"";
        inflateDocument(buildDocument(REQUIRED_PROPERTIES, optionalProperties));
        Text text = getTestComponent();
        text.measureTextContent(160, 100, RootContext.MeasureMode.Undefined, 150, RootContext.MeasureMode.Undefined);

        assertEquals(100, text.getMeasuredWidthPx());
        assertEquals(150, text.getMeasuredHeightPx());
    }

    @Test
    @SmallTest
    public void testComponent_receives_language_from_document() {

        // Use a document where text has a parent, otherwise measurement isn't needed

        // MeasureMode.AT_MOST
        inflateDocument(buildDocument(PARENT_DOC_WITH_LANG_AND_LAYOUT_DIRECTION, "",
                " \"text\": \"77°F\"", ""));
        Text text = getTestComponent();
        assertEquals("en-US", text.getFontLanguage());
    }

    @Test
    @SmallTest
    public void testMeasure_withoutSpanTags() {
        inflateDocument(buildDocument(PARENT_DOC, "",
                " \"text\": \"And here's to you, Mrs. Robinson Jesus loves you more than you will know\", \"fontSize\": \"20dp\"", ""));
        Text text = getTestComponent();
        text.measureTextContent(mView.getDensity(), text.getInnerBounds().getWidth(), RootContext.MeasureMode.AtMost, text.getInnerBounds().getHeight(), RootContext.MeasureMode.AtMost);

        assertEquals(26, text.getMeasuredHeightPx());
    }

    @Test
    @SmallTest
    public void testMeasure_withSpanTag() {
        inflateDocument(buildDocument(PARENT_DOC, "",
                " \"text\": \"And here's to you, <span fontSize='100dp'>Mrs. Robinson</span> Jesus loves you more than you will know\", \"fontSize\": \"20dp\"", ""));
        Text text = getTestComponent();
        text.measureTextContent(mView.getDensity(), text.getInnerBounds().getWidth(), RootContext.MeasureMode.AtMost, text.getInnerBounds().getHeight(), RootContext.MeasureMode.AtMost);

        assertEquals(126, text.getMeasuredHeightPx());
    }

    @Test
    @SmallTest
    public void testMeasure_withTwoSpanTag() {
        inflateDocument(buildDocument(PARENT_DOC, "",
                " \"text\": \"And here's to you, <span fontSize='100dp'>Mrs. Robinson</span><br> <span fontSize='50dp'>Jesus</span> loves you more than you will know\", \"fontSize\": \"20dp\"", ""));
        Text text = getTestComponent();
        text.measureTextContent(mView.getDensity(), text.getInnerBounds().getWidth(), RootContext.MeasureMode.AtMost, text.getInnerBounds().getHeight(), RootContext.MeasureMode.AtMost);

        assertEquals(185, text.getMeasuredHeightPx());
    }
}
