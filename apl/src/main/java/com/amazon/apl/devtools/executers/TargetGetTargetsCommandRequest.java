/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.executers;

import android.util.Log;

import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.target.TargetGetTargetsCommandRequestModel;
import com.amazon.apl.devtools.models.target.TargetGetTargetsCommandResponse;
import com.amazon.apl.devtools.util.TargetCatalog;

import org.json.JSONException;
import org.json.JSONObject;

public final class TargetGetTargetsCommandRequest extends TargetGetTargetsCommandRequestModel {
    private static final String TAG = TargetGetTargetsCommandRequest.class.getSimpleName();
    private final TargetCatalog mTargetCatalog;

    public TargetGetTargetsCommandRequest(TargetCatalog targetCatalog, JSONObject obj)
            throws JSONException {
        super(obj);
        mTargetCatalog = targetCatalog;
    }

    @Override
    public TargetGetTargetsCommandResponse execute() {
        Log.i(TAG, "Executing " + CommandMethod.TARGET_GET_TARGETS + " command");
        return new TargetGetTargetsCommandResponse(getId(), mTargetCatalog.getAll());
    }
}
