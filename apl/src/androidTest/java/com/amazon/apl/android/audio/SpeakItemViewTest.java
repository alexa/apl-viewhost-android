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
import androidx.test.filters.LargeTest;
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
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.not;

import static com.amazon.apl.android.utils.TtsHelper.generateSpeechMarks;

import java.util.List;

public class SpeakItemViewTest extends AbstractDocViewTest {
    private static final String TAG = SpeakListViewTest.class.getSimpleName();
    private static final int WAIT_TIME = 200;

    private final String COMPONENT = "\"type\": \"Container\",\n" +
            "    \"direction\": \"column\",\n" +
            "    \"width\": \"500\",\n" +
            "    \"height\": \"500\",\n" +
            "    \"item\": {\n" +
            "        \"type\": \"ScrollView\",\n" +
            "        \"width\": \"100%\",\n" +
            "        \"height\": \"100%\",\n" +
            "        \"id\": \"scrollView\",\n" +
            "        \"preserve\": \"scrollOffset\",\n" +
            "        \"item\": {\n" +
            "            \"type\": \"Container\",\n" +
            "            \"direction\": \"column\",\n" +
            "            \"alignItems\": \"center\",\n" +
            "            \"transform\": [\n" +
            "                {\n" +
            "                    \"scaleX\": 0.5\n" +
            "                }\n" +
            "            ],\n" +
            "            \"items\": [\n" +
            "                {\n" +
            "                    \"type\": \"Frame\",\n" +
            "                    \"width\": \"300\",\n" +
            "                    \"height\": 150,\n" +
            "                    \"opacity\": 0.3,\n" +
            "                    \"alignSelf\": \"center\",\n" +
            "                    \"backgroundColor\": \"purple\",\n" +
            "                    \"id\": \"frameComponent\"\n" +
            "                },\n" +
            "                {\n" +
            "                    \"type\": \"Text\",\n" +
            "                    \"id\": \"textComponent\",\n" +
            "                    \"style\": \"karaoke\",\n" +
            "                    \"text\": \"Since <i>you</i> are not going <u>on a holiday this year Boss</u> I thought I should give your office a holiday look. Since you are not going on a holiday this year Boss I thought I should give your office a holiday look\",\n" +
            "                    \"speech\": \"outputSpeechUrl\",\n" +
            "                    \"textAlign\": \"center\",\n" +
            "                    \"fontSize\": \"56dp\",\n" +
            "                    \"width\": \"300\"\n" +
            "                }\n" +
            "            ]\n" +
            "        }\n" +
            "    }";

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

    private static final String SPEAK_ITEM_COMMAND = "[{\n" +
            "                    \"type\": \"SpeakItem\",\n" +
            "                    \"componentId\": \"%s\",\n" +
            "                    \"highlightMode\": \"%s\",\n" +
            "                    \"align\": \"first\",\n" +
            "                    \"minimumDwellTime\": 100,\n" +
            "                    \"sequencer\": \"MAGIC\"\n" +
            "                }]";

    private static final String SPEECH_TEXT = "Since you are not going on a holiday this year Boss I thought I should give your office a holiday look.";

    private ITtsPlayer mMockPlayer;
    private ITtsPlayerProvider mMockPlayerProvider;
    private IdlingResource mIdlingResource;
    private APLAbsoluteLayout mView;
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

        Component mComponent = mTestContext.getRootContext().findComponentById("scrollView").getChildAt(0);
        mView = (APLAbsoluteLayout) mTestContext.getPresenter().findView(mComponent);
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

    private void test_scrolls_to_textComponent(final String mode) {
        Component textComponent = mTestContext.getRootContext().findComponentById("textComponent");
        Component frameComponent = mTestContext.getRootContext().findComponentById("frameComponent");

        // Has not scrolled to textComponent
        onView(withComponent(frameComponent)).check(matches(isDisplayed()));

        onView(isRoot())
                .perform(executeCommandsNoLoop(mTestContext.getRootContext(), String.format(SPEAK_ITEM_COMMAND, "textComponent", mode)))
                .perform(waitFor(WAIT_TIME));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Has scrolled to textComponent
        onView(withComponent(frameComponent)).check(matches(not(isDisplayed())));
        onView(withComponent(textComponent)).check(matches(isDisplayed()));
    }

    @Test
    public void testSpeakItemView_line_scrolls_to_textComponent() {
        test_scrolls_to_textComponent("line");
    }

    @Test
    public void testSpeakItemView_block_scrolls_to_textComponent() {
        test_scrolls_to_textComponent("block");
    }

    @Test
    @LargeTest
    public void testSpeakItemView_line() {
        Component textComponent = mTestContext.getRootContext().findComponentById("textComponent");

        // Black before SpeakItem
        onView(withComponent(textComponent))
                .check(matches(APLMatchers.withTextColor(Color.BLACK)))
                .check(matches(isDisplayed()));

        onView(isRoot())
                .perform(executeCommandsNoLoop(mTestContext.getRootContext(), String.format(SPEAK_ITEM_COMMAND, "textComponent", "line")))
                .perform(waitFor(WAIT_TIME));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Text is in karaoke state
        onView(withComponent(textComponent))
                .check(matches(APLMatchers.withTextColor(Color.CYAN)))
                .check(matches(isDisplayed()));

        // Get audio player reference for sending speechmarks
        ArgumentCaptor<AudioPlayer> audioPlayerArgumentCaptor = ArgumentCaptor.forClass(AudioPlayer.class);
        verify(mMockPlayer).setStateChangeListener(audioPlayerArgumentCaptor.capture());
        AudioPlayer mAudioPlayer = audioPlayerArgumentCaptor.getAllValues().get(0);

        // Signal start of speech
        mAudioPlayer.onStateChange(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_READY);

        // Send SpeechMark for "Since" which is on the first line
        ITtsPlayer.ISpeechMarksListener.SpeechMark mark = marks.get(1);
        assert(mark.value.equals("Since"));
        mAudioPlayer.onSpeechMark(mark);

        onView(isRoot()).perform(waitFor(WAIT_TIME));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // First line is highlighted but not the second line
        onView(withComponent(textComponent))
                .check(matches(APLMatchers.withSpannableTextColor(0, 10, Color.YELLOW)))
                .check(matches(isDisplayed()));
        onView(withComponent(textComponent))
                .check(matches(not(APLMatchers.withSpannableTextColor(10, 18, Color.YELLOW))))
                .check(matches(isDisplayed()));

        // Send SpeechMark for "are" which is on the second line
        mark = marks.get(3);
        assert(mark.value.equals("are"));
        mAudioPlayer.onSpeechMark(mark);

        onView(isRoot()).perform(waitFor(WAIT_TIME));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Second line is highlighted but not the first line
        onView(withComponent(textComponent))
                .check(matches(not(APLMatchers.withSpannableTextColor(0, 10, Color.YELLOW))))
                .check(matches(isDisplayed()));
        onView(withComponent(textComponent))
                .check(matches(APLMatchers.withSpannableTextColor(10, 18, Color.YELLOW)))
                .check(matches(isDisplayed()));

        // Signal end of speech
        mAudioPlayer.onStateChange(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_ENDED);

        onView(isRoot()).perform(waitFor(WAIT_TIME));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Text is not in karaoke state
        onView(withComponent(textComponent))
                .check(matches(APLMatchers.withTextColor(Color.BLACK)))
                .check(matches(isDisplayed()));
    }

    @Test
    @LargeTest
    public void testSpeakItemView_block() {
        Component textComponent = mTestContext.getRootContext().findComponentById("textComponent");

        // Black before SpeakItem
        onView(withComponent(textComponent))
                .check(matches(APLMatchers.withTextColor(Color.BLACK)))
                .check(matches(isDisplayed()));

        onView(isRoot())
                .perform(executeCommandsNoLoop(mTestContext.getRootContext(), String.format(SPEAK_ITEM_COMMAND, "textComponent", "block")))
                .perform(waitFor(WAIT_TIME));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Text is in karaoke state
        onView(withComponent(textComponent))
                .check(matches(APLMatchers.withTextColor(Color.CYAN)))
                .check(matches(isDisplayed()));

        // Get audio player reference for sending speechmarks
        ArgumentCaptor<AudioPlayer> audioPlayerArgumentCaptor = ArgumentCaptor.forClass(AudioPlayer.class);
        verify(mMockPlayer).setStateChangeListener(audioPlayerArgumentCaptor.capture());
        AudioPlayer mAudioPlayer = audioPlayerArgumentCaptor.getAllValues().get(0);

        // Signal start of speech
        mAudioPlayer.onStateChange(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_READY);

        // Signal end of speech
        mAudioPlayer.onStateChange(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_ENDED);

        onView(isRoot()).perform(waitFor(WAIT_TIME));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Text is not in karaoke state
        onView(withComponent(textComponent))
                .check(matches(APLMatchers.withTextColor(Color.BLACK)))
                .check(matches(isDisplayed()));
    }

    @Test
    @LargeTest
    public void testSpeakItemView_line_preserve() {
        Component textComponent = mTestContext.getRootContext().findComponentById("textComponent");

        // Black before SpeakItem
        onView(withComponent(textComponent))
                .check(matches(APLMatchers.withTextColor(Color.BLACK)))
                .check(matches(isDisplayed()));

        onView(isRoot())
                .perform(executeCommandsNoLoop(mTestContext.getRootContext(), String.format(SPEAK_ITEM_COMMAND, "textComponent", "line")))
                .perform(waitFor(WAIT_TIME));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Text is in karaoke state
        onView(withComponent(textComponent))
                .check(matches(APLMatchers.withTextColor(Color.CYAN)))
                .check(matches(isDisplayed()));

        // Get audio player reference for sending speechmarks
        ArgumentCaptor<AudioPlayer> audioPlayerArgumentCaptor = ArgumentCaptor.forClass(AudioPlayer.class);
        verify(mMockPlayer).setStateChangeListener(audioPlayerArgumentCaptor.capture());
        AudioPlayer mAudioPlayer = audioPlayerArgumentCaptor.getAllValues().get(0);

        mAudioPlayer.onStateChange(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_READY);


        // Send SpeechMark for "Since"
        ITtsPlayer.ISpeechMarksListener.SpeechMark mark = marks.get(1);
        assert(mark.value.equals("Since"));
        mAudioPlayer.onSpeechMark(mark);
        onView(isRoot()).perform(waitFor(WAIT_TIME));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // First line is highlighted but not the second line
        onView(withComponent(textComponent))
                .check(matches(APLMatchers.withSpannableTextColor(0, 10, Color.YELLOW)))
                .check(matches(isDisplayed()));
        onView(withComponent(textComponent))
                .check(matches(not(APLMatchers.withSpannableTextColor(10, 18, Color.YELLOW))))
                .check(matches(isDisplayed()));

        // Reinflate
        ConfigurationChange configChange = mTestContext.getRootContext().createConfigurationChange()
                .build();
        mTestContext.getRootContext().handleConfigurationChange(configChange);

        onView(isRoot()).perform(waitFor(WAIT_TIME));
        textComponent = mTestContext.getRootContext().findComponentById("textComponent");

        onView(withComponent(textComponent))
                .check(matches(isDisplayed()));

        // Send SpeechMark for "are"
        mark = marks.get(3);
        assert(mark.value.equals("are"));
        mAudioPlayer.onSpeechMark(mark);
        onView(isRoot()).perform(waitFor(WAIT_TIME));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Second line is highlighted but not the first line
        onView(withComponent(textComponent))
                .check(matches(not(APLMatchers.withSpannableTextColor(0, 10, Color.YELLOW))))
                .check(matches(isDisplayed()));
        onView(withComponent(textComponent))
                .check(matches(APLMatchers.withSpannableTextColor(10, 18, Color.YELLOW)))
                .check(matches(isDisplayed()));

    }

    @Test
    @LargeTest
    public void testSpeakItemView_block_preserve() {
        Component textComponent = mTestContext.getRootContext().findComponentById("textComponent");

        // Black before SpeakItem
        onView(withComponent(textComponent))
                .check(matches(APLMatchers.withTextColor(Color.BLACK)))
                .check(matches(isDisplayed()));

        onView(isRoot())
                .perform(executeCommandsNoLoop(mTestContext.getRootContext(), String.format(SPEAK_ITEM_COMMAND, "textComponent", "block")))
                .perform(waitFor(WAIT_TIME));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        // Text is in karaoke state
        onView(withComponent(textComponent))
                .check(matches(APLMatchers.withTextColor(Color.CYAN)))
                .check(matches(isDisplayed()));

        // Get audio player reference for sending speechmarks
        ArgumentCaptor<AudioPlayer> audioPlayerArgumentCaptor = ArgumentCaptor.forClass(AudioPlayer.class);
        verify(mMockPlayer).setStateChangeListener(audioPlayerArgumentCaptor.capture());
        AudioPlayer mAudioPlayer = audioPlayerArgumentCaptor.getAllValues().get(0);

        mAudioPlayer.onStateChange(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_READY);

        onView(withComponent(textComponent))
                .check(matches(APLMatchers.withTextColor(Color.CYAN)))
                .check(matches(isDisplayed()));

        // Reinflate
        ConfigurationChange configChange = mTestContext.getRootContext().createConfigurationChange()
                .build();
        mTestContext.getRootContext().handleConfigurationChange(configChange);

        onView(isRoot()).perform(waitFor(WAIT_TIME));

        textComponent = mTestContext.getRootContext().findComponentById("textComponent");
        // Text is in karaoke state
        onView(withComponent(textComponent))
                .check(matches(APLMatchers.withTextColor(Color.CYAN)))
                .check(matches(isDisplayed()));

        // Signal end of speech
        mAudioPlayer.onStateChange(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_ENDED);

        onView(isRoot()).perform(waitFor(WAIT_TIME));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        onView(withComponent(textComponent))
                .check(matches(APLMatchers.withTextColor(Color.BLACK)))
                .check(matches(isDisplayed()));
    }
}
