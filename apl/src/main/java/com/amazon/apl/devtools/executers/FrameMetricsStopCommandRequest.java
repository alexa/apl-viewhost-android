package com.amazon.apl.devtools.executers;

import android.util.Log;

import com.amazon.apl.devtools.controllers.impl.DTConnection;
import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.Session;
import com.amazon.apl.devtools.models.ViewTypeTarget;
import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.models.frameMetrics.FrameMetricsStopCommandRequestModel;
import com.amazon.apl.devtools.models.frameMetrics.FrameMetricsStopCommandResponse;
import com.amazon.apl.devtools.util.CommandRequestValidator;

import com.amazon.apl.devtools.util.IDTCallback;
import org.json.JSONException;
import org.json.JSONObject;

public class FrameMetricsStopCommandRequest extends FrameMetricsStopCommandRequestModel {
    private static final String TAG = FrameMetricsStopCommandRequest.class.getSimpleName();

    public FrameMetricsStopCommandRequest(CommandRequestValidator commandRequestValidator, JSONObject obj, DTConnection connection)
            throws JSONException, DTException {
        super(obj, commandRequestValidator, connection);
    }

    @Override
    public void execute(IDTCallback<FrameMetricsStopCommandResponse> callback) {
        Log.i(TAG, "Executing " + CommandMethod.FRAMEMETRICS_STOP+ " command");
        getViewTypeTarget().stopFrameMetricsRecording(getId(), (frameStatsList, requestStatus) -> {
            callback.execute(new FrameMetricsStopCommandResponse(getId(), getSessionId(), frameStatsList), requestStatus);
        });
    }
}
