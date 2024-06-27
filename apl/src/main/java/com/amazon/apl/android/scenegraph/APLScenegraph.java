package com.amazon.apl.android.scenegraph;

import com.amazon.apl.android.RootContext;

public class APLScenegraph {

    private final RootContext mRootContext;

    public APLScenegraph(final RootContext rootContext) {
        mRootContext = rootContext;
    }

    public String getDOM() {
        return nGetDOM(mRootContext.getNativeHandle());
    }

    public String getSerializedScenegraph() {
        return nSerializeScenegraph(mRootContext.getNativeHandle());
    }

    public long getTop() {
        return nGetTop(mRootContext.getNativeHandle());
    }

    public void applyUpdates() {
        nApplyUpdates(mRootContext.getNativeHandle());
    }

    private native long nGetTop(long nativeHandle);

    private native void nApplyUpdates(long nativeHandle);

    private native String nSerializeScenegraph(long nativeHandle);

    private static native String nGetDOM(long nativeHandle);
}
