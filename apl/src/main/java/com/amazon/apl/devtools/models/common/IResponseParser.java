/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.common;

import org.json.JSONException;
import org.json.JSONObject;

public interface IResponseParser {
    JSONObject toJSONObject() throws JSONException;

    default String toJSONString() throws JSONException {
        return toJSONObject().toString();
    }
}
