/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.media;

import static com.amazon.apl.android.dependencies.IMediaPlayer.IMediaListener.MediaState.PLAYING;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.amazon.apl.android.BuildConfig;
import com.amazon.apl.android.dependencies.IMediaPlayer;
import com.amazon.apl.android.primitive.MediaSources;
import com.amazon.apl.android.utils.HttpUtils;
import com.amazon.apl.enums.AudioTrack;
import com.amazon.apl.enums.MediaPlayerEventType;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MediaPlayer implements IMediaPlayer.IMediaListener {
    private static final String TAG = "MediaPlayer";
    private final long mAddress;
    private MediaState mState = MediaState.IDLE;
    private IMediaPlayer mPlayer;
    private final Queue<Runnable> mQueuedTasks = new LinkedList<>();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    // Variables used for reviving media player on back navigation
    private int mCurrentTrackIndex;
    private int mCurrentSeekPosition;
    private MediaSources mMediaSources = MediaSources.create();
    private AudioTrack mAudioTrack = AudioTrack.kAudioTrackForeground;
    private boolean mMuted;

    public MediaPlayer(long nativeHandle) {
        mAddress = nativeHandle;
    }


    public void play() {
        runOrDeferFunction("play", () -> {
            if (mPlayer != null) {
                mPlayer.play();
            }
        });
    }

    public void pause() {
        runOrDeferFunction("pause", () -> {
            if (mPlayer != null) {
                mPlayer.pause();
            }
        });
    }

    public void next() {
        runOrDeferFunction("next", () -> {
            if (mPlayer != null) {
                mPlayer.next();
            }
        });
    }

    public void previous() {
        runOrDeferFunction("previous", () -> {
            if (mPlayer != null) {
                mPlayer.previous();
            }
        });
    }

    public void rewind() {
        runOrDeferFunction("rewind", () -> {
            if (mPlayer != null) {
                mPlayer.rewind();
            }
        });
    }

    public void seek(int offset) {
        runOrDeferFunction("seek", () -> {
            if (mPlayer != null) {
                mPlayer.seek(mPlayer.getCurrentSeekPosition() + offset);
            }
        });
    }

    public void seekTo(int offset) {
        runOrDeferFunction("seekTo", () -> {
            if (mPlayer != null) {
                mPlayer.seekTo(offset);
            }
        });
    }

    public void setTrackList(List<MediaTrack> trackList) {
        MediaSources sources = MediaSources.create();
        for (MediaTrack mediaTrack : trackList) {
            MediaSources.MediaSource source = MediaSources.MediaSource.builder()
                    .url(mediaTrack.getUrl())
                    .duration(mediaTrack.getDuration())
                    .offset(mediaTrack.getOffset())
                    .repeatCount(mediaTrack.getRepeatCount())
                    .headers(HttpUtils.listToHeadersMap(mediaTrack.getHeaders()))
                    .textTracks(Arrays.asList(mediaTrack.getTextTracks()))
                    .build();
            sources.add(source);
        }
        mMediaSources = sources;
        runOrDeferFunction("setTrackList", () -> {
            if (mPlayer != null) {
                mPlayer.setMediaSources(sources);
                if (mPlayer.getCurrentMediaState() == PLAYING) {
                    mPlayer.setTrack(0);
                }
            }
        });
    }

    public void setTrackIndex(int trackIndex) {
        runOrDeferFunction("setTrackIndex", () -> {
            if (mPlayer != null) {
                mPlayer.setTrack(trackIndex);
            }
        });
    }

    public void release() {
        Log.e(TAG, "Release is called");
        runOrDeferFunction("release", () -> {
            if (mPlayer != null) {
                mPlayer.stop();
                mPlayer.release();
                mPlayer = null;
            }
        });
    }

    public void setAudioTrack(int index) {
        mAudioTrack = AudioTrack.valueOf(index);
        runOrDeferFunction("setAudioTrack", () -> {
            if (mPlayer != null) {
                mPlayer.setAudioTrack(AudioTrack.valueOf(index));
            }
        });
    }

    public void setMute(boolean mute) {
        mMuted = mute;
        runOrDeferFunction("setMute", () -> {
            if (mPlayer != null) {
                if (mute) {
                    mPlayer.mute();
                } else {
                    mPlayer.unmute();
                }
            }
        });
    }

    public IMediaPlayer getMediaPlayer() {
        return mPlayer;
    }

    public void setMediaPlayer(IMediaPlayer player) {
        mPlayer = player;
        player.addMediaStateListener(this);
        runQueuedOperations();
    }

    private void runOrDeferFunction(String functionName, Runnable task) {
        // So queue all the operations until MediaPlayer is available.
        if (mPlayer != null) {
            if (BuildConfig.DEBUG_LOGGING) Log.d(TAG, "Calling " + functionName);
            task.run();
        } else {
            if (BuildConfig.DEBUG_LOGGING) Log.d(TAG, "Queuing " + functionName);
            mQueuedTasks.add(task);
        }
    }

    private void runQueuedOperations() {
        Runnable task = mQueuedTasks.poll();
        while (task != null) {
            task.run();
            task = mQueuedTasks.poll();
        }
    }

    public int getCurrentTrackIndex() {
        return mCurrentTrackIndex;
    }

    public int getCurrentSeekPosition() {
        return mCurrentSeekPosition;
    }

    @NonNull
    public MediaSources getMediaSources() {
        return mMediaSources;
    }

    public boolean isCurrentlyPlaying() {
        return mState == PLAYING;
    }

    @NonNull
    public AudioTrack getCurrentAudioTrack() {
        return mAudioTrack;
    }

    public boolean isCurrentlyMuted() {
        return mMuted;
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
            nUpdateMediaState(mAddress,
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
