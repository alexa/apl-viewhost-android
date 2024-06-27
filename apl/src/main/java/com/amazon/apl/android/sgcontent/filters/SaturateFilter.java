package com.amazon.apl.android.sgcontent.filters;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class SaturateFilter extends Filter {

    public abstract Filter filter();

    public abstract float amount();

    public static SaturateFilter create(long handle, Filter filter, float amount) {
        AutoValue_SaturateFilter val = new AutoValue_SaturateFilter(filter, amount);
        val.mNativeHandle = handle;
        return val;
    }
}
