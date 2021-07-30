/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import com.amazon.apl.android.primitive.Dimension;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.android.text.TextMeasuringInput;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;

public class TextMeasurementInputTest extends ViewhostRobolectricTest {
    @Mock
    Dimension mDimensionMock;

    @Test
    public void testComparePaints_WithSameValues_Equals() {
        final TextMeasuringInput input1 = new TextMeasuringInput.Builder().
                textColor(0).classicFonts(true).fontFamily("myFamily").fontLanguage("ja-JP").italic(false).fontSize(10).
                fontWeight(500).density(160F).shadowOffsetX(1).shadowOffsetY(2).shadowColor(128).shadowBlur(2).build();

        final TextMeasuringInput input2 = new TextMeasuringInput.Builder().
                textColor(0).classicFonts(true).fontFamily("myFamily").fontLanguage("ja-JP").italic(false).fontSize(10).
                fontWeight(500).density(160F).shadowOffsetX(1).shadowOffsetY(2).shadowColor(128).shadowBlur(2).build();

        Assert.assertTrue(input1.paintsEqual(input2));
    }

    @Test
    public void testComparePaints_WithDifferentValues_NotEquals() {
        final TextMeasuringInput input1 = new TextMeasuringInput.Builder().
                textColor(0).classicFonts(true).fontFamily("myFamily").fontLanguage("ja-JP").italic(false).fontSize(10).
                fontWeight(500).density(238F).shadowOffsetX(1).shadowOffsetY(2).shadowColor(128).shadowBlur(2).build();

        final TextMeasuringInput input2 = new TextMeasuringInput.Builder().
                textColor(0).classicFonts(true).fontFamily("myFamily").fontLanguage("ja-JP").italic(false).fontSize(10).
                fontWeight(500).density(160F).shadowOffsetX(1).shadowOffsetY(2).shadowColor(128).shadowBlur(2).build();

        Assert.assertFalse(input1.paintsEqual(input2));
    }
}
