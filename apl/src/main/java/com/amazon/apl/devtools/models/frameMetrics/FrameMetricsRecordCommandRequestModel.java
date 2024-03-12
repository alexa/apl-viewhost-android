package com.amazon.apl.devtools.models.frameMetrics;

import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.common.FrameMetricsDomainCommandRequest;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class FrameMetricsRecordCommandRequestModel extends FrameMetricsDomainCommandRequest<FrameMetricsRecordCommandResponse> {
    protected FrameMetricsRecordCommandRequestModel(JSONObject obj) throws JSONException {
        super(CommandMethod.FRAMEMETRICS_RECORD, obj);
    }
}
