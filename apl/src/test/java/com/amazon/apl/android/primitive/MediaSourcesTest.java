/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.primitive;

import static org.junit.Assert.assertEquals;

import com.amazon.apl.android.media.TextTrack;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.enums.TextTrackType;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class MediaSourcesTest extends ViewhostRobolectricTest {
    private static final String DUMMY_URL = "dummyUrl";
    private static final String DUMMY_TEXT_TRACK_URL = "https://videotest.invalid/video-capion.srt";
    private static final String DUMMY_TEXT_TRACK_DESCRIPTION = "Testing textrack in video component";
    private static final int DURATION = 100;
    private static final int OFFSET = 100;
    private static final int REPEAT_COUNT = 10;
    private static final Map<String, String> HEADERS_MAP = new HashMap<>();

    @Test
    public void testMediaSourcesCreate() {
        HEADERS_MAP.put("key1", "value1");
        HEADERS_MAP.put("key2", "value2");
        MediaSources mediaSources = MediaSources.create();
        List<TextTrack> testTrack = new ArrayList<TextTrack>();
        TextTrack track = new TextTrack(TextTrackType.kTextTrackTypeCaption, DUMMY_TEXT_TRACK_URL, DUMMY_TEXT_TRACK_DESCRIPTION);
        testTrack.add(track);
        assertEquals(0, mediaSources.size());
        MediaSources.MediaSource mediaSource = MediaSources.MediaSource.builder()
                .repeatCount(REPEAT_COUNT)
                .offset(OFFSET)
                .duration(DURATION)
                .url(DUMMY_URL)
                .headers(HEADERS_MAP)
                .textTracks(testTrack)
                .build();
        mediaSources.add(mediaSource);
        assertEquals(1, mediaSources.size());
        assertEquals(DURATION, mediaSources.at(0).duration());
        assertEquals(OFFSET, mediaSources.at(0).offset());
        assertEquals(DUMMY_URL, mediaSources.at(0).url());
        assertEquals(DUMMY_TEXT_TRACK_URL, mediaSources.at(0).textTracks().get(0).getUrl());
        assertEquals(DUMMY_TEXT_TRACK_DESCRIPTION, mediaSources.at(0).textTracks().get(0).getDescription());
        assertEquals(TextTrackType.kTextTrackTypeCaption, mediaSources.at(0).textTracks().get(0).getType());
        assertEquals(REPEAT_COUNT, mediaSources.at(0).repeatCount());
        assertEquals(HEADERS_MAP, mediaSources.at(0).headers());
    }
}
