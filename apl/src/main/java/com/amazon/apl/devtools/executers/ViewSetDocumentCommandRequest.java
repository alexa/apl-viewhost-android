/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.executers;

import android.util.Log;

import com.amazon.apl.android.LiveArray;
import com.amazon.apl.android.LiveMap;
import com.amazon.apl.devtools.controllers.impl.DTConnection;
import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.Session;
import com.amazon.apl.devtools.models.ViewTypeTarget;
import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.models.view.ViewSetDocumentCommandRequestModel;
import com.amazon.apl.devtools.models.view.ViewSetDocumentCommandResponse;
import com.amazon.apl.devtools.util.CommandRequestValidator;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public final class ViewSetDocumentCommandRequest
        extends ViewSetDocumentCommandRequestModel {
    private static final String TAG = ViewSetDocumentCommandRequest.class.getSimpleName();

    public ViewSetDocumentCommandRequest(CommandRequestValidator commandRequestValidator,
                                         JSONObject obj,
                                         DTConnection connection)
            throws JSONException, DTException {
        super(obj, commandRequestValidator, connection);
    }

    @Override
    public ViewSetDocumentCommandResponse execute() {
        Log.i(TAG, "Executing " + CommandMethod.VIEW_SET_DOCUMENT + " command");
        final ViewTypeTarget target = getViewTypeTarget();
        target.post(() -> {
            Params params = getParams();
            if (params != null) {
                if (params.getConfiguration() != null && params.getConfiguration().mLiveArrays != null) {
                    for (Map.Entry<String, List<Object>> entry : params.getConfiguration().mLiveArrays.entrySet()) {
                        target.addLiveData(entry.getKey(), LiveArray.create(entry.getValue()));
                    }
                }
                if (params.getConfiguration() != null && params.getConfiguration().mLiveMaps != null) {
                    for (Map.Entry<String, Map<String, Object>> entry : params.getConfiguration().mLiveMaps.entrySet()) {
                        target.addLiveData(entry.getKey(), LiveMap.create(entry.getValue()));
                    }
                }
                target.setDocument(params.getDocument(), params.getData());
            }
        });
        return new ViewSetDocumentCommandResponse(getId(), getSessionId());
    }
}
