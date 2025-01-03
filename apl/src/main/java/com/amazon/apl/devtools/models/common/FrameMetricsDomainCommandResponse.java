package com.amazon.apl.devtools.models.common;

import org.json.JSONException;
import org.json.JSONObject;

public class FrameMetricsDomainCommandResponse extends Response {
    private final String mSessionId;

    public FrameMetricsDomainCommandResponse(int id, String sessionId) {
        super(id);
        mSessionId = sessionId;
    }

    public String getSessionId() {
        return mSessionId;
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        return super.toJSONObject().put("sessionId", getSessionId());
    }
}
