/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.network;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazon.apl.devtools.enums.EventMethod;

import org.json.JSONObject;
import org.junit.Test;

public class NetworkLoadingFailedEventUnitTest {

    @Test
    public void testNetworkLoadingFailedEvent() {
        String sessionId = "123";
        int requestId = 1;
        double timestamp = 123456789.0;

        NetworkLoadingFailedEvent networkLoadingFailedEvent = new NetworkLoadingFailedEvent(sessionId, requestId,
                timestamp);

        assertEquals(EventMethod.NETWORK_LOADING_FAILED, networkLoadingFailedEvent.getMethod());
        assertEquals(sessionId, networkLoadingFailedEvent.getSessionId());

        NetworkLoadingFailedEvent.Params params = networkLoadingFailedEvent.getParams();

        assertEquals(requestId, params.getRequestId());
        assertEquals(timestamp, params.getTimestamp(), 0.0);
    }

    @Test
    public void testNetworkLoadingFailedEventToJSONObject() throws Exception {
        String sessionId = "123";
        int requestId = 1;
        double timestamp = 123456789.0;

        NetworkLoadingFailedEvent networkLoadingFailedEvent = new NetworkLoadingFailedEvent(sessionId, requestId,
                timestamp);

        assertEquals(EventMethod.NETWORK_LOADING_FAILED, networkLoadingFailedEvent.getMethod());
        assertEquals(sessionId, networkLoadingFailedEvent.getSessionId());

        JSONObject superJsonObject = mock(JSONObject.class);
        when(superJsonObject.put(anyString(), any())).thenReturn(superJsonObject);
        JSONObject jsonObject = networkLoadingFailedEvent.toJSONObject();

        assertEquals(EventMethod.NETWORK_LOADING_FAILED.toString(), jsonObject.getString("method"));
        assertEquals(sessionId, jsonObject.getString("sessionId"));

        JSONObject paramsJson = jsonObject.getJSONObject("params");

        assertEquals(requestId, paramsJson.getInt("requestId"));
        assertEquals(timestamp, paramsJson.getDouble("timestamp"), 0.0);
    }
}
