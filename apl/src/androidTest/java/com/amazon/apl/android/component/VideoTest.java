/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.content.Context;
import android.view.View;

import androidx.test.filters.SmallTest;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.Video;
import com.amazon.apl.android.dependencies.IMediaPlayer;
import com.amazon.apl.android.dependencies.impl.NoOpMediaPlayer;
import com.amazon.apl.android.primitive.MediaSources;
import com.amazon.apl.android.providers.AbstractMediaPlayerProvider;
import com.amazon.apl.android.providers.impl.MediaPlayerProvider;
import com.amazon.apl.android.providers.impl.NoOpMediaPlayerProvider;
import com.amazon.apl.enums.AudioTrack;
import com.amazon.apl.enums.ComponentType;
import com.amazon.apl.enums.RootProperty;
import com.amazon.apl.enums.VideoScale;

import org.junit.Before;
import org.junit.Test;

import static com.amazon.apl.enums.ComponentType.kComponentTypeContainer;
import static com.amazon.apl.enums.ComponentType.kComponentTypeVideo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VideoTest extends AbstractComponentUnitTest<View, Video> {
    private static final String DUMMY_URL =
            "http://videotest.invalid/video-sample.mp4";
    private static final String DUMMY_DESCRIPTION = "Testing video component";

    @Override
    String getComponentType() {
        return "Video";
    }

    @Before
    public void doBefore() {
        REQUIRED_PROPERTIES = ""; // no required properties in Video Component.
        OPTIONAL_PROPERTIES =
                "      \"audioTrack\": \"background\",\n" +
                        "      \"autoplay\": true,\n" +
                        "      \"scale\": \"best-fill\",\n" +
                        "      \"source\": [\n" +
                        "        {\n" +
                        "          \"url\": \"" + DUMMY_URL + "\",\n" +
                        "          \"description\": \"" + DUMMY_DESCRIPTION + "\",\n" +
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
        assertEquals(AudioTrack.kAudioTrackForeground, component.getAudioTrack());
        assertFalse(component.shouldAutoPlay());
        assertEquals(VideoScale.kVideoScaleBestFit, component.getVideoScale());
        MediaSources sources = component.getMediaSources();
        assertEquals(0, sources.size());
        assertEquals(0, component.getTrackIndex());
        assertEquals(0, component.getCurrentTrackTime());
    }

    @Override
    void testProperties_optionalExplicitValues(Video component) {
        assertEquals(AudioTrack.kAudioTrackBackground, component.getAudioTrack());
        assertTrue(component.shouldAutoPlay());
        assertEquals(VideoScale.kVideoScaleBestFill, component.getVideoScale());
        MediaSources sources = component.getMediaSources();
        assertEquals(1, sources.size());
        assertEquals(DUMMY_URL, sources.at(0).url());
        assertEquals(30000, sources.at(0).duration());
        assertEquals(5, sources.at(0).repeatCount());
        assertEquals(3000, sources.at(0).offset());
        assertEquals(0, component.getTrackIndex());
        assertEquals(0, component.getCurrentTrackTime());
    }

    @Test
    public void testVideo_initialPropertiesApplied() {
        //Mock dependencies
        IMediaPlayer mockMediaPlayer = mock(IMediaPlayer.class);
        AbstractMediaPlayerProvider mockMediaPlayerProvider = mock(AbstractMediaPlayerProvider.class);
        when(mockMediaPlayerProvider.getNewPlayer(any(Context.class), any(View.class))).thenReturn(mockMediaPlayer);
        APLOptions options = APLOptions.builder()
                .mediaPlayerProvider(mockMediaPlayerProvider)
                .build();

        // Inflate document
        inflateDocument(buildDocument(), null, options);
        Video video = spy(getTestComponent());

        // Verify initial interactions
        verify(mockMediaPlayer, never()).setTrack(any(int.class));
        verify(mockMediaPlayer, never()).seek(any(int.class));

        // Setup expectations
        doReturn(1).when(video).getTrackIndex();
        doReturn(500).when(video).getCurrentTrackTime();

        // Run inflate
        VideoViewAdapter.getInstance().applyAllProperties(video, new View(mContext));

        // Verify interactions
        verify(mockMediaPlayer).setTrack(1);
        verify(mockMediaPlayer).seek(500);
        verify(mockMediaPlayer, never()).play();
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
    @SmallTest
    public void testMedia_Source() {
        inflateDocument(MEDIA_SOURCE, MEDIA_SOURCE_DATA);

        Component container = mRootContext.getTopComponent();
        assertEquals(kComponentTypeContainer, container.getComponentType());
        assertEquals(4, container.getChildCount());

        for (int i = 0; i < 4; i++) {

            Component child = container.getChildAt(i);

            assertEquals(kComponentTypeVideo, child.getComponentType());
            assertTrue(child instanceof Video);

            MediaSources sources = ((Video) child).getMediaSources();
            assertEquals(sources.size(), 1);

            assertEquals(sources.at(0).url(), "URL1");
        }
    }

    @Test
    @SmallTest
    public void testDisallowVideo_True() {
        mRootConfig = mRootConfig.set(RootProperty.kDisallowVideo, true);
        inflateDocument(MEDIA_SOURCE, MEDIA_SOURCE_DATA);
        AbstractMediaPlayerProvider mediaPlayerProvider = getTestComponent().getMediaPlayerProvider();
        assertTrue(mediaPlayerProvider instanceof NoOpMediaPlayerProvider);
    }

    @Test
    @SmallTest
    public void testDisallowVideo_False() {
        mRootConfig = mRootConfig.set(RootProperty.kDisallowVideo, false);
        inflateDocument(MEDIA_SOURCE, MEDIA_SOURCE_DATA);
        AbstractMediaPlayerProvider mediaPlayerProvider = getTestComponent().getMediaPlayerProvider();
        assertTrue(mediaPlayerProvider instanceof MediaPlayerProvider);
    }
}
