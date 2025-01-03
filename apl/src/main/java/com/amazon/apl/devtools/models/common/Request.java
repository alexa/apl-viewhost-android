/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.common;

import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.executers.ICommandExecutor;
import com.amazon.apl.devtools.executers.ICommandValidator;
import com.amazon.apl.devtools.models.error.DTException;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class Request<TResponse extends Response> implements ICommandExecutor<TResponse>, ICommandValidator {
    private final CommandMethod mMethod;
    private final int mId;

    protected Request(CommandMethod method, JSONObject obj) throws JSONException {
        mMethod = method;
        mId = obj.getInt("id");
    }

    public CommandMethod getMethod() {
        return mMethod;
    }

    public int getId() {
        return mId;
    }

    @Override
    public void validate() throws DTException {}
}
