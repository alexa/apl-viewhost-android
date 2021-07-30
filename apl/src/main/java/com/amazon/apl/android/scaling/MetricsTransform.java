/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.scaling;

import com.amazon.apl.android.BoundObject;
import com.amazon.apl.enums.ViewportMode;

/**
 * Class responsible for scaling between core dp units and viewhost px units.
 *
 * Takes into consideration dpi as well as scaling.
 */
public class MetricsTransform extends BoundObject implements IMetricsTransform {
    private final ViewportMetrics mMetrics;

    private MetricsTransform(ViewportMetrics metrics) {
        mMetrics = metrics;
        long nativeHandle = nCreate(metrics.width(), metrics.height(), metrics.dpi(),
                metrics.shape().getIndex(), mMetrics.theme(),
                metrics.mode().getIndex(), metrics.scaling().getNativeHandle());
        bind(nativeHandle);
    }

    /**
     * Create an instance of a MetricsTransform with the supplied {@link ViewportMetrics}.
     * @param metrics   the viewport information
     * @return          a new MetricsTransform
     */
    public static MetricsTransform create(ViewportMetrics metrics) {
        return new MetricsTransform(metrics);
    }

    /**
     * @return the viewport metrics after scaling
     */
    public ViewportMetrics getScaledMetrics() {
        return ViewportMetrics.builder()
                .width(Math.round(nPixelWidth(getNativeHandle())))
                .height(Math.round(nPixelHeight(getNativeHandle())))
                .dpi(mMetrics.dpi())
                .shape(mMetrics.shape())
                .theme(mMetrics.theme())
                .mode(ViewportMode.valueOf(nViewportMode(getNativeHandle())))
                .scaling(mMetrics.scaling())
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float toCore(float value) {
        return nToCore(getNativeHandle(), value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float toViewhost(float value) {
        return nToViewhost(getNativeHandle(), value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getScaledViewhostWidth() { return Math.round(nViewhostWidth(getNativeHandle())); }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getScaledViewhostHeight() { return Math.round(nViewhostHeight(getNativeHandle())); }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getViewportOffsetX() {
        return (mMetrics.width() - getScaledViewhostWidth()) / 2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getViewportOffsetY() {
        return (mMetrics.height() - getScaledViewhostHeight()) / 2;
    }

    private static native long nCreate(int width, int height, int dpi, int shape, String theme, int mode, long scalingHandle);
    private static native float nToViewhost(long nativeHandle, float value);
    private static native float nToCore(long nativeHandle, float value);
    private static native float nViewhostWidth(long nativeHandle);
    private static native float nViewhostHeight(long nativeHandle);
    private static native float nPixelWidth(long nativeHandle);
    private static native float nPixelHeight(long nativeHandle);
    private static native int nViewportMode(long nativeHandle);
}