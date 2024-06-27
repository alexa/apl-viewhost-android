package com.amazon.apl.android.sgcontent;

import com.amazon.apl.android.utils.ColorUtils;

public class Shadow {
    public long mNativeHandle;

    public Shadow(long nativeHandle) {
        mNativeHandle = nativeHandle;
    }

    public int getColor() { return ColorUtils.toARGB(nGetColor(mNativeHandle));}

    public float[] getOffset() {
        return nGetOffset(mNativeHandle);
    }

    public float getRadius() {
        return nGetRadius(mNativeHandle);
    }

    private static native int nGetColor(long nativeHandle);

    private static native float[] nGetOffset(long nativeHandle);

    private static native float nGetRadius(long nativeHandle);
}
