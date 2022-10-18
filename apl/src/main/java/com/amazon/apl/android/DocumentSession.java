/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import com.amazon.common.BoundObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines a grouping of one or more related documents to render. Typically, an APL runtime will map this to
 * AVS skill sessions.
 */
public class DocumentSession extends BoundObject {
    public interface ISessionEndedCallback {
        void onEnded(DocumentSession session);
    }

    private List<ISessionEndedCallback> mCallbacks;
    private IAPLController mController;

    public static DocumentSession create() {
        return new DocumentSession();
    }

    public void bind(IAPLController controller) {
        mController = controller;
    }

    private DocumentSession() {
        mCallbacks = new ArrayList<>();
        final long handle = nCreate();
        bind(handle);
    }

    public String getId() {
        return nGetId(getNativeHandle());
    }

    public boolean hasEnded() {
        return nHasEnded(getNativeHandle());
    }

    void onSessionEnded(ISessionEndedCallback callback) {
        mCallbacks.add(callback);
    }

    public void end() {
        if (hasEnded()) return;

        if (mController != null) {
            mController.executeOnCoreThread(() -> nEnd(getNativeHandle()));
        }

        for (ISessionEndedCallback callback : mCallbacks) {
            callback.onEnded(this);
        }
    }

    private native long nCreate();
    private native String nGetId(long sessionHandler_);
    private native boolean nHasEnded(long sessionHandler_);
    private native void nEnd(long sessionHandler_);
}
