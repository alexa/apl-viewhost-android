/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.internal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.apl.android.Action;
import com.amazon.apl.android.utils.JNIUtils;
import com.amazon.common.BoundObject;

/**
 * This is the peer class for Document Context in core.
 */
public class DocumentContext extends BoundObject {
    /**
     * Store a local copy of the current contexts. It's null if we know the context is dirty and
     * needs to be refreshed from core.
     */
    @Nullable
    private String mCachedVisualContext;
    @Nullable
    private String mCachedDataSourceContext;

    // Track whether notification has been sent for the any current dirty state
    private boolean mVisualContextChangeNotified = false;
    private boolean mDataSourceContextChangeNotified = false;

    public DocumentContext(long nativeHandle) {
        bind(nativeHandle);
    }
    /**
     * Executes an array of commands
     *
     * @param commands The commands to execute
     * @return An action to know when the commands are done or terminated.
     */
    @Nullable
    public Action executeCommands(@NonNull String commands) {
        long handle = nExecuteCommands(getNativeHandle(), commands);
        if (handle == 0) {
            return null;
        }
        Action action = new Action(handle, null, this);
        return action;
    }

    public long getId() {
        return nGetId(getNativeHandle());
    }

    /**
     * @return true if the visual context has changed from clean to dirty
     */
    public boolean getAndClearHasVisualContextChanged() {
        if (!mVisualContextChangeNotified && nIsVisualContextDirty(getNativeHandle())) {
            mVisualContextChangeNotified = true;
            mCachedVisualContext = null;
            return true;
        }
        return false;
    }

    /**
     * @return Serialized visual context for this document
     */
    public String serializeVisualContext() {
        if (null == mCachedVisualContext) {
            mCachedVisualContext =
                    JNIUtils.safeStringValues(nSerializeVisualContext(getNativeHandle()));
            nClearVisualContextDirty(getNativeHandle());
            mVisualContextChangeNotified = false;
        }
        return mCachedVisualContext;
    }

    /**
     * @return true if the data source context has changed from clean to dirty
     */
    public boolean getAndClearHasDataSourceContextChanged() {
        if (!mDataSourceContextChangeNotified && nIsDataSourceContextDirty(getNativeHandle())) {
            mDataSourceContextChangeNotified = true;
            mCachedDataSourceContext = null;
            return true;
        }
        return false;
    }

    /**
     * @return Serialized data source context for this document
     */
    public String serializeDataSourceContext() {
        if (null == mCachedDataSourceContext) {
            mCachedDataSourceContext =
                    JNIUtils.safeStringValues(nSerializeDataSourceContext(getNativeHandle()));
            nClearDataSourceContextDirty(getNativeHandle());
            mDataSourceContextChangeNotified = false;
        }
        return mCachedDataSourceContext;
    }

    private static native long nExecuteCommands(long nativeHandle, String commands);
    private native long nGetId(long nativeHandle);
    private native boolean nIsVisualContextDirty(long nativeHandle);
    private native void nClearVisualContextDirty(long nativeHandle);
    private native String nSerializeVisualContext(long nativeHandle);
    private native boolean nIsDataSourceContextDirty(long nativeHandle);
    private native void nClearDataSourceContextDirty(long nativeHandle);
    private native String nSerializeDataSourceContext(long nativeHandle);
}
