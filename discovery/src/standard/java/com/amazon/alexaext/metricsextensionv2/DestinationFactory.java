/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.alexaext.metricsextensionv2;

import android.util.Log;

import com.amazon.common.BoundObject;

import org.json.JSONObject;
import org.json.JSONException;

/**
 * The DestinationFactory Interface corresponding to {@link com.amazon.alexaext.metricsExtensionV2.DestinationFactoryInterface}.
 * Allows runtime to provide implementation for creating destination required for publishing metrics
 */
public abstract class DestinationFactory extends BoundObject {
    private static final String TAG = "DestinationFactory";
    private Destination mDestination;

    public DestinationFactory() {
        init();
    }

    protected void init() {
        final long handle = nCreate();
        bind(handle);
    }

    abstract protected Destination createDestination(JSONObject settings);

    @SuppressWarnings("unused")
    protected long createDestinationInternal(String settings) {
        try {
            JSONObject jsonSettings = new JSONObject(settings);
            mDestination = createDestination(jsonSettings);
            return mDestination.getNativeHandle();
        } catch (JSONException e) {
            Log.e(TAG, "Failed to create destination", e);
        }
        return -1;
    }

    private native long nCreate();
}
