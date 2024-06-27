/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.content.Context;
import android.view.View;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.Video;
import com.amazon.apl.android.dependencies.IMediaPlayer;
import com.amazon.apl.android.media.RuntimeMediaPlayerFactory;
import com.amazon.apl.android.media.TextTrack;
import com.amazon.apl.android.primitive.MediaSources;
import com.amazon.apl.android.providers.AbstractMediaPlayerProvider;
import com.amazon.apl.android.providers.impl.MediaPlayerProvider;
import com.amazon.apl.android.providers.impl.NoOpMediaPlayerProvider;
import com.amazon.apl.android.scenegraph.APLLayer;
import com.amazon.apl.android.scenegraph.APLScenegraph;
import com.amazon.apl.android.scenegraph.media.APLVideoLayer;
import com.amazon.apl.android.sgcontent.Node;
import com.amazon.apl.android.sgcontent.VideoNode;
import com.amazon.apl.enums.AudioTrack;
import com.amazon.apl.enums.ComponentType;
import com.amazon.apl.enums.RootProperty;
import com.amazon.apl.enums.TextTrackType;
import com.amazon.apl.enums.VideoScale;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static com.amazon.apl.enums.AudioTrack.kAudioTrackBackground;
import static com.amazon.apl.enums.AudioTrack.kAudioTrackForeground;
import static com.amazon.apl.enums.ComponentType.kComponentTypeContainer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class VideoTest extends AbstractComponentUnitTest<View, Video> {
    private static final String DUMMY_URL =
            "http://videotest.invalid/video-sample.mp4";
    private static final String DUMMY_DESCRIPTION = "Testing video component";
    private static final String DUMMY_TEXT_TRACK_URL = "https://videotest.invalid/video-capion.srt";
    private static final String DUMMY_TEXT_TRACK_DESCRIPTION = "Testing textrack in video component";
    private static final String DUMMY_TEXT_TRACK_TYPE = "caption";

    @Override
    String getComponentType() {
        return "Video";
    }

    @Mock
    IMediaPlayer mockMediaPlayer;

    @Before
    public void doBefore() {
        mockMediaPlayer = mock(IMediaPlayer.class);
        AbstractMediaPlayerProvider mockMediaPlayerProvider = mock(AbstractMediaPlayerProvider.class);
        when(mockMediaPlayerProvider.getNewPlayer(any(Context.class), any(View.class))).thenReturn(mockMediaPlayer);
        mRootConfig = RootConfig.create("Unit Test", "1.0")
                .mediaPlayerFactory(new RuntimeMediaPlayerFactory(mockMediaPlayerProvider));
        REQUIRED_PROPERTIES = ""; // no required properties in Video Component.
        OPTIONAL_PROPERTIES =
                "      \"audioTrack\": \"background\",\n" +
                        "      \"autoplay\": true,\n" +
                        "      \"scale\": \"best-fill\",\n" +
                        "      \"source\": [\n" +
                        "        {\n" +
                        "          \"url\": \"" + DUMMY_URL + "\",\n" +
                        "          \"description\": \"" + DUMMY_DESCRIPTION + "\",\n" +
                        "          \"textTrack\": [\n" +
                        "          {\n" +
                        "          \"url\": \"" + DUMMY_TEXT_TRACK_URL + "\",\n" +
                        "          \"description\": \"" + DUMMY_TEXT_TRACK_DESCRIPTION + "\",\n" +
                        "          \"type\": \"" + DUMMY_TEXT_TRACK_TYPE + "\"\n" +
                        "          }\n" +
                        "          ],\n" +
                        "          \"duration\": 30000,\n" +
                        "          \"repeatCount\": 5,\n" +
                        "          \"offset\": 3000\n" +
                        "        }\n" +
                        "      ]";
    }

    @Override
    void testProperties_required(Video component) {
        assertEquals(ComponentType.kComponentTypeVideo, component.getComponentType());
    }

    @Override
    void testProperties_optionalDefaultValues(Video component) {
        assertEquals(kAudioTrackForeground, component.getAudioTrack());
        assertFalse(component.shouldAutoPlay());
        assertEquals(VideoScale.kVideoScaleBestFit, component.getVideoScale());
        assertEquals(0, component.getMediaSources().size());
        assertEquals(0, component.getTrackIndex());
        assertEquals(0, component.getCurrentTrackTime());
    }

    @Override
    void testProperties_optionalExplicitValues(Video component) {
        assertEquals(AudioTrack.kAudioTrackBackground, component.getAudioTrack());
        assertTrue(component.shouldAutoPlay());
        assertEquals(VideoScale.kVideoScaleBestFill, component.getVideoScale());
        MediaSources sources = MediaSources.create();
        List<TextTrack> testTrack = new ArrayList<TextTrack>();
        TextTrack track = new TextTrack(TextTrackType.kTextTrackTypeCaption.getIndex(), DUMMY_TEXT_TRACK_URL, DUMMY_TEXT_TRACK_DESCRIPTION);
        testTrack.add(track);
        sources.add(MediaSources.MediaSource.builder()
                .url(DUMMY_URL)
                .duration(30000)
                .repeatCount(5)
                .offset(3000)
                .headers(new HashMap<>())
                .textTracks(testTrack)
                .build());
        assertEquals(1, sources.size());
        assertEquals(DUMMY_URL, sources.at(0).url());
        assertEquals(DUMMY_TEXT_TRACK_URL, sources.at(0).textTracks().get(0).getUrl());
        assertEquals(30000, sources.at(0).duration());
        assertEquals(5, sources.at(0).repeatCount());
        assertEquals(3000, sources.at(0).offset());
        assertEquals(0, component.getTrackIndex());
        assertEquals(0, component.getCurrentTrackTime());
    }

    @Test
    public void testVideo_initialPropertiesApplied() {
        //Mock dependencies
        AbstractMediaPlayerProvider mockMediaPlayerProvider = mock(AbstractMediaPlayerProvider.class);
        when(mockMediaPlayerProvider.getNewPlayer(any(Context.class), any(View.class))).thenReturn(mockMediaPlayer);
        APLOptions options = APLOptions.builder()
                .mediaPlayerProvider(mockMediaPlayerProvider)
                .build();
        // Inflate document
        inflateDocument(buildDocument(OPTIONAL_PROPERTIES), null, options);
        Video video = spy(getTestComponent());

        // Verify initial interactions
        verify(mockMediaPlayer, never()).setTrack(any(int.class));
        verify(mockMediaPlayer, never()).seek(any(int.class));

        // Setup expectations
        doReturn(1).when(video).getTrackIndex();
        doReturn(500).when(video).getCurrentTrackTime();

        View view = new View(mContext);
        // Run inflate
        VideoViewAdapter.getInstance().applyAllProperties(video, view);

        // Verify interactions
        verify(mockMediaPlayer).setAudioTrack(kAudioTrackBackground);
        verify(mockMediaPlayer).setVideoScale(VideoScale.kVideoScaleBestFill);
        verify(mockMediaPlayer).setTrack(1);
        verify(mockMediaPlayer).seek(500);
        verify(mockMediaPlayer).play();
    }

    static String MEDIA_SOURCE = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.1\"," +
            "  \"mainTemplate\": {" +
            "    \"parameters\": [" +
            "      \"payload\"" +
            "    ]," +
            "    \"item\": {" +
            "      \"type\": \"Container\"," +
            "      \"items\": [" +
            "        {" +
            "          \"type\": \"Video\"," +
            "          \"id\": \"testcomp\"," +
            "          \"source\": \"${payload.movie.properties.single}\"" +
            "        }," +
            "        {" +
            "          \"type\": \"Video\"," +
            "          \"source\": [" +
            "            \"${payload.movie.properties.single}\"" +
            "          ]" +
            "        }," +
            "        {" +
            "          \"type\": \"Video\"," +
            "          \"source\": {" +
            "            \"url\": \"${payload.movie.properties.single}\"" +
            "          }" +
            "        }," +
            "        {" +
            "          \"type\": \"Video\"," +
            "          \"source\": [" +
            "            {" +
            "              \"url\": \"${payload.movie.properties.single}\"" +
            "            }" +
            "          ]" +
            "        }" +
            "      ]" +
            "    }" +
            "  }" +
            "}";

    static final String MEDIA_SOURCE_DATA = "{" +
            "  \"movie\": {" +
            "    \"properties\": {" +
            "      \"single\": \"URL1\"" +
            "    }" +
            "  }" +
            "}";

    @Test
    public void testMedia_Source() {
        inflateDocument(MEDIA_SOURCE, MEDIA_SOURCE_DATA);

        Component container = mRootContext.getTopComponent();
        assertEquals(kComponentTypeContainer, container.getComponentType());
        assertEquals(4, container.getChildCount());
        for (int i = 0; i < container.getChildCount(); i++) {
            Component child = container.getChildAt(i);
            assertEquals(ComponentType.kComponentTypeVideo, child.getComponentType());
            ((Video) child).getNativePlayer().setMediaPlayer(mockMediaPlayer);
        }
        ArgumentCaptor<MediaSources> argumentCaptor = ArgumentCaptor.forClass(MediaSources.class);
        verify(mockMediaPlayer, times(container.getChildCount())).setMediaSources(argumentCaptor.capture());
        assertEquals("URL1", argumentCaptor.getValue().at(0).url());
    }

    @Test
    public void testGetMediaSources() {
        String doc = buildDocument(REQUIRED_PROPERTIES, OPTIONAL_PROPERTIES, OPTIONAL_TEMPLATE_PROPERTIES);
        inflateDocument(doc);
        Video component = getTestComponent();
        MediaSources mediaSources = component.getMediaSources();
        assertEquals(DUMMY_TEXT_TRACK_URL, mediaSources.at(0).textTracks().get(0).getUrl());
        assertEquals(DUMMY_TEXT_TRACK_DESCRIPTION, mediaSources.at(0).textTracks().get(0).getDescription());
        assertEquals(TextTrackType.kTextTrackTypeCaption, mediaSources.at(0).textTracks().get(0).getType());
    }

    @Test
    public void testMediaPlayerProvider() {
        mRootConfig = mRootConfig.mediaPlayerFactory(new RuntimeMediaPlayerFactory(new MediaPlayerProvider()));
        inflateDocument(MEDIA_SOURCE, MEDIA_SOURCE_DATA);
        AbstractMediaPlayerProvider mediaPlayerProvider = getTestComponent().getMediaPlayerProvider();
        assertTrue(mediaPlayerProvider instanceof MediaPlayerProvider);
    }

    @Test
    public void testAPLVideoLayer() {
        String doc = buildDocument(REQUIRED_PROPERTIES, OPTIONAL_PROPERTIES, OPTIONAL_TEMPLATE_PROPERTIES);
        inflateDocument(doc);
        if (!mAplOptions.isScenegraphEnabled()) {
            return;
        }
        APLScenegraph aplScenegraph = new APLScenegraph(mRootContext);
        APLLayer aplLayer = APLLayer.ensure(aplScenegraph.getTop(), mRenderingContext);
        assertTrue(aplLayer instanceof APLVideoLayer);
        assertEquals(1, aplLayer.getContent().length);
        assertEquals("Video", aplLayer.getContent()[0].getType());
        Node node = aplLayer.getContent()[0];
        assertTrue(node instanceof VideoNode);
        VideoNode videoNode = (VideoNode) node;
        assertEquals(VideoScale.kVideoScaleBestFill, videoNode.getVideoScale());
        assertNotNull(videoNode.getMediaPlayer());
    }
}