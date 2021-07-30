/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.content.Context;
import android.view.View;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.Video;
import com.amazon.apl.android.dependencies.IMediaPlayer;
import com.amazon.apl.android.primitive.MediaSources;
import com.amazon.apl.android.providers.AbstractMediaPlayerProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.amazon.apl.android.espresso.APLViewActions.executeCommands;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class VideoViewTest extends AbstractComponentViewTest<View, Video> {
    private static final String DUMMY_URL =
            "http://mirrors.standaloneinstaller.com/video-sample/DLP_PART_2_768k.mp4";
    private static final String DUMMY_DESCRIPTION = "Testing video component";

    private static final String[] SOURCES = {
            "http://mirrors.standaloneinstaller.com/video-sample/DLP_PART_2_768k.mp4",
            "https://delivery.vidible.tv/video/redirect/5b634e2dbf488517fc64fbb4.mp4?bcid=5593271909eab110d8f43789&w=852&h=480&enc=mp4&domain=cdn.vidible.tv",
    };

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


    /**
     * @return The string representation used in the APL document for this component.  For example
     * when testing the Text Component this method should return "Text".
     */
    @Override
    String getComponentType() {
        return "Video";
    }

    @Override
    Class<View> getViewClass() {
        return View.class;
    }

    @Override
    void testView_applyProperties(View view) {
        assertEquals(100, view.getWidth());
        assertEquals(100, view.getHeight());
    }

    @Test
    public void testView_dynamicSource() {
        //Mock dependencies
        IMediaPlayer mockMediaPlayer = mock(IMediaPlayer.class);
        ArgumentCaptor<MediaSources> mediaPlayerCaptor = ArgumentCaptor.forClass(MediaSources.class);
        AbstractMediaPlayerProvider<View> playerProvider = new AbstractMediaPlayerProvider<View>() {
            @Override
            public View createView(Context context) {
                return new View(context);
            }

            @Override
            public IMediaPlayer createPlayer(Context context, View view) {
                return mockMediaPlayer;
            }
        };

        APLOptions options = APLOptions.builder()
                .mediaPlayerProvider(playerProvider)
                .build();

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflateWithOptions(options, REQUIRED_PROPERTIES, CHILD_LAYOUT_PROPERTIES))
                .check(hasRootContext());

        for (String expectedSource : SOURCES) {
            // SetValue
            onView(isRoot())
                    .perform(executeCommands(mTestContext.getRootContext(), setValueCommand("source", expectedSource)));
            // Capture the MediaSource array
            verify(mockMediaPlayer, atLeastOnce()).setMediaSources(mediaPlayerCaptor.capture());
            final MediaSources mediaSourcesCapture = mediaPlayerCaptor.getValue();

            // Check state
            assertEquals(1, mediaSourcesCapture.size());
            assertEquals(expectedSource, mediaSourcesCapture.at(0).url());
        }
    }
}
