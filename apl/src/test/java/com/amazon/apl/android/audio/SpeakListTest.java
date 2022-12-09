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

public class SpeakListTest extends AbstractDocUnitTest {
    // Test content
    private static final String DOC = "{\n" +
            "    \"type\": \"APL\",\n" +
            "    \"version\": \"1.1\",\n" +
            "    \"theme\": \"auto\",\n" +
            "    \"styles\": {\n" +
            "        \"karaoke\": {\n" +
            "            \"description\": \"Color the Frame for Karaoke\",\n" +
            "            \"extend\": \"textStylePrimary2\",\n" +
            "            \"values\": [\n" +
            "                {\n" +
            "                    \"backgroundColor\": \"@colorTextPrimary\"\n" +
            "                },\n" +
            "                {\n" +
            "                    \"when\": \"${state.karaoke}\",\n" +
            "                    \"backgroundColor\": \"@colorAccent\"\n" +
            "                }\n" +
            "            ]\n" +
            "        }\n" +
            "    },\n" +
            "    \"mainTemplate\": {\n" +
            "        \"item\": {\n" +
            "            \"type\": \"Sequence\",\n" +
            "            \"id\": \"list\",\n" +
            "            \"data\": [\n" +
            "                {\n" +
            "                    \"item\": \"speechText1\",\n" +
            "                    \"speech\": \"speechUrl1\"\n" +
            "                },\n" +
            "                {\n" +
            "                    \"item\": \"speechText2\",\n" +
            "                    \"speech\": \"speechUrl2\"\n" +
            "                }\n" +
            "            ]," +
            "            \"width\": \"100%\",\n" +
            "            \"height\": \"100%\",\n" +
            "            \"scrollDirection\": \"vertical\",\n" +
            "            \"paddingLeft\": 100,\n" +
            "            \"paddingRight\": 100,\n" +
            "            \"item\": {\n" +
            "                \"type\": \"Text\",\n" +
            "                \"style\": \"karaoke\",\n" +
            "                \"text\": \"${data.item}\",\n" +
            "                \"speech\": \"${data.speech}\",\n" +
            "                \"spacing\": 20\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}";

    private static final String SPEAK_LIST_COMMAND = "[{\n" +
            "                    \"type\": \"SpeakList\",\n" +
            "                    \"componentId\": \"list\",\n" +
            "                    \"count\": 3,\n" +
            "                    \"start\": 1,\n" +
            "                    \"align\": \"first\",\n" +
            "                    \"minimumDwellTime\": 1000\n" +
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
    public void testSpeakList() throws JSONException {
        mRootContext.executeCommands(SPEAK_LIST_COMMAND);
        update(100);
        verify(mMockPlayerProvider).prepare("speechUrl2");
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

    // TODO: Opportunity to add tests for block by block karaoke highlighting and removal.
}
