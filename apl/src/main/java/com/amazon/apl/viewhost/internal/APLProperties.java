/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.internal;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a bag of APL properties used to configure a Viewhost.
 */
public class APLProperties {
    private static final String TAG = "APLProperties";
    private Map<APLProperty, Object> mProperties = new HashMap<APLProperty, Object>(){{
        put(APLProperty.kFluidityIncidentUpsThreshold, 1.1d);
        put(APLProperty.kFluidityIncidentUpsWindowSize, 60);
        put(APLProperty.kFluidityIncidentMinimumDurationMs, 1000);
    }};

    /**
     * Sets a value for a configuration property. This new value overrides any existing value previously set.
     *
     * @param property The {@link APLProperty} to set
     * @param value The value for this property
     * @return true if the value was set, false otherwise
     */
    public Boolean set(APLProperty property, Object value) {
        mProperties.put(property, value);
        return true;
    }

    /**
     * Sets a value for a configuration property. This new value overrides any existing value previously set.
     * Valid properties are defined in {@link APLProperty#sPropertyToEnumMap}
     *
     * @param property The property to set.
     * @param value The value for this property
     * @return true if the value was set, false otherwise
     */
    public Boolean set(String property, Object value) {
        // TODO: Modify to add RootProperty values through this setter
        APLProperty key = APLProperty.sPropertyToEnumMap.get(property);
        if (key == null) {
            Log.w(TAG, String.format("%s is not a valid APL property, skipping", property));
            return false;
        }
        return set(key, value);
    }

    /**
     * Retrieves a previously set property value
     *
     * @param property The name of the property to retrieve
     * @return the property value, null if the property is not set
     */
    public Object get(APLProperty property) {
        return mProperties.get(property);
    }

    /**
     * Retrieves a previously set property value as a Double
     *
     * @param property The name of the property to retrieve
     * @return the property value, null if the property is not set or is not a Double
     */
    public Double getDouble(APLProperty property) {
        Object val = get(property);
        if (val instanceof Double) return (Double) val;
        return null;
    }
}
