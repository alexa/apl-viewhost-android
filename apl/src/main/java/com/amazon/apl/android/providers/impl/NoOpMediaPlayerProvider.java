/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.providers.impl;

import android.content.Context;
import androidx.annotation.NonNull;
import android.view.View;

import com.amazon.apl.android.dependencies.IMediaPlayer;
import com.amazon.apl.android.dependencies.impl.NoOpMediaPlayer;
import com.amazon.apl.android.providers.AbstractMediaPlayerProvider;

public class NoOpMediaPlayerProvider extends AbstractMediaPlayerProvider<View> {
    private static NoOpMediaPlayerProvider INSTANCE;

    private NoOpMediaPlayerProvider() { }

    public static NoOpMediaPlayerProvider getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NoOpMediaPlayerProvider();
        }
        return INSTANCE;
    }

    @Override
    public IMediaPlayer getNewPlayer(Context context, View view) {
        return new NoOpMediaPlayer();
    }

    @Override
    public boolean hasPlayingMediaPlayer() {
        return false;
    }

    @Override
    public View createView(Context context) {
        return new View(context);
    }

    @Override
    public IMediaPlayer createPlayer(Context context, View view) {
        return new NoOpMediaPlayer();
    }

    @Override
    public void onDocumentFinish() {
    }

    @Override
    public void updateMediaState(@NonNull IMediaPlayer player) {
    }
}
