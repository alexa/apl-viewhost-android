/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.alexaext.metricsextensionv2;

import com.amazon.common.BoundObject;

import java.util.HashMap;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The Metric class corresponding to {@link com.amazon.alexaext.metricsExtensionV2.MetricData} to represent information about a metric
 */
@AllArgsConstructor
@Getter
public class Metric {
    private final String name;
    private final double value;
    private final HashMap<String, String> dimensions;
}
