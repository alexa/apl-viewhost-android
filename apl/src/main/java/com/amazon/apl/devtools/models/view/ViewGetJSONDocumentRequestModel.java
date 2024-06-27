package com.amazon.apl.devtools.models.view;

import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.common.ViewDomainCommandRequest;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class ViewGetJSONDocumentRequestModel extends ViewDomainCommandRequest<ViewGetJSONDocumentResponse> {
    protected ViewGetJSONDocumentRequestModel(JSONObject obj) throws JSONException {
        super(CommandMethod.DOCUMENT_GET_SCENE_GRAPH, obj);
    }
}
