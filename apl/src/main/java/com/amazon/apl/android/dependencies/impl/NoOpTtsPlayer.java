/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies.impl;

import com.amazon.apl.android.dependencies.ITtsPlayer;

import java.io.InputStream;
import java.net.URL;

/**
 * Implementation of {@link ITtsPlayer} that just logs method calls.
 */
public class NoOpTtsPlayer implements ITtsPlayer {
    private static final String TAG = "NoOpTtsPlayer";

    @Override
    public void prepare(String source, InputStream stream) {
    }

    @Override
    public void prepare(String source, URL url) {
    }

    @Override
    public void play() {
    }

    @Override
    public void stop() {
    }

    @Override
    public void release() {
    }

    @Override
    public String getSource() {
        return "";
    }

    @Override
    public void setWordMarkListener(ISpeechMarksListener listener) {
    }

    @Override
    public void setStateChangeListener(IStateChangeListener listener) {
    }
}
