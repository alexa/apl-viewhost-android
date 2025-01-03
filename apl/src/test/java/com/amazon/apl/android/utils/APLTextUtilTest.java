package com.amazon.apl.android.utils;

import static org.junit.Assert.assertEquals;

import android.text.Layout;

import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.android.scenegraph.text.APLTextLayout;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


public class APLTextUtilTest extends ViewhostRobolectricTest {
    @Mock
    private Layout mockLayout;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testMultiByteCharacters() {
        CharSequence text = "Abcdef役場産業課および";

        // Existing behaviour for ASCII
        assertForCalculateCharacterOffsetByRange(text, 0, 0, 0, 0);
        assertForCalculateCharacterOffsetByRange(text, 1, 1, 1, 1);
        assertForCalculateCharacterOffsetByRange(text, 0, 5, 0, 5);
        assertForCalculateCharacterOffsetByRange(text, 1, 5, 1, 5);

        // 3 byte characters.
        assertForCalculateCharacterOffsetByRange(text,6, 6, 6, 8);
        assertForCalculateCharacterOffsetByRange(text,7, 7,9, 11);
        assertForCalculateCharacterOffsetByRange(text,0, 9, 0, 17);
    }

    private void assertForCalculateCharacterOffsetByRange(CharSequence text,
                                                          int characterRangeStart, int characterRangeEnd,
                                                          int rangeStart, int rangeEnd) {
        int[] characterRange = APLTextUtil.calculateCharacterOffsetByRange(text.toString(), rangeStart, rangeEnd);
        assertEquals(characterRangeStart, characterRange[0]);
        assertEquals(characterRangeEnd, characterRange[1]);
    }
}
