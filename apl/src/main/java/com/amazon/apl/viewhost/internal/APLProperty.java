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
    kFluidityIncidentMinimumDurationMs;

    public static final ImmutableBiMap<String, APLProperty> sPropertyToEnumMap = ImmutableBiMap.of(
            "fluidityIncident.upsThreshold", APLProperty.kFluidityIncidentUpsThreshold,
            "fluidityIncident.upsWindowSize", APLProperty.kFluidityIncidentUpsWindowSize,
            "fluidityIncident.minimumDurationMs", APLProperty.kFluidityIncidentMinimumDurationMs
    );
}
