/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.primitive;

import com.amazon.common.BoundObject;
import com.amazon.apl.android.utils.HttpUtils;
import com.amazon.apl.enums.APLEnum;
import com.google.auto.value.AutoValue;

import java.util.ArrayList;
import java.util.Map;

/**
 * MediaSource Property
 * See https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-video.html#source
 */
@AutoValue
public abstract class BoundMediaSources implements IterableProperty<BoundMediaSources.MediaSource> {
    public static final int REPEAT_FOREVER = -1;

    public static BoundMediaSources create(BoundObject boundObject, APLEnum propertyKey) {
        return IterableProperty.create(new BoundMediaSourceGetter(boundObject, propertyKey));
    }

    @AutoValue
    public static abstract class MediaSource {
        public abstract String url();
        public abstract int duration();
        public abstract int repeatCount();
        public abstract int offset();
        public abstract Map<String, String> headers();
        public static Builder builder() {
            return new AutoValue_BoundMediaSources_MediaSource.Builder();
        }

        @AutoValue.Builder
        static abstract class Builder {
            abstract Builder url(String url);
            abstract Builder duration(int duration);
            abstract Builder repeatCount(int repeatCount);
            abstract Builder offset(int offset);
            abstract Builder headers(Map<String, String> headers);
            abstract MediaSource build();
        }
    }

    private static class BoundMediaSourceGetter extends ArrayGetter<BoundMediaSources, MediaSource> {
        private BoundMediaSourceGetter(BoundObject boundObject, APLEnum propertyKey) {
            super(boundObject, propertyKey);
        }

        @Override
        BoundMediaSources builder() {
            return new AutoValue_BoundMediaSources(new ArrayList<>());
        }

        @Override
        public MediaSource get(int index) {
            return MediaSource.builder()
                    .url(nGetMediaSourceUrlAt(getNativeHandle(), getIndex(), index))
                    .duration(nGetMediaSourceDurationAt(getNativeHandle(), getIndex(), index))
                    .offset(nGetMediaSourceOffsetAt(getNativeHandle(), getIndex(), index))
                    .repeatCount(nGetMediaSourceRepeatCountAt(getNativeHandle(), getIndex(), index))
                    .headers(HttpUtils.listToHeadersMap(nGetMediaSourceHeadersAt(getNativeHandle(), getIndex(), index)))
                    .build();
        }
    }

    private static native String nGetMediaSourceUrlAt(long componentHandle, int propertyKey, int index);
    private static native String[] nGetMediaSourceHeadersAt(long componentHandle, int propertyKey, int index);
    private static native int nGetMediaSourceDurationAt(long componentHandle, int propertyKey, int index);
    private static native int nGetMediaSourceRepeatCountAt(long componentHandle, int propertyKey, int index);
    private static native int nGetMediaSourceOffsetAt(long componentHandle, int propertyKey, int index);
}
