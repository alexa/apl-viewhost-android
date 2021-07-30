/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies.impl;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.util.Log;
import android.view.TextureView;

import com.amazon.apl.android.dependencies.IMediaPlayer;
import com.amazon.apl.android.dependencies.IMediaPlayer.IMediaListener.MediaState;
import com.amazon.apl.android.primitive.MediaSources;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.enums.AudioTrack;
import com.amazon.apl.enums.VideoScale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowAudioManager;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowMediaPlayer;
import org.robolectric.shadows.ShadowMediaPlayer.MediaInfo;
import org.robolectric.shadows.ShadowMediaPlayer.State;
import org.robolectric.shadows.util.DataSource;
import org.robolectric.util.Scheduler;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import static com.amazon.apl.android.dependencies.IMediaPlayer.IMediaListener.MediaState.END;
import static com.amazon.apl.android.dependencies.IMediaPlayer.IMediaListener.MediaState.IDLE;
import static com.amazon.apl.android.dependencies.IMediaPlayer.IMediaListener.MediaState.PAUSED;
import static com.amazon.apl.android.dependencies.IMediaPlayer.IMediaListener.MediaState.PLAYING;
import static com.amazon.apl.android.dependencies.IMediaPlayer.IMediaListener.MediaState.PREPARING;
import static com.amazon.apl.android.dependencies.IMediaPlayer.IMediaListener.MediaState.RELEASED;
import static com.amazon.apl.android.dependencies.IMediaPlayer.IMediaListener.MediaState.TRACK_UPDATE;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Robolectric.buildActivity;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.shadows.util.DataSource.toDataSource;

public class MediaPlayerTest extends ViewhostRobolectricTest {
    private static final String TAG = "MediaPlayerTest";
    private static final String VIDEO_URL = "dummy-url";
    private static final int PREPARATION_DELAY_MS = 3000; // 3 sec
    private static final int TRACK_TOTAL_DURATION_MS = 60000; // 60 sec
    private static final int TRACK_HALF_DURATION_MS = TRACK_TOTAL_DURATION_MS / 2; // 30 sec

    private ShadowMediaPlayer mShadowMediaPlayer;
    private MediaPlayer mAplMediaPlayer;
    private Scheduler mScheduler;
    private Queue<MediaState> mExpectedStates;
    private MediaSources mMediaSources;

    @Before
    public void setup() {
        Activity activity = buildActivity(Activity.class).create().get();
        ShadowLog.stream = System.out;
        TextureView mTextureView = new TextureView(activity);
        SurfaceTexture mSurfaceTexture = new SurfaceTexture(0);
        PlaybackListener mListener = new PlaybackListener();
        mExpectedStates = new LinkedList<>();

        AudioManager mAudioManager = Shadow.newInstanceOf(AudioManager.class);
        // TODO Handle audio manager
        ShadowAudioManager mShadowAudioManager = shadowOf(mAudioManager);

        android.media.MediaPlayer mMediaPlayer = Shadow.newInstanceOf(android.media.MediaPlayer.class);
        mShadowMediaPlayer = shadowOf(mMediaPlayer);
        MediaInfo mInfo = new MediaInfo(TRACK_TOTAL_DURATION_MS, PREPARATION_DELAY_MS);
        mAplMediaPlayer = new MediaPlayer(mMediaPlayer, mTextureView, mAudioManager);
        mAplMediaPlayer.setAudioTrack(AudioTrack.kAudioTrackForeground);
        mAplMediaPlayer.setVideoScale(VideoScale.kVideoScaleBestFit);
        mAplMediaPlayer.addMediaStateListener(mListener);

        DataSource ds = toDataSource(VIDEO_URL);
        ShadowMediaPlayer.addMediaInfo(ds, mInfo);
        mShadowMediaPlayer.doSetDataSource(ds);

        mScheduler = Robolectric.getForegroundThreadScheduler();
        mScheduler.pause();

        mTextureView.getSurfaceTextureListener().onSurfaceTextureAvailable(mSurfaceTexture, 0, 0);
    }

    @After
    public void cleanup() {
        mAplMediaPlayer.release();
        checkState(State.END, RELEASED);
    }

    @Test
    public void testPlay_singleTrack() {
        Log.d(TAG, "Testing play_singleTrack");
        setupMediaSources(1, 0, 0, 0);
        initExpectedStatesForPlay();
        mAplMediaPlayer.setMediaSources(mMediaSources);
        testPlayInternal();
    }

    @Test
    public void testPlay_singleTrack_withDuration() {
        Log.d(TAG, "Testing play_singleTrack_withDuration");
        setupMediaSources(1, TRACK_HALF_DURATION_MS, 0, 0);
        initExpectedStatesForPlay();
        mAplMediaPlayer.setMediaSources(mMediaSources);
        testPlayInternal();
    }

    @Test
    public void testPlay_singleTrack_withRepeatCount() {
        Log.d(TAG, "Testing play_singleTrack_withRepeatCount");
        setupMediaSources(1, 0, 1, 0);
        initExpectedStatesForPlay();
        mAplMediaPlayer.setMediaSources(mMediaSources);
        testPlayInternal();
    }

    @Test
    public void testPlay_singleTrack_withOffset() {
        Log.d(TAG, "Testing play_singleTrack");
        setupMediaSources(1, 0, 0, 3000);
        initExpectedStatesForPlay();
        mAplMediaPlayer.setMediaSources(mMediaSources);
        testPlayInternal();
    }

    @Test
    public void testPlay_multipleTracks() {
        Log.d(TAG, "Testing play_multipleTracks");
        setupMediaSources(10, 0, 0, 0);
        initExpectedStatesForPlay();
        mAplMediaPlayer.setMediaSources(mMediaSources);
        testPlayInternal();
    }

    @Test
    public void testPlay_multipleTracks_withDuration() {
        Log.d(TAG, "Testing play_multipleTracks_withDuration");
        setupMediaSources(10, TRACK_HALF_DURATION_MS, 0, 0);
        initExpectedStatesForPlay();
        mAplMediaPlayer.setMediaSources(mMediaSources);
        testPlayInternal();
    }

    @Test
    public void testPlay_multipleTracks_withRepeatCount() {
        Log.d(TAG, "Testing play_multipleTracks_withRepeatCount");
        setupMediaSources(10, 0, 10, 0);
        initExpectedStatesForPlay();
        mAplMediaPlayer.setMediaSources(mMediaSources);
        testPlayInternal();
    }

    @Test
    public void testPlay_multipleTracks_withOffset() {
        Log.d(TAG, "Testing play_multipleTracks");
        setupMediaSources(10, 0, 0, 0);
        initExpectedStatesForPlay();
        mAplMediaPlayer.setMediaSources(mMediaSources);
        testPlayInternal();
    }

    @Test
    public void testPause() {
        Log.d(TAG, "test pause");
        setupMediaSources(1, 0, 0, 0);
        mAplMediaPlayer.setMediaSources(mMediaSources);
        Collections.addAll(mExpectedStates, PLAYING, PAUSED, PLAYING, IDLE);
        checkState(State.IDLE, IDLE);

        mAplMediaPlayer.play();
        checkState(State.PREPARING, PREPARING);
        mScheduler.advanceBy(PREPARATION_DELAY_MS, TimeUnit.MILLISECONDS);
        checkState(State.STARTED, PLAYING);

        mAplMediaPlayer.pause();
        checkState(State.PAUSED, PAUSED);

        mAplMediaPlayer.play();
        checkState(State.STARTED, PLAYING);

        mAplMediaPlayer.stop();
        checkState(State.IDLE, IDLE);
    }

    @Test
    public void testNext() {
        Log.d(TAG, "test next");
        setupMediaSources(2, 0, 0, 0);
        mAplMediaPlayer.setMediaSources(mMediaSources);
        Collections.addAll(mExpectedStates, PLAYING, TRACK_UPDATE, PLAYING, IDLE, RELEASED);
        checkState(State.IDLE, IDLE);

        mAplMediaPlayer.play();
        checkState(State.PREPARING, PREPARING);
        mScheduler.advanceBy(PREPARATION_DELAY_MS, TimeUnit.MILLISECONDS);
        checkState(State.STARTED, PLAYING);

        mAplMediaPlayer.next();
        checkState(State.PREPARING, PREPARING);
        assertEquals(1, mAplMediaPlayer.getCurrentTrackIndex());

        mAplMediaPlayer.play();
        mScheduler.advanceBy(PREPARATION_DELAY_MS, TimeUnit.MILLISECONDS);
        checkState(State.STARTED, PLAYING);

        mAplMediaPlayer.next(); // this should be ignored
        checkState(State.STARTED, PLAYING);

        mAplMediaPlayer.stop();
        checkState(State.IDLE, IDLE);
    }

    @Test
    public void testPrevious() {
        Log.d(TAG, "test previous");
        setupMediaSources(2, 0, 0, 0);
        mAplMediaPlayer.setMediaSources(mMediaSources);
        Collections.addAll(mExpectedStates, PLAYING, TRACK_UPDATE, TRACK_UPDATE, PLAYING, IDLE, RELEASED);
        checkState(State.IDLE, IDLE);

        mAplMediaPlayer.play();
        checkState(State.PREPARING, PREPARING);
        mScheduler.advanceBy(PREPARATION_DELAY_MS, TimeUnit.MILLISECONDS);
        checkState(State.STARTED, PLAYING);
        mScheduler.advanceBy(TRACK_TOTAL_DURATION_MS + PREPARATION_DELAY_MS, TimeUnit.MILLISECONDS);

        mAplMediaPlayer.previous();
        checkState(State.PREPARING, PREPARING);
        assertEquals(0, mAplMediaPlayer.getCurrentTrackIndex());

        mAplMediaPlayer.play();
        mScheduler.advanceBy(PREPARATION_DELAY_MS, TimeUnit.MILLISECONDS);
        checkState(State.STARTED, PLAYING);

        mAplMediaPlayer.previous(); // this should be ignored
        checkState(State.STARTED, PLAYING);

        mAplMediaPlayer.stop();
        checkState(State.IDLE, IDLE);
    }

    @Test
    public void testSetTrack() {
        Log.d(TAG, "test setTrack");
        setupMediaSources(10, 0, 0, 0);
        mAplMediaPlayer.setMediaSources(mMediaSources);
        Collections.addAll(mExpectedStates, TRACK_UPDATE, PLAYING, TRACK_UPDATE, PLAYING, IDLE, RELEASED);
        checkState(State.IDLE, IDLE);
        mAplMediaPlayer.setTrack(5);
        mAplMediaPlayer.play();
        checkState(State.PREPARING, PREPARING);

        mScheduler.advanceBy(PREPARATION_DELAY_MS, TimeUnit.MILLISECONDS);
        checkState(State.STARTED, PLAYING);
        assertEquals(5, mAplMediaPlayer.getCurrentTrackIndex());

        mAplMediaPlayer.setTrack(9);
        mAplMediaPlayer.play();
        checkState(State.PREPARING, PREPARING);

        mScheduler.advanceBy(PREPARATION_DELAY_MS, TimeUnit.MILLISECONDS);
        checkState(State.STARTED, PLAYING);
        assertEquals(9, mAplMediaPlayer.getCurrentTrackIndex());

        mAplMediaPlayer.setTrack(20); // this should be ignored
        checkState(State.STARTED, PLAYING);

        mAplMediaPlayer.stop();
        checkState(State.IDLE, IDLE);
    }

    @Test
    public void testSeek() {
        Log.d(TAG, "Testing seek");
        setupMediaSources(2, 0, 0, 0);
        Collections.addAll(mExpectedStates, PLAYING, TRACK_UPDATE, END, IDLE, RELEASED);
        mAplMediaPlayer.setMediaSources(mMediaSources);
        checkState(State.IDLE, IDLE);

        mAplMediaPlayer.seek(0);
        checkState(State.PREPARING, PREPARING);
        mScheduler.advanceBy(PREPARATION_DELAY_MS, TimeUnit.MILLISECONDS);
        checkState(State.STARTED, PLAYING);

        mAplMediaPlayer.seek(TRACK_HALF_DURATION_MS);
        checkState(State.STARTED, PLAYING);
        mScheduler.advanceBy(TRACK_HALF_DURATION_MS + PREPARATION_DELAY_MS, TimeUnit.MILLISECONDS);
        checkState(State.STARTED, PLAYING);

        // try to seek further than the track duration, this should
        mAplMediaPlayer.seek(TRACK_TOTAL_DURATION_MS * 2);
        checkState(State.STARTED, PLAYING);
        mScheduler.advanceBy(TRACK_TOTAL_DURATION_MS, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testRewind() {
        Log.d(TAG, "Testing rewind");
        setupMediaSources(2, 0, 0, 0);
        Collections.addAll(mExpectedStates, PLAYING, IDLE, RELEASED);
        mAplMediaPlayer.setMediaSources(mMediaSources);
        checkState(State.IDLE, IDLE);

        mAplMediaPlayer.rewind();
        checkState(State.IDLE, IDLE);
        assertEquals(0, mAplMediaPlayer.getCurrentSeekPosition());
        assertEquals(0, mAplMediaPlayer.getCurrentTrackIndex());

        mAplMediaPlayer.play();
        checkState(State.PREPARING, PREPARING);
        mScheduler.advanceBy(PREPARATION_DELAY_MS, TimeUnit.MILLISECONDS);
        checkState(State.STARTED, PLAYING);

        mAplMediaPlayer.rewind();
        checkState(State.STARTED, PLAYING);
    }

    @Test
    public void test_No_op() {
        mAplMediaPlayer.pause();
        checkState(State.IDLE, IDLE);

        mAplMediaPlayer.next();
        checkState(State.IDLE, IDLE);

        mAplMediaPlayer.previous();
        checkState(State.IDLE, IDLE);

        mAplMediaPlayer.seek(1000);
        checkState(State.IDLE, IDLE);

        mAplMediaPlayer.rewind();
        checkState(State.IDLE, IDLE);

        mAplMediaPlayer.stop();
        checkState(State.IDLE, IDLE);

        mAplMediaPlayer.setTrack(5);
        checkState(State.IDLE, IDLE);

        mExpectedStates.add(RELEASED);
        mAplMediaPlayer.release();
        checkState(State.END, RELEASED);

        assertEquals(0, mAplMediaPlayer.getDuration());
        checkState(State.END, RELEASED);
    }

    /**
     * Covers all the testPlay_XXXX scenarios defined in this class.
     */
    private void testPlayInternal() {
        checkState(State.IDLE, IDLE);
        mAplMediaPlayer.play();
        checkState(State.PREPARING, PREPARING);

        for (int trackIndex = 0; trackIndex < mMediaSources.size(); ++trackIndex) { // loop through all the sources
            MediaSources.MediaSource mediaSource = mMediaSources.at(trackIndex);
            int duration = mediaSource.duration() <= 0 ? TRACK_TOTAL_DURATION_MS :
                    mediaSource.duration();
            mScheduler.advanceBy(PREPARATION_DELAY_MS, TimeUnit.MILLISECONDS);
            assertEquals(duration, mAplMediaPlayer.getDuration());

            for (int repeatCount = 0; repeatCount <= mediaSource.repeatCount(); ++repeatCount) { // loop through all the repeatCount of a source
                checkState(State.STARTED, PLAYING);
                assertEquals(trackIndex, mAplMediaPlayer.getCurrentTrackIndex());
                mScheduler.advanceBy(duration, TimeUnit.MILLISECONDS);
            }
        }
        checkState(State.IDLE, IDLE);
    }

    private void checkState(State shadowState, MediaState playerState) {
        assertEquals(shadowState, mShadowMediaPlayer.getState());
        assertEquals(playerState, mAplMediaPlayer.getCurrentMediaState());
    }

    private void setupMediaSources(int count, int duration, int repeatCount, int offset) {
        mMediaSources = mock(MediaSources.class);
        MediaSources.MediaSource mediaSource = mock(MediaSources.MediaSource.class);
        when(mediaSource.url()).thenReturn(VIDEO_URL);
        when(mediaSource.duration()).thenReturn(duration);
        when(mediaSource.repeatCount()).thenReturn(repeatCount);
        when(mediaSource.offset()).thenReturn(offset);
        
        when(mMediaSources.size()).thenReturn(count);
        when(mMediaSources.at(anyInt())).thenReturn(mediaSource);
    }

    private void initExpectedStatesForPlay() {
        mExpectedStates.add(PLAYING);
        // there won't be a TRACK_UPDATE event for a single track
        if (mMediaSources.size() > 0) {
            mExpectedStates.addAll(Collections.nCopies(mMediaSources.size() - 1, TRACK_UPDATE));
        }
        Collections.addAll(mExpectedStates, END, IDLE, RELEASED);
    }

    class PlaybackListener implements IMediaPlayer.IMediaListener {

        @Override
        public void updateMediaState(IMediaPlayer player) {
        }
    }
}
