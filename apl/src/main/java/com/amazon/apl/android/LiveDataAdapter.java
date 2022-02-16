/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import com.amazon.alexaext.ILiveDataUpdateCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Adapter for locally created LiveData.
 */
public abstract class LiveDataAdapter {
    protected final ILiveDataUpdateCallback mUpdateCallback;
    protected final String mName;
    protected final String mUri;

    protected LiveDataAdapter(ILiveDataUpdateCallback updateCallback, String uri, String name) {
        mUpdateCallback = updateCallback;
        mUri = uri;
        mName = name;
    }

    /**
     * Send operation as LiveData update to the proxy.
     * @param operations operations to perform.
     * @return true if succeeded, false otherwise.
     */
    protected boolean sendUpdate(JSONArray operations) {
        try {
            JSONObject update = new JSONObject();
            update.put("version", "1.0");
            update.put("method", "LiveDataUpdate");
            update.put("target", mUri);
            update.put("name", mName);
            update.put("operations", operations);
            return mUpdateCallback.invokeLiveDataUpdate(mUri, update.toString());
        } catch (JSONException ex) {}
        return false;
    }

    /**
     * @return Generated LiveData definition.
     */
    public JSONObject getLiveDataDefinition() {
        JSONObject result = new JSONObject();
        try {
            result.put("type", mName + "Type");
            result.put("name", mName);
        } catch (JSONException ex) {}

        return result;
    }

    /**
     * @return Generated data type definition.
     */
    public JSONObject getTypeDefinition() {
        return new JSONObject();
    }
}
