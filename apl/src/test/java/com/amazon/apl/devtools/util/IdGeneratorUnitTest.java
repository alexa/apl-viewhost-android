/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.util;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class IdGeneratorUnitTest {
    private IdGenerator mIdGenerator;

    @Before
    public void setup() {
        mIdGenerator = new IdGenerator();
    }

    @Test
    public void generateId_withoutPrefix_returnsCorrectly() {
        int expected = 100;
        int actual = mIdGenerator.generateId();
        assertEquals(expected, actual);
    }

    @Test
    public void generateId_withPrefix_returnsCorrectly() {
        String expected = "session100";
        String actual = mIdGenerator.generateId("session");
        assertEquals(expected, actual);
    }
}
