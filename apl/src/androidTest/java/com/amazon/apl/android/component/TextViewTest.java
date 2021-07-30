/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;


import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.text.ParcelableSpan;
import android.text.Spannable;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.StyleSpan;

import com.amazon.apl.android.Text;
import com.amazon.apl.android.views.APLTextView;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.amazon.apl.android.espresso.APLViewActions.executeCommands;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TextViewTest extends AbstractComponentViewTest<APLTextView, Text> {
    private static final String[] LINES = {
            "Hello APL123!\uD83D\uDE1C\uD83D\uDC47\uD83C\uDFC6❤️❤️️\uD83E\uDD26",
            "\uD83E\uDD80\uD83C\uDF59second?^&*><#$@ \uD83C\uDFBB❤️❤️❤️️line\uD83C\uDFC1\uD83E\uDD26",
            "\uD83C\uDFC1third \uD83C\uDF99line"
    };

    private static final String[] TEXT = {
            "This is the first line",
            "This is the last line"
    };

    private static final String[] SPANNED_TEXT = {
            "This is line of text",
            "<b>This is line of text</b>",
            "<i>This is line of text<i>"
    };

    private static final Map<String, Integer> COLORS = new HashMap<>();

    static {
        COLORS.put("blue", Color.BLUE);
        COLORS.put("red", Color.RED);
        COLORS.put("black", Color.BLACK);
        COLORS.put("yellow", Color.YELLOW);
        COLORS.put("white", Color.WHITE);
    }

    @Before
    public void doBefore() {
        REQUIRED_PROPERTIES = ""; // no required properties in Text Component.
        OPTIONAL_PROPERTIES =
                " \"color\": \"blue\"," +
                        " \"fontFamily\": \"Sans Serif\"," +
                        " \"fontSize\": \"20dp\"," +
                        " \"fontStyle\": \"italic\"," +
                        " \"fontWeight\": 700," +
                        " \"letterSpacing\": 0.25," +
                        " \"lineHeight\": 1.5," +
                        " \"maxLines\": 3," +
                        " \"text\": \"" + LINES[0] + "<br/>" + LINES[1] + "<br/>" + LINES[2] + "\"," +
                        " \"textAlign\":\"center\"," +
                        " \"textAlignVertical\": \"center\"";
    }


    /**
     * @return The string representation used in the APL document for this component.  For example
     * when testing the Text Component this method should return "Text".
     */
    @Override
    String getComponentType() {
        return "Text";
    }


    @Override
    Class<APLTextView> getViewClass() {
        return APLTextView.class;
    }

    interface LineUtil {
        String GetLine(StaticLayout staticLayout, int line);
    }

    /**
     * Test the view after properties have been assigned. This uses the OPTIONAL_PROPERTIES.
     *
     * @param view The Component view for testing.
     */
    @Override
    void testView_applyProperties(APLTextView view) {
        StaticLayout staticLayout = view.getLayout();
        TextPaint paint = staticLayout.getPaint();

        // text content
        LineUtil lineUtil = (StaticLayout layout, int line) -> staticLayout.getText().subSequence(
                staticLayout.getLineStart(line), staticLayout.getLineEnd(line)
        ).toString();
        assertEquals(LINES[0] + "\n", lineUtil.GetLine(staticLayout, 0));
        assertEquals(LINES[1] + "\n", lineUtil.GetLine(staticLayout, 1));
        assertEquals(LINES[2], lineUtil.GetLine(staticLayout, 2));
        assertEquals(LINES[0] + "\n" + LINES[1] + "\n" + LINES[2], staticLayout.getText().toString());

        assertEquals(Paint.ANTI_ALIAS_FLAG, Paint.ANTI_ALIAS_FLAG & paint.getFlags());
        assertEquals(Color.BLUE, paint.getColor());

        // font size and density
        assertEquals(20f, paint.getTextSize() / paint.density, 0);

        assertEquals(0.0125f, paint.getLetterSpacing(), 0.001);
        assertEquals(1.0, paint.getTextScaleX(), 0);

        Typeface tf = paint.getTypeface();
        assertTrue(tf.isItalic());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            assertEquals(700, tf.getWeight());
        }
    }

    @Test
    public void testView_dynamicSpannedText() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(REQUIRED_PROPERTIES, CHILD_LAYOUT_PROPERTIES))
                .check(hasRootContext());

        assertEquals("", getTestView().getLayout().getText().toString());

        for (int i = 0; i < SPANNED_TEXT.length; i++) {
            final String inputString = SPANNED_TEXT[i];
            final String expectedString = "This is line of text";

            onView(isRoot())
                    .perform(executeCommands(mTestContext.getRootContext(), setValueCommand("text", inputString)));
            assertEquals(expectedString, getTestView().getLayout().getText().toString());

            assertTrue(getTestView().getLayout().getText() instanceof Spannable);

            final Spannable spannable = (Spannable) getTestView().getLayout().getText();
            final ParcelableSpan[] obj = spannable.getSpans(0, spannable.length(), ParcelableSpan.class);
            switch (i) {
                case 0:
                    assertTrue(obj == null || obj.length == 0);
                    break;
                case 1:
                    assertTrue(obj != null && obj.length == 1);
                    assertTrue(obj[0] instanceof StyleSpan);
                    assertEquals(Typeface.BOLD, ((StyleSpan)obj[0]).getStyle());
                    break;
                case 2:
                    assertTrue(obj != null && obj.length == 1);
                    assertTrue(obj[0] instanceof StyleSpan);
                    assertEquals(Typeface.ITALIC, ((StyleSpan)obj[0]).getStyle());
                    break;
                default:
                    break;
            }
        }
    }

    @Test
    public void testView_dynamicText() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(REQUIRED_PROPERTIES, CHILD_LAYOUT_PROPERTIES))
                .check(hasRootContext());

        assertEquals("", getTestView().getLayout().getText().toString());

        for (String expectedString : TEXT) {
            onView(isRoot())
                    .perform(executeCommands(mTestContext.getRootContext(), setValueCommand("text", expectedString)));
            assertEquals(expectedString, getTestView().getLayout().getText().toString());
        }
    }

    @Test
    public void testView_dynamicColor() {
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(REQUIRED_PROPERTIES, CHILD_LAYOUT_PROPERTIES))
                .check(hasRootContext());

        for (String expectedColor : COLORS.keySet()) {
            onView(isRoot())
                    .perform(executeCommands(mTestContext.getRootContext(), setValueCommand("color", expectedColor)));
            assertEquals((int) COLORS.get(expectedColor), getTestView().getLayout().getPaint().getColor());
        }
    }

    @Test
    public void testView_localeStringUpperCaseUmlaut(){
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(REQUIRED_PROPERTIES, CHILD_LAYOUT_PROPERTIES))
                .check(hasRootContext());

        onView(isRoot())
                .perform(executeCommands(mTestContext.getRootContext(), setValueCommand("text", "${String.toUpperCase('ößä!')}")));

        assertEquals("ÖSSÄ!", getTestView().getLayout().getText().toString());
    }

    @Test
    public void testView_localeStringLowerCaseUmlaut(){
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(REQUIRED_PROPERTIES, CHILD_LAYOUT_PROPERTIES))
                .check(hasRootContext());

        onView(isRoot())
                .perform(executeCommands(mTestContext.getRootContext(), setValueCommand("text", "${String.toLowerCase('ÄÖß!')}")));

        assertEquals("äöß!", getTestView().getLayout().getText().toString());
    }

    @Test
    public void testView_localeStringTestUTF8(){
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(REQUIRED_PROPERTIES, CHILD_LAYOUT_PROPERTIES))
                .check(hasRootContext());

        onView(isRoot())
                .perform(executeCommands(mTestContext.getRootContext(), setValueCommand("text", "${String.toLowerCase('\uD83D\uDC0B')}")));

        assertEquals("\uD83D\uDC0B", getTestView().getLayout().getText().toString());
    }

    @Test
    public void testView_localeStringTestLocale(){
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(REQUIRED_PROPERTIES, CHILD_LAYOUT_PROPERTIES))
                .check(hasRootContext());

        onView(isRoot())
                .perform(executeCommands(mTestContext.getRootContext(), setValueCommand("text", "${String.toLowerCase('TITLE', 'tr-TR')}")));

        assertEquals("tıtle", getTestView().getLayout().getText().toString());
        onView(isRoot())
                .perform(executeCommands(mTestContext.getRootContext(), setValueCommand("text", "${String.toLowerCase('TITLE', 'en-CA')}")));

        assertEquals("title", getTestView().getLayout().getText().toString());
    }

    @Test
    public void testView_localeStringUnknownLocale(){
        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflate(REQUIRED_PROPERTIES, CHILD_LAYOUT_PROPERTIES))
                .check(hasRootContext());

        onView(isRoot())
                .perform(executeCommands(mTestContext.getRootContext(), setValueCommand("text", "${String.toLowerCase('TITLE', 'test-TEST')}")));

        assertEquals("title", getTestView().getLayout().getText().toString());
    }
}
