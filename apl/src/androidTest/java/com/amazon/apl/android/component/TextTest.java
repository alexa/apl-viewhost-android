/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;


import android.graphics.Color;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amazon.apl.android.Text;
import com.amazon.apl.android.TextProxy;
import com.amazon.apl.android.views.APLTextView;
import com.amazon.apl.enums.ComponentType;
import com.amazon.apl.enums.FontStyle;
import com.amazon.apl.enums.LayoutDirection;
import com.amazon.apl.enums.TextAlign;
import com.amazon.apl.enums.TextAlignVertical;

import org.junit.Before;
import org.junit.runner.RunWith;

import static com.amazon.apl.enums.PropertyKey.kPropertyText;
import static org.junit.Assert.assertEquals;


@RunWith(AndroidJUnit4.class)
public class TextTest extends AbstractComponentUnitTest<APLTextView, Text> {


    @Override
    String getComponentType() {
        return "Text";
    }

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
                        " \"textAlignVertical\": \"center\"," +
                        " \"layoutDirection\": \"RTL\"," +
                        " \"lang\": \"en-US\"";
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
        TextProxy proxy = component.getProxy();
        assertEquals(0xFFFAFAFA, proxy.getColor());
        assertEquals("sans-serif", proxy.getFontFamily());
        assertEquals(40, proxy.getFontSize().intValue());
        assertEquals(FontStyle.kFontStyleNormal, proxy.getFontStyle());
        assertEquals(400, proxy.getFontWeight());
        assertEquals(0.0f, proxy.getLetterSpacing().value(), 0);
        assertEquals(1.25f, proxy.getLineHeight(), 0);
        assertEquals(0, proxy.getMaxLines());
        assertEquals("", proxy.getText(proxy.getStyledText(kPropertyText), null).toString());
        assertEquals(TextAlign.kTextAlignAuto, proxy.getTextAlign());
        assertEquals(TextAlignVertical.kTextAlignVerticalAuto, proxy.getTextAlignVertical());
        assertEquals("", proxy.getFontLanguage());
        assertEquals(LayoutDirection.kLayoutDirectionLTR, proxy.getLayoutDirection());
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
        TextProxy proxy = component.getProxy();
        assertEquals(Color.BLUE, proxy.getColor());
        assertEquals("Sans Serif", proxy.getFontFamily());
        assertEquals(100, proxy.getFontSize().intValue());
        assertEquals(FontStyle.kFontStyleItalic, proxy.getFontStyle());
        assertEquals(700, proxy.getFontWeight());
        assertEquals(.25f, proxy.getLetterSpacing().value(), 0);
        assertEquals(1.5f, proxy.getLineHeight(), 0);
        assertEquals(2, proxy.getMaxLines());
        assertEquals("Hello APL!", proxy.getText(proxy.getStyledText(kPropertyText), null).toString());
        assertEquals(TextAlign.kTextAlignCenter, proxy.getTextAlign());
        assertEquals(TextAlignVertical.kTextAlignVerticalCenter, proxy.getTextAlignVertical());
        assertEquals("en-US", proxy.getFontLanguage());
        assertEquals(LayoutDirection.kLayoutDirectionRTL, proxy.getLayoutDirection());
    }

}
