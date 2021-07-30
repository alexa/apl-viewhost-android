/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.events;

import androidx.annotation.NonNull;

import com.amazon.apl.android.Event;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.dependencies.IOnAplFinishCallback;

/**
 * APL FinishEvent.
 *
 * This event supports the FinishCommand (see https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-standard-commands.html#finish_command)
 */
public class FinishEvent extends Event {
    private IOnAplFinishCallback mOnAplFinishListener;

    private FinishEvent(long nativeHandle, RootContext rootContext, IOnAplFinishCallback listener) {
        super(nativeHandle, rootContext);
        mOnAplFinishListener = listener;
    }

    public static FinishEvent create(final long nativeHandle,
                                     @NonNull final RootContext rootContext,
                                     @NonNull final IOnAplFinishCallback listener) {
        return new FinishEvent(nativeHandle, rootContext, listener);
    }

    @Override
    public void execute() {
        resolve();
        mOnAplFinishListener.onAplFinish();
    }

    @Override
    public void terminate() {
        // Do nothing.
    }
}
