/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.text;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LineSpanTest {
    @Test
    public void testEquals_start_not_equal_returns_false() {
        LineSpan lineSpan1 = new LineSpan(0, 0, 0);
        LineSpan lineSpan2 = new LineSpan(1, 0, 0);
        assertFalse(lineSpan1.equals(lineSpan2));
    }

    @Test
    public void testEquals_end_not_equal_returns_false() {
        LineSpan lineSpan1 = new LineSpan(0, 0, 0);
        LineSpan lineSpan2 = new LineSpan(0, 1, 0);
        assertFalse(lineSpan1.equals(lineSpan2));
    }

    @Test
    public void testEquals_color_not_equal_returns_false() {
        LineSpan lineSpan1 = new LineSpan(0, 0, 0);
        LineSpan lineSpan2 = new LineSpan(0, 0, 1);
        assertFalse(lineSpan1.equals(lineSpan2));
    }

    @Test
    public void testEquals_all_equal_returns_true() {
        LineSpan lineSpan1 = new LineSpan(0, 0, 0);
        LineSpan lineSpan2 = new LineSpan(0, 0, 0);
        assertTrue(lineSpan1.equals(lineSpan2));
    }
}
