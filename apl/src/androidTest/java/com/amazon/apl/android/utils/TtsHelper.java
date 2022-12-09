/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.utils;

import com.amazon.apl.android.dependencies.ITtsPlayer;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class TtsHelper {
    /**
     * Generates SpeechMarks for sanitized speech text containing no tags
     * eg: "This is a simple test"
     *
     * @param simpleSpeechText
     * @return List<ITtsPlayer.ISpeechMarksListener.SpeechMark>
     */
    public static List<ITtsPlayer.ISpeechMarksListener.SpeechMark> generateSpeechMarks(final String simpleSpeechText) throws JSONException {
        List<ITtsPlayer.ISpeechMarksListener.SpeechMark> speechMarks = new ArrayList<>();

        // Speech mark for the sentence
        JSONObject json = new JSONObject();
        json.put("type", "sentence");
        json.put("time", "0");
        json.put("value", simpleSpeechText);
        json.put("start", 0);
        json.put("end", simpleSpeechText.length());
        ITtsPlayer.ISpeechMarksListener.SpeechMark mark = ITtsPlayer.ISpeechMarksListener.SpeechMark.create(json);
        speechMarks.add(mark);

        int start = 0;
        // Generate speech marks for each word
        String[] splits = simpleSpeechText.split("\\s+");
        for (int i = 0 ; i < splits.length ; i++) {
            json.put("type", "word");
            json.put("time", String.valueOf(50 * i));
            json.put("value", splits[i]);
            json.put("start", start);
            json.put("end", start + splits[i].length());

            mark = ITtsPlayer.ISpeechMarksListener.SpeechMark.create(json);
            speechMarks.add(mark);

            // Add back 1 for space
            start += splits[i].length() + 1;
        }

        return speechMarks;
    }
}
