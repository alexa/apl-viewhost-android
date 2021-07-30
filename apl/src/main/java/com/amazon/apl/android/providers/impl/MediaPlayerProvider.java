/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.providers.impl;

import android.content.Context;
import androidx.annotation.NonNull;
import android.view.TextureView;

import com.amazon.apl.android.dependencies.IMediaPlayer;
import com.amazon.apl.android.dependencies.impl.MediaPlayer;
import com.amazon.apl.android.providers.AbstractMediaPlayerProvider;

/**
 * Default media player provider.
 */
public class MediaPlayerProvider extends AbstractMediaPlayerProvider<TextureView> {

    @NonNull
    @Override
    public TextureView createView(Context context) {
        return new TextureView(context);
    }

    @NonNull
    @Override
    public IMediaPlayer createPlayer(@NonNull Context context, @NonNull TextureView view) {
        return new MediaPlayer(context, view);
    }
}
