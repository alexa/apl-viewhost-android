package com.amazon.apl.android.sgcontent.filters;

import com.amazon.apl.enums.BlendMode;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class BlendFilter extends Filter {

    public abstract BlendMode blendMode();

    public abstract Filter frontFilter();

    public abstract Filter backFilter();

    public static BlendFilter create(long handle, BlendMode blendMode, Filter frontFilter, Filter backFilter) {
        AutoValue_BlendFilter val = new AutoValue_BlendFilter(blendMode, frontFilter, backFilter);
        val.mNativeHandle = handle;
        return val;
    }
}
