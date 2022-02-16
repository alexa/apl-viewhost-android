/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.amazon.apl.android.dependencies.IMediaPlayer;
import com.amazon.apl.android.primitive.MediaSources;
import com.amazon.apl.android.providers.AbstractMediaPlayerProvider;
import com.amazon.apl.enums.AudioTrack;
import com.amazon.apl.enums.PropertyKey;
import com.amazon.apl.enums.VideoScale;

/**
 * APL Video Component.
 * See <a href="https://developer.amazon.com/docs/alexa-presentation-language/apl-video.html">
 * APL Video Specification</a>
 */
public class Video extends Component implements IMediaPlayer.IMediaListener {

    private IMediaPlayer mMediaPlayer;

    /**
     * Indicates whether a pending MediaPlayer state change is from an Event (e.g. PlayMediaEvent)
     */
    private boolean mFromEvent;

    public void setMediaPlayer(IMediaPlayer mMediaPlayer) {
        this.mMediaPlayer = mMediaPlayer;
    }

    /**
     * Video constructor.
     * {@inheritDoc}
     */
    Video(long nativeHandle, String componentId, @NonNull RenderingContext renderingContext) {
        super(nativeHandle, componentId, renderingContext);
    }

    private static native void nUpdateMediaState(long nativeHandle, int trackIndex, int trackCount,
                                                 int currentTime, int duration, boolean paused,
                                                 boolean ended, boolean fromEvent, int trackState,
                                                 int errorCode);

    /**
     * @return True if the video should begin playback by itself as soon as it is ready. Defaults to
     * false.
     */
    public boolean shouldAutoPlay() {
        return mProperties.getBoolean(PropertyKey.kPropertyAutoplay);
    }

    /**
     * @return The audio track set for the video. Defaults to
     * {@link AudioTrack#kAudioTrackForeground}.
     */
    public AudioTrack getAudioTrack() {
        return AudioTrack.valueOf(mProperties.getEnum(PropertyKey.kPropertyAudioTrack));
    }

    /**
     * @return The scale type for the video. Defaults to {@link VideoScale#kVideoScaleBestFit}.
     */
    public VideoScale getVideoScale() {
        return VideoScale.valueOf(mProperties.getEnum(PropertyKey.kPropertyScale));
    }

    public MediaSources getMediaSources() {
        return mProperties.getMediaSources(PropertyKey.kPropertySource);
    }

    /**
     * Internal property used to get the current track time in ms.
     * @return the current track time in ms.
     */
    @VisibleForTesting
    public int getCurrentTrackTime() {
        return mProperties.getInt(PropertyKey.kPropertyTrackCurrentTime);
    }

    /**
     * Internal property used to get the current track index.
     * @return the current track index.
     */
    @VisibleForTesting
    public int getTrackIndex() {
        return mProperties.getInt(PropertyKey.kPropertyTrackIndex);
    }

    /**
     * @return the media player instance.
     */
    public IMediaPlayer getMediaPlayer() {
        return mMediaPlayer;
    }

    /**
     * Indicates whether a pending media state change is the result of an Event, such as
     * {@link com.amazon.apl.android.events.PlayMediaEvent}. This is needed to correctly apply
     * "fast mode" to commands.
     *
     * @param fromEvent True to indicate that upcoming state change is from an event, else false
     */
    public void setFromEvent(boolean fromEvent) {
        mFromEvent = fromEvent;
    }

    public AbstractMediaPlayerProvider getMediaPlayerProvider() {
        return getRenderingContext().getMediaPlayerProvider();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateMediaState(@NonNull IMediaPlayer player) {
        MediaState state = mMediaPlayer.getCurrentMediaState();
        if (state == MediaState.RELEASED) {
            return;
        }
        nUpdateMediaState(getNativeHandle(),
                player.getCurrentTrackIndex(),
                player.getTrackCount(),
                player.getCurrentSeekPosition(),
                player.getDuration(),
                !player.isPlaying(),
                state == MediaState.END,
                mFromEvent,
                player.getTrackState(),
                player.getCurrentError());
        // Reset the fromEvent flag after a PlayMedia or ControlMedia command have been applied
        if (state == MediaState.PLAYING) {
            mFromEvent = false;
        }
    }


}