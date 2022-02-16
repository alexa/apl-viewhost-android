/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.annotation.NonNull;

import com.amazon.apl.android.APLController;
import com.amazon.apl.android.APLVersionCodes;
import com.amazon.apl.enums.DisplayState;

import java.lang.ref.WeakReference;

/**
 * This class adapts the standard Android lifecycle (start, resume, pause,
 * stop) to corresponding callbacks in APLController. This automatically wires
 * DisplayState and starting/stopping the frame loop.
 * <p>
 * Note that the behavior of this adapter is sensitive to the APL version.
 * Prior to APL 1.8, there was no DisplayState and the status quo was to stop
 * the document's frame loop as soon as the Android activity became paused.
 * This class preserves that legacy behavior while allowing processing in the
 * paused state for documents >= APL 1.8 (i.e. the document is still processing
 * while in the background because it is still visible to the customer)
 */
public class APLAndroidLifecycleAdapter implements LifecycleObserver {
    private final WeakReference<IAPLController> mWeakController;
    private final boolean mIsBackgroundProcessingEnabled;

    private enum Action {
        NONE,
        PAUSE,
        RESUME
    };

    public APLAndroidLifecycleAdapter(@NonNull IAPLController controller) {
        mWeakController = new WeakReference<>(controller);
        mIsBackgroundProcessingEnabled = controller.getDocVersion() >= APLVersionCodes.APL_1_8;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        Action action = mIsBackgroundProcessingEnabled ? Action.RESUME : Action.NONE;
        updateController(DisplayState.kDisplayStateBackground, action);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        Action action = mIsBackgroundProcessingEnabled ? Action.NONE : Action.RESUME;
        updateController(DisplayState.kDisplayStateForeground, action);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        Action action = mIsBackgroundProcessingEnabled ? Action.NONE : Action.PAUSE;
        updateController(DisplayState.kDisplayStateBackground, action);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        Action action = mIsBackgroundProcessingEnabled ? Action.PAUSE : Action.NONE;
        updateController(DisplayState.kDisplayStateHidden, action);
    }

    private void updateController(final DisplayState displayState, final Action action) {
        IAPLController controller = mWeakController.get();
        if (controller == null) {
            return;
        }

        controller.updateDisplayState(displayState);

        if (action == Action.PAUSE) {
            controller.pauseDocument();
        } else if (action == Action.RESUME) {
            controller.resumeDocument();
        }
    }
}
