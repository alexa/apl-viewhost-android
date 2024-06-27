package com.amazon.apl.devtools.executers;

import android.util.Log;

import com.amazon.apl.devtools.models.view.ViewGetJSONDocumentRequestModel;
import com.amazon.apl.devtools.models.view.ViewGetJSONDocumentResponse;
import com.amazon.apl.devtools.controllers.DTConnection;
import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.Session;
import com.amazon.apl.devtools.models.ViewTypeTarget;
import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.util.CommandRequestValidator;
import com.amazon.apl.devtools.util.IDTCallback;

import org.json.JSONException;
import org.json.JSONObject;

public class ViewGetJSONDocumentRequest extends ViewGetJSONDocumentRequestModel implements ICommandValidator {

    private static final String TAG = "ViewGetJSONDocumentRequest";
    private final DTConnection mConnection;
    private final CommandRequestValidator mCommandRequestValidator;
    private final CommandMethod mCommandMethod;
    private Session<ViewTypeTarget> mSession;
    private ViewTypeTarget mViewTypeTarget;

    public ViewGetJSONDocumentRequest(CommandRequestValidator commandRequestValidator,
                                      JSONObject obj,
                                      DTConnection connection,
                                      CommandMethod method) throws JSONException, DTException {
        super(obj);
        mCommandRequestValidator = commandRequestValidator;
        mConnection = connection;
        mCommandMethod = method;
        validate();
    }

    @Override
    public void validate() throws DTException {
        Log.i(TAG, "Validating " + CommandMethod.VIEW_SET_DOCUMENT + " command");
        mCommandRequestValidator.validateBeforeGettingSession(getId(), getSessionId(), mConnection);
        mSession = mConnection.getSession(getSessionId());
        mViewTypeTarget = mSession.getTarget();
    }

    @Override
    public void execute(IDTCallback<ViewGetJSONDocumentResponse> callback) {
        switch(mCommandMethod) {
            case DOCUMENT_GET_SCENE_GRAPH:
                mViewTypeTarget.requestScenegraph((result, requestStatus) -> callback.execute(new ViewGetJSONDocumentResponse(getId(), getSessionId(), result), requestStatus));
            case DOCUMENT_GET_VISUAL_CONTEXT:
                mViewTypeTarget.requestVisualContext((result, requestStatus) -> callback.execute(new ViewGetJSONDocumentResponse(getId(), getSessionId(), result), requestStatus));
            case DOCUMENT_GET_DOM:
                mViewTypeTarget.requestDOM((result, requestStatus) -> callback.execute(new ViewGetJSONDocumentResponse(getId(), getSessionId(), result), requestStatus));
            default:
                mViewTypeTarget.requestScenegraph((result, requestStatus) -> callback.execute(new ViewGetJSONDocumentResponse(getId(), getSessionId(), result), requestStatus));
        }
    }
}
