package com.amazon.apl.android.sgcontent.filters;

import com.amazon.apl.enums.NoiseFilterKind;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class NoiseFilter extends Filter {

    public abstract Filter filter();

    public abstract NoiseFilterKind kind();

    public abstract float sigma();

    public abstract boolean useColor();

    public static NoiseFilter create(long handle, Filter filter, NoiseFilterKind kind, float sigma, boolean useColor) {
        AutoValue_NoiseFilter val = new AutoValue_NoiseFilter(filter, kind, sigma, useColor);
        val.mNativeHandle = handle;
        return val;
    }
}
