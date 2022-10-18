/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.audio;

import com.amazon.apl.android.providers.ITtsPlayerProvider;

/**
 * Interface for getting the TtsPlayerProvider
 */
public interface IAudioPlayerFactory {
    /**
     * Get the TtsPlayerProvider
     * @return - the TtsPlayerProvider
     */
    ITtsPlayerProvider getTtsPlayerProvider();
}
