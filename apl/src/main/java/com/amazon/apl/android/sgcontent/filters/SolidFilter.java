package com.amazon.apl.android.sgcontent.filters;

import com.amazon.apl.android.sgcontent.Paint;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class SolidFilter extends Filter {

    public abstract Paint paint();

    public static SolidFilter create(long handle, Paint paint) {
        AutoValue_SolidFilter val = new AutoValue_SolidFilter(paint);
        val.mNativeHandle = handle;
        return val;
    }
}
