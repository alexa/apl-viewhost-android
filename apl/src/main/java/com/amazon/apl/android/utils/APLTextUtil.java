package com.amazon.apl.android.utils;

public class APLTextUtil {
    public static int[] calculateCharacterOffsetByRange(String text, int rangeStart, int rangeEnd) {
        // rangeStart and rangeEnd are byte ranges, so we need to convert them to character ranges.

        // Since the ranges are inclusive, we need to add 1 to turn it into a size.
        int rangeSize = (rangeEnd - rangeStart) + 1;

        // Count the number of utf8 codepoints between 0 and rangeStart.
        int characterRangeStart = nCountCharactersInRange(text, 0, rangeStart);

        // Similar to the above, we also need to count the number of utf8 codepoints for rangeEnd. However, we know the number of code points
        // for rangeStart, so we just need to count the ones between rangeStart and rangeEnd.
        int characterRangeCount = nCountCharactersInRange(text, rangeStart, rangeSize);

        // Subtract 1 to turn it back into an inclusive range.
        int characterRangeEnd = (characterRangeStart + characterRangeCount - 1);

        return new int[] {characterRangeStart, characterRangeEnd};
    }

    private static native int nCountCharactersInRange(String text, int index, int count);
}
