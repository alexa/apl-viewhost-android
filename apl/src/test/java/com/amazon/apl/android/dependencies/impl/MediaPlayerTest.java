/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies.impl;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer.OnPreparedListener;
import android.util.Log;
import android.view.TextureView;

import com.amazon.apl.android.dependencies.IMediaPlayer;
import com.amazon.apl.android.dependencies.IMediaPlayer.IMediaListener.MediaState;
import com.amazon.apl.android.media.TextTrack;
import com.amazon.apl.android.primitive.MediaSources;
import com.amazon.apl.enums.AudioTrack;
import com.amazon.apl.enums.VideoScale;
import com.amazon.apl.enums.TextTrackType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowAudioManager;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowMediaPlayer;
import org.robolectric.shadows.ShadowMediaPlayer.MediaInfo;
import org.robolectric.shadows.ShadowMediaPlayer.State;
import org.robolectric.shadows.util.DataSource;
import org.robolectric.util.Scheduler;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.List;

import static com.amazon.apl.android.dependencies.IMediaPlayer.IMediaListener.MediaState.END;
import static com.amazon.apl.android.dependencies.IMediaPlayer.IMediaListener.MediaState.IDLE;
import static com.amazon.apl.android.dependencies.IMediaPlayer.IMediaListener.MediaState.PAUSED;
import static com.amazon.apl.android.dependencies.IMediaPlayer.IMediaListener.MediaState.PLAYING;
import static com.amazon.apl.android.dependencies.IMediaPlayer.IMediaListener.MediaState.PREPARING;
import static com.amazon.apl.android.dependencies.IMediaPlayer.IMediaListener.MediaState.READY;
import static com.amazon.apl.android.dependencies.IMediaPlayer.IMediaListener.MediaState.RELEASED;
import static com.amazon.apl.android.dependencies.IMediaPlayer.IMediaListener.MediaState.TRACK_UPDATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.shadows.util.DataSource.toDataSource;

import androidx.test.core.app.ApplicationProvider;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 22)
public class MediaPlayerTest {
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
    private TextureView mTextureView;
    private AudioManager mAudioManager;
    @Mock
    private PlaybackListener mListener;
    @Mock
    private Context mContext;
    @Mock
    private AssetManager mAssetManager;
    @Mock
    private AssetFileDescriptor mAssetFileDescriptor;
    @Mock
    private FileDescriptor mFileDescriptor;

    @Before
    public void setup() throws IOException {
        MockitoAnnotations.openMocks(this);
        ShadowLog.stream = System.out;
        mTextureView = new TextureView(ApplicationProvider.getApplicationContext());
        SurfaceTexture mSurfaceTexture = new SurfaceTexture(0);
        mExpectedStates = new LinkedList<>();

        mAudioManager = Shadow.newInstanceOf(AudioManager.class);
        // TODO Handle audio manager
        ShadowAudioManager mShadowAudioManager = shadowOf(mAudioManager);

        when(mAssetFileDescriptor.getFileDescriptor()).thenReturn(mFileDescriptor);
        when(mAssetManager.openFd(anyString())).thenReturn(mAssetFileDescriptor);
        when(mContext.getAssets()).thenReturn(mAssetManager);

        android.media.MediaPlayer mMediaPlayer = Shadow.newInstanceOf(android.media.MediaPlayer.class);
        mShadowMediaPlayer = shadowOf(mMediaPlayer);
        mAplMediaPlayer = new MediaPlayer(mMediaPlayer, mTextureView, mContext, mAudioManager);
        mAplMediaPlayer.setAudioTrack(AudioTrack.kAudioTrackForeground);
        mAplMediaPlayer.setVideoScale(VideoScale.kVideoScaleBestFit);
        mAplMediaPlayer.addMediaStateListener(mListener);

        MediaInfo mInfo = new MediaInfo(TRACK_TOTAL_DURATION_MS, PREPARATION_DELAY_MS);
        DataSource ds = toDataSource(VIDEO_URL);
        ShadowMediaPlayer.addMediaInfo(ds, mInfo);
        DataSource assetDs = toDataSource(mFileDescriptor, 0, 0);
        ShadowMediaPlayer.addMediaInfo(assetDs, mInfo);
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
    public void testOnPreparedListener() {
        android.media.MediaPlayer mMediaPlayer = spy(Shadow.newInstanceOf(android.media.MediaPlayer.class));
        IMediaPlayer aplMediaPlayer = new MediaPlayer(mMediaPlayer, mTextureView, mContext, mAudioManager);
        aplMediaPlayer.addMediaStateListener(mListener);
        assertEquals(IDLE, aplMediaPlayer.getCurrentMediaState());
        ArgumentCaptor<OnPreparedListener> captor = ArgumentCaptor.forClass(OnPreparedListener.class);
        verify(mMediaPlayer).setOnPreparedListener(captor.capture());
        OnPreparedListener onPreparedListener = captor.getValue();
        onPreparedListener.onPrepared(mMediaPlayer);

        // This triggers onTrackReady on APL document
        assertEquals(READY, aplMediaPlayer.getCurrentMediaState());
        verify(mListener).updateMediaState(aplMediaPlayer);
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
    public void testPlay_singleTrack_withHeaders() {
        Log.d(TAG, "Testing play_singleTrack");
        Map<String, String> headers = Collections.singletonMap("headerKey", "headerValue");
        setupMediaSources(1, 0, 0, 0, headers, null);
        initExpectedStatesForPlay();
        mAplMediaPlayer.setMediaSources(mMediaSources);
        testPlayInternal();
    }

    @Test
    public void testPlay_singleTrack_withTextTrack() {
        Log.d(TAG, "Testing play_singleTrack");
        List<TextTrack> textTracks = new ArrayList<TextTrack>();
        textTracks.add(new TextTrack(TextTrackType.kTextTrackTypeCaption.getIndex(), "file://intro.mp4", "intro caption"));
        setupMediaSources(1, 0, 0, 0, Collections.emptyMap(), textTracks);
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
    public void testSeekTo() {
        Log.d(TAG, "Testing seekTo");
        setupMediaSources(2, 0, 0, 0);
        Collections.addAll(mExpectedStates, PLAYING, TRACK_UPDATE, END, IDLE, RELEASED);
        mAplMediaPlayer.setMediaSources(mMediaSources);
        checkState(State.IDLE, IDLE);

        mAplMediaPlayer.seekTo(0);
        checkState(State.PREPARING, PREPARING);
        mScheduler.advanceBy(PREPARATION_DELAY_MS, TimeUnit.MILLISECONDS);
        checkState(State.STARTED, PLAYING);

        mAplMediaPlayer.seekTo(TRACK_HALF_DURATION_MS);
        checkState(State.STARTED, PLAYING);
        mScheduler.advanceBy(TRACK_HALF_DURATION_MS + PREPARATION_DELAY_MS, TimeUnit.MILLISECONDS);
        checkState(State.STARTED, PLAYING);

        // try to seek further than the track duration, this should
        mAplMediaPlayer.seekTo(TRACK_TOTAL_DURATION_MS * 2);
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
    public void testMute() {
        android.media.MediaPlayer mediaPlayer = mock(android.media.MediaPlayer.class);
        MediaPlayer aplMediaPlayer = new MediaPlayer(mediaPlayer, mTextureView, mContext, mAudioManager);
        clearInvocations(mediaPlayer);
        clearInvocations(mListener);
        aplMediaPlayer.setAudioTrack(AudioTrack.kAudioTrackForeground);
        aplMediaPlayer.addMediaStateListener(mListener);
        aplMediaPlayer.mute();
        verify(mediaPlayer).setVolume(0.0f, 0.0f);
        assertTrue(aplMediaPlayer.isMuted());

        // Test mute again is a no-op
        aplMediaPlayer.mute();
        verifyNoMoreInteractions(mediaPlayer);
        verifyNoMoreInteractions(mListener);
        assertTrue(aplMediaPlayer.isMuted());
    }

    @Test
    public void testUnmute() {
        android.media.MediaPlayer mediaPlayer = mock(android.media.MediaPlayer.class);
        MediaPlayer aplMediaPlayer = new MediaPlayer(mediaPlayer, mTextureView, mContext, mAudioManager);
        clearInvocations(mediaPlayer);
        clearInvocations(mListener);
        aplMediaPlayer.setAudioTrack(AudioTrack.kAudioTrackForeground);
        aplMediaPlayer.addMediaStateListener(mListener);
        aplMediaPlayer.unmute();
        // By default the MediaPlayer is unmuted. So call to unmute() is a no-op
        verifyNoInteractions(mediaPlayer);
        verifyNoInteractions(mListener);
        assertFalse(aplMediaPlayer.isMuted());
        // Mute the MediaPlayer
        aplMediaPlayer.mute();
        verify(mediaPlayer).setVolume(0.0f, 0.0f);
        assertTrue(aplMediaPlayer.isMuted());
        clearInvocations(mListener);
        // Unmute the MediaPlayer
        aplMediaPlayer.unmute();
        verify(mediaPlayer).setVolume(1.0f, 1.0f);
        assertFalse(aplMediaPlayer.isMuted());
    }

    @Test
    public void testUnmute_when_attenuated_sets_volume_to_attenuated_level() {
        // Setup MediaPlayer
        android.media.MediaPlayer mediaPlayer = mock(android.media.MediaPlayer.class);
        MediaPlayer aplMediaPlayer = new MediaPlayer(mediaPlayer, mTextureView, mContext, mAudioManager);
        aplMediaPlayer.setAudioTrack(AudioTrack.kAudioTrackBackground);
        aplMediaPlayer.addMediaStateListener(mListener);
        aplMediaPlayer.mute();
        clearInvocations(mediaPlayer);
        clearInvocations(mListener);
        // Simulate focus loss or attenuation
        aplMediaPlayer.getAudioFocusChangeListener().onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK);
        verify(mediaPlayer).setVolume(0f, 0f);
        // Unmute while attenuated
        aplMediaPlayer.unmute();
        verify(mediaPlayer).setVolume(0.25f, 0.25f);
        // Simulate focus gain or no attenuation
        aplMediaPlayer.getAudioFocusChangeListener().onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN);
        verify(mediaPlayer).setVolume(1f, 1f);
        clearInvocations(mediaPlayer);
        clearInvocations(mListener);
        // Mute and unmute while not attenuated
        aplMediaPlayer.mute();
        verify(mediaPlayer).setVolume(0f, 0f);
        aplMediaPlayer.unmute();
        verify(mediaPlayer).setVolume(1f, 1f);
    }

    @Test
    public void testFocusChange() {
        // Setup MediaPlayer
        android.media.MediaPlayer mediaPlayer = mock(android.media.MediaPlayer.class);
        MediaPlayer aplMediaPlayer = new MediaPlayer(mediaPlayer, mTextureView, mContext, mAudioManager);
        aplMediaPlayer.setAudioTrack(AudioTrack.kAudioTrackBackground);
        aplMediaPlayer.addMediaStateListener(mListener);
        aplMediaPlayer.mute();
        clearInvocations(mediaPlayer);
        clearInvocations(mListener);
        // Verify that audio focus changes do not update volume, when player is muted.
        aplMediaPlayer.getAudioFocusChangeListener().onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN);
        verify(mediaPlayer).setVolume(0f, 0f);
        clearInvocations(mediaPlayer);
        aplMediaPlayer.getAudioFocusChangeListener().onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK);
        verify(mediaPlayer).setVolume(0f, 0f);
        aplMediaPlayer.unmute();
        clearInvocations(mediaPlayer);
        clearInvocations(mListener);
        // Verify that audio focus changes update volume, when player is unmuted.
        aplMediaPlayer.getAudioFocusChangeListener().onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN);
        verify(mediaPlayer).setVolume(1f, 1f);
        aplMediaPlayer.getAudioFocusChangeListener().onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK);
        verify(mediaPlayer).setVolume(0.25f, 0.25f);
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

    @Test
    public void testPlay_localAsset() throws IOException {
        mMediaSources = mock(MediaSources.class);
        MediaSources.MediaSource mediaSource = mock(MediaSources.MediaSource.class);
        when(mediaSource.url()).thenReturn("file:///android_asset/sample.mp4");
        when(mediaSource.headers()).thenReturn(Collections.emptyMap());

        when(mMediaSources.size()).thenReturn(1);
        when(mMediaSources.at(anyInt())).thenReturn(mediaSource);

        initExpectedStatesForPlay();
        mAplMediaPlayer.setMediaSources(mMediaSources);
        testPlayInternal();

        verify(mAssetManager).openFd("sample.mp4");
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
        setupMediaSources(count, duration, repeatCount, offset, Collections.emptyMap(), null);
    }

    private void setupMediaSources(int count, int duration, int repeatCount, int offset, Map<String, String> headers, List<TextTrack> textTracks) {
        mMediaSources = mock(MediaSources.class);
        MediaSources.MediaSource mediaSource = mock(MediaSources.MediaSource.class);
        when(mediaSource.url()).thenReturn(VIDEO_URL);
        when(mediaSource.duration()).thenReturn(duration);
        when(mediaSource.repeatCount()).thenReturn(repeatCount);
        when(mediaSource.offset()).thenReturn(offset);
        when(mediaSource.headers()).thenReturn(headers);
        when(mediaSource.textTracks()).thenReturn(textTracks);

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
