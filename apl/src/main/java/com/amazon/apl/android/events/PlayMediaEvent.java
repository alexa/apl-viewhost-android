/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.events;

import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import com.amazon.apl.android.Event;
import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.Video;
import com.amazon.apl.android.component.VideoViewAdapter;
import com.amazon.apl.android.dependencies.IMediaPlayer;
import com.amazon.apl.android.primitive.MediaSources;
import com.amazon.apl.android.primitive.Rect;
import com.amazon.apl.enums.AudioTrack;
import com.amazon.apl.enums.EventProperty;

import java.util.Objects;

import static com.amazon.apl.enums.AudioTrack.kAudioTrackForeground;

/**
 * APL Play Media Event
 * See @{link <a https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-commands-media.html#playmedia>
 * APL Play Media Command Specification</a>}
 */
public class PlayMediaEvent extends Event {
    private static final String TAG = "PlayMediaEvent";

    private IMediaPlayer.IMediaListener mListener;

    /**
     * Constructs the Event.
     *
     * @param nativeHandle Handle to the native event.
     * @param rootContext  The root context for the event.
     */
    private PlayMediaEvent(long nativeHandle, RootContext rootContext) {
        super(nativeHandle, rootContext);
    }


    static public PlayMediaEvent create(long nativeHandle, RootContext rootContext) {
        return new PlayMediaEvent(nativeHandle, rootContext);
    }

    /**
     * Execute the command.
     */
    @Override
    public void execute() {
        try {
            final Video video = (Video) getComponent();

            // Temporary fix to inflate the video if it doesn't exist for backwards compatibility
            // TODO: remove this once long term fix added
            IAPLViewPresenter viewPresenter = mRootContext.getViewPresenter();
            View view = viewPresenter.findView(video);
            Rect bounds = video.getBounds();

            if (view == null && (bounds.getWidth() == 0.0f || bounds.getHeight() == 0.0f)) {
                VideoViewAdapter.getInstance().inflateViewWithNonZeroDimensions(video, viewPresenter);
            }

            final IMediaPlayer mediaPlayer = Objects.requireNonNull(video.getMediaPlayer());
            final MediaSources sources = Objects.requireNonNull(mProperties.getMediaSources(EventProperty.kEventPropertySource));
            final AudioTrack audioTrack = Objects.requireNonNull(AudioTrack.valueOf(
                    mProperties.getEnum(EventProperty.kEventPropertyAudioTrack)));

            video.setFromEvent(true);
            handlePlayMediaEvent(mediaPlayer, sources, audioTrack);
        } catch (NullPointerException | ClassCastException e) {
            Log.e(TAG, "Cannot execute PlayMediaEvent.", e);
        }
    }

    /**
     * Terminate the event process.
     */
    @Override
    public void terminate() {
        try {
            final Video video = (Video) getComponent();
            final IMediaPlayer mediaPlayer = Objects.requireNonNull(video.getMediaPlayer());

            mediaPlayer.pause();
            if (mListener != null) {
                mediaPlayer.removeMediaStateListener(mListener);
            }
        } catch (NullPointerException | ClassCastException e) {
            Log.e(TAG, "Error terminating PlayMediaEvent.", e);
        }
        mListener = null;
    }

    private void handlePlayMediaEvent(@NonNull IMediaPlayer mediaPlayer, @NonNull MediaSources sources, @NonNull AudioTrack audioTrack) {
        mediaPlayer.setAudioTrack(audioTrack);
        mediaPlayer.setMediaSources(sources);
        mediaPlayer.setTrack(0);
        mediaPlayer.play();
        if (audioTrack == kAudioTrackForeground) {
            // for foreground audio, resolve only when the MediaState changes to END.
            mListener = new PlayMediaListener();
            mediaPlayer.addMediaStateListener(mListener);
        } else {
            resolve();
        }
    }

    private class PlayMediaListener implements IMediaPlayer.IMediaListener {
        @Override
        public void updateMediaState(IMediaPlayer player) {
            MediaState state = player.getCurrentMediaState();
            if (state == MediaState.END) {
                player.removeMediaStateListener(this);
                mListener = null;
                resolve();
            }
        }
    }
}
