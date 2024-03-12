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

public class NetworkLoadingFinishedEventUnitTest {

    @Test
    public void testNetworkLoadingFinishedEvent() {
        String sessionId = "123";
        int requestId = 1;
        double timestamp = 123456789.0;
        int encodedDataLength = 1;

        NetworkLoadingFinishedEvent networkLoadingFinishedEvent = new NetworkLoadingFinishedEvent(sessionId, requestId,
                timestamp, encodedDataLength);

        assertEquals(EventMethod.NETWORK_LOADING_FINISHED, networkLoadingFinishedEvent.getMethod());
        assertEquals(sessionId, networkLoadingFinishedEvent.getSessionId());

        NetworkLoadingFinishedEvent.Params params = networkLoadingFinishedEvent.getParams();

        assertEquals(requestId, params.getRequestId());
        assertEquals(timestamp, params.getTimestamp(), 0.0);
        assertEquals(encodedDataLength, params.getEncodedDataLength());
    }

    @Test
    public void testNetworkLoadingFinishedEventToJSONObject() throws Exception {
        String sessionId = "123";
        int requestId = 1;
        double timestamp = 123456789.0;
        int encodedDataLength = 1;

        NetworkLoadingFinishedEvent networkLoadingFinishedEvent = new NetworkLoadingFinishedEvent(sessionId, requestId,
                timestamp, encodedDataLength);

        assertEquals(EventMethod.NETWORK_LOADING_FINISHED, networkLoadingFinishedEvent.getMethod());
        assertEquals(sessionId, networkLoadingFinishedEvent.getSessionId());

        JSONObject superJsonObject = mock(JSONObject.class);
        when(superJsonObject.put(anyString(), any())).thenReturn(superJsonObject);
        JSONObject jsonObject = networkLoadingFinishedEvent.toJSONObject();

        assertEquals(EventMethod.NETWORK_LOADING_FINISHED.toString(), jsonObject.getString("method"));
        assertEquals(sessionId, jsonObject.getString("sessionId"));

        JSONObject paramsJson = jsonObject.getJSONObject("params");

        assertEquals(requestId, paramsJson.getInt("requestId"));
        assertEquals(timestamp, paramsJson.getDouble("timestamp"), 0.0);
        assertEquals(encodedDataLength, paramsJson.getInt("encodedDataLength"));
    }
}
