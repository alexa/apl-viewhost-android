package com.amazon.apl.devtools.models.frameMetrics;

import com.amazon.apl.devtools.models.common.FrameMetricsDomainCommandResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class FrameMetricsStopCommandResponse extends FrameMetricsDomainCommandResponse {
    private static final String TAG = FrameMetricsStopCommandResponse.class.getSimpleName();
    private List<JSONObject> mFramestatsList;

    public FrameMetricsStopCommandResponse(int id, String sessionId, List<JSONObject> framestatsList) {
        super(id, sessionId);
        mFramestatsList = framestatsList;
    }


    @Override
    public JSONObject toJSONObject() throws JSONException {
        JSONObject result = new JSONObject();
        JSONObject value = new JSONObject();
        JSONArray framestatsArray = new JSONArray();

        for (JSONObject framestatsObject : mFramestatsList) {
            JSONObject framestatsItem = new JSONObject();
            framestatsItem.put("begin", framestatsObject.getLong("begin"));
            framestatsItem.put("end", framestatsObject.getLong("end"));
            framestatsArray.put(framestatsItem);
        }
        value.put("framestats", framestatsArray);
        result.put("value", value);
        return super.toJSONObject().put("result", result);
    }


}
