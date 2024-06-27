package com.amazon.apl.android.media;

import android.graphics.Rect;

import com.amazon.apl.android.bitmap.BitmapKey;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class DecodedImageBitmapKey implements BitmapKey {
    abstract String sourceUrl();

    abstract Rect decodeRegion();

    abstract int sampleSize();

    public static DecodedImageBitmapKey create(String sourceUrl, Rect decodeRegion, int sampleSize) {
        return new AutoValue_DecodedImageBitmapKey(sourceUrl, decodeRegion, sampleSize);
    }
}
