/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.executers;

import android.util.Log;

import com.amazon.apl.devtools.controllers.impl.DTConnection;
import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.Session;
import com.amazon.apl.devtools.models.common.NetworkDomainResponse;
import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.models.network.NetworkDomainCommandRequestModel;
import com.amazon.apl.devtools.util.CommandRequestValidator;

import org.json.JSONException;
import org.json.JSONObject;

public class NetworkDisableCommandRequest extends NetworkDomainCommandRequestModel {
    private static final String TAG = NetworkDisableCommandRequest.class.getSimpleName();

    public NetworkDisableCommandRequest(CommandRequestValidator commandRequestValidator,
                                       JSONObject obj,
                                       DTConnection connection) throws JSONException, DTException {
        super(CommandMethod.NETWORK_DISABLE, obj, commandRequestValidator, connection);
    }

    @Override
    public NetworkDomainResponse execute() {
        Log.i(TAG, "Executing " + CommandMethod.NETWORK_DISABLE + " command");
        getSession().setNetworkEnabled(false);
        return new NetworkDomainResponse(getId(), getSessionId());
    }
}
