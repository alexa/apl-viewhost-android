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

import com.amazon.apl.android.APLLayoutParams;
import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.Video;
import com.amazon.apl.android.dependencies.IMediaPlayer;
import com.amazon.apl.android.primitive.MediaSources;
import com.amazon.apl.android.views.APLAbsoluteLayout;
import com.amazon.apl.enums.AudioTrack;
import com.amazon.apl.enums.PropertyKey;
import com.amazon.apl.enums.VideoScale;

public class VideoViewAdapter extends ComponentViewAdapter<Video, View> {
    private static VideoViewAdapter INSTANCE;

    private VideoViewAdapter() {
        super();
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
    private void applySource(Video component, View view) {
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

    private void applyMuted(Video component, View view) {
        if (component.shouldMute()) {
            component.getMediaPlayer().mute();
        } else {
            component.getMediaPlayer().unmute();
        }
    }

    public void applyAudioTrack(Video component) {
        final AudioTrack audioTrack = component.getAudioTrack();
        if (audioTrack != null) {
            component.getMediaPlayer().setAudioTrack(audioTrack);
        }
    }

    public void applyVideoScale(Video component) {
        final VideoScale scale = component.getVideoScale();
        if (scale == null) return;
        IMediaPlayer mediaPlayer = component.getMediaPlayer();
        if (mediaPlayer == null) return;
        mediaPlayer.setVideoScale(scale);
    }

    public void applyCurrentTrackIndex(Video component) {
        final int currentTrackIndex = component.getTrackIndex();
        if (currentTrackIndex != 0) {
            component.getMediaPlayer().setTrack(currentTrackIndex);
        }
    }

    public void applyCurrentTrackTime(Video component) {
        final int currentTrackTime = component.getCurrentTrackTime();
        if (currentTrackTime != 0) {
            component.getMediaPlayer().seek(currentTrackTime);
        }
    }

    public void applyAutoPlay(Video component) {
        if (component.shouldAutoPlay()) {
            component.getMediaPlayer().play();
        }
    }
    /**
     * Apply properties to the APL component.
     */
    @Override
    public void applyAllProperties(Video component, @NonNull View view) {

        super.applyAllProperties(component, view);
        // This releases the current media player
        applySource(component, view);
        applyMuted(component, view);
        applyAudioTrack(component);
        applyVideoScale(component);
        applyCurrentTrackIndex(component);
        applyCurrentTrackTime(component);
        // Order matters here as we want to apply the autoPlay last.
        applyAutoPlay(component);
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
