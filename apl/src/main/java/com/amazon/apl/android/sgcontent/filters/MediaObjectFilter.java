package com.amazon.apl.android.sgcontent.filters;

import com.amazon.apl.android.media.MediaObject;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class MediaObjectFilter extends Filter {

    public abstract MediaObject mediaObject();

    public static MediaObjectFilter create(long handle, MediaObject mediaObject) {
        AutoValue_MediaObjectFilter val = new AutoValue_MediaObjectFilter(mediaObject);
        val.mNativeHandle = handle;
        return val;
    }
}
