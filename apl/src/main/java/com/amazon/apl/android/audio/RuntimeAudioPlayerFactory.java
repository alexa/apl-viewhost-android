/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.audio;

import com.amazon.apl.android.providers.ITtsPlayerProvider;

public class RuntimeAudioPlayerFactory implements IAudioPlayerFactory {

    private final ITtsPlayerProvider mAudioPlayer;

    public RuntimeAudioPlayerFactory(ITtsPlayerProvider ttsPlayerProvider) {
        mAudioPlayer = ttsPlayerProvider;
    }
    @Override
    public ITtsPlayerProvider getTtsPlayerProvider() {
        return mAudioPlayer;
    }
}
