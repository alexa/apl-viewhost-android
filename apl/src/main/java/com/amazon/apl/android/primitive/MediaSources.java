/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.primitive;

import com.google.auto.value.AutoValue;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import com.amazon.apl.android.media.TextTrack;

/**
 * MediaSource Property
 * See https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-video.html#source
 */
@AutoValue
public abstract class MediaSources implements IterableProperty<MediaSources.MediaSource> {
    public static final int REPEAT_FOREVER = -1;

    public static MediaSources create() {
        return IterableProperty.create(new MediaSourceGetter());
    }

    @AutoValue
    public static abstract class MediaSource {
        public abstract String url();
        public abstract int duration();
        public abstract int repeatCount();
        public abstract int offset();
        public abstract Map<String, String> headers();
        public abstract List<TextTrack> textTracks();
        public static Builder builder() {
            return new AutoValue_MediaSources_MediaSource.Builder();
        }

        @AutoValue.Builder
        public static abstract class Builder {
            public abstract Builder url(String url);
            public abstract Builder duration(int duration);
            public abstract Builder repeatCount(int repeatCount);
            public abstract Builder offset(int offset);
            public abstract Builder headers(Map<String, String> headers);
            public abstract Builder textTracks(List<TextTrack> textTracks);
            public abstract MediaSource build();
        }
    }

    private static class MediaSourceGetter extends SimpleArrayGetter<MediaSources, MediaSource> {
        @Override
        public MediaSources builder() {
            return new AutoValue_MediaSources(new ArrayList<>());
        }
    }
}
