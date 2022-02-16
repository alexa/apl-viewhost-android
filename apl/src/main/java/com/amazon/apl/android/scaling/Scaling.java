/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.scaling;

import com.amazon.common.BoundObject;
import com.amazon.apl.enums.ViewportMode;
import com.google.auto.value.AutoValue;

import java.util.Collection;
import java.util.Collections;

/**
 * Scaling options
 */
public class Scaling extends BoundObject {
    /**
     * The viewport specifications that describe the viewport ranges an APL document has been tested int
     */
    @AutoValue
    public abstract static class ViewportSpecification {
        public abstract int getMinWidth();
        public abstract int getMaxWidth();
        public abstract int getMinHeight();
        public abstract int getMaxHeight();
        public abstract boolean isRound();
        public abstract ViewportMode getMode();

        public static Builder builder() {
            return new AutoValue_Scaling_ViewportSpecification.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder minWidth(int minWidth);
            public abstract Builder maxWidth(int maxWidth);
            public abstract Builder minHeight(int minHeight);
            public abstract Builder maxHeight(int maxHeight);
            public abstract Builder round(boolean isRound);
            public abstract Builder mode(ViewportMode mode);

            public abstract ViewportSpecification build();
        }
    }

    /**
     * Constructor for no scaling.
     */
    public Scaling() { }

    /**
     * @param biasConstant Higher values will scale less but use screen real estate less efficiently
     * @param specifications Tested configurations
     */
    public Scaling(double biasConstant, Collection<ViewportSpecification> specifications) {
        this(biasConstant, specifications, Collections.EMPTY_LIST);
    }

    public boolean isScalingRequested() {
        return isBound();
    }

    /**
     * @param biasConstant Higher values will scale less but use screen real estate less efficiently
     * @param specifications Tested configurations
     * @param allowModes Additional viewport modes to be considered in selection if original viewport mode does not match
     */
    public Scaling(double biasConstant, Collection<ViewportSpecification> specifications, Collection<ViewportMode> allowModes) {
        long handle = nScalingCreate(biasConstant);
        for(ViewportSpecification spec : specifications) {
            nAddViewportSpecification(handle, spec.getMinWidth(), spec.getMaxWidth(), spec.getMinHeight(), spec.getMaxHeight(), spec.isRound(), spec.getMode().getIndex());
        }
        for (ViewportMode mode : allowModes) {
            nAddAllowMode(handle, mode.getIndex());
        }
        bind(handle);
    }

    /**
     * Removes the MetricsTransform's chosen ViewportSpecification from the vector of ViewportSpecifications.
     * @param metricsTransform  the MetricsTransform with a chosen viewport spec.
     * @return                  true if a specification was removed,
     *                          false otherwise.
     */
    public boolean removeChosenViewportSpecification(MetricsTransform metricsTransform) {
        if (!isBound()) {
            return false;
        }
        return nRemoveChosenViewportSpecification(getNativeHandle(), metricsTransform.getNativeHandle());
    }

    private static native long nScalingCreate(double biasConstant);
    private static native void nAddViewportSpecification(long handle, int wmin, int wmax, int hmin, int hmax, boolean isRound, int mode);
    private static native boolean nRemoveChosenViewportSpecification(long nativeHandle, long metricsTransformHandle);
    private static native void nAddAllowMode(long handle, int mode);
}
