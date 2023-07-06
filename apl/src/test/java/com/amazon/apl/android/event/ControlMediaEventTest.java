/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.event;

import android.content.Context;
import android.view.View;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.Component;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.Video;
import com.amazon.apl.android.component.VideoViewAdapter;
import com.amazon.apl.android.dependencies.IMediaPlayer;
import com.amazon.apl.android.document.AbstractDocUnitTest;
import com.amazon.apl.android.media.RuntimeMediaPlayerFactory;
import com.amazon.apl.android.providers.AbstractMediaPlayerProvider;
import com.amazon.apl.android.views.APLAbsoluteLayout;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ControlMediaEventTest extends AbstractDocUnitTest {

    // Test content
    private static final String DOC = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"1.0\",\n" +
            "  \"theme\": \"auto\",\n" +
            "  \"mainTemplate\": {\n" +
            "    \"items\": [\n" +
            "      {\n" +
            "        \"type\": \"Video\",\n" +
            "        \"height\": \"100%\",\n" +
            "        \"width\": \"100%\",\n" +
            "        \"id\": \"VideoPlayer\",\n" +
            "        \"source\": [\n" +
            "          {\n" +
            "            \"description\": \"The first video clip to play\",\n" +
            "            \"repeatCount\": 0,\n" +
            "            \"url\": \"dummy-url-1\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"description\": \"The second video clip to play\",\n" +
            "            \"url\": \"dummy-url-2\",\n" +
            "            \"repeatCount\": -1\n" +
            "          },\n" +
            "          {\n" +
            "            \"description\": \"This video clip will only be reached by a command\",\n" +
            "            \"url\": \"dummy-url-3\",\n" +
            "            \"repeatCount\": 2\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";
    private static final String CONTROL_MEDIA_COMMAND = "[{\n" +
            "  \"type\": \"ControlMedia\",\n" +
            "  \"componentId\": \"VideoPlayer\",\n" +
            "  \"command\": \"%s\",\n" +
            "  \"value\": %d\n" +
            "}]";

    private static final String SET_MUTE = "[{\n" +
            "  \"type\": \"SetValue\",\n" +
            "  \"componentId\": \"VideoPlayer\",\n" +
            "  \"property\": \"muted\",\n" +
            "  \"value\": %b\n" +
            "}]";

    @Mock private IMediaPlayer mMockPlayer;
    @Mock private View mMockView;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mRootConfig = RootConfig.create("Unit Test", "1.0")
                .allowOpenUrl(true)
                .mediaPlayerFactory(new RuntimeMediaPlayerFactory(new MediaPlayerProvider()));
        MockitoAnnotations.initMocks(this);


        loadDocument(DOC, APLOptions.builder()
                .mediaPlayerProvider(new MediaPlayerProvider())
                .build());
        Video video = (Video) mRootContext.findComponentById("VideoPlayer");
        VideoViewAdapter.getInstance().applyAllProperties(video, mMockView);
        when(mAPLPresenter.findView(video)).thenReturn(mMockView);

        assertNotNull(video);
        assertNotNull(mMockPlayer);
    }

    @Test
    public void testCommands_play() {
        mRootContext.executeCommands(getCommand("play", 0));
        update(100); // executes play media command
        verify(mMockPlayer).play();
    }

    @Test
    public void testCommands_pause() {
        mRootContext.executeCommands(getCommand("pause", 0));
        update(100); // executes play media command
        verify(mMockPlayer).pause();
    }

    @Test
    public void testCommands_next() {
        mRootContext.executeCommands(getCommand("next", 0));
        update(100); // executes play media command
        verify(mMockPlayer).next();
    }

    @Test
    public void testCommands_previous() {
        mRootContext.executeCommands(getCommand("previous", 0));
        update(100); // executes play media command
        verify(mMockPlayer).previous();
    }

    @Test
    public void testCommands_rewind() {
        mRootContext.executeCommands(getCommand("rewind", 0));
        update(100); // executes play media command
        verify(mMockPlayer).rewind();
    }

    @Test
    public void testCommands_seek() {
        mRootContext.executeCommands(getCommand("seek", 500));
        update(100); // executes play media command
        verify(mMockPlayer).seek(500);
    }

    @Test
    public void testCommands_seekTo() {
        mRootContext.executeCommands(getCommand("seekTo", 500));
        update(100); // executes play media command
        verify(mMockPlayer).seekTo(500);
    }

    @Test
    public void testCommands_setTrack() {
        mRootContext.executeCommands(getCommand("setTrack", 1));
        update(100); // executes play media command
        verify(mMockPlayer).setTrack(1);
    }

    @Test
    public void testCommands_mute() {
        mRootContext.executeCommands(String.format(SET_MUTE, "mute", true));
        update(100); // executes SetValue command to change muted property of Video to true
        verify(mMockPlayer).mute();
    }

    @Test
    public void testCommands_unmute() {
        mRootContext.executeCommands(String.format(SET_MUTE, "mute", false));
        update(100); // executes SetValue command to change muted property of Video to false
        verify(mMockPlayer).unmute();
    }

    @Test
    public void testCommands_setTrack_invalid() {
        mRootContext.executeCommands(getCommand("setTrack", 10)); // invalid track
        update(100); // executes play media command
        verify(mMockPlayer, never()).setTrack(10);
    }



    private static String getCommand(final String command, final int value) {
        return String.format(CONTROL_MEDIA_COMMAND, command, value);
    }

    private class MediaPlayerProvider extends AbstractMediaPlayerProvider<View> {

        @Override
        public IMediaPlayer createPlayer(Context context, View view) {
            return mMockPlayer;
        }

        @Override
        public View createView(Context context) {
            return mMockView;
        }

    }
}
