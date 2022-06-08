/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies;

import androidx.annotation.NonNull;

import com.amazon.apl.android.IDocumentLifecycleListener;
import com.amazon.apl.android.primitive.MediaSources;
import com.amazon.apl.enums.AudioTrack;
import com.amazon.apl.enums.TrackState;
import com.amazon.apl.enums.VideoScale;

/**
 * Defines the API for media player.
 */
public interface IMediaPlayer {
    /**
     * Return the current playback position.
     *
     * @return the current playback position.
     */
    int getCurrentSeekPosition();

    /**
     * Sets the audio track mode for the playback.
     *
     * @param audioTrack The audio track mode.
     */
    void setAudioTrack(@NonNull AudioTrack audioTrack);

    /**
     * Sets the video scale mode for the playback.
     *
     * @param scale The video scale mode.
     */
    void setVideoScale(@NonNull VideoScale scale);

    /**
     * Sets the sources to play.
     *
     * @param mediaSources The media sources to play.
     */
    void setMediaSources(@NonNull MediaSources mediaSources);

    /**
     * Adds the listener to listen for media events.
     *
     * @param listener The media listener.
     */
    void addMediaStateListener(@NonNull IMediaListener listener);

    /**
     * Removes the listener from listening to the media events.
     *
     * @param listener The media listener.
     */
    void removeMediaStateListener(@NonNull IMediaListener listener);

    /**
     * Start playing media. The media source and the {@code videoView} needs to
     * be set before calling this method.
     */
    void play();

    /**
     * Pause the current track.
     */
    void pause();

    /**
     * Stop the current track.
     */
    void stop();

    /**
     * Go to the next media track in the source list.
     */
    void next();

    /**
     * Go to the previous media track in the source list.
     */
    void previous();

    /**
     * Mute the audio
     */
    default void mute() {}

    /**
     * Unmute the audio
     */
    default void unmute() {}

    /**
     * Change the current track in the source list.
     *
     * @param trackIndex the index of the next track.
     */
    void setTrack(int trackIndex);

    /**
     * Change the media playback position.
     *
     * @param msec The desired offset to set in the milliseconds. This offset is relative to the
     *             track offset and duration and will be clipped to that range
     */
    void seek(int msec);

    /**
     * Rewind the current media track to its start.
     */
    void rewind();

    /**
     * @return true if the player is playing a track currently.
     */
    boolean isPlaying();

    /**
     * @return true if the player audio is muted, false otherwise
     */
    default boolean isMuted() {
        return false;
    }

    /**
     * Return the total duration of the current track.
     *
     * @return the total duration of the current track.
     */
    int getDuration();

    /**
     * Return the current track index from the source list.
     *
     * @return the current playback position.
     */
    int getCurrentTrackIndex();

    /**
     * Return the total track count.
     * @return the total track count.
     */
    int getTrackCount();

    /**
     * Returns current error code.
     * @return non negative error code if current media state is error, -1 otherwise.
     */
    default int getCurrentError() {
        return getCurrentMediaState() == IMediaListener.MediaState.ERROR ? 0 : -1;
    }

    /**
     * Interface definition for a callback to playback updates.
     */
    interface IMediaListener {

        /**
         * Called when the media state or the current track position changes.
         *
         * @param player the media player.
         */
        void updateMediaState(IMediaPlayer player);

        /**
         * Playback states
         */
        enum MediaState {
            /**
             * Player is in error state.
             */
            ERROR,
            /**
             * No tracks are being played or loaded for the playback.
             */
            IDLE,
            /**
             * Player preparing itself for playback.
             */
            PREPARING,
            /**
             * Ready for playback. Can accept control media command.
             */
            READY,
            /**
             * Playing a track. Can accept control media command.
             */
            PLAYING,
            /**
             * Player paused. Can accept control media command.
             */
            PAUSED,
            /**
             * Track updated for playback.
             */
            TRACK_UPDATE,
            /**
             * Playback of all tracks is complete.
             */
            END,
            /**
             * This player has been destroyed
             */
            RELEASED
        }
    }

    /**
     * Gets the current playback state of the player.
     *
     * @return the current playback state.
     */
    @NonNull
    IMediaListener.MediaState getCurrentMediaState();

    /**
     * Release media player resources. Always call this method when you are done with the media
     * player. After this call, this player should not be used.
     */
    void release();

    /**
     * Releases the request to receive {@link android.media.AudioManager} callbacks.
     */
    default void releaseAudioFocus() {};

    default int getTrackState() {
        TrackState trackState;
        switch (getCurrentMediaState()) {
            case READY:
            case PLAYING:
            case PAUSED:
                trackState = TrackState.kTrackReady;
                break;
            case ERROR:
                trackState = TrackState.kTrackFailed;
                break;
            default:
                trackState = TrackState.kTrackNotReady;
        }
        return trackState.getIndex();
    }
}