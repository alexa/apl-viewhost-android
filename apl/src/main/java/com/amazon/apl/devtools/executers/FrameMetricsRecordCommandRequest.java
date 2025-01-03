package com.amazon.apl.devtools.executers;

import android.util.Log;

import com.amazon.apl.devtools.controllers.impl.DTConnection;
import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.Session;
import com.amazon.apl.devtools.models.ViewTypeTarget;
import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.models.frameMetrics.FrameMetricsRecordCommandRequestModel;
import com.amazon.apl.devtools.models.frameMetrics.FrameMetricsRecordCommandResponse;
import com.amazon.apl.devtools.util.CommandRequestValidator;

import com.amazon.apl.devtools.util.IDTCallback;
import org.json.JSONException;
import org.json.JSONObject;

public class FrameMetricsRecordCommandRequest extends FrameMetricsRecordCommandRequestModel {
    private static final String TAG = FrameMetricsRecordCommandRequest.class.getSimpleName();

    public FrameMetricsRecordCommandRequest(CommandRequestValidator commandRequestValidator, JSONObject obj, DTConnection connection)
            throws JSONException, DTException {
        super(obj, commandRequestValidator, connection);
    }

    @Override
    public void execute(IDTCallback<FrameMetricsRecordCommandResponse> callback) {
        Log.i(TAG, "Executing " + CommandMethod.FRAMEMETRICS_RECORD+ " command");
        getViewTypeTarget().startFrameMetricsRecording(getId(), (result, requestStatus) ->
            callback.execute(new FrameMetricsRecordCommandResponse(getId(), getSessionId()), requestStatus));
    }
}
