package com.amazon.apl.devtools.models.frameMetrics;

import com.amazon.apl.devtools.controllers.impl.DTConnection;
import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.common.SessionCommandRequest;
import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.util.CommandRequestValidator;

import org.json.JSONException;
import org.json.JSONObject;

public class FrameMetricsStopCommandRequestModel extends SessionCommandRequest<FrameMetricsStopCommandResponse> {
    protected FrameMetricsStopCommandRequestModel(JSONObject obj, CommandRequestValidator commandRequestValidator, DTConnection connection) throws JSONException, DTException {
        super(CommandMethod.FRAMEMETRICS_STOP, obj, commandRequestValidator, connection);
    }
}
