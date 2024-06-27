/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.alexaext.metricsextensionv2;

import com.amazon.common.BoundObject;

import java.util.List;

/**
 * The Destination Interface corresponding to {@link com.amazon.alexaext.metricsExtensionV2.DestinationInterface}.
 * Allows runtime to provide implementation for publishing metrics
 */
public abstract class Destination extends BoundObject {
    public Destination() {
        init();
    }

    protected void init() {
        final long handle = nCreate();
        bind(handle);
    }

    abstract protected void publish(Metric metric);

    abstract protected void publish(List<Metric> metrics);

    @SuppressWarnings("unused")
    protected void publishInternal(Metric metric) {
        publish(metric);
    }

    @SuppressWarnings("unused")
    protected void publishInternal(List<Metric> metrics) {
        publish(metrics);
    }

    private native long nCreate();
}
