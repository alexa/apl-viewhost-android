package com.amazon.apl.devtools.executers;

import android.util.Log;

import com.amazon.apl.devtools.controllers.DTConnection;
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

public class FrameMetricsRecordCommandRequest extends FrameMetricsRecordCommandRequestModel implements ICommandValidator {
    private static final String TAG = FrameMetricsRecordCommandRequest.class.getSimpleName();
    private final CommandRequestValidator mCommandRequestValidator;
    private final DTConnection mConnection;
    private ViewTypeTarget mViewTypeTarget;

    public FrameMetricsRecordCommandRequest(CommandRequestValidator commandRequestValidator, JSONObject obj, DTConnection connection)
            throws JSONException, DTException {
        super(obj);
        mCommandRequestValidator = commandRequestValidator;
        mConnection = connection;
        validate();
    }

    @Override
    public void execute(IDTCallback<FrameMetricsRecordCommandResponse> callback) {
        Log.i(TAG, "Executing " + CommandMethod.FRAMEMETRICS_RECORD+ " command");
        mViewTypeTarget.startFrameMetricsRecording(getId(), (result, requestStatus) ->
            callback.execute(new FrameMetricsRecordCommandResponse(getId(), getSessionId()), requestStatus));
    }

    @Override
    public void validate() throws DTException {
        Log.i(TAG, "Validating " + CommandMethod.FRAMEMETRICS_RECORD + " command");
        mCommandRequestValidator.validateBeforeGettingSession(getId(), getSessionId(), mConnection);
        Session session = mConnection.getSession(getSessionId());
        mViewTypeTarget = (ViewTypeTarget) session.getTarget();
    }
}
