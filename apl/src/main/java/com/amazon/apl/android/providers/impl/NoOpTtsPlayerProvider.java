/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.providers.impl;

import androidx.annotation.NonNull;
import android.util.Log;

import com.amazon.apl.android.dependencies.ITtsPlayer;
import com.amazon.apl.android.dependencies.ITtsSourceProvider;
import com.amazon.apl.android.dependencies.impl.NoOpTtsPlayer;
import com.amazon.apl.android.providers.ITtsPlayerProvider;


/**
 * Default {@link ITtsPlayerProvider} that provides a no-op {@link ITtsPlayer}.
 */
public class NoOpTtsPlayerProvider implements ITtsPlayerProvider {
    private static final String TAG = "NoOpTtsPlayerProvider";
    /**
     * Player which does nothing.
     */
    @NonNull
    @Override
    public ITtsPlayer getPlayer() throws IllegalStateException {
        return new NoOpTtsPlayer();
    }

    @Override
    public void prepare(String source, @NonNull ITtsSourceProvider ttsSourceProvider) {
    }

    /**
     * The document is no longer valid for display.
     */
    @Override
    public void onDocumentFinish() {
    }
}
