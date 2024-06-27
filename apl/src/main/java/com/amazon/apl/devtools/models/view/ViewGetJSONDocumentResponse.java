package com.amazon.apl.devtools.models.view;

import android.util.Log;

import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.common.ViewDomainCommandResponse;

import org.json.JSONException;
import org.json.JSONObject;

public class ViewGetJSONDocumentResponse extends ViewDomainCommandResponse {
    private static final String TAG ="ViewGetJSONDocumentResponse";
    private JSONObject mJSONResult;

    public ViewGetJSONDocumentResponse(int id, String sessionId, String result) {
        super(id, sessionId);
        try {
            mJSONResult = new JSONObject().put("value", new JSONObject(result));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        return super.toJSONObject().put("result", mJSONResult);
    }
}
