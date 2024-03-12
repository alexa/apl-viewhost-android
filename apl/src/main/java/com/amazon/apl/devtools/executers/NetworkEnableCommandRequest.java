/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.executers;

import android.util.Log;

import com.amazon.apl.devtools.controllers.DTConnection;
import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.Session;
import com.amazon.apl.devtools.models.common.NetworkDomainResponse;
import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.models.network.NetworkDomainCommandRequestModel;
import com.amazon.apl.devtools.util.CommandRequestValidator;
import com.amazon.apl.devtools.util.IDTCallback;
import com.amazon.apl.devtools.util.RequestStatus;

import org.json.JSONException;
import org.json.JSONObject;

public class NetworkEnableCommandRequest extends NetworkDomainCommandRequestModel implements ICommandValidator {
    private static final String TAG = NetworkEnableCommandRequest.class.getSimpleName();
    private final CommandRequestValidator mCommandRequestValidator;
    private final DTConnection mConnection;
    private Session mSession;

    public NetworkEnableCommandRequest(CommandRequestValidator commandRequestValidator,
                                       JSONObject obj,
                                       DTConnection connection) throws JSONException, DTException {
        super(CommandMethod.NETWORK_ENABLE, obj);
        mCommandRequestValidator = commandRequestValidator;
        mConnection = connection;
        validate();
    }

    @Override
    public NetworkDomainResponse execute() {
        Log.i(TAG, "Executing " + CommandMethod.NETWORK_ENABLE + " command");
        mSession.setNetworkEnabled(true);
        return new NetworkDomainResponse(getId(), getSessionId());
    }

    @Override
    public void validate() throws DTException {
        Log.i(TAG, "Validating " + CommandMethod.NETWORK_ENABLE + " command");
        mCommandRequestValidator.validateBeforeGettingSession(getId(), getSessionId(), mConnection);
        mSession = mConnection.getSession(getSessionId());
    }
}
