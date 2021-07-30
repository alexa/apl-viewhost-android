/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.providers;

import android.content.Context;
import android.view.View;

import com.amazon.apl.android.dependencies.IMediaPlayer;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AbstractMediaPlayerProviderTest extends ViewhostRobolectricTest {

    private IMediaPlayer playerOne;
    private IMediaPlayer playerTwo;

    private AbstractMediaPlayerProvider<View> mProvider = spy(new AbstractMediaPlayerProvider<View>() {
        @Override
        public View createView(Context context) {
            return mock(View.class);
        }

        @Override
        public IMediaPlayer createPlayer(Context context, View view) {
            return mock(IMediaPlayer.class);
        }
    });

    @Before
    public void setup() {
        playerOne = mProvider.getNewPlayer(getApplication(), mProvider.createView(getApplication()));
        playerTwo = mProvider.getNewPlayer(getApplication(), mProvider.createView(getApplication()));

        when(playerOne.isPlaying()).thenReturn(true);
        when(playerTwo.isPlaying()).thenReturn(false);

        mProvider.updateMediaState(playerOne);
        mProvider.updateMediaState(playerTwo);
        assertTrue(mProvider.hasPlayingMediaPlayer());
    }

    @Test
    public void test_documentLifecycle_playingMediaPlayerPauses () {
        assertTrue(mProvider.hasPlayingMediaPlayer());

        mProvider.onDocumentPaused();

        verify(playerOne).pause();
        verify(playerTwo, never()).pause();
        verify(playerOne).releaseAudioFocus();
        verify(playerTwo).releaseAudioFocus();
        assertFalse(mProvider.hasPlayingMediaPlayer());
    }

    @Test
    public void test_documentLifecycle_pausedMediaPlayerResumes() {
        mProvider.onDocumentPaused();

        mProvider.onDocumentResumed();
        verify(playerOne).play();
        verify(playerTwo, never()).play();

        mProvider.updateMediaState(playerOne);
        mProvider.updateMediaState(playerTwo);
        
        assertTrue(mProvider.hasPlayingMediaPlayer());
    }

    @Test
    public void test_releasePlayers() {
        mProvider.releasePlayers();

        verify(playerOne).release();
        verify(playerTwo).release();
        assertFalse(mProvider.hasPlayingMediaPlayer());
    }

    @Test
    public void test_documentLifecycle_release() {
        mProvider.onDocumentFinish();
        verify(mProvider).releasePlayers();
    }
}
