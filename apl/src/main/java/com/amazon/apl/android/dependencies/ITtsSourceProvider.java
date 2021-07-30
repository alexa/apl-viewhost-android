/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies;

import java.io.InputStream;
import java.net.URL;

/**
 * Defines the TTS source.
 */
public interface ITtsSourceProvider {

    /**
     * Called when the TTS source is a stream.
     * @param stream the TTS stream.
     */
    void onSource(InputStream stream);

    /**
     * Called when the TTS source is a {@link URL}.
     * @param url the TTS url
     */
    void onSource(URL url);
}
