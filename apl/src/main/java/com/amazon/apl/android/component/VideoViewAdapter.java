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
        if (!component.getRenderingContext().isMediaPlayerV2Enabled()) {
            IMediaPlayer mediaPlayer = component.getMediaPlayer();
            if (mediaPlayer != null) {
                // In case media player was already initialized.
                mediaPlayer.release();
            }
            mediaPlayer = component.getMediaPlayerProvider().getNewPlayer(
                    view.getContext(), view);
            final MediaSources mediaSources = component.getMediaSources();
            if (mediaSources != null) {
                mediaPlayer.setMediaSources(mediaSources);
            }
            component.setMediaPlayer(mediaPlayer);
            mediaPlayer.addMediaStateListener(component);
        }
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
        if (!component.getRenderingContext().isMediaPlayerV2Enabled()) {
            if (component.shouldMute()) {
                component.getMediaPlayer().mute();
            } else {
                component.getMediaPlayer().unmute();
            }
        }
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
        if (!component.getRenderingContext().isMediaPlayerV2Enabled()) {
            // This releases the current media player and gets a new one
            applySource(component, view);
            // Assign all properties
            applyMuted(component, view);
            applyAudioTrack(component, component.getMediaPlayer());
            applyVideoScale(component, component.getMediaPlayer());
            applyCurrentTrackIndex(component, component.getMediaPlayer());
            applyCurrentTrackTime(component, component.getMediaPlayer());
            // Order matters here as we want to apply the autoPlay last.
            applyAutoPlay(component, component.getMediaPlayer());
        } else {
            applyView(component, view);
        }
    }

    @Override
    public View createView(Context context, IAPLViewPresenter presenter) {
        return presenter.getMediaPlayerProvider().createView(context);
    }

    @Override
    void applyPadding(Video component, View view) {
        setPaddingFromBounds(component, view, false);
    }

    // Temporary fix to inflate the video if it doesn't exist for backwards compatibility
    // TODO: remove this once long term fix added
    public void inflateViewWithNonZeroDimensions(Video component, IAPLViewPresenter viewPresenter) {
        View view = viewPresenter.inflateComponentHierarchy(component);
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams.width == 0 && layoutParams.height == 0) {
            if (layoutParams instanceof APLAbsoluteLayout.LayoutParams) {
                view.setLayoutParams(new APLAbsoluteLayout.LayoutParams(1, 1, 0, 0));
            } else if (layoutParams instanceof APLLayoutParams) {
                view.setLayoutParams(new APLLayoutParams(1, 1, 0, 0));
            } else {
                view.setLayoutParams(new ViewGroup.MarginLayoutParams(1, 1));
            }
        }
    }
}
