/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.audio;

import androidx.annotation.NonNull;

import com.amazon.apl.android.providers.ITtsPlayerProvider;
import com.amazon.common.BoundObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating AudioPlayers.
 */
public class AudioPlayerFactoryProxy extends BoundObject {
    private final IAudioPlayerFactory mAudioPlayerFactory;

    public AudioPlayerFactoryProxy(@NonNull IAudioPlayerFactory audioPlayerFactory) {
        final long handle = nCreate();
        bind(handle);
        mAudioPlayerFactory = audioPlayerFactory;
    }

    public ITtsPlayerProvider getAudioProvider() {
        return mAudioPlayerFactory.getTtsPlayerProvider();
    }

    /**
     * Construct an audio-only player.
     */
    @SuppressWarnings("unused")
    private AudioPlayer createPlayer(long nativeHandle) {
        return new AudioPlayer(nativeHandle, getAudioProvider());
    }

    private native long nCreate();
}
