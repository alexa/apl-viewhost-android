/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.audio;

import android.graphics.Color;
import android.util.Log;

import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.platform.app.InstrumentationRegistry;

import com.amazon.apl.android.Component;
import com.amazon.apl.android.configuration.ConfigurationChange;
import com.amazon.apl.android.dependencies.ITtsPlayer;
import com.amazon.apl.android.document.AbstractDocViewTest;
import com.amazon.apl.android.espresso.APLMatchers;
import com.amazon.apl.android.espresso.APLViewIdlingResource;
import com.amazon.apl.android.providers.ITtsPlayerProvider;

import static com.amazon.apl.android.espresso.APLViewActions.executeCommandsNoLoop;
import static com.amazon.apl.android.espresso.APLViewActions.waitFor;
import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.views.APLAbsoluteLayout;

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static com.amazon.apl.android.utils.TtsHelper.generateSpeechMarks;

import java.util.List;


public class SpeakListViewTest extends AbstractDocViewTest {
    private static final String TAG = SpeakListViewTest.class.getSimpleName();
    private static final int WAIT_TIME = 200;

    private final String COMPONENT = "\n" +
            "\n" +
            "    \"type\": \"Container\",\n" +
            "    \"direction\": \"column\",\n" +
            "    \"width\": \"700\",\n" +
            "    \"height\": \"700\",\n" +
            "    \"items\": [\n" +
            "        {\n" +
            "            \"type\": \"Sequence\",\n" +
            "                \"id\": \"list\",\n" +
            "                \"data\": [\n" +
            "                    {\n" +
            "                        \"item\": \"Since <i>you</i> are not going <u>on a holiday this year Boss</u> I thought I should give your office a holiday look. Since you are not going on a holiday this year Boss I thought I should give your office a holiday look\\\",\\n\",\n" +
            "                        \"speech\": \"speechUrl1\"\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"item\": \"Since <i>you</i> are not going <u>on a holiday this year Boss</u> I thought I should give your office a holiday look. Since you are not going on a holiday this year Boss I thought I should give your office a holiday look\\\",\\n\",\n" +
            "                        \"speech\": \"speechUrl2\"\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"item\": \"Since <i>you</i> are not going <u>on a holiday this year Boss</u> I thought I should give your office a holiday look. Since you are not going on a holiday this year Boss I thought I should give your office a holiday look\\\",\\n\",\n" +
            "                        \"speech\": \"speechUrl3\"\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"item\": \"Since <i>you</i> are not going <u>on a holiday this year Boss</u> I thought I should give your office a holiday look. Since you are not going on a holiday this year Boss I thought I should give your office a holiday look\\\",\\n\",\n" +
            "                        \"speech\": \"speechUrl4\"\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"item\": \"Since <i>you</i> are not going <u>on a holiday this year Boss</u> I thought I should give your office a holiday look. Since you are not going on a holiday this year Boss I thought I should give your office a holiday look\\\",\\n\",\n" +
            "                        \"speech\": \"speechUrl5\"\n" +
            "                    }\n" +
            "                ],            \"width\": \"100%\",\n" +
            "                \"height\": \"100%\",\n" +
            "                \"scrollDirection\": \"vertical\",\n" +
            "                \"paddingLeft\": 100,\n" +
            "                \"paddingRight\": 100,\n" +
            "                \"item\": {\n" +
            "                    \"type\": \"Text\",\n" +
            "                    \"style\": \"karaoke\",\n" +
            "                    \"text\": \"${data.item}\",\n" +
            "                    \"speech\": \"${data.speech}\",\n" +
            "                    \"id\": \"${data.speech}\",\n" +
            "                    \"spacing\": 20\n" +
            "                }\n" +
            "    \n" +
            "        }\n" +
            "    ]\n";

    private final String PROPS = "    \"styles\": {\n" +
            "        \"karaoke\": {\n" +
            "            \"description\": \"Color the Frame for Karaoke\",\n" +
            "            \"values\": [\n" +
            "                {\n" +
            "                    \"when\": \"${state.karaoke}\",\n" +
            "                    \"color\": \"cyan\"\n" +
            "                },\n" +
            "                {\n" +
            "                    \"when\": \"${!state.karaoke}\",\n" +
            "                    \"color\": \"black\"\n" +
            "                },\n" +
            "                {\n" +
            "                    \"when\": \"${state.karaokeTarget}\",\n" +
            "                    \"color\": \"yellow\"\n" +
            "                }\n" +
            "            ]\n" +
            "        }\n" +
            "    }";

    private static final String SPEAK_LIST_COMMAND = "[\n" +
            "                {\n" +
            "                    \"type\": \"SpeakList\",\n" +
            "                    \"componentId\": \"list\",\n" +
            "                    \"count\": %d,\n" +
            "                    \"start\": %d,\n" +
            "                    \"align\": \"last\",\n" +
            "                    \"minimumDwellTime\": 100,\n" +
            "                    \"sequencer\": \"MAGIC\"\n" +
            "                }\n" +
            "            ]";

    private static final String SPEECH_TEXT = "Since you are not going on a holiday this year Boss I thought I should give your office a holiday look.";

    private ITtsPlayer mMockPlayer;
    private ITtsPlayerProvider mMockPlayerProvider;
    private IdlingResource mIdlingResource;
    APLOptions mOptions;
    RootConfig mRootConfig;
    List<ITtsPlayer.ISpeechMarksListener.SpeechMark> marks;

    @Before
    public void setup() {
        mMockPlayer = mock(ITtsPlayer.class);
        mMockPlayerProvider = mock(ITtsPlayerProvider.class);
        when(mMockPlayerProvider.getPlayer()).thenReturn(mMockPlayer);
        MockitoAnnotations.initMocks(this);

        mOptions = APLOptions.builder().ttsPlayerProvider(mMockPlayerProvider).build();

        mRootConfig = RootConfig.create("Unit Test", "1.0")
                .allowOpenUrl(true)
                .audioPlayerFactory(new RuntimeAudioPlayerFactory(mMockPlayerProvider));

        onView(withId(com.amazon.apl.android.test.R.id.apl))
                .perform(inflateWithOptions(COMPONENT, PROPS, mOptions, mRootConfig))
                .check(hasRootContext());

        Component mComponent = mTestContext.getRootContext().findComponentById("testcomp");
        APLAbsoluteLayout mView = (APLAbsoluteLayout) mTestContext.getPresenter().findView(mComponent);
        mIdlingResource = new APLViewIdlingResource(mView);
        IdlingRegistry.getInstance().register(mIdlingResource);

        try {
            marks = generateSpeechMarks(SPEECH_TEXT);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    @After
    public void teardown() {
        if (mIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(mIdlingResource);
        }
    }

    @Test
    public void testSpeakListView_scroll() {
        onView(withComponent(mTestContext.getRootContext().findComponentById("speechUrl1"))).check(matches(isDisplayed()));

        onView(isRoot())
                .perform(executeCommandsNoLoop(mTestContext.getRootContext(), String.format(SPEAK_LIST_COMMAND, 2, 2)))
                .perform(waitFor(WAIT_TIME));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        onView(withComponent(mTestContext.getRootContext().findComponentById("speechUrl3"))).check(matches(isDisplayed()));
    }


    @Test
    public void testSpeakListView_highlight() {
        Component root = mTestContext.getRootContext().getTopComponent();

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // speechUrl1 is not in karaoke mode
        onView(withComponent(mTestContext.getRootContext().findComponentById("speechUrl1")))
                .check(matches(APLMatchers.withTextColor(Color.BLACK)));

        onView(isRoot())
                .perform(executeCommandsNoLoop(mTestContext.getRootContext(), String.format(SPEAK_LIST_COMMAND, 2, 0)))
                .perform(waitFor(WAIT_TIME));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Get audio player reference for sending speechmarks
        ArgumentCaptor<AudioPlayer> audioPlayerArgumentCaptor = ArgumentCaptor.forClass(AudioPlayer.class);
        verify(mMockPlayer).setStateChangeListener(audioPlayerArgumentCaptor.capture());
        AudioPlayer mAudioPlayer = audioPlayerArgumentCaptor.getAllValues().get(0);


        // Signal start of speech
        mAudioPlayer.onStateChange(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_READY);

        // Send SpeechMark for the complete sentence for speechUrl1
        ITtsPlayer.ISpeechMarksListener.SpeechMark mark = marks.get(0);
        mAudioPlayer.onSpeechMark(mark);

        onView(isRoot()).perform(waitFor(WAIT_TIME));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // speechUrl1 is in karaoke mode
        onView(withComponent(mTestContext.getRootContext().findComponentById("speechUrl1")))
                .check(matches(APLMatchers.withTextColor(Color.CYAN)))
                .check(matches(isDisplayed()));

        // Signal end of speech
        mAudioPlayer.onStateChange(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_ENDED);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        // Signal start of speech
        mAudioPlayer.onStateChange(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_READY);

        // Wait for scroll
        onView(isRoot()).perform(waitFor(WAIT_TIME));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // speechUrl1 is out of karaoke mode
        onView(withComponent(mTestContext.getRootContext().findComponentById("speechUrl1")))
                .check(matches(APLMatchers.withTextColor(Color.BLACK)))
                .check(matches(isDisplayed()));
        // speechUrl2 is in karaoke mode
        onView(withComponent(mTestContext.getRootContext().findComponentById("speechUrl2")))
                .check(matches(APLMatchers.withTextColor(Color.CYAN)))
                .check(matches(isDisplayed()));

        // Signal end of speech
        mAudioPlayer.onStateChange(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_ENDED);
    }

    @Test
    public void testSpeakListView_preserve() {
        Component root = mTestContext.getRootContext().getTopComponent();

        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // speechUrl1 is not in karaoke mode
        onView(withComponent(mTestContext.getRootContext().findComponentById("speechUrl1")))
                .check(matches(APLMatchers.withTextColor(Color.BLACK)));

        onView(isRoot())
                .perform(executeCommandsNoLoop(mTestContext.getRootContext(), String.format(SPEAK_LIST_COMMAND, 2, 0)))
                .perform(waitFor(WAIT_TIME));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Get audio player reference for sending speechmarks
        ArgumentCaptor<AudioPlayer> audioPlayerArgumentCaptor = ArgumentCaptor.forClass(AudioPlayer.class);
        verify(mMockPlayer).setStateChangeListener(audioPlayerArgumentCaptor.capture());
        AudioPlayer mAudioPlayer = audioPlayerArgumentCaptor.getAllValues().get(0);

        // Signal start of speech
        mAudioPlayer.onStateChange(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_READY);

        // Send SpeechMark for the complete sentence for speechUrl1
        ITtsPlayer.ISpeechMarksListener.SpeechMark mark = marks.get(0);
        mAudioPlayer.onSpeechMark(mark);

        // Reinflate
        ConfigurationChange configChange = mTestContext.getRootContext().createConfigurationChange()
                .build();
        mTestContext.getRootContext().handleConfigurationChange(configChange);

        onView(isRoot()).perform(waitFor(WAIT_TIME));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // speechUrl1 is in karaoke mode
        onView(withComponent(mTestContext.getRootContext().findComponentById("speechUrl1")))
                .check(matches(APLMatchers.withTextColor(Color.CYAN)))
                .check(matches(isDisplayed()));

        // Signal end of speech
        mAudioPlayer.onStateChange(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_ENDED);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        // Signal start of speech
        mAudioPlayer.onStateChange(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_READY);

        // Wait for scroll
        onView(isRoot()).perform(waitFor(WAIT_TIME));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // speechUrl1 is out of karaoke mode
        onView(withComponent(mTestContext.getRootContext().findComponentById("speechUrl1")))
                .check(matches(APLMatchers.withTextColor(Color.BLACK)))
                .check(matches(isDisplayed()));
        // speechUrl2 is in karaoke mode
        onView(withComponent(mTestContext.getRootContext().findComponentById("speechUrl2")))
                .check(matches(APLMatchers.withTextColor(Color.CYAN)))
                .check(matches(isDisplayed()));

        // Signal end of speech
        mAudioPlayer.onStateChange(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_ENDED);
    }
}
