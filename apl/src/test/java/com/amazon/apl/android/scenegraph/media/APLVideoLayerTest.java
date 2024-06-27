/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.scenegraph.media;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;

import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.android.dependencies.IMediaPlayer;
import com.amazon.apl.android.media.MediaPlayer;
import com.amazon.apl.android.primitive.MediaSources;
import com.amazon.apl.android.providers.AbstractMediaPlayerProvider;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.android.scenegraph.accessibility.Accessibility;
import com.amazon.apl.android.sgcontent.Node;
import com.amazon.apl.android.sgcontent.VideoNode;
import com.amazon.apl.android.views.APLView;
import com.amazon.apl.enums.AudioTrack;
import com.amazon.apl.enums.VideoScale;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.util.List;

public class APLVideoLayerTest extends ViewhostRobolectricTest {

    private APLVideoLayer mAPLVideoLayer;

    @Mock
    private RenderingContext mMockRenderingContext;
    @Mock
    private IMediaPlayer mMockMediaPlayer;
    @Mock
    private MediaPlayer mMockNativePlayer;
    @Mock
    private MediaSources mMockMediaSources;
    @Mock
    private AbstractMediaPlayerProvider<View> mMockMediaPlayerProvider;
    @Mock
    private View mMockVideoView;
    @Mock
    private VideoNode mMockNode;
    @Mock
    private APLView mMockAPLView;
    @Mock
    private Accessibility mockAccessibility;

    private static final VideoScale TEST_VIDEO_SCALE = VideoScale.kVideoScaleBestFit;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(mMockNode.getType()).thenReturn("Video");
        when(mMockVideoView.getContext()).thenReturn(ApplicationProvider.getApplicationContext());
        when(mMockAPLView.getContext()).thenReturn(ApplicationProvider.getApplicationContext());
        when(mMockMediaPlayerProvider.createView(any(Context.class))).thenReturn(mMockVideoView);
        when(mMockMediaPlayerProvider.getNewPlayer(any(Context.class), any(View.class))).thenReturn(mMockMediaPlayer);
        when(mMockRenderingContext.getMediaPlayerProvider()).thenReturn(mMockMediaPlayerProvider);
        when(mMockNode.getVideoScale()).thenReturn(TEST_VIDEO_SCALE);
        mAPLVideoLayer = spy(new APLVideoLayer(mMockRenderingContext));
        doReturn(new Node[]{mMockNode}).when(mAPLVideoLayer).getContent();
        when(mockAccessibility.getAccessibilityLabel()).thenReturn(null);
        doReturn(mockAccessibility).when(mAPLVideoLayer).getAccessibility();
    }

    @Test
    public void testAttachView_no_crash_when_nativePlayer_is_null() {
        mAPLVideoLayer.applyNodeProperties(mMockAPLView);
        List<ShadowLog.LogItem> logs = ShadowLog.getLogsForTag("APLVideoLayer");
        ShadowLog.LogItem lastLog = logs.get(logs.size() - 1);
        assertEquals("Native player not initialized, won't render video", lastLog.msg);
        assertEquals("APLVideoLayer", lastLog.tag);
    }

    @Test
    public void testAttachView_normal_rendering_when_nativePlayer_is_not_null() {
        when(mMockNode.getMediaPlayer()).thenReturn(mMockNativePlayer);
        mAPLVideoLayer.applyNodeProperties(mMockAPLView);
        verify(mMockMediaPlayer).setVideoScale(TEST_VIDEO_SCALE);
        verify(mMockNativePlayer).setMediaPlayer(mMockMediaPlayer);
    }

    @Test
    public void testAttachView_rendering_in_back_navigation_previously_not_playing_not_muted() {
        when(mMockNode.getMediaPlayer()).thenReturn(mMockNativePlayer);
        // Setup the native player with a state
        when(mMockNativePlayer.getMediaPlayer()).thenReturn(mMockMediaPlayer);
        when(mMockNativePlayer.getCurrentTrackIndex()).thenReturn(1);
        when(mMockNativePlayer.getCurrentSeekPosition()).thenReturn(1000);
        when(mMockNativePlayer.isCurrentlyMuted()).thenReturn(false);
        when(mMockNativePlayer.isCurrentlyPlaying()).thenReturn(false);
        when(mMockNativePlayer.getCurrentAudioTrack()).thenReturn(AudioTrack.kAudioTrackNone);
        when(mMockNativePlayer.getMediaSources()).thenReturn(mMockMediaSources);
        when(mMockNode.getVideoScale()).thenReturn(VideoScale.kVideoScaleBestFill);
        // Act
        mAPLVideoLayer.applyNodeProperties(mMockAPLView);
        // Verify
        InOrder inOrder = Mockito.inOrder(mMockMediaPlayer, mMockMediaPlayerProvider, mMockNativePlayer);
        inOrder.verify(mMockMediaPlayer).release();
        inOrder.verify(mMockMediaPlayerProvider).getNewPlayer(mMockVideoView.getContext(), mMockVideoView);
        inOrder.verify(mMockNativePlayer).setMediaPlayer(mMockMediaPlayer);
        inOrder.verify(mMockMediaPlayer).setMediaSources(mMockMediaSources);
        inOrder.verify(mMockMediaPlayer).setAudioTrack(AudioTrack.kAudioTrackNone);
        inOrder.verify(mMockMediaPlayer).setVideoScale(VideoScale.kVideoScaleBestFill);
        inOrder.verify(mMockMediaPlayer).setTrack(1);
        inOrder.verify(mMockMediaPlayer).seek(1000);
    }

    @Test
    public void testAttachView_rendering_in_back_navigation_previously_playing_muted() {
        when(mMockNode.getMediaPlayer()).thenReturn(mMockNativePlayer);
        // Setup the native player with a state
        when(mMockNativePlayer.getMediaPlayer()).thenReturn(mMockMediaPlayer);
        when(mMockNativePlayer.getCurrentTrackIndex()).thenReturn(1);
        when(mMockNativePlayer.getCurrentSeekPosition()).thenReturn(1000);
        when(mMockNativePlayer.isCurrentlyMuted()).thenReturn(true);
        when(mMockNativePlayer.isCurrentlyPlaying()).thenReturn(true);
        when(mMockNativePlayer.getCurrentAudioTrack()).thenReturn(AudioTrack.kAudioTrackNone);
        when(mMockNativePlayer.getMediaSources()).thenReturn(mMockMediaSources);
        when(mMockNode.getVideoScale()).thenReturn(VideoScale.kVideoScaleBestFill);
        // Act
        mAPLVideoLayer.applyNodeProperties(mMockAPLView);
        // Verify
        InOrder inOrder = Mockito.inOrder(mMockMediaPlayer, mMockMediaPlayerProvider, mMockNativePlayer);
        inOrder.verify(mMockMediaPlayer).release();
        inOrder.verify(mMockMediaPlayerProvider).getNewPlayer(mMockVideoView.getContext(), mMockVideoView);
        inOrder.verify(mMockNativePlayer).setMediaPlayer(mMockMediaPlayer);
        inOrder.verify(mMockMediaPlayer).setMediaSources(mMockMediaSources);
        inOrder.verify(mMockMediaPlayer).mute();
        inOrder.verify(mMockMediaPlayer).setAudioTrack(AudioTrack.kAudioTrackNone);
        inOrder.verify(mMockMediaPlayer).setVideoScale(VideoScale.kVideoScaleBestFill);
        inOrder.verify(mMockMediaPlayer).setTrack(1);
        inOrder.verify(mMockMediaPlayer).seek(1000);
        inOrder.verify(mMockMediaPlayer).play();
    }
}
