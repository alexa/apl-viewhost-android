/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies;

/**
 * Defines API for listening to screen lock updates.
 */
public interface IScreenLockListener {

    /**
     * Called when the screen lock is updated.
     *
     * @param status true if the screen lock is on.
     */
    void onScreenLockChange(boolean status);

}