/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.scenegraph.media;

import static com.amazon.apl.enums.TextTrackType.kTextTrackTypeCaption;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.amazon.apl.android.dependencies.IMediaPlayer;
import com.amazon.apl.android.dependencies.IMediaPlayer.IMediaListener.MediaState;
import com.amazon.apl.android.media.MediaPlayer;
import com.amazon.apl.android.media.MediaTrack;
import com.amazon.apl.android.media.TextTrack;
import com.amazon.apl.android.primitive.MediaSources;
import com.amazon.apl.enums.AudioTrack;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 22, manifest = Config.NONE)
public class MediaPlayerTest {
    private MediaPlayer mMediaPlayer;

    @Before
    public void setup() {
        mMediaPlayer = new MediaPlayer(0);
    }

    @Test
    public void testSetAudioTrack() {
        mMediaPlayer.setAudioTrack(AudioTrack.kAudioTrackBackground.getIndex());
        assertEquals(AudioTrack.kAudioTrackBackground, mMediaPlayer.getCurrentAudioTrack());
    }

    @Test
    public void testSetMute() {
        mMediaPlayer.setMute(true);
        assertTrue(mMediaPlayer.isCurrentlyMuted());
    }

    @Test
    public void testSetTrackList() {
        final String url = "dummy://url";
        final String[] headers = new String[] {"method:get"};
        final int offset = 0;
        final int duration = 10000;
        final int repeatCount = -1;
        final TextTrack[] textTracks = new TextTrack[]{new TextTrack(kTextTrackTypeCaption.getIndex(), "dummy://captionUrl", "dummyDescription")};
        MediaTrack mediaTrack = new MediaTrack(url, headers, offset, duration, repeatCount, textTracks);
        List<MediaTrack> mediaTracks = new ArrayList<>();
        mediaTracks.add(mediaTrack);
        mMediaPlayer.setTrackList(mediaTracks);
        MediaSources mediaSources = mMediaPlayer.getMediaSources();
        assertEquals(url, mediaSources.at(0).url());
        assertEquals(1, mediaSources.size());
        assertEquals(offset, mediaSources.at(0).offset());
        assertEquals(duration, mediaSources.at(0).duration());
        assertEquals(repeatCount, mediaSources.at(0).repeatCount());
    }

    @Test
    public void testUpdateMediaState() {
        // Setup mediaPlayer
        IMediaPlayer mockPlayer = Mockito.mock(IMediaPlayer.class);
        when(mockPlayer.getCurrentMediaState()).thenReturn(MediaState.READY);
        when(mockPlayer.getCurrentTrackIndex()).thenReturn(1);
        when(mockPlayer.getCurrentSeekPosition()).thenReturn(1000);

        // Test a few state transitions
        mMediaPlayer.updateMediaState(mockPlayer);
        assertFalse(mMediaPlayer.isCurrentlyPlaying());
        // Start playing
        when(mockPlayer.getCurrentMediaState()).thenReturn(MediaState.PLAYING);
        mMediaPlayer.updateMediaState(mockPlayer);
        assertTrue(mMediaPlayer.isCurrentlyPlaying());
        // Advance 1000 ms
        when(mockPlayer.getCurrentSeekPosition()).thenReturn(1000);
        mMediaPlayer.updateMediaState(mockPlayer);
        assertEquals(1, mMediaPlayer.getCurrentTrackIndex());
        assertEquals(1000, mMediaPlayer.getCurrentSeekPosition());
        // Move to next track
        when(mockPlayer.getCurrentMediaState()).thenReturn(MediaState.TRACK_UPDATE);
        when(mockPlayer.getCurrentTrackIndex()).thenReturn(2);
        when(mockPlayer.getCurrentSeekPosition()).thenReturn(0);
        mMediaPlayer.updateMediaState(mockPlayer);
        assertEquals(2, mMediaPlayer.getCurrentTrackIndex());
        assertEquals(0, mMediaPlayer.getCurrentSeekPosition());
        // Pause
        when(mockPlayer.getCurrentMediaState()).thenReturn(MediaState.PAUSED);
        assertFalse(mMediaPlayer.isCurrentlyPlaying());
        // Play and advance 2000 ms
        when(mockPlayer.getCurrentMediaState()).thenReturn(MediaState.PLAYING);
        when(mockPlayer.getCurrentSeekPosition()).thenReturn(2000);
        mMediaPlayer.updateMediaState(mockPlayer);
        assertEquals(2, mMediaPlayer.getCurrentTrackIndex());
        assertEquals(2000, mMediaPlayer.getCurrentSeekPosition());
        // End playback
        when(mockPlayer.getCurrentMediaState()).thenReturn(MediaState.END);
        mMediaPlayer.updateMediaState(mockPlayer);
        assertFalse(mMediaPlayer.isCurrentlyPlaying());
    }

    @Test
    public void testUpdateMediaState_does_not_crash_when_mediaPlayer_is_null() {
        mMediaPlayer.updateMediaState(null);
    }
}
