/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.scenegraph.media;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.VisibleForTesting;

import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.android.dependencies.IMediaPlayer;
import com.amazon.apl.android.media.MediaPlayer;
import com.amazon.apl.android.scenegraph.APLLayer;
import com.amazon.apl.android.sgcontent.VideoNode;

public class APLVideoLayer extends APLLayer {
    private static final String TAG = "APLVideoLayer";

    public APLVideoLayer(RenderingContext renderingContext) {
        super(renderingContext);
    }

    @Override
    public void attachView(ViewGroup view) {
        super.attachView(view);
        View videoView = applyNodeProperties(view);
        if (videoView != null) {
            view.addView(videoView);
        }
    }

    @VisibleForTesting
    // TODO: Can make this private, but we will lose the ability to mock the view.
    View applyNodeProperties(ViewGroup view) {
        VideoNode videoNode = (VideoNode) getContent()[0];
        MediaPlayer nativePlayer = videoNode.getMediaPlayer();
        if (nativePlayer == null) {
            Log.e(TAG, "Native player not initialized, won't render video");
            return null;
        }
        View videoView = getRenderingContext().getMediaPlayerProvider().createView(view.getContext());
        IMediaPlayer mediaPlayer = nativePlayer.getMediaPlayer();
        if (mediaPlayer == null) {
            mediaPlayer = getRenderingContext().getMediaPlayerProvider().getNewPlayer(videoView.getContext(), videoView);
            mediaPlayer.setVideoScale(videoNode.getVideoScale());
            nativePlayer.setMediaPlayer(mediaPlayer);
        } else {
            // For supporting back navigation
            mediaPlayer.release();
            // The track index and track time are not available from component in V2
            // Retrieve the current track index before the player was released.
            final int preReleaseTrackIndex = nativePlayer.getCurrentTrackIndex();
            // Retrieve the current track time before the player was released.
            final int preReleaseTrackPosition = nativePlayer.getCurrentSeekPosition();
            // Add the mediaPlayer to the list of playing set
            mediaPlayer = getRenderingContext().getMediaPlayerProvider().getNewPlayer(videoView.getContext(), videoView);
            // Add the native player to listen to media state updates from the revived media player
            nativePlayer.setMediaPlayer(mediaPlayer);
            // Assign all the properties from cached document state
            mediaPlayer.setMediaSources(nativePlayer.getMediaSources());
            // Set the muted state
            if (nativePlayer.isCurrentlyMuted()) {
                mediaPlayer.mute();
            }
            // Set the audio track
            mediaPlayer.setAudioTrack(nativePlayer.getCurrentAudioTrack());
            // Set the video scale
            mediaPlayer.setVideoScale(videoNode.getVideoScale());
            // Set the track to the pre-release track index
            mediaPlayer.setTrack(preReleaseTrackIndex);
            // Seek to the pre-release track position
            mediaPlayer.seek(preReleaseTrackPosition);
            // Play if needed
            if (nativePlayer.isCurrentlyPlaying()) {
                mediaPlayer.play();
            }
        }
        return videoView;
    }
}
