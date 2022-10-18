/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.media;

import androidx.annotation.NonNull;

import com.amazon.apl.android.providers.AbstractMediaPlayerProvider;

public class RuntimeMediaPlayerFactory implements IMediaPlayerFactory {
    private final AbstractMediaPlayerProvider mMediaPlayerProvider;

    public RuntimeMediaPlayerFactory(@NonNull AbstractMediaPlayerProvider mediaPlayerProvider) {
        mMediaPlayerProvider = mediaPlayerProvider;
    }

    @Override
    public AbstractMediaPlayerProvider getMediaPlayerProvider() {
        return mMediaPlayerProvider;
    }
}
