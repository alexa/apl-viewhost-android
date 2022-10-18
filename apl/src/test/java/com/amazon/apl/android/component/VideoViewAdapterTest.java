/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.content.Context;
import android.view.View;

import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.android.Video;
import com.amazon.apl.android.dependencies.IMediaPlayer;
import com.amazon.apl.android.media.MediaPlayer;
import com.amazon.apl.android.primitive.MediaSources;
import com.amazon.apl.android.providers.AbstractMediaPlayerProvider;
import com.amazon.apl.enums.AudioTrack;
import com.amazon.apl.enums.PropertyKey;
import com.amazon.apl.enums.VideoScale;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class VideoViewAdapterTest extends AbstractComponentViewAdapterTest<Video, View> {

    @Mock
    Video mComponent;
    @Mock
    private IMediaPlayer mMockMediaPlayer;
    @Mock
    private MediaPlayer mMockNativePlayer;
    @Mock
    private MediaSources mMockMediaSources;
    @Mock
    private AbstractMediaPlayerProvider<View> mMediaPlayerProvider;
    @Mock
    private RenderingContext mMockRenderingContextMediaPlayerV2Enabled;
    @Mock
    private RenderingContext mMockRenderingContextMediaPlayerV2Disabled;

    @Before
    public void setup() throws Exception {
        when(mMockPresenter.getMediaPlayerProvider())
                .thenReturn(mMediaPlayerProvider);
        when(mMockRenderingContextMediaPlayerV2Enabled.isMediaPlayerV2Enabled()).thenReturn(true);
        when(mMockRenderingContextMediaPlayerV2Disabled.isMediaPlayerV2Enabled()).thenReturn(false);
        when(component().getRenderingContext()).thenReturn(mMockRenderingContextMediaPlayerV2Disabled);
        when(component().getMediaPlayerProvider())
                .thenReturn(mMediaPlayerProvider);
        when(component().getNativePlayer()).thenReturn(mMockNativePlayer);
        when(mMediaPlayerProvider.createView(any(Context.class)))
                .thenReturn(new View(RuntimeEnvironment.systemContext));
        when(mMediaPlayerProvider.getNewPlayer(any(Context.class), any(View.class)))
                .thenReturn(mMockMediaPlayer);
        super.setup();
    }

    @Override
    Video component() {
        return mComponent;
    }

    @Override
    void componentSetup() {
        when(component().getTrackIndex()).thenReturn(8);
        when(component().getCurrentTrackTime()).thenReturn(180);

        when(component().getAudioTrack()).thenReturn(AudioTrack.kAudioTrackBackground);
        when(component().getVideoScale()).thenReturn(VideoScale.kVideoScaleBestFill);

        when(component().getMediaPlayer()).thenReturn(mMockMediaPlayer);
        when(component().getMediaSources()).thenReturn(mMockMediaSources);
        when(component().shouldMute()).thenReturn(false);
    }

    @Test
    public void test_applySource_replacesMediaPlayer() {
        VideoViewAdapter adapter = VideoViewAdapter.getInstance();
        adapter.refreshProperties(component(), getView(), Arrays.asList(PropertyKey.kPropertySource));

        verify(mMediaPlayerProvider).getNewPlayer(any(Context.class), any(View.class));
        verify(mComponent).setMediaPlayer(any(IMediaPlayer.class));
    }

    @Test
    public void test_applyAllProperties_isMediaPlayerV2Disabled() {
        when(component().getNativePlayer()).thenReturn(null);
        when(component().shouldAutoPlay()).thenReturn(true);

        VideoViewAdapter adapter = VideoViewAdapter.getInstance();
        adapter.applyAllProperties(component(), getView());

        verify(mComponent).setMediaPlayer(any(IMediaPlayer.class));
        verify(mMockMediaPlayer).setMediaSources(any(MediaSources.class));
        verify(mMockMediaPlayer).setAudioTrack(eq(AudioTrack.kAudioTrackBackground));
        verify(mMockMediaPlayer).setVideoScale(VideoScale.kVideoScaleBestFill);
        verify(mMockMediaPlayer).setTrack(8);
        verify(mMockMediaPlayer).seek(180);
        verify(mMockMediaPlayer).play();
        verify(mMockMediaPlayer).unmute();
    }

    @Test
    public void test_applyAllProperties_isMediaPlayerV2Enabled() {
        when(component().getRenderingContext()).thenReturn(mMockRenderingContextMediaPlayerV2Enabled);
        when(component().shouldAutoPlay()).thenReturn(true);
        when(component().getMediaPlayer()).thenReturn(mMockMediaPlayer);

        VideoViewAdapter adapter = VideoViewAdapter.getInstance();
        adapter.applyAllProperties(component(), getView());

        verify(mMockMediaPlayer).setVideoScale(VideoScale.kVideoScaleBestFill);
        verify(mMockMediaPlayer).setTrack(8);
        verify(mMockMediaPlayer).seek(180);
        verifyNoMoreInteractions(mMockMediaPlayer);
    }

    @Test
    public void test_setVideo_mediaPlayerSetBeforeAddingListener() {
        VideoViewAdapter adapter = VideoViewAdapter.getInstance();
        adapter.applyAllProperties(component(), getView());

        InOrder inOrder = inOrder(mMockMediaPlayer, mComponent);
        inOrder.verify(mComponent).setMediaPlayer(mMockMediaPlayer);
        inOrder.verify(mMockMediaPlayer).addMediaStateListener(mComponent);
    }

    @Test
    public void test_applyAllProperties_muted_is_true() {
        when(component().shouldAutoPlay()).thenReturn(true);
        when(component().shouldMute()).thenReturn(true);

        VideoViewAdapter adapter = VideoViewAdapter.getInstance();
        adapter.applyAllProperties(component(), getView());
        verify(mMockMediaPlayer).mute();
    }

    @Test
    public void test_refreshProperties_muted() {
        when(component().shouldAutoPlay()).thenReturn(true);
        VideoViewAdapter adapter = VideoViewAdapter.getInstance();
        adapter.applyAllProperties(component(), getView());
        // By default the muted property is false.
        verify(mMockMediaPlayer).unmute();
        reset(mMockMediaPlayer);
        // Mute the audio
        when(component().shouldMute()).thenReturn(true);
        refreshProperties(PropertyKey.kPropertyMuted);
        verify(mMockMediaPlayer).mute();
        // Unmute the audio
        when(component().shouldMute()).thenReturn(false);
        refreshProperties(PropertyKey.kPropertyMuted);
        verify(mMockMediaPlayer).unmute();
    }

    /**
     * Test to make sure dynamic property updates are not processed when MediaPlayerV2 is enabled.
     */
    @Test
    public void test_refreshProperties_mediaPlayerV2Enabled_does_not_call_mediaPlayer() {
        when(component().getRenderingContext()).thenReturn(mMockRenderingContextMediaPlayerV2Enabled);
        when(component().shouldAutoPlay()).thenReturn(true);
        VideoViewAdapter adapter = VideoViewAdapter.getInstance();
        adapter.applyAllProperties(component(), getView());
        // Verify initial interactions with MediaPlayer while the component is inflated.
        verify(mMockMediaPlayer).setVideoScale(component().getVideoScale());
        // By default the muted property is false.
        reset(mMockMediaPlayer);
        // Mute the audio
        when(component().shouldMute()).thenReturn(true);
        refreshProperties(PropertyKey.kPropertyMuted);
        // Unmute the audio
        when(component().shouldMute()).thenReturn(false);
        refreshProperties(PropertyKey.kPropertyMuted);
        refreshProperties(PropertyKey.kPropertySource);
        // Verify no interactions with MediaPlayer from adapter during dynamic property updates.
        verifyZeroInteractions(mMockMediaPlayer);
    }

    @Test
    public void testApplyView_back_navigation_revives_mediaPlayer() {
        // Set the mediaPlayer in RELEASED state
        when(mMockMediaPlayer.getCurrentMediaState()).thenReturn(IMediaPlayer.IMediaListener.MediaState.RELEASED);
        when(mMockNativePlayer.getMediaPlayer()).thenReturn(mMockMediaPlayer);
        when(mMockNativePlayer.getCurrentTrackIndex()).thenReturn(2);
        when(mMockNativePlayer.getCurrentSeekPosition()).thenReturn(200);
        when(component().getRenderingContext()).thenReturn(mMockRenderingContextMediaPlayerV2Enabled);
        when(component().shouldAutoPlay()).thenReturn(true);

        VideoViewAdapter adapter = VideoViewAdapter.getInstance();
        adapter.applyAllProperties(component(), getView());

        InOrder inOrder = Mockito.inOrder(mMockMediaPlayer, mMockNativePlayer, mMediaPlayerProvider);
        inOrder.verify(mMockMediaPlayer).release();
        inOrder.verify(mMockNativePlayer).getCurrentTrackIndex();
        inOrder.verify(mMockNativePlayer).getCurrentSeekPosition();
        inOrder.verify(mMockMediaPlayer).setMediaSources(mMockMediaSources);
        inOrder.verify(mMockMediaPlayer).unmute();
        inOrder.verify(mMockMediaPlayer).setAudioTrack(AudioTrack.kAudioTrackBackground);
        inOrder.verify(mMockMediaPlayer).setVideoScale(VideoScale.kVideoScaleBestFill);
        inOrder.verify(mMockMediaPlayer).setTrack(2);
        inOrder.verify(mMockMediaPlayer).seek(200);
        inOrder.verify(mMockMediaPlayer).play();
        verifyNoMoreInteractions(mMockMediaPlayer);
    }

    @Test
    public void test_mediaCommandsOrdering_isMediaPlayerV2Enabled() {
        when(component().getMediaPlayer()).thenReturn(mMockMediaPlayer);
        when(component().getRenderingContext()).thenReturn(mMockRenderingContextMediaPlayerV2Enabled);
        when(component().shouldAutoPlay()).thenReturn(true);

        VideoViewAdapter adapter = VideoViewAdapter.getInstance();
        adapter.applyAllProperties(component(), getView());

        // verify seek before setMediaPlayer call
        InOrder inOrder = inOrder(mMockMediaPlayer, mMockNativePlayer);
        inOrder.verify(mMockMediaPlayer).setVideoScale(VideoScale.kVideoScaleBestFill);
        inOrder.verify(mMockMediaPlayer).setTrack(8);
        inOrder.verify(mMockMediaPlayer).seek(180);
        inOrder.verify(mMockNativePlayer).setMediaPlayer(mMockMediaPlayer);
        verifyNoMoreInteractions(mMockMediaPlayer);
    }
}
