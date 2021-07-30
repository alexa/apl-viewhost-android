/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.graphics.Color;

import com.amazon.apl.android.EditText;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.views.APLEditText;
import com.amazon.apl.enums.ComponentType;
import com.amazon.apl.enums.FontStyle;
import com.amazon.apl.enums.KeyboardType;
import com.amazon.apl.enums.SubmitKeyType;

import org.junit.Before;
import org.junit.Test;

import androidx.test.filters.SmallTest;

import static org.junit.Assert.assertEquals;

public class EditTextTest extends AbstractComponentUnitTest<APLEditText, EditText> {

    @Before
    public void doBefore() {
        REQUIRED_PROPERTIES = ""; // no required properties in EditText Component.
        OPTIONAL_PROPERTIES =
                " \"borderColor\": \"black\"," +
                        " \"borderStrokeWidth\": 20," +
                        " \"borderWidth\": 30," +
                        " \"color\": \"red\"," +
                        " \"fontFamily\": \"times new roman, times, georgia, serif\"," +
                        " \"fontSize\": 18," +
                        " \"fontStyle\": \"italic\"," +
                        " \"fontWeight\": 500," +
                        " \"highlightColor\": \"yellow\"," +
                        " \"hint\": \"This is a hint\"," +
                        " \"hintColor\":\"blue\" ," +
                        " \"hintStyle\": \"italic\"," +
                        " \"hintWeight\": 300," +
                        " \"keyboardType\": \"emailAddress\"," +
                        " \"maxLength\": 2," +
                        " \"secureInput\": true," +
                        " \"selectOnFocus\": true," +
                        " \"size\": 10," +
                        " \"submitKeyType\": \"send\"," +
                        " \"text\": \"This is a text\"," +
                        " \"validCharacters\": \"0-9\"";
    }

    @Override
    String getComponentType() {
        return "EditText";
    }

    /**
     * Test the required properties of the Component.
     * @param component The Component for testing.
     */
    @Override
    void testProperties_required(EditText component) {
        assertEquals(ComponentType.kComponentTypeEditText, component.getComponentType());
    }

    /**
     * Test the optional properties of the Component.  This test should check for default value
     * and values. No need to set {@link #OPTIONAL_PROPERTIES}.
     *
     * @param component The Component for testing.
     */
    @Override
    void testProperties_optionalDefaultValues(EditText component) {
        assertEquals(Color.TRANSPARENT, component.getBorderColor());
        assertEquals(component.getBorderWidth(), component.getDrawnBorderWidth());
        assertEquals(0, component.getBorderWidth());
        assertEquals(Color.argb(255, 250, 250, 250), component.getColor()); // #fafafaff (dark theme)
        assertEquals("sans-serif", component.getFontFamily());
        assertEquals(40, component.getFontSize());
        assertEquals(FontStyle.kFontStyleNormal, component.getFontStyle());
        assertEquals(400, component.getFontWeight());
        assertEquals(Color.argb(77, 0, 202, 255), component.getHighlightColor()); // #00caff4d (dark theme)
        assertEquals("", component.getHint());
        assertEquals(Color.argb(255, 250, 250, 250), component.getHintColor()); // #fafafaff (dark theme)
        assertEquals(FontStyle.kFontStyleNormal, component.getHintFontStyle());
        assertEquals(400, component.getHintFontWeight());
        assertEquals(KeyboardType.kKeyboardTypeNormal, component.getKeyboardType());
        assertEquals(0, component.getMaxLength());
        assertEquals(false, component.isSecureInput());
        assertEquals(false, component.isSelectOnFocus());
        assertEquals(8, component.getSize());
        assertEquals(SubmitKeyType.kSubmitKeyTypeDone, component.getSubmitKeyType());
        assertEquals("", component.getText());
        assertEquals("", component.getValidCharacters());
    }

    /**
     * Test the optional properties of the Component.  This test should check values when the property
     * is set explicitly, and should use values other than the default.  Set the {@link #OPTIONAL_PROPERTIES}
     * value before this test.
     *
     * @param component The Component for testing.
     */
    @Override
    void testProperties_optionalExplicitValues(EditText component) {
        assertEquals(Color.BLACK, component.getBorderColor());
        assertEquals(20, component.getDrawnBorderWidth()); // Minimum of border width and border stroke width
        assertEquals(Color.RED, component.getColor());
        assertEquals("times new roman, times, georgia, serif", component.getFontFamily());
        assertEquals(18, component.getFontSize());
        assertEquals(FontStyle.kFontStyleItalic, component.getFontStyle());
        assertEquals(500, component.getFontWeight());
        assertEquals(Color.YELLOW, component.getHighlightColor());
        assertEquals("This is a hint", component.getHint());
        assertEquals(Color.BLUE, component.getHintColor());
        assertEquals(FontStyle.kFontStyleItalic, component.getHintFontStyle());
        assertEquals(300, component.getHintFontWeight());
        assertEquals(KeyboardType.kKeyboardTypeEmailAddress, component.getKeyboardType());
        assertEquals(2, component.getMaxLength()); // Viewhost enforces max length on the text
        assertEquals(true, component.isSecureInput());
        assertEquals(true, component.isSelectOnFocus());
        assertEquals(10, component.getSize()); // Viewhost enforces size on the text
        assertEquals(SubmitKeyType.kSubmitKeyTypeSend, component.getSubmitKeyType());
        assertEquals("This is a text", component.getText());
        assertEquals("0-9", component.getValidCharacters()); // Viewhost enforces restriction of characters on the text
    }

    /**
     * Verify that component width is calculated based on size.
     */
    @Test
    @SmallTest
    public void testMeasure_modeAtMostBoring_width_depends_on_size() {

        // Use a document where text has a parent, otherwise measurement isn't needed

        // MeasureMode.AT_MOST
        inflateDocument(buildDocument(PARENT_DOC, "",
                " \"text\": \"Hello APL!\"", ""));
        EditText text = getTestComponent();
        text.measureTextContent(160, 960, RootContext.MeasureMode.AtMost, 480, RootContext.MeasureMode.AtMost);
        int widthPx = text.getMeasuredWidthPx();
        int heightPx = text.getMeasuredHeightPx();

        inflateDocument(buildDocument(PARENT_DOC, "",
                " \"text\": \"Hello APL!\", \"size\": 4", ""));
        text = getTestComponent();
        text.measureTextContent(160, 960, RootContext.MeasureMode.AtMost, 480, RootContext.MeasureMode.AtMost);
        assertEquals(widthPx/2, text.getMeasuredWidthPx());
        assertEquals(heightPx, text.getMeasuredHeightPx());
    }

    /**
     * Verify that component height is calculated based on fontSize.
     */
    @Test
    @SmallTest
    public void testMeasure_modeAtMostBoring_specified_fontSize() {

        // Use a document where text has a parent, otherwise measurement isn't needed

        // MeasureMode.AT_MOST
        inflateDocument(buildDocument(PARENT_DOC, "",
                " \"text\": \"Hello APL!\"", ""));
        EditText text = getTestComponent();
        text.measureTextContent(160, 960, RootContext.MeasureMode.AtMost, 480, RootContext.MeasureMode.AtMost);
        int widthPx = text.getMeasuredWidthPx();
        int heightPx = text.getMeasuredHeightPx();

        inflateDocument(buildDocument(PARENT_DOC, "",
                " \"text\": \"Hello APL!\", \"size\": 4, \"fontSize\": \"80dp\"", ""));
        text = getTestComponent();
        text.measureTextContent(160, 960, RootContext.MeasureMode.AtMost, 480, RootContext.MeasureMode.AtMost);
        // size reduced to half and fontSize doubled from default should approximately result in same width.
        assertEquals(widthPx, text.getMeasuredWidthPx(), 10);
        assertEquals(2 * heightPx - 1, text.getMeasuredHeightPx());
    }
}
