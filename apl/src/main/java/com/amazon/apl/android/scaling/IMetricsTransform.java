/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.scaling;

public interface IMetricsTransform {
    /**
     * Converts px units into dp units
     * @param value px unit
     * @return dp unit
     */
    float toCore(float value);

    /**
     * Conversion for ints.
     */
    default int toCore(int value) { return Math.round(toCore((float) value)); }

    /**
     * Converts dp units into px units
     * @param value px unit
     * @return dp unit
     */
    float toViewhost(float value);

    /**
     * Conversion for ints.
     */
    default int toViewhost(int value) { return Math.round(toViewhost((float) value)); }

    /**
     * @return the width of the viewhost viewport in px
     */
    int getScaledViewhostWidth();

    /**
     * @return the height of the viewhost viewport in px
     */
    int getScaledViewhostHeight();

    /**
     * @return X difference between viewport and scaled layout
     */
    int getViewportOffsetX();

    /**
     * @return Y difference between viewport and scaled layout
     */
    int getViewportOffsetY();
}
