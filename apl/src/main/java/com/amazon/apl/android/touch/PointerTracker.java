/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.touch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.MotionEvent;

import com.amazon.apl.enums.PointerEventType;

/**
 * Class responsible for tracking {@link MotionEvent}s and producing {@link Pointer}s.
 */
public final class PointerTracker {
    /**
     * The active pointer (may be null).
     */
    @Nullable
    private Pointer mPointer;

    /**
     * The cached pointer info (only created once).
     */
    private PointerInfo mPointerInfo;

    /**
     * Tracks a motion event and returns the primary pointer from it.
     *
     * Will return null if either the primary pointer is no longer active,
     * or the motion event doesn't target the primary pointer.
     *
     * @param event a motion event
     * @return the primary pointer from the event,
     *      or null if the primary pointer is no longer active.
     */
    @Nullable
    public Pointer trackAndGetPointer(@NonNull MotionEvent event) {
        createOrUpdatePointerInfo(event);
        return mPointerInfo.getPointer();
    }

    /**
     * @return the active pointer as a cancel pointer,
     *      or null if no pointer is active.
     */
    @Nullable
    public final Pointer cancelPointer() {
        Pointer cancel = mPointer != null ? mPointer.toCancel() : null;
        mPointer = null;
        return cancel;
    }

    private void createOrUpdatePointerInfo(@NonNull MotionEvent event) {
        if (mPointerInfo == null) {
            mPointerInfo = PointerInfo.create(mPointer, event);
        } else {
            mPointerInfo.update(mPointer, event);
        }

        updatePointer();
    }

    private void updatePointer() {
        mPointer = mPointerInfo.getPointer();
        if (isPointerUp()) {
            mPointer = null;
        }
    }

    private boolean isPointerUp() {
        return mPointer != null
                && (mPointer.getPointerEventType() == PointerEventType.kPointerUp
                   || mPointer.getPointerEventType() == PointerEventType.kPointerCancel);

    }
}
