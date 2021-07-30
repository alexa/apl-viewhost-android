/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import com.amazon.apl.android.font.TypefaceResolver;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;

public class StaticLayoutBuilderTest extends ViewhostRobolectricTest {

    @Mock
    private Typeface mockTypeface;

    private static final String[] LINES = {
            "Hello APL123!\uD83D\uDE1C\uD83D\uDC47\uD83C\uDFC6❤️❤️️\uD83E\uDD26",
            "\uD83E\uDD80\uD83C\uDF59second?^&*><#$@ \uD83C\uDFBB❤️❤️❤️️line\uD83C\uDFC1\uD83E\uDD26",
            "\uD83C\uDFC1third \uD83C\uDF99line"
    };

    // TODO: Remove this once the unit tests are added.
    private static String OPTIONAL_PROPERTIES = "";
    @Before
    public void doBefore() {
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

    @Test
    public void test_layout_creation() throws StaticLayoutBuilder.LayoutBuilderException {
        // TODO: Will fast follow this test with different values
        StaticLayout textLayout = StaticLayoutBuilder.create().
                text("<br/>" + LINES[1] + "<br/>" + LINES[2]).
                textPaint(getTextPaint(160)).
                lineSpacing(0.0f).
                innerWidth(0).
                alignment(Layout.Alignment.ALIGN_NORMAL).
                limitLines(true).
                maxLines(3).
                ellipsizedWidth(0).
                aplVersionCode(APLVersionCodes.APL_1_4)
                .build();
        assertEquals(1, textLayout.getLineCount());
    }

    private TextPaint getTextPaint(final float density) {
        // Create the text paint
        final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        final int shadowOffsetX = 0;
        final int shadowOffsetY = 0;
        final int shadowColor = 0;
        float shadowBlur = 0;
        // Avoid shadow layer being removed when shadowBlur is 0
        // https://developer.android.com/reference/android/graphics/Paint.html#setShadowLayer(float,%20float,%20float,%20int)
        if (shadowBlur == 0 && (shadowOffsetX != 0 || shadowOffsetY != 0)) {
            // If this is less than 1 then shadow wont render at all on fos5 devices
            shadowBlur = 1.0f;
        }
        textPaint.setColor(Color.BLUE);
        textPaint.setTextSize(40);
        textPaint.setTypeface(mockTypeface);
        // letterSpacing needs to be calculated as EM
        textPaint.density = density;
        textPaint.setTextScaleX(1.0f);
        textPaint.setShadowLayer(shadowBlur, shadowOffsetX, shadowOffsetY, shadowColor);

        return textPaint;
    }
}
