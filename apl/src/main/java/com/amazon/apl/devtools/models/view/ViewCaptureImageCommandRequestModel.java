/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.view;

import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.common.ViewDomainCommandRequest;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class ViewCaptureImageCommandRequestModel
        extends ViewDomainCommandRequest<ViewCaptureImageCommandResponse> {

    protected ViewCaptureImageCommandRequestModel(JSONObject obj) throws JSONException {
        super(CommandMethod.VIEW_CAPTURE_IMAGE, obj);
    }
}
