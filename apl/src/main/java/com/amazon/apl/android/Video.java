/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.amazon.apl.android.dependencies.IMediaPlayer;
import com.amazon.apl.android.media.MediaPlayer;
import com.amazon.apl.android.primitive.BoundMediaSources;
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
public class Video extends Component {

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

    private static native MediaPlayer nGetMediaPlayer(long nativeHandle);

    /**
     * @return True if the video should begin playback by itself as soon as it is ready. Defaults to
     * false.
     */
    public boolean shouldAutoPlay() {
        return mProperties.getBoolean(PropertyKey.kPropertyAutoplay);
    }

    public boolean shouldMute() {
        return mProperties.getBoolean(PropertyKey.kPropertyMuted);
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
        BoundMediaSources boundMediaSources = mProperties.getBoundMediaSources(PropertyKey.kPropertySource);
        MediaSources sources = MediaSources.create();
        for (int i = 0; i < boundMediaSources.size(); i++) {
            MediaSources.MediaSource source = MediaSources.MediaSource.builder()
                    .url(boundMediaSources.at(i).url())
                    .duration(boundMediaSources.at(i).duration())
                    .offset(boundMediaSources.at(i).offset())
                    .repeatCount(boundMediaSources.at(i).repeatCount())
                    .headers(boundMediaSources.at(i).headers())
                    .textTracks(boundMediaSources.at(i).textTracks())
                    .build();
            sources.add(source);
        }
        return sources;
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

    @Nullable
    public MediaPlayer getNativePlayer() {
        return nGetMediaPlayer(getNativeHandle());
    }

    public AbstractMediaPlayerProvider getMediaPlayerProvider() {
        return getRenderingContext().getMediaPlayerProvider();
    }
}