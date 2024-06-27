package com.amazon.apl.android.sgcontent.filters;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class GrayscaleFilter extends Filter {

    public abstract Filter filter();

    public abstract float amount();

    public static GrayscaleFilter create(long handle, Filter filter, float amount) {
        AutoValue_GrayscaleFilter val = new AutoValue_GrayscaleFilter(filter, amount);
        val.mNativeHandle = handle;
        return val;
    }
}
