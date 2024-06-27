/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.sgcontent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.apl.android.media.MediaPlayer;
import com.amazon.apl.enums.VideoScale;

/**
 * Java representation of a Video node
 */
public class VideoNode extends Node {
    public VideoNode(long address) {
        super(address);
    }

    @Nullable
    public MediaPlayer getMediaPlayer() {
        return nGetMediaPlayer(mAddress);
    }

    @NonNull
    public VideoScale getVideoScale() {
        return VideoScale.valueOf(nGetVideoScale(mAddress));
    }

    private static native MediaPlayer nGetMediaPlayer(long address);
    private static native int nGetVideoScale(long address);
}
