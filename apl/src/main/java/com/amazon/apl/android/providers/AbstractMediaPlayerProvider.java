/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.providers;

import android.content.Context;
import android.view.View;

import com.amazon.apl.android.IDocumentLifecycleListener;
import com.amazon.apl.android.dependencies.IMediaPlayer;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;

/**
 * Abstact class that manages media player lifecycle. Extend this class to implement a custom
 * provider
 */
public abstract class AbstractMediaPlayerProvider<T extends View> implements IMediaPlayer.IMediaListener, IDocumentLifecycleListener {

    @NonNull
    private final Set<IMediaPlayer> mPlayers = new HashSet<>();
    @NonNull
    private final Set<IMediaPlayer> mPlayingSet = new HashSet<>();
    @NonNull
    private final Collection<IMediaPlayer> mPausedSet = new LinkedList<>();

    /**
     * Called internally by viewhost to get and track new players.
     * @param context the {@link Context}.
     * @param view the view for displaying video.
     * @return a new media player.
     */
    public IMediaPlayer getNewPlayer(Context context, T view) {
        IMediaPlayer player = createPlayer(context, view);
        mPlayers.add(player);
        player.addMediaStateListener(this);
        return player;
    }

    /**
     * Checks if any media player is playing.
     * @return true if mPlayingSet is not empty, false otherwise.
     */
    public boolean hasPlayingMediaPlayer() {
        return !mPlayingSet.isEmpty();
    }

    /**
     * Gets an instance of a view meant to be used for video playback by the media player. Note that
     * each call to this method should return a new instance.
     *
     * @param context the context of the layout.
     * @return a new instance of the view.
     */
    public abstract T createView(Context context);


    /**
     * Gets an instance of the media player. Each call to this function must onSource a new
     * media player instance.
     *
     * @param context The context of the layout.
     * @param view    the video needed to display the video.
     * @return The media player instance.
     */
    public abstract IMediaPlayer createPlayer(Context context, T view);

    /**
     * Releases all media player instances.
     */
    @CallSuper
    public void releasePlayers() {
        // create a new array to avoid a ConcurrentModificationException
        List<IMediaPlayer> players = new LinkedList<>(mPlayers);
        for(IMediaPlayer player : players) {
            MediaState state = player.getCurrentMediaState();
            if(state != MediaState.RELEASED) {
                player.release();
            }
        }
        mPlayers.clear();
        mPlayingSet.clear();
        mPausedSet.clear();
    }

    /**
     * The document is no longer valid for display.
     */
    @Override
    public void onDocumentFinish() {
        releasePlayers();
    }

    /**
     * Pauses any playing media players.
     */
    @Override
    public void onDocumentPaused() {
        List<IMediaPlayer> playingPlayers = new LinkedList<>(mPlayingSet);
        for(IMediaPlayer player : playingPlayers) {
            mPausedSet.add(player);
            player.pause();
        }

        for (IMediaPlayer player : mPlayers) {
            player.releaseAudioFocus();
        }
        mPlayingSet.clear();
    }

    /**
     * Resumes any playing media players.
     */
    @Override
    public void onDocumentResumed() {
        List<IMediaPlayer> playingPlayers = new LinkedList<>(mPausedSet);
        for(IMediaPlayer player : playingPlayers) {
            player.play();
        }
        mPausedSet.clear();
    }

    @Override
    public void updateMediaState(@NonNull IMediaPlayer player) {
        MediaState state = player.getCurrentMediaState();
        if (state == MediaState.RELEASED) {
            mPlayers.remove(player);
            // don't need to remove this as a listener. Media Player should
            // clean up it's own assets
        }
        if (state == MediaState.PAUSED) {
            mPausedSet.add(player);
        }

        if (player.isPlaying()) {
            mPlayingSet.add(player);
        } else {
            mPlayingSet.remove(player);
        }
    }
}
