/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies.impl;

import androidx.annotation.NonNull;

import com.amazon.apl.android.dependencies.IMediaPlayer;
import com.amazon.apl.android.primitive.MediaSources;
import com.amazon.apl.enums.AudioTrack;
import com.amazon.apl.enums.VideoScale;

public class NoOpMediaPlayer implements IMediaPlayer {
    @Override
    public int getCurrentSeekPosition() {
        return 0;
    }

    @Override
    public void setAudioTrack(@NonNull AudioTrack audioTrack) {

    }

    @Override
    public void setVideoScale(@NonNull VideoScale scale) {

    }

    @Override
    public void setMediaSources(@NonNull MediaSources mediaSources) {

    }

    @Override
    public void addMediaStateListener(@NonNull IMediaListener listener) {

    }

    @Override
    public void removeMediaStateListener(@NonNull IMediaListener listener) {

    }

    @Override
    public void play() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void next() {

    }

    @Override
    public void previous() {

    }

    @Override
    public void setTrack(int trackIndex) {

    }

    @Override
    public void seek(int msec) {

    }

    @Override
    public void rewind() {

    }

    @Override
    public boolean isPlaying() {
        return false;
    }

    @Override
    public int getDuration() {
        return 0;
    }

    @Override
    public int getCurrentTrackIndex() {
        return 0;
    }

    @Override
    public int getTrackCount() {
        return 0;
    }

    @NonNull
    @Override
    public IMediaListener.MediaState getCurrentMediaState() {
        return null;
    }

    @Override
    public void release() {

    }
}
