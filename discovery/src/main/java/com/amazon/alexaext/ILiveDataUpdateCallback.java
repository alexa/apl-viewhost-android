/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.alexaext;

/**
 * Callback for update messages.
 */
public interface ILiveDataUpdateCallback {
    boolean invokeLiveDataUpdate(String uri, String liveDataUpdate);
}
