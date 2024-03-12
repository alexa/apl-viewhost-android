/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.view;

import android.util.Log;

import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.common.ViewDomainCommandResponse;

import org.json.JSONException;
import org.json.JSONObject;

public final class ViewSetDocumentCommandResponse extends ViewDomainCommandResponse {
    private static final String TAG = ViewSetDocumentCommandResponse.class.getSimpleName();

    public ViewSetDocumentCommandResponse(int id, String sessionId) {
        super(id, sessionId);
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        Log.i(TAG, "Serializing " + CommandMethod.VIEW_SET_DOCUMENT + " response object");
        return super.toJSONObject();
    }
}
