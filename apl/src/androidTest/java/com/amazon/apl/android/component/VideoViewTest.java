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
import com.amazon.apl.android.configuration.ConfigurationChange;
import com.amazon.apl.android.dependencies.IMediaPlayer;
import com.amazon.apl.android.media.MediaPlayer;
import com.amazon.apl.android.media.RuntimeMediaPlayerFactory;
import com.amazon.apl.android.primitive.MediaSources;
import com.amazon.apl.android.providers.AbstractMediaPlayerProvider;
import com.amazon.apl.enums.AudioTrack;
import com.amazon.apl.enums.VideoScale;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.amazon.apl.android.espresso.APLViewActions.executeCommands;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class VideoViewTest extends AbstractComponentViewTest<View, Video> {
    private static final String DUMMY_URL =
            "http://videoviewtest.invalid/video-sample.mp4";
    private static final String DUMMY_DESCRIPTION = "Testing video component";

    private static final String[] SOURCES = {
            "http://videoviewtest.invalid/video-sample.mp4",
            "http://videoviewtest.invalid/video-sample2.mp4",
    };

    @Before
    public void doBefore() {
        REQUIRED_PROPERTIES = ""; // no required properties in Video Component.
        OPTIONAL_PROPERTIES =
                "      \"audioTrack\": \"background\",\n" +
                        "      \"autoplay\": true,\n" +
                        "      \"scale\": \"best-fill\",\n" +
                        "      \"preserve\": [\"playingState\"],\n" +
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

        APLOptions options = APLOptions.builder().build();

        RootConfig rootConfig = RootConfig.create("Unit Test", "1.0")
                .mediaPlayerFactory(new RuntimeMediaPlayerFactory(playerProvider));

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflateWithOptions(rootConfig, options, REQUIRED_PROPERTIES, CHILD_LAYOUT_PROPERTIES))
                .check(hasRootContext());

        ArgumentCaptor<IMediaPlayer.IMediaListener> mediaStateListenerCaptor = ArgumentCaptor.forClass(IMediaPlayer.IMediaListener.class);
        verify(mockMediaPlayer, times(2)).addMediaStateListener(mediaStateListenerCaptor.capture());
        verify(mockMediaPlayer).setVideoScale(VideoScale.kVideoScaleBestFit);
        verify(mockMediaPlayer).setMediaSources(mediaPlayerCaptor.capture());
        reset(mockMediaPlayer);

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
        assertEquals(2, mediaStateListenerCaptor.getAllValues().size());
        assertThat(mediaStateListenerCaptor.getAllValues().get(0), instanceOf(AbstractMediaPlayerProvider.class));
        assertThat(mediaStateListenerCaptor.getAllValues().get(1), instanceOf(MediaPlayer.class));
    }

    @Test
    public void testView_updateMediaState() {
        //Mock dependencies
        FakeMediaPlayer fakeMediaPlayer = new FakeMediaPlayer();
        AbstractMediaPlayerProvider<View> playerProvider = new AbstractMediaPlayerProvider<View>() {
            @Override
            public View createView(Context context) {
                return new View(context);
            }

            @Override
            public IMediaPlayer createPlayer(Context context, View view) {
                return fakeMediaPlayer;
            }
        };

        APLOptions options = APLOptions.builder().build();

        RootConfig rootConfig = RootConfig.create("Unit Test", "1.0")
                .mediaPlayerFactory(new RuntimeMediaPlayerFactory(playerProvider));

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflateWithOptions(rootConfig, options, REQUIRED_PROPERTIES, OPTIONAL_PROPERTIES))
                .check(hasRootContext());

        fakeMediaPlayer.setTrack(-1);
        fakeMediaPlayer.seek(-1);
        fakeMediaPlayer.notifyMediaState();
        // Execute SetValue command so that visual context is marked dirty
        onView(isRoot())
                .perform(executeCommands(mTestContext.getRootContext(), setValueCommand("source", "someNewSource")));
        Component component = mTestContext.getTestComponent();
        onView(withComponent(component))
                .check(matches(isDisplayed()));

        fakeMediaPlayer.setTrack(0);
        fakeMediaPlayer.seek(0);
        fakeMediaPlayer.notifyMediaState();

        // Execute SetValue command so that visual context is marked dirty
        onView(isRoot())
                .perform(executeCommands(mTestContext.getRootContext(), setValueCommand("source", "someNewSource")));
        component = mTestContext.getTestComponent();
        onView(withComponent(component))
                .check(matches(isDisplayed()));
    }

    @Test
    public void test_reinflation_preserves_playingState() {
        //Mock dependencies
        IMediaPlayer fakeMediaPlayer = mock(IMediaPlayer.class);
        AbstractMediaPlayerProvider<View> playerProvider = new AbstractMediaPlayerProvider<View>() {
            @Override
            public View createView(Context context) {
                return new View(context);
            }

            @Override
            public IMediaPlayer createPlayer(Context context, View view) {
                return fakeMediaPlayer;
            }
        };

        APLOptions options = APLOptions.builder().build();

        RootConfig rootConfig = RootConfig.create("Unit Test", "1.0")
                .mediaPlayerFactory(new RuntimeMediaPlayerFactory(playerProvider));

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflateWithOptions(rootConfig, options, REQUIRED_PROPERTIES, OPTIONAL_PROPERTIES))
                .check(hasRootContext());

        ArgumentCaptor<IMediaPlayer.IMediaListener> listenerCaptor = ArgumentCaptor.forClass(IMediaPlayer.IMediaListener.class);
        verify(fakeMediaPlayer, atLeastOnce()).addMediaStateListener(listenerCaptor.capture());
        IMediaPlayer.IMediaListener listener = listenerCaptor.getValue();

        doReturn(0)
                .when(fakeMediaPlayer).getCurrentTrackIndex();

        doReturn(200)
                .when(fakeMediaPlayer).getCurrentSeekPosition();

        doReturn(IMediaPlayer.IMediaListener.MediaState.PLAYING)
                .when(fakeMediaPlayer).getCurrentMediaState();

        // Snapshot the media state.
        listener.updateMediaState(fakeMediaPlayer);

        // Reinflate
        // Dummy config
        ConfigurationChange configChange = mTestContext.getRootContext().createConfigurationChange()
                .build();
        
        clearInvocations(fakeMediaPlayer);

        mTestContext.getRootContext().handleConfigurationChange(configChange);

        // Check that the player plays from the position where it was before reinflation.
        verify(fakeMediaPlayer, timeout(1000)).seek(200);
        verify(fakeMediaPlayer, timeout(1000)).play();
    }

    /**
     * Since the FakeMediaPlayer is used in other tests, it needs to be tested as well.
     */
    @Test
    public void testFakeMediaPlayer() {
        FakeMediaPlayer fakeMediaPlayer = new FakeMediaPlayer();
        verifySettersAndGetters(fakeMediaPlayer, -1, -1);
        verifySettersAndGetters(fakeMediaPlayer, 0, 1);
        verifyAddMediaStateListener(fakeMediaPlayer,
                mock(IMediaPlayer.IMediaListener.class),
                mock(IMediaPlayer.IMediaListener.class));
        verifyStateTransition(fakeMediaPlayer);
    }

    private void verifySettersAndGetters(final FakeMediaPlayer fakeMediaPlayer,
                                         final int currentTrackIndex,
                                         final int currentSeekPosition) {
        fakeMediaPlayer.setTrack(currentTrackIndex);
        fakeMediaPlayer.seek(currentSeekPosition);
        assertEquals(currentTrackIndex, fakeMediaPlayer.getCurrentTrackIndex());
        assertEquals(currentSeekPosition, fakeMediaPlayer.getCurrentSeekPosition());
    }

    private void verifyAddMediaStateListener(FakeMediaPlayer fakeMediaPlayer,
                                          IMediaPlayer.IMediaListener ...listeners) {
        for (IMediaPlayer.IMediaListener listener : listeners) {
            fakeMediaPlayer.addMediaStateListener(listener);
        }
        fakeMediaPlayer.notifyMediaState();
        for (IMediaPlayer.IMediaListener listener : listeners) {
            verify(listener).updateMediaState(fakeMediaPlayer);
        }
    }

    private void verifyStateTransition(FakeMediaPlayer fakeMediaPlayer) {
        // Initially IDLE
        assertEquals(IMediaPlayer.IMediaListener.MediaState.IDLE, fakeMediaPlayer.getCurrentMediaState());
        // PLAYING after calling play
        fakeMediaPlayer.play();
        assertEquals(IMediaPlayer.IMediaListener.MediaState.PLAYING, fakeMediaPlayer.getCurrentMediaState());
        // IDLE after setting the state
        fakeMediaPlayer.setCurrentMediaState(IMediaPlayer.IMediaListener.MediaState.IDLE);
        assertEquals(IMediaPlayer.IMediaListener.MediaState.IDLE, fakeMediaPlayer.getCurrentMediaState());
    }

    /**
     * Utility class to facilitate integration testing of LocalMediaPlayer with Runtime MediaPlayer
     */
    private static final class FakeMediaPlayer implements IMediaPlayer {

        private final List<IMediaListener> mListeners = new ArrayList<>();

        private int mCurrentTrackIndex = 0;
        private int mCurrentSeekPosition = 0;
        private IMediaListener.MediaState mState = IMediaListener.MediaState.IDLE;

        @Override
        public int getCurrentSeekPosition() {
            return mCurrentSeekPosition;
        }

        @Override
        public void setAudioTrack(@NonNull AudioTrack audioTrack) {

        }

        @Override
        public void setVideoScale(@NonNull VideoScale scale) {

        }

        @Override
        public void setMediaSources(@NonNull MediaSources mediaSources) {

        }

        @Override
        public void addMediaStateListener(@NonNull IMediaListener listener) {
            mListeners.add(listener);
        }

        @Override
        public void removeMediaStateListener(@NonNull IMediaListener listener) {

        }

        @Override
        public void play() {
            mState = IMediaListener.MediaState.PLAYING;
        }

        @Override
        public void pause() {

        }

        @Override
        public void stop() {

        }

        @Override
        public void next() {

        }

        @Override
        public void previous() {

        }

        @Override
        public void setTrack(int trackIndex) {
            mCurrentTrackIndex = trackIndex;
        }

        @Override
        public void seek(int msec) {
            mCurrentSeekPosition = msec;
        }

        @Override
        public void rewind() {

        }

        @Override
        public boolean isPlaying() {
            return false;
        }

        @Override
        public int getDuration() {
            return 0;
        }

        @Override
        public int getCurrentTrackIndex() {
            return mCurrentTrackIndex;
        }

        @Override
        public int getTrackCount() {
            return 0;
        }

        @NonNull
        @Override
        public IMediaListener.MediaState getCurrentMediaState() {
            return mState;
        }

        public void setCurrentMediaState(IMediaListener.MediaState state) {
            mState = state;
        }

        @Override
        public void release() {
            mListeners.clear();
        }

        public void notifyMediaState() {
            for (IMediaListener listener : mListeners) {
                listener.updateMediaState(this);
            }
        }
    }
}
