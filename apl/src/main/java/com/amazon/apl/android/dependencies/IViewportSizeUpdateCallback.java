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
     * @param width  updated width
     * @param height updated height
     */
    void onViewportSizeUpdate(int width, int height);
}
