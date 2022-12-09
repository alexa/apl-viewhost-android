/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.command;

import android.content.Context;
import android.view.View;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.Action;
import com.amazon.apl.android.Event;
import com.amazon.apl.android.Video;
import com.amazon.apl.android.dependencies.IMediaPlayer;
import com.amazon.apl.android.document.AbstractDocUnitTest;
import com.amazon.apl.android.events.PlayMediaEvent;
import com.amazon.apl.android.providers.AbstractMediaPlayerProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.amazon.apl.android.dependencies.IMediaPlayer.IMediaListener.MediaState.PLAYING;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class PlayMediaTest extends AbstractDocUnitTest {

    private static final String DOC = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"1.0\",\n" +
            "  \"theme\": \"auto\",\n" +
            "  \"mainTemplate\": {\n" +
            "    \"items\": [\n" +
            "      {\n" +
            "        \"type\": \"Container\",\n" +
            "        \"direction\": \"column\",\n" +
            "        \"height\": \"100vh\",\n" +
            "        \"width\": \"100vw\",\n" +
            "        \"items\": [\n" +
            "          {\n" +
            "            \"type\": \"Video\",\n" +
            "            \"height\": \"50%\",\n" +
            "            \"width\": \"50%\",\n" +
            "            \"align\": \"right\",\n" +
            "            \"id\": \"myVideoPlayer\",\n" +
            "            \"source\": [\n" +
            "              {\n" +
            "                \"description\": \"The first video clip to play\",\n" +
            "                \"repeatCount\": 0,\n" +
            "                \"url\": \"source-url\"\n" +
            "              }\n" +
            "            ]\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";

    private String PLAY_MEDIA_COMMAND = "[\n" +
            "  {\n" +
            "    \"type\": \"PlayMedia\",\n" +
            "    \"componentId\": \"myVideoPlayer\",\n" +
            "    \"audioTrack\": \"%s\",\n" +
            "    \"source\": \"source-url\"\n" +
            "  }\n" +
            "]";

    @Mock private IMediaPlayer mMockPlayer;
    @Mock private View mMockView;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(mMockPlayer.getCurrentMediaState()).thenReturn(PLAYING);
        loadDocument(DOC, APLOptions.builder()
                            .mediaPlayerProvider(new MediaPlayerProvider())
                .visualContextListener(mMockVisualContextListener)
                .build());
        Video video = (Video) mRootContext.findComponentById("myVideoPlayer");
        assertNotNull(video);
    }

    @Test
    public void testFeature_PlayMedia_audio_foreground() {
        AtomicBoolean completed = new AtomicBoolean(false);
        doAnswer(answer -> {
            long nativeHandle = answer.getArgument(0);
            PlayMediaEvent event = spy(PlayMediaEvent.create(nativeHandle, mRootContext));

            doAnswer(executeAnswer -> {
                Event mockedEvent = (Event)executeAnswer.getMock();
                mockedEvent.resolve();
                return null;
            }).when(event).execute();

            doAnswer(executeAnswer -> null).when((Event)event).terminate();
            return event;

        }).when(mRootContext).buildEvent(anyLong(), anyInt());


        Action action = mRootContext.executeCommands(String.format(PLAY_MEDIA_COMMAND, "foreground"));
        action.then(() -> completed.set(true));

        update(100); // executes play media command
        assertTrue(completed.get());
    }

    @Test
    public void testFeature_PlayMedia_audio_background() {
        AtomicBoolean completed = new AtomicBoolean(false);
        doAnswer(answer -> {
            long nativeHandle = answer.getArgument(0);
            PlayMediaEvent event = spy(PlayMediaEvent.create(nativeHandle, mRootContext));

            doAnswer(executeAnswer -> {
                Event mockedEvent = (Event)executeAnswer.getMock();
                mockedEvent.resolve();
                return null;
            }).when(event).execute();

            doAnswer(executeAnswer -> null).when((Event)event).terminate();
            return event;

        }).when(mRootContext).buildEvent(anyLong(), anyInt());

        Action action = mRootContext.executeCommands(String.format(PLAY_MEDIA_COMMAND, "background"));
        action.then(() -> completed.set(true));

        update(100); // executes play media command
        assertTrue(completed.get());
    }

    @Test
    public void testFeature_PlayMedia_audio_none() {
        AtomicBoolean completed = new AtomicBoolean(false);
        doAnswer(answer -> {
            long nativeHandle = answer.getArgument(0);
            PlayMediaEvent event = spy(PlayMediaEvent.create(nativeHandle, mRootContext));

            doAnswer(executeAnswer -> {
                Event mockedEvent = (Event)executeAnswer.getMock();
                mockedEvent.resolve();
                return null;
            }).when(event).execute();

            doAnswer(executeAnswer -> null).when((Event)event).terminate();
            return event;

        }).when(mRootContext).buildEvent(anyLong(), anyInt());

        Action action = mRootContext.executeCommands(String.format(PLAY_MEDIA_COMMAND, "none"));
        action.then(() -> completed.set(true));

        update(100); // executes play media command
        assertTrue(completed.get());
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
