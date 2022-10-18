/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.audio;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.dependencies.ITtsPlayer;
import com.amazon.apl.android.document.AbstractDocUnitTest;

import com.amazon.apl.android.providers.ITtsPlayerProvider;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;

public class SpeakItemTest extends AbstractDocUnitTest {
    // Test content
    private static final String DOC = "{\n" +
            "    \"type\": \"APL\",\n" +
            "    \"version\": \"1.1\",\n" +
            "    \"theme\": \"dark\",\n" +
            "    \"styles\": {\n" +
            "        \"flip\": {\n" +
            "            \"description\": \"Color the Frame for Karaoke\",\n" +
            "            \"values\": [\n" +
            "                {\n" +
            "                    \"when\": \"${state.karaoke}\",\n" +
            "                    \"color\": \"@colorAccent\"\n" +
            "                },\n" +
            "                {\n" +
            "                    \"when\": \"${!state.karaoke}\",\n" +
            "                    \"color\": \"white\"\n" +
            "                },\n" +
            "                {\n" +
            "                    \"when\": \"${state.karaokeTarget}\",\n" +
            "                    \"color\": \"yellow\"\n" +
            "                }\n" +
            "            ]\n" +
            "        }\n" +
            "    },\n" +
            "    \"mainTemplate\": {\n" +
            "        \"items\": [\n" +
            "            {\n" +
            "                \"type\": \"Container\",\n" +
            "                \"direction\": \"column\",\n" +
            "                \"width\": \"100%\",\n" +
            "                \"height\": \"100%\",\n" +
            "                \"items\": [\n" +
            "                    {\n" +
            "                        \"type\": \"Frame\",\n" +
            "                        \"borderColor\": \"red\",\n" +
            "                        \"borderWidth\": 3,\n" +
            "                        \"width\": \"100%\",\n" +
            "                        \"height\": \"50%\",\n" +
            "                        \"item\": {\n" +
            "                            \"type\": \"ScrollView\",\n" +
            "                            \"width\": \"100%\",\n" +
            "                            \"height\": \"100%\",\n" +
            "                            \"id\": \"scroll\",\n" +
            "                            \"item\": {\n" +
            "                                \"type\": \"Container\",\n" +
            "                                \"direction\": \"column\",\n" +
            "                                \"alignItems\": \"center\",\n" +
            "                                \"transform\": [\n" +
            "                                    {\n" +
            "                                        \"scaleX\": 0.5\n" +
            "                                    }\n" +
            "                                ],\n" +
            "                                \"items\": [\n" +
            "                                    {\n" +
            "                                        \"type\": \"Frame\",\n" +
            "                                        \"width\": \"80%\",\n" +
            "                                        \"height\": 300,\n" +
            "                                        \"opacity\": 0.3,\n" +
            "                                        \"alignSelf\": \"center\",\n" +
            "                                        \"backgroundColor\": \"purple\",\n" +
            "                                        \"id\": \"frame1\"\n" +
            "                                    },\n" +
            "                                    {\n" +
            "                                        \"type\": \"Text\",\n" +
            "                                        \"id\": \"text1\",\n" +
            "                                        \"style\": \"flip\",\n" +
            "                                        \"text\": \"outputText\",\n" +
            "                                        \"speech\": \"outputTextTtsUrl\",\n" +
            "                                        \"textAlign\": \"center\",\n" +
            "                                        \"fontSize\": \"56dp\",\n" +
            "                                        \"width\": \"80%\"\n" +
            "                                    },\n" +
            "                                    {\n" +
            "                                        \"type\": \"Text\",\n" +
            "                                        \"id\": \"text2\",\n" +
            "                                        \"style\": \"flip\",\n" +
            "                                        \"text\": \"outputText\",\n" +
            "                                        \"speech\": \"outputTextTTSURL\",\n" +
            "                                        \"textAlign\": \"center\",\n" +
            "                                        \"fontSize\": \"40dp\",\n" +
            "                                        \"width\": \"80%\"\n" +
            "                                    },\n" +
            "                                    {\n" +
            "                                        \"type\": \"Frame\",\n" +
            "                                        \"width\": \"80%\",\n" +
            "                                        \"height\": 200,\n" +
            "                                        \"alignSelf\": \"center\",\n" +
            "                                        \"backgroundColor\": \"green\",\n" +
            "                                        \"speech\": \"outputTextTTSURL\",\n" +
            "                                        \"id\": \"frame2\"\n" +
            "                                    },\n" +
            "                                    {\n" +
            "                                        \"type\": \"Text\",\n" +
            "                                        \"id\": \"text3\",\n" +
            "                                        \"style\": \"flip\",\n" +
            "                                        \"text\": \"outputText\",\n" +
            "                                        \"speech\": \"outputTextTTSURL\",\n" +
            "                                        \"textAlign\": \"center\",\n" +
            "                                        \"fontSize\": \"70dp\",\n" +
            "                                        \"width\": \"80%\"\n" +
            "                                    }\n" +
            "                                ]\n" +
            "                            }\n" +
            "                        }\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"type\": \"Text\",\n" +
            "                        \"id\": \"textNotInScrollView\",\n" +
            "                        \"style\": \"flip\",\n" +
            "                        \"text\": \"outputText\",\n" +
            "                        \"speech\": \"outputTextTTSURL\",\n" +
            "                        \"textAlign\": \"center\",\n" +
            "                        \"fontSize\": \"25dp\",\n" +
            "                        \"width\": \"100%\",\n" +
            "                        \"paddingLeft\": \"30%\",\n" +
            "                        \"paddingRight\": \"30%\",\n" +
            "                        \"paddingTop\": 150\n" +
            "                    },\n" +
            "                    {\n" +
            "                        \"type\": \"Text\",\n" +
            "                        \"text\": \"disallowDialog\",\n" +
            "                        \"textAlign\": \"center\",\n" +
            "                        \"fontSize\": \"25dp\",\n" +
            "                        \"width\": \"100%\",\n" +
            "                        \"paddingLeft\": \"30%\",\n" +
            "                        \"paddingRight\": \"30%\",\n" +
            "                        \"paddingTop\": 10\n" +
            "                    }\n" +
            "                ]\n" +
            "            }\n" +
            "        ]\n" +
            "    }\n" +
            "}\n";

    private static final String SPEAK_ITEM_COMMAND = "[{\n" +
            "                    \"type\": \"SpeakItem\",\n" +
            "                    \"componentId\": \"text1\",\n" +
            "                    \"highlightMode\": \"line\",\n" +
            "                    \"align\": \"first\"\n" +
            "                }]";

    private ITtsPlayer mMockPlayer;
    private ITtsPlayerProvider mMockPlayerProvider;

    @Before
    public void setup() {
        mMockPlayer = mock(ITtsPlayer.class);
        mMockPlayerProvider = mock(ITtsPlayerProvider.class);
        when(mMockPlayerProvider.getPlayer()).thenReturn(mMockPlayer);
        mRootConfig = RootConfig.create("Unit Test", "1.0")
                .allowOpenUrl(true)
                .audioPlayerFactory(new RuntimeAudioPlayerFactory(mMockPlayerProvider));
        MockitoAnnotations.initMocks(this);

        loadDocument(DOC, APLOptions.builder().build());
    }

    @Test
    public void testSpeakItem() throws JSONException {
        mRootContext.executeCommands(SPEAK_ITEM_COMMAND);
        verify(mMockPlayerProvider).prepare("outputTextTtsUrl");
        ArgumentCaptor<AudioPlayer> audioPlayerArgumentCaptor = ArgumentCaptor.forClass(AudioPlayer.class);
        verify(mMockPlayer).setStateChangeListener(audioPlayerArgumentCaptor.capture());
        update(100);
        audioPlayerArgumentCaptor.getValue().onStateChange(ITtsPlayer.IStateChangeListener.AudioPlayerState.STATE_READY);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "word");
        jsonObject.put("time", "100");
        jsonObject.put("value", "Hello");
        jsonObject.put("start", 100);
        jsonObject.put("end", 200);
        ITtsPlayer.ISpeechMarksListener.SpeechMark mark = ITtsPlayer.ISpeechMarksListener.SpeechMark.create(jsonObject);
        audioPlayerArgumentCaptor.getValue().onSpeechMark(mark); // Make sure no crash
    }

    // TODO: Opportunity to add tests for line by line karaoke highlighting and removal.
}
