package com.amazon.apl.devtools.models.frameMetrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class FrameMetricsCommandUnitTest {

    @Test
    public void testFrameMetricsRecord() {
        try {
            FrameMetricsRecordCommandResponse response = new FrameMetricsRecordCommandResponse(100, "Session100");
            JSONObject object = response.toJSONObject();
            assertTrue(object.has("id"));
            assertEquals(object.getInt("id"), 100);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testFrameMetricsStop() {
        try {
            List<JSONObject> framestatsList = new ArrayList<>();
            JSONObject framestatsObject = new JSONObject();
            framestatsObject.put("begin", 100);
            framestatsObject.put("end", 200);
            framestatsList.add(framestatsObject);
            FrameMetricsStopCommandResponse response = new FrameMetricsStopCommandResponse(1, "sessionId", framestatsList);
            JSONObject object = response.toJSONObject();
            assertTrue(object.has("id"));
            assertEquals(object.getInt("id"), 1);
            assertTrue(object.has("result"));
            JSONObject result = object.getJSONObject("result");
            assertTrue(result.has("value"));
            JSONObject value = result.getJSONObject("value");
            assertTrue(value.has("framestats"));
            JSONArray framestatsArray = value.getJSONArray("framestats");

            JSONObject expectedFramestatsObject = new JSONObject();
            expectedFramestatsObject.put("begin", 100);
            expectedFramestatsObject.put("end", 200);
            JSONArray expectedFramestatsArray = new JSONArray();
            expectedFramestatsArray.put(expectedFramestatsObject);

            // Compare the expected JSONArray with the actual JSONArray
            assertEquals(expectedFramestatsArray.toString(), framestatsArray.toString());

        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
