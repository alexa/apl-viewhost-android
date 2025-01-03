/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.apl.android.APLLayoutParams;
import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.Video;
import com.amazon.apl.android.dependencies.IMediaPlayer;
import com.amazon.apl.android.media.MediaPlayer;
import com.amazon.apl.android.primitive.MediaSources;
import com.amazon.apl.android.views.APLAbsoluteLayout;
import com.amazon.apl.enums.AudioTrack;
import com.amazon.apl.enums.PropertyKey;
import com.amazon.apl.enums.VideoScale;

public class VideoViewAdapter extends ComponentViewAdapter<Video, View> {
    private static final String TAG = "VideoViewAdapter";
    private static VideoViewAdapter INSTANCE;

    private VideoViewAdapter() {
        super();
        // The below methods are for legacy purposes only, should not be used when using Core mediaPlayer interface
        putPropertyFunction(PropertyKey.kPropertySource, this::applySource);
        putPropertyFunction(PropertyKey.kPropertyMuted, this::applyMuted);
    }

    public static VideoViewAdapter getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new VideoViewAdapter();
        }
        return INSTANCE;
    }

    /**
     * Updates the source.
     *
     * Note we release and rebuild the player on source changes. This could be optimized.
     *
     * TODO the current default MediaPlayer fails on source changes
     *
     * @param view the view associated with the player.
     */
    @Deprecated
    private void applySource(Video component, View view) {
        // Before MediaPlayerV2, source and mute property updates were handled through dynamic property updates.
        // Now they are handled by the MediaPlayer interface.
        assert component.getRenderingContext().isMediaPlayerV2Enabled();
    }

    /**
     * Apply the view to the media player
     * @param component
     * @param view
     */
    private void applyView(Video component, View view) {
        final MediaPlayer nativePlayer = component.getNativePlayer();
        if (nativePlayer == null) {
            Log.e(TAG, "Native player not initialized, won't render video");
            return;
        }
        IMediaPlayer mediaPlayer = nativePlayer.getMediaPlayer();
        if (mediaPlayer == null) {
            // Order is not guaranteed and may change in future
            mediaPlayer = component.getMediaPlayerProvider().getNewPlayer(view.getContext(), view);
            applyVideoScale(component, mediaPlayer);
            applyCurrentTrackIndex(component, mediaPlayer);
            applyCurrentTrackTime(component, mediaPlayer);
            nativePlayer.setMediaPlayer(mediaPlayer);
        }
        // For supporting back navigation.
        else {
            mediaPlayer.release();
            // The track index and track time are not available from component in V2
            // Retrieve the current track index before the player was released.
            final int preReleaseTrackIndex = nativePlayer.getCurrentTrackIndex();
            // Retrieve the current track time before the player was released.
            final int preReleaseTrackPosition = nativePlayer.getCurrentSeekPosition();
            // Add the mediaPlayer to the list of playing set
            mediaPlayer = component.getMediaPlayerProvider().getNewPlayer(view.getContext(), view);
            // Add the native player to listen to media state updates from the revived media player
            nativePlayer.setMediaPlayer(mediaPlayer);
            // Assign all the properties from cached document state
            mediaPlayer.setMediaSources(component.getMediaSources());
            applyMuted(component, mediaPlayer);
            applyAudioTrack(component, mediaPlayer);
            applyVideoScale(component, mediaPlayer);
            // Set the track to the pre-release track index
            mediaPlayer.setTrack(preReleaseTrackIndex);
            // Seek to the pre-release track position
            mediaPlayer.seek(preReleaseTrackPosition);
            // Play if needed
            applyAutoPlay(component, mediaPlayer);
        }
    }

    private void applyMuted(Video component, IMediaPlayer mediaPlayer) {
        if (component.shouldMute()) {
            mediaPlayer.mute();
        } else {
            mediaPlayer.unmute();
        }
    }

    @Deprecated
    private void applyMuted(Video component, View view) {
        // Before MediaPlayerV2, source and mute property updates were handled through dynamic property updates.
        // Now they are handled by the MediaPlayer interface.
        assert (component.getRenderingContext().isMediaPlayerV2Enabled());
    }

    private void applyAudioTrack(Video component, IMediaPlayer mediaPlayer) {
        final AudioTrack audioTrack = component.getAudioTrack();
        if (mediaPlayer != null && audioTrack != null) {
            mediaPlayer.setAudioTrack(audioTrack);
        }
    }

    private void applyVideoScale(Video component, IMediaPlayer mediaPlayer) {
        final VideoScale scale = component.getVideoScale();
        if (mediaPlayer != null && scale != null) {
            mediaPlayer.setVideoScale(scale);
        }
    }

    private void applyCurrentTrackIndex(Video component, IMediaPlayer mediaPlayer) {
        final int currentTrackIndex = component.getTrackIndex();
        if (mediaPlayer != null && currentTrackIndex != 0) {
            mediaPlayer.setTrack(currentTrackIndex);
        }
    }

    private void applyCurrentTrackTime(Video component, IMediaPlayer mediaPlayer) {
        final int currentTrackTime = component.getCurrentTrackTime();
        if (mediaPlayer != null && currentTrackTime != 0) {
            mediaPlayer.seek(currentTrackTime);
        }
    }

    private void applyAutoPlay(Video component, IMediaPlayer mediaPlayer) {
        if (mediaPlayer != null && component.shouldAutoPlay()) {
            mediaPlayer.play();
        }
    }

    /**
     * Apply properties to the APL component.
     */
    @Override
    public void applyAllProperties(Video component, @NonNull View view) {

        super.applyAllProperties(component, view);
        assert component.getRenderingContext().isMediaPlayerV2Enabled();
        applyView(component, view);
    }

    @Override
    public View createView(Context context, IAPLViewPresenter presenter) {
        return presenter.getMediaPlayerProvider().createView(context);
    }

    @Override
    void applyPadding(Video component, View view) {
        setPaddingFromBounds(component, view, false);
    }
}
