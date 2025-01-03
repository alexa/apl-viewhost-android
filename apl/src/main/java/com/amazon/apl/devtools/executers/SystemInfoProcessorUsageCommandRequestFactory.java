/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.executers;

import android.os.Build;
import android.util.Log;

import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.models.processor.SystemInfoGetApplicationProcessorUsageCommandRequestModel;
import com.amazon.apl.devtools.models.processor.SystemInfoGetEnvironmentProcessorUsageCommandRequestModel;

import org.json.JSONException;
import org.json.JSONObject;

public class SystemInfoProcessorUsageCommandRequestFactory {
    private static final String TAG = SystemInfoProcessorUsageCommandRequestFactory.class.getSimpleName();

    public static SystemInfoGetEnvironmentProcessorUsageCommandRequestModel getEnvironmentProcessorUsageCommandRequest(JSONObject obj) throws JSONException, DTException {
        Log.i(TAG, "Build version: " + Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT < 26) {
            return new SystemInfoGetEnvironmentProcessorUsageCommandRequestApi25(obj);
        }
        return new SystemInfoGetEnvironmentProcessorUsageCommandRequest(obj);
    }

    public static SystemInfoGetApplicationProcessorUsageCommandRequestModel getApplicationProcessorUsageCommandRequest(JSONObject obj) throws JSONException {
        Log.i(TAG, "Build version: " + Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT < 26) {
            return new SystemInfoGetApplicationProcessorUsageCommandRequestApi25(obj);
        }
        return new SystemInfoGetApplicationProcessorUsageCommandRequest(obj);
    }
}
