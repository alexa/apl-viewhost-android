/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.audio;

import android.os.Handler;
import android.os.Looper;

import com.amazon.apl.android.dependencies.ITtsPlayer;
import com.amazon.apl.android.media.MediaTrack;
import com.amazon.apl.android.providers.ITtsPlayerProvider;
import com.amazon.apl.enums.AudioPlayerEventType;
import com.amazon.apl.enums.SpeechMarkType;
import com.amazon.apl.enums.TrackState;
import com.amazon.common.BoundObject;

import java.util.concurrent.atomic.AtomicBoolean;

public class AudioPlayer extends BoundObject implements ITtsPlayer.IStateChangeListener, ITtsPlayer.ISpeechMarksListener {
    private static final long UPDATE_TIME_INTERVAL_MS = 100;

    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final ITtsPlayerProvider mTtsPlayerProvider;
    private final AtomicBoolean mIsReleased = new AtomicBoolean();
    private TrackState mLastPublishedTrackState = TrackState.kTrackNotReady;
    private long mStartTime;
    private int mCurrentOffset;
    private boolean mTimeUpdatesStarted = false;

    public AudioPlayer(long nativeHandle, ITtsPlayerProvider provider) {
        mTtsPlayerProvider = provider;
        bind(nativeHandle);
    }

    /**
     * Release this audio player and associated resources.  After this method is called the audio
     * player should not respond to commands from the core or from the view host.
     */
    @SuppressWarnings("unused")
    private void release() {
        mIsReleased.set(true);
        mMainHandler.removeCallbacksAndMessages(null);
        final ITtsPlayer ttsPlayer = mTtsPlayerProvider.getPlayer();
        ttsPlayer.stop();
        ttsPlayer.setStateChangeListener(null);
        ttsPlayer.setWordMarkListener(null);
    }

    /**
     * Assign a media track.  This will pause all audio playback and set up a new audio track.
     * The player should queue the track up for playing, but not start playing.
     *
     * @param track A single MediaTrack for playback.  The repeatCount field should be ignored.
     */
    @SuppressWarnings("unused")
    private void setTrack(MediaTrack track) {
        String source = track.getUrl();
        mTtsPlayerProvider.prepare(source);
        final ITtsPlayer ttsPlayer = mTtsPlayerProvider.getPlayer();
        ttsPlayer.setStateChangeListener(this);
    }

    /**
     * Start or resume playing the current media track.  This is ignored if no media track has
     * been set, if the media track has finished playing, or if the media track has an error.
     */
    @SuppressWarnings("unused")
    private void play() {
        final ITtsPlayer ttsPlayer = mTtsPlayerProvider.getPlayer();
        ttsPlayer.setWordMarkListener(this);
        ttsPlayer.play();
    }

    /**
     * Pause audio playback.
     */
    @SuppressWarnings("unused")
    private void pause() {
        mTtsPlayerProvider.getPlayer().stop();
    }

    /**
     * This is called on external thread by TTSPlayer.
     * Schedule on to Core's thread.
     * @param mark the speech mark
     */
    @Override
    public void onSpeechMark(ITtsPlayer.ISpeechMarksListener.SpeechMark mark) {
        if (mIsReleased.get()) {
            return;
        }

        mMainHandler.post(() ->
                nSpeechMark(getNativeHandle(),
                    viewhostMarkToAPLSM(mark).getIndex(),
                    mark.start,
                    mark.end,
                    mark.time,
                    mark.value));
    }

    private synchronized void startTimeUpdates() {
        if (mIsReleased.get() || mLastPublishedTrackState != TrackState.kTrackReady) {
            return;
        }

        long now = System.currentTimeMillis();
        mCurrentOffset = (int) (now - mStartTime);
        publishTrackState(new AudioPlayerEvent(AudioPlayerEventType.kAudioPlayerEventTimeUpdate, TrackState.kTrackReady, mCurrentOffset));
        mMainHandler.postDelayed(this::startTimeUpdates, UPDATE_TIME_INTERVAL_MS);
    }

    public synchronized void publishTrackState(AudioPlayerEvent audioPlayerEvent) {
        if (mIsReleased.get()) {
            return;
        }

        TrackState oldState = mLastPublishedTrackState;
        mLastPublishedTrackState = audioPlayerEvent.mTrackState;

        nStateChange(getNativeHandle(),
                audioPlayerEvent.mAudioPlayerEventType.getIndex(),
                audioPlayerEvent.mOffset,
                audioPlayerEvent.mDuration,
                audioPlayerEvent.mIsPaused,
                audioPlayerEvent.mIsEnded,
                audioPlayerEvent.mTrackState.getIndex());

        // Start time updates when transitioning to ready
        if (oldState == TrackState.kTrackNotReady && mLastPublishedTrackState == TrackState.kTrackReady) {
            // TODO: Can we remove mCurrentTrackState and instead just call startTimeUpdates in onStateChanged
            // when there is track state of ready?
            startTimeUpdates();
        }
    }

    /**
     * This is called on external thread by TTSPlayer.
     * Schedule onto Core's thread.
     * @param state the new state.
     */
    @Override
    public synchronized void onStateChange(ITtsPlayer.IStateChangeListener.AudioPlayerState state) {
        if (mIsReleased.get()) {
            return;
        }

        AudioPlayerEvent audioPlayerEvent;
        switch (state) {
            case STATE_READY:
                audioPlayerEvent = new AudioPlayerEvent(AudioPlayerEventType.kAudioPlayerEventPlay, TrackState.kTrackReady, mCurrentOffset);
                mStartTime = System.currentTimeMillis();
                break;
            case STATE_ENDED:
                audioPlayerEvent = new AudioPlayerEvent(AudioPlayerEventType.kAudioPlayerEventEnd, TrackState.kTrackNotReady, mCurrentOffset);
                break;
            case STATE_ERROR:
                audioPlayerEvent = new AudioPlayerEvent(AudioPlayerEventType.kAudioPlayerEventFail, TrackState.kTrackFailed, mCurrentOffset);
                mStartTime = 0;
                break;
            default:
                audioPlayerEvent = new AudioPlayerEvent(AudioPlayerEventType.kAudioPlayerEventTimeUpdate, TrackState.kTrackNotReady, mCurrentOffset);
                break;
        }

        mMainHandler.post(() -> publishTrackState(audioPlayerEvent));
    }

    private SpeechMarkType viewhostMarkToAPLSM(SpeechMark mark) {
        SpeechMarkType markType = SpeechMarkType.kSpeechMarkUnknown;
        switch (mark.markType) {
            case WORD:
                markType = SpeechMarkType.kSpeechMarkWord;
                break;
            case SENTENCE:
                markType = SpeechMarkType.kSpeechMarkSentence;
                break;
            case SSML:
                markType = SpeechMarkType.kSpeechMarkSSML;
                break;
        }
        return markType;
    }

    private static native void nSpeechMark(long handle, int type, int start, int end, long time, String value);

    private static native void nStateChange(
            long handle,
            int type,
            int currentTime,
            int duration,
            boolean paused,
            boolean ended,
            int trackState);

    /**
     * Contains an immutable snapshot of the audio player and track states at the time of an event.
     */
    public static final class AudioPlayerEvent {
        private final AudioPlayerEventType mAudioPlayerEventType;
        private final TrackState mTrackState;
        private final int mOffset; // TODO update API to get current player time in player updates.
        private final int mDuration = 1; // TODO duration is not currently available in TtsPlayer, but it is unused in core.
        private final boolean mIsPaused = false;
        private final boolean mIsEnded;
        public AudioPlayerEvent(AudioPlayerEventType audioPlayerEventType, TrackState trackState, int offset) {
            mAudioPlayerEventType = audioPlayerEventType;
            mTrackState = trackState;
            mOffset = offset;
            mIsEnded = mAudioPlayerEventType == AudioPlayerEventType.kAudioPlayerEventEnd;
        }
    }
}
