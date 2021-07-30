/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Vector;

public class Action extends BoundObject {
    private static final String TAG = "Action";
    @NonNull
    final private Vector<Runnable> terminateCallbacks = new Vector<>();
    @Nullable
    private Runnable thenCallback;

    public Action(long handle) {
        bind(handle);
        nInit(getNativeHandle());
    }

    /**
     * Will call `Runnable.run` when this action has been terminated, for example,
     * if `RootContext.cancelExecution` is called.
     *
     * @param callback
     */
    public void addTerminateCallback(final Runnable callback) {
        terminateCallbacks.add(callback);
    }

    /**
     * Will call `Runnable.run` when this action is done executing.
     *
     * @param callback
     */
    public void then(final Runnable callback) {
        thenCallback = callback;
    }

    /**
     * Called from JNI if this action is terminated
     */
    private void onTerminate() {
        for (Runnable terminateCallback : terminateCallbacks) {
            terminateCallback.run();
        }
        terminateCallbacks.clear();
        thenCallback = null;
    }

    /**
     * Called from JNI if this action is resolved
     */
    private void onThen() {
        if (thenCallback != null) {
            thenCallback.run();
        }
        terminateCallbacks.clear();
        thenCallback = null;
    }

    /**
     * @return True if this action is still pending and has not resolved or terminated.
     */
    public boolean isPending() {
        return nIsPending(getNativeHandle());
    }

    /**
     * @return True if this action was terminated.
     */
    public boolean isTerminated() {
        return nIsTerminated(getNativeHandle());
    }

    /**
     * @return True if this action has resolved.
     */
    public boolean isResolved() {
        return nIsResolved(getNativeHandle());
    }

    private native void nInit(long nativeHandle);

    private static native boolean nIsPending(long nativeHandle);

    private static native boolean nIsTerminated(long nativeHandle);

    private static native boolean nIsResolved(long nativeHandle);

}
