/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.font;

import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FontUtilTest extends ViewhostRobolectricTest {

    @Test
    public void testIsArabicFontKey_WithArabicFontKey() {
        FontKey key = FontKey.build("bookerly", 100).italic(true).language("ar-SA").build();
        boolean isArabic = FontUtil.isArabicFontKey(key);
        assertEquals(isArabic, true);
    }

    @Test
    public void testIsArabicFontKey_WithEmptyLangKey() {
        FontKey key = FontKey.build("bookerly", 100).italic(true).language("").build();
        boolean isArabic = FontUtil.isArabicFontKey(key);
        assertEquals(isArabic, false);
    }

    @Test
    public void testIsArabicFontKey_WithNonArabicFontKey() {
        FontKey key = FontKey.build("bookerly", 100).italic(true).language("ja-JP").build();
        boolean isArabic = FontUtil.isArabicFontKey(key);
        assertEquals(isArabic, false);
    }

}
