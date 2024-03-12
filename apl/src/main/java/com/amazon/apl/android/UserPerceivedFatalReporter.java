/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android;

import android.util.Log;

import com.amazon.apl.android.dependencies.IUserPerceivedFatalCallback;
import com.amazon.apl.android.dependencies.impl.NoOpUserPerceivedFatalCallback;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * UserPerceivedFatalReporter. Controls the lifecycle of UPF reporting and
 * ensure only one success or failure is reported for a given interaction.
 */
public class UserPerceivedFatalReporter {

    private static final String TAG = "APLController";
    private IUserPerceivedFatalCallback mUserPerceivedFatalCallback;
    private final AtomicBoolean mIsUpfAlreadyReported = new AtomicBoolean(false);

    public UserPerceivedFatalReporter() {
        this(new NoOpUserPerceivedFatalCallback());
    }

    public UserPerceivedFatalReporter(IUserPerceivedFatalCallback userPerceivedFatalCallback) {
        this.mUserPerceivedFatalCallback = userPerceivedFatalCallback;
        mIsUpfAlreadyReported.set(false);
    }

    /**
     * To be called when an interaction is successful.
     */
    public void reportSuccess() {
        if (!mIsUpfAlreadyReported.get()) {
            mUserPerceivedFatalCallback.onSuccess();
            mIsUpfAlreadyReported.set(true);
        } else {
            Log.w(TAG, "Attempt to report UPF success ignored, since UPF status has already been reported for this interaction.");
        }
    }

    /**
     * To be called when user perceives UPF and report it to runtime.
     * @param fatalError  UpfConstant.
     */
    public void reportFatal(UpfReason fatalError) {
        if (!mIsUpfAlreadyReported.get()) {
            mUserPerceivedFatalCallback.onFatalError(fatalError.toString());
            mIsUpfAlreadyReported.set(true);
        } else {
            Log.w(TAG, "Attempt to report UPF fatal ignored, since UPF status has already been reported for this interaction.");
        }
    }
    
    public enum UpfReason {
        BACK_EXTENSION_FAILURE("APL_FATAL.BackExtensionFailed"),
        APL_INITIALIZATION_FAILURE("APL_FATAL.AplInitializationFailed"),
        REQUIRED_EXTENSION_LOADING_FAILURE("APL_FATAL.RequiredExtensionLoadingFailed"),
        ROOT_CONTEXT_CREATION_FAILURE("APL_FATAL.RootContextCreationFailed"),
        CONTENT_CREATION_FAILURE("APL_FATAL.ContentCreationFailed"),
        CONTENT_RESOLUTION_FAILURE("APL_FATAL.ContentResolutionFailed");

        private final String mUpfError;

        UpfReason(String upfError) {
            mUpfError = upfError;
        }

        @Override
        public String toString() {
            return mUpfError;
        }
    }
}