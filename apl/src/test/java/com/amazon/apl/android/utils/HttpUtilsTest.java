/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class HttpUtilsTest {

    @Test
    public void testValidHeader() {
        // Given
        String key = "headerKey";
        String value = "headerValue";
        String[] headers = new String[]{ key + ": " + value };
        // When
        Map<String, String> result = HttpUtils.listToHeadersMap(headers);

        // Then
        assertEquals(1, result.size());
        assertTrue(result.containsKey(key));
        String resultValue = result.get(key);
        assertEquals(value, resultValue);
    }

    @Test
    public void testEmptyValueHeader() {
        // Given
        String key = "headerKey";
        String[] headers = new String[]{ key };
        // When
        Map<String, String> result = HttpUtils.listToHeadersMap(headers);

        // Then
        assertEquals(1, result.size());
        assertTrue(result.containsKey(key));
        String resultValue = result.get(key);
        assertEquals("", resultValue); // empty string
    }

    @Test
    public void testHeaderNameTrailingWhitespaceIsRemoved() {
        // Given
        String expectedKey = "headerKey";
        String key = expectedKey + "   ";
        String[] headers = new String[]{ key };
        // When
        Map<String, String> result = HttpUtils.listToHeadersMap(headers);

        // Then
        assertEquals(1, result.size());
        assertTrue(result.containsKey(expectedKey));
        assertFalse(result.containsKey(key));
    }

    @Test
    public void testHeaderMultipleColons() {
        // Given
        String key = "headerKey";
        String value = "value: value: value";
        String[] headers = new String[]{ key + ": " + value };
        // When
        Map<String, String> result = HttpUtils.listToHeadersMap(headers);

        // Then
        assertEquals(1, result.size());
        assertTrue(result.containsKey(key));
        String resultValue = result.get(key);
        assertEquals(value, resultValue);
    }

    @Test
    public void testNullHeader() {
        // Given
        String key = null;
        String[] headers = new String[]{ key };
        // When
        Map<String, String> result = HttpUtils.listToHeadersMap(headers);

        // Then
        assertEquals(0, result.size());
    }

    @Test
    public void testNullHeaderArray() {
        // Given

        // When
        Map<String, String> result = HttpUtils.listToHeadersMap(null);

        // Then
        assertEquals(0, result.size());
    }

    @Test
    public void testEmptyHeaderArray() {
        // Given

        // When
        Map<String, String> result = HttpUtils.listToHeadersMap(new String[]{});

        // Then
        assertEquals(0, result.size());
    }
}
