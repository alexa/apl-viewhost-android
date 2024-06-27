/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.media;

import androidx.annotation.NonNull;

import com.amazon.apl.android.providers.AbstractMediaPlayerProvider;
import com.amazon.common.BoundObject;

public class MediaPlayerFactoryProxy extends BoundObject {
    private final IMediaPlayerFactory mFactory;

    public MediaPlayerFactoryProxy(final @NonNull IMediaPlayerFactory factory) {
        final long handle = nCreate();
        bind(handle);
        mFactory = factory;
    }

    public AbstractMediaPlayerProvider getMediaPlayerProvider() {
        return mFactory.getMediaPlayerProvider();
    }

    /**
     * Construct a media player, called from JNI
     */
    @SuppressWarnings("unused")
    private MediaPlayer createPlayer(long nativeHandler) {
        return new MediaPlayer(nativeHandler);
    }

    private native long nCreate();
}
