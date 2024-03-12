/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.memory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import android.os.Debug;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class MemoryGetMemoryCommandResponseTest {
    @Test
    public void testCollectionOfProcessMemory() {
        try {
            Debug.MemoryInfo memoryInfo = mock(Debug.MemoryInfo.class);
            MemoryGetMemoryCommandResponse response = new MemoryGetMemoryCommandResponse(100, memoryInfo);

            JSONObject object = response.toJSONObject();
            assertTrue(object.has("id"));
            assertEquals(object.getInt("id"), 100);

            assertTrue(object.has("result"));
            JSONObject result = object.getJSONObject("result");

            assertTrue(result.has("total"));
            assertTrue(result.has("stats"));

            JSONArray stats = result.getJSONArray("stats");
            assertEquals(stats.length(), 9);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
