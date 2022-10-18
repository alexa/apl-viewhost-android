/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.providers;

import androidx.annotation.NonNull;

import com.amazon.apl.android.IDocumentLifecycleListener;
import com.amazon.apl.android.dependencies.ITtsPlayer;
import com.amazon.apl.android.dependencies.ITtsSourceProvider;

import java.io.IOException;


/**
 * Provides an instance of the tts player.
 */
public interface ITtsPlayerProvider extends IDocumentLifecycleListener {


    /**
     * @return The TTS Player.
     */
    @NonNull
    ITtsPlayer getPlayer();

    /**
     * Prepares a source for the TTS Player.
     * @param source the path of the tts mp3.
     * @param ttsSourceProvider the TTS source.
     */
    default void prepare(@NonNull String source, @NonNull ITtsSourceProvider ttsSourceProvider) throws IOException {}

    /**
     * Prepares a source for the TTS Player.
     * @param source the path of the tts mp3
     */
    default void prepare(@NonNull String source) {}
}
