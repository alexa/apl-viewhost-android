/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.dependencies;

/**
 * Callback for any viewport size updates.
 */
public interface IViewportSizeUpdateCallback {

    /**
     * To be called when view port is updated, on auto size trigger.
     * @param height updated height
     * @param width  updated width
     */
    void onViewportSizeUpdate(int height, int width);
}
