/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.scaling;

/**
 * No-Op implementation of {@link IMetricsTransform}.
 */
public class NoOpMetricsTransform implements IMetricsTransform {
    private static NoOpMetricsTransform INSTANCE;

    private NoOpMetricsTransform() {}

    public static NoOpMetricsTransform getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NoOpMetricsTransform();
        }
        return INSTANCE;
    }

    @Override
    public float toCore(float value) {
        return value;
    }

    @Override
    public float toViewhost(float value) {
        return value;
    }

    @Override
    public int getScaledViewhostWidth() {
        return 0;
    }

    @Override
    public int getScaledViewhostHeight() {
        return 0;
    }

    @Override
    public int getViewportOffsetX() {
        return 0;
    }

    @Override
    public int getViewportOffsetY() {
        return 0;
    }

    @Override
    public ViewportMetrics getUnscaledMetrics() {
        return null;
    }
}
