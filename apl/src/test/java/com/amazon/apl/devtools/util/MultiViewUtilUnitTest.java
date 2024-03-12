/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.util;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class MultiViewUtilUnitTest {
    private MultiViewUtil mMultiViewUtil;

    @Before
    public void setup() {
        mMultiViewUtil = new MultiViewUtil();
    }

    @Test
    public void computeNumberOfColumns_forNegativeNumberOfViews_returnsCorrectly() {
        int expected = 1;
        int actual = mMultiViewUtil.computeNumberOfColumns(-1);
        assertEquals(expected, actual);
    }

    @Test
    public void computeNumberOfColumns_forZeroNumberOfViews_returnsCorrectly() {
        int expected = 1;
        int actual = mMultiViewUtil.computeNumberOfColumns(0);
        assertEquals(expected, actual);
    }

    @Test
    public void computeNumberOfColumns_forOneView_returnsCorrectly() {
        int expected = 1;
        int actual = mMultiViewUtil.computeNumberOfColumns(1);
        assertEquals(expected, actual);
    }

    @Test
    public void computeNumberOfColumns_forMultipleNumberOfViews_returnsCorrectly() {
        int expected = 3;
        int actual = mMultiViewUtil.computeNumberOfColumns(5);
        assertEquals(expected, actual);
    }

    @Test
    public void computeNumberOfRows_withPerfectDivisors_returnsCorrectly() {
        int expected = 2;
        int actual = mMultiViewUtil.computeNumberOfRows(6, 3);
        assertEquals(expected, actual);
    }

    @Test
    public void computeNumberOfRows_withImperfectDivisors_roundsUp() {
        int expected = 3;
        int actual = mMultiViewUtil.computeNumberOfRows(7, 3);
        assertEquals(expected, actual);
    }

    @Test
    public void computeNumberOfRows_forNegativeNumberOfViews_returnsCorrectly() {
        int expected = 1;
        int actual = mMultiViewUtil.computeNumberOfRows(-1, 1);
        assertEquals(expected, actual);
    }

    @Test
    public void computeNumberOfRows_forZeroNumberOfViews_returnsCorrectly() {
        int expected = 1;
        int actual = mMultiViewUtil.computeNumberOfRows(0, 1);
        assertEquals(expected, actual);
    }

    @Test
    public void computeNumberOfRows_forOneView_returnsCorrectly() {
        int expected = 1;
        int actual = mMultiViewUtil.computeNumberOfRows(1, 1);
        assertEquals(expected, actual);
    }

    @Test
    public void computeNumberOfRows_forZeroNumberOfColumns_returnsCorrectly() {
        int expected = 1;
        int actual = mMultiViewUtil.computeNumberOfRows(2, 0);
        assertEquals(expected, actual);
    }
}
