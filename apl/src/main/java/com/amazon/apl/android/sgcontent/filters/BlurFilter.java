package com.amazon.apl.android.sgcontent.filters;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class BlurFilter extends Filter {

    public abstract Filter filter();

    public abstract float radius();

    public static BlurFilter create(long handle, Filter filter, float radius) {
        AutoValue_BlurFilter val = new AutoValue_BlurFilter(filter, radius);
        val.mNativeHandle = handle;
        return val;
    }
}
