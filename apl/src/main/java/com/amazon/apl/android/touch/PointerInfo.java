/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.touch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.MotionEvent;

import com.amazon.apl.enums.PointerEventType;
import com.amazon.apl.enums.PointerType;

/**
 * A class that extracts the primary {@link Pointer} from a {@link MotionEvent}.
 */
public final class PointerInfo {
    private Pointer mPrimaryPointer;
    private MotionEvent mMotionEvent;
    private int mActionIndex;
    private int mPrimaryPointerIndex;

    private PointerInfo(@Nullable final Pointer primaryPointer, @NonNull final MotionEvent event) {
        update(primaryPointer, event);
    }

    /**
     * Creates a PointerInfo from a {@link Pointer} and a {@link MotionEvent}.
     *
     * @param primaryPointer    the active primary pointer (may be null)
     * @param event             the motion event
     * @return a new PointerInfo.
     */
    static PointerInfo create(@Nullable final Pointer primaryPointer, @NonNull final MotionEvent event) {
        return new PointerInfo(primaryPointer, event);
    }
    /**
     * Updates this PointerInfo with a new {@link Pointer} and {@link MotionEvent}.
     *
     * @param primaryPointer    the active primary pointer (may be null)
     * @param event             the motion event
     */
    void update(@Nullable final Pointer primaryPointer, @NonNull final MotionEvent event) {
        mPrimaryPointer = primaryPointer;
        mMotionEvent = event;
        mActionIndex = mMotionEvent.getActionIndex();
        mPrimaryPointerIndex = mPrimaryPointer != null ? mMotionEvent.findPointerIndex(mPrimaryPointer.getId()) : -1;
        translate();
    }

    /**
     * @return the primary pointer from the motion event (may be null).
     */
    @Nullable
    Pointer getPointer() {
        return mPrimaryPointer;
    }

    private void translate() {
        final int actionMasked = mMotionEvent.getActionMasked();
        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN:
                handleDownAction();
                break;
            case MotionEvent.ACTION_UP:
                handleUpAction();
                break;
            case MotionEvent.ACTION_POINTER_UP:
                handlePointerUpAction();
                break;
            case MotionEvent.ACTION_CANCEL:
                handleCancelAction();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_MOVE:
            default:
                handleMoveAction();
                break;
        }
    }

    private void handleDownAction() {
        final int pointerId = mMotionEvent.getPointerId(mActionIndex);
        final PointerType pointerType = mMotionEvent.getToolType(mActionIndex) == MotionEvent.TOOL_TYPE_MOUSE
                ? PointerType.kMousePointer
                : PointerType.kTouchPointer;
        final float x = mMotionEvent.getX();
        final float y = mMotionEvent.getY();
        
        mPrimaryPointer = Pointer.create(
                pointerId,
                pointerType,
                PointerEventType.kPointerDown,
                x, 
                y);
    }

    private void handleUpAction() {
        if (isPrimaryPointerInEvent()) {
            mPrimaryPointer.update(PointerEventType.kPointerUp, mMotionEvent.getX(), mMotionEvent.getY());
        }
    }

    private void handlePointerUpAction() {
        if (isPrimaryPointerEventTarget()) {
            mPrimaryPointer.update(PointerEventType.kPointerUp, mMotionEvent.getX(mActionIndex), mMotionEvent.getY(mActionIndex));
        } else {
            handleMoveAction();
        }
    }

    private void handleMoveAction() {
        if (isPrimaryPointerInEvent()) {
            mPrimaryPointer.update(PointerEventType.kPointerMove, mMotionEvent.getX(mPrimaryPointerIndex), mMotionEvent.getY(mPrimaryPointerIndex));
        }
    }

    private void handleCancelAction() {
        if (isPrimaryPointerInEvent()) {
            mPrimaryPointer.update(PointerEventType.kPointerCancel, mMotionEvent.getX(), mMotionEvent.getY());
        }
    }

    private boolean isPrimaryPointerInEvent() {
        return mPrimaryPointerIndex != -1;
    }

    private boolean isPrimaryPointerEventTarget() {
        return mPrimaryPointerIndex == mActionIndex;
    }
}
