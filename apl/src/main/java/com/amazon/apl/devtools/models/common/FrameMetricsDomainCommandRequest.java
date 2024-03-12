package com.amazon.apl.devtools.models.common;

import com.amazon.apl.devtools.enums.CommandMethod;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class FrameMetricsDomainCommandRequest<TResponse extends Response> extends Request<TResponse> {
    private final String mSessionId;

    protected FrameMetricsDomainCommandRequest(CommandMethod method, JSONObject obj) throws JSONException {
        super(method, obj);
        mSessionId = obj.getString("sessionId");
    }

    public String getSessionId() {
        return mSessionId;
    }

}
