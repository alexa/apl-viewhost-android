/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.graphics.Color;

import androidx.test.filters.SmallTest;

import com.amazon.apl.android.EditText;
import com.amazon.apl.android.EditTextProxy;
import com.amazon.apl.android.views.APLEditText;
import com.amazon.apl.enums.ComponentType;
import com.amazon.apl.enums.FontStyle;
import com.amazon.apl.enums.KeyboardType;
import com.amazon.apl.enums.SubmitKeyType;

import org.junit.Before;
import org.junit.Test;

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
                        " \"text\": \"19896 Text\"," +
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
        EditTextProxy proxy = component.getProxy();
        assertEquals(Color.TRANSPARENT, proxy.getBorderColor());
        assertEquals(proxy.getBorderWidth(), proxy.getDrawnBorderWidth());
        assertEquals(0, proxy.getBorderWidth());
        assertEquals(Color.argb(255, 250, 250, 250), proxy.getColor()); // #fafafaff (dark theme)
        assertEquals("sans-serif", proxy.getFontFamily());
        assertEquals(40, proxy.getFontSize());
        assertEquals(FontStyle.kFontStyleNormal, proxy.getFontStyle());
        assertEquals(400, proxy.getFontWeight());
        assertEquals(Color.argb(77, 0, 202, 255), proxy.getHighlightColor()); // #00caff4d (dark theme)
        assertEquals("", proxy.getHint());
        assertEquals(Color.argb(255, 250, 250, 250), proxy.getHintColor()); // #fafafaff (dark theme)
        assertEquals(FontStyle.kFontStyleNormal, proxy.getHintFontStyle());
        assertEquals(400, proxy.getHintFontWeight());
        assertEquals(KeyboardType.kKeyboardTypeNormal, proxy.getKeyboardType());
        assertEquals(0, proxy.getMaxLength());
        assertEquals(false, proxy.isSecureInput());
        assertEquals(false, proxy.isSelectOnFocus());
        assertEquals(8, proxy.getSize());
        assertEquals(SubmitKeyType.kSubmitKeyTypeDone, proxy.getSubmitKeyType());
        assertEquals("", proxy.getText());
        assertEquals("", proxy.getValidCharacters());
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
        EditTextProxy proxy = component.getProxy();
        assertEquals(Color.BLACK, proxy.getBorderColor());
        assertEquals(20, proxy.getDrawnBorderWidth()); // Minimum of border width and border stroke width
        assertEquals(Color.RED, proxy.getColor());
        assertEquals("times new roman, times, georgia, serif", proxy.getFontFamily());
        assertEquals(18, proxy.getFontSize());
        assertEquals(FontStyle.kFontStyleItalic, proxy.getFontStyle());
        assertEquals(500, proxy.getFontWeight());
        assertEquals(Color.YELLOW, proxy.getHighlightColor());
        assertEquals("This is a hint", proxy.getHint());
        assertEquals(Color.BLUE, proxy.getHintColor());
        assertEquals(FontStyle.kFontStyleItalic, proxy.getHintFontStyle());
        assertEquals(300, proxy.getHintFontWeight());
        assertEquals(KeyboardType.kKeyboardTypeEmailAddress, proxy.getKeyboardType());
        assertEquals(2, proxy.getMaxLength()); // Viewhost enforces max length on the text
        assertEquals(true, proxy.isSecureInput());
        assertEquals(true, proxy.isSelectOnFocus());
        assertEquals(10, proxy.getSize()); // Viewhost enforces size on the text
        assertEquals(SubmitKeyType.kSubmitKeyTypeSend, proxy.getSubmitKeyType());
        assertEquals("19", proxy.getText());
        assertEquals("0-9", proxy.getValidCharacters()); // Viewhost enforces restriction of characters on the text
    }

}
