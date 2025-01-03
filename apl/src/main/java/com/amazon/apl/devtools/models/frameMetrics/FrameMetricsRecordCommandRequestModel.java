package com.amazon.apl.devtools.models.frameMetrics;

import com.amazon.apl.devtools.controllers.impl.DTConnection;
import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.common.SessionCommandRequest;
import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.util.CommandRequestValidator;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class FrameMetricsRecordCommandRequestModel extends SessionCommandRequest<FrameMetricsRecordCommandResponse> {
    protected FrameMetricsRecordCommandRequestModel(JSONObject obj, CommandRequestValidator commandRequestValidator, DTConnection connection) throws JSONException, DTException {
        super(CommandMethod.FRAMEMETRICS_RECORD, obj, commandRequestValidator, connection);
    }
}
