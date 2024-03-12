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

public class NetworkRequestWillBeSentEventUnitTest {

    @Test
    public void testNetworkRequestWillBeSentEvent() {
        String sessionId = "123";
        int requestId = 1;
        double timestamp = 123456789.0;
        String documentURL = "testUrl";
        String type = "testType";

        NetworkRequestWillBeSentEvent networkRequestWillBeSentEvent = new NetworkRequestWillBeSentEvent(sessionId, requestId,
                timestamp, documentURL, type);

        assertEquals(EventMethod.NETWORK_REQUEST_WILL_BE_SENT, networkRequestWillBeSentEvent.getMethod());
        assertEquals(sessionId, networkRequestWillBeSentEvent.getSessionId());

        NetworkRequestWillBeSentEvent.Params params = networkRequestWillBeSentEvent.getParams();

        assertEquals(requestId, params.getRequestId());
        assertEquals(timestamp, params.getTimestamp(), 0.0);
        assertEquals(documentURL, params.getDocumentURL());
        assertEquals(type, params.getType());
    }

    @Test
    public void testNetworkRequestWillBeSentEventToJSONObject() throws Exception {
        String sessionId = "123";
        int requestId = 1;
        double timestamp = 123456789.0;
        String documentURL = "testUrl";
        String type = "testType";

        NetworkRequestWillBeSentEvent networkRequestWillBeSentEvent = new NetworkRequestWillBeSentEvent(sessionId, requestId,
                timestamp, documentURL, type);

        assertEquals(EventMethod.NETWORK_REQUEST_WILL_BE_SENT, networkRequestWillBeSentEvent.getMethod());
        assertEquals(sessionId, networkRequestWillBeSentEvent.getSessionId());

        JSONObject superJsonObject = mock(JSONObject.class);
        when(superJsonObject.put(anyString(), any())).thenReturn(superJsonObject);
        JSONObject jsonObject = networkRequestWillBeSentEvent.toJSONObject();

        assertEquals(EventMethod.NETWORK_REQUEST_WILL_BE_SENT.toString(), jsonObject.getString("method"));
        assertEquals(sessionId, jsonObject.getString("sessionId"));

        JSONObject paramsJson = jsonObject.getJSONObject("params");

        assertEquals(requestId, paramsJson.getInt("requestId"));
        assertEquals(timestamp, paramsJson.getDouble("timestamp"), 0.0);
        assertEquals(documentURL, paramsJson.getString("documentURL"));
        assertEquals(type, paramsJson.getString("type"));
    }
}
