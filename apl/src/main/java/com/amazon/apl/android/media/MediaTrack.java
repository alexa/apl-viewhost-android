/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.media;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * A description of a media track to be played by the media player.
 */
@AllArgsConstructor
@Getter
public class MediaTrack {
    private final String url;             // Source of the video clip
    private final String[] headers;       // HeaderArray required for the track
    private final int offset;             // Starting offset within the media object, in milliseconds
    private final int duration;           // Duration from the starting offset to play.  If non-positive, play the entire track
    private final int repeatCount;        // Number of times to repeat this track before moving to the next. Negative numbers repeat forever.
    private final TextTrack[] textTracks; // Time based text data to render
}
