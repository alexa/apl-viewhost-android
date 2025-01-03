/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.internal;

import com.google.common.collect.ImmutableBiMap;

/**
 * Represents a configuration property for a Viewhost
 */
public enum APLProperty {
    // The maximum tolerance for UPS calculation. A fluidity incident has occurred if the UPS value goes above this value.
    kFluidityIncidentUpsThreshold,
    // The number of last consecutive UPS values to use for the fluidity incident calculation
    kFluidityIncidentUpsWindowSize,
    // The minimum duration for which UPS needs to be above upsThreshold before a fluidity incident is reported
    kFluidityIncidentMinimumDurationMs,
    // The refresh rate for the fluidity counter
    kFluidityRefreshRate,
    // The boolean that controls if hardware acceleration should be turned on as per the runtimes configuration
    kIsRuntimeHardwareAccelerationEnabled,
    // The setting for viewhost to use for inflating the top document asynchronously
    kPerformanceInflateOnMainThread;

    public static final ImmutableBiMap<String, APLProperty> sPropertyToEnumMap = ImmutableBiMap.<String, APLProperty>builder()
            .put("fluidityIncident.upsThreshold", APLProperty.kFluidityIncidentUpsThreshold)
            .put("fluidityIncident.upsWindowSize", APLProperty.kFluidityIncidentUpsWindowSize)
            .put("fluidityIncident.minimumDurationMs", APLProperty.kFluidityIncidentMinimumDurationMs)
            .put("fluidityIncident.refreshRate", APLProperty.kFluidityRefreshRate)
            .put("isRuntimeHardwareAccelerationEnabled", APLProperty.kIsRuntimeHardwareAccelerationEnabled)
            .put("inflateOnMainThread", APLProperty.kPerformanceInflateOnMainThread)
            .build();
}
