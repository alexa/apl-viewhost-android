/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.media;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.amazon.apl.android.dependencies.IMediaPlayer;
import com.amazon.apl.enums.MediaPlayerEventType;
import com.amazon.common.BoundObject;

import java.util.List;

public abstract class MediaPlayer extends BoundObject implements IMediaPlayer.IMediaListener {
    private static final String TAG = "MediaPlayer";
    private MediaState mState = MediaState.IDLE;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    // Variables used for reviving media player on back navigation
    private int mCurrentTrackIndex;
    private int mCurrentSeekPosition;

    public MediaPlayer(long nativeHandle) {
        bind(nativeHandle);
    }

    public abstract void play();

    public abstract void pause();

    public abstract void next();

    public abstract void previous();

    public abstract void rewind();

    public abstract void seek(int offset);

    public abstract void setTrackList(List<MediaTrack> trackList);

    public abstract void setTrackIndex(int trackIndex);

    public abstract void release();

    public abstract void setAudioTrack(int index);

    public abstract void setMute(boolean mute);

    public abstract IMediaPlayer getMediaPlayer();

    public abstract void setMediaPlayer(IMediaPlayer player);

    public int getCurrentTrackIndex() {
        return mCurrentTrackIndex;
    }

    public int getCurrentSeekPosition() {
        return mCurrentSeekPosition;
    }

    @Override
    public void updateMediaState(IMediaPlayer player) {
        if (player == null) {
            return;
        }
        final MediaState state = player.getCurrentMediaState();
        final int trackIndex = player.getCurrentTrackIndex();
        final int currentSeekPosition = player.getCurrentSeekPosition();
        if (currentSeekPosition < 0) {
            Log.w(TAG, "Current seek position is " + currentSeekPosition);
        }
        if (state == MediaState.RELEASED || trackIndex < 0) {
            // Player is not initialized, just update the state here.
            mState = state;
            return;
        }
        MediaPlayerEventType event = MediaPlayerEventType.kMediaPlayerEventTimeUpdate;
        if (mState != player.getCurrentMediaState()) {
            switch (state) {
                case ERROR:
                    event = MediaPlayerEventType.kMediaPlayerEventTrackFail;
                    break;
                case END:
                    event = MediaPlayerEventType.kMediaPlayerEventEnd;
                    break;
                case READY:
                    event = MediaPlayerEventType.kMediaPlayerEventTrackReady;
                    break;
                case TRACK_UPDATE:
                    event = MediaPlayerEventType.kMediaPlayerEventTrackUpdate;
                    break;
                case PLAYING:
                    event = MediaPlayerEventType.kMediaPlayerEventPlay;
                    break;
                case PAUSED:
                    event = MediaPlayerEventType.kMediaPlayerEventPause;
                    break;
            }
            mState = state;
        }
        mCurrentTrackIndex = trackIndex;
        mCurrentSeekPosition = currentSeekPosition;
        final int trackCount = player.getTrackCount();
        final int duration = player.getDuration();
        final boolean isPaused = !player.isPlaying();
        final boolean isEnded = state == MediaState.END;
        final boolean isMuted = player.isMuted();
        final int trackState = player.getTrackState();
        final int error = player.getCurrentError();
        final MediaPlayerEventType mediaEvent = event;
        mMainHandler.post(() -> {
            nUpdateMediaState(getNativeHandle(),
                    mCurrentTrackIndex,
                    trackCount,
                    mCurrentSeekPosition,
                    duration,
                    isPaused,
                    isEnded,
                    isMuted,
                    trackState,
                    error,
                    mediaEvent.getIndex());
        });
    }

    private static native void nUpdateMediaState(long nativeHandle, int trackIndex, int trackCount,
                                                 int currentTime, int duration, boolean paused,
                                                 boolean ended, boolean muted, int trackState,
                                                 int errorCode, int eventType);
}
