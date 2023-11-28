/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.media;
import com.amazon.apl.enums.TextTrackType;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class TextTrack {
    TextTrackType type;
    private final String url;
    private final String description;

    public TextTrack(int textTrackEnum, String textUrl, String textDescription){
        type = TextTrackType.valueOf(textTrackEnum);
        url = textUrl;
        description = textDescription;
    }
}