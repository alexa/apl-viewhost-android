/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.media;

import com.amazon.apl.android.providers.AbstractMediaPlayerProvider;

/**
 * Interface for getting the {@link AbstractMediaPlayerProvider}
 * Runtimes need to initialize this using the
 * {@link com.amazon.apl.android.RootConfig#mediaPlayerFactory(RuntimeMediaPlayerFactory)} API
 */
public interface IMediaPlayerFactory {
    /**
     * Get the MediaPlayerProvider
     * @return - the media player provider
     */
    AbstractMediaPlayerProvider getMediaPlayerProvider();
}
