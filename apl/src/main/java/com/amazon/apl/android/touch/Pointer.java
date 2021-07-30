/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.touch;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.amazon.apl.enums.PointerEventType;
import com.amazon.apl.enums.PointerType;

import java.util.Objects;

/**
 * Simple value class to pass pointer information to core.
 */
public final class Pointer {
    private final int id;
    private final PointerType pointerType;

    // Mutable fields
    private PointerEventType eventType;
    private float x;
    private float y;

    private Pointer(final int id,
            @NonNull final PointerType pointerType,
            @NonNull final PointerEventType eventType,
            final float x,
            final float y) {
        this.id = id;
        this.pointerType = pointerType;
        this.eventType = eventType;
        this.x = x;
        this.y = y;
    }

    @VisibleForTesting
    Pointer(Pointer other) {
        this(other.id, other.pointerType, other.eventType, other.x, other.y);
    }

    /**
     * Creates a new Pointer object with the following data.
     *
     * @param id            the id of the pointer (typically 0)
     * @param pointerType   the type (typically touch)
     * @param eventType     the event type
     * @param x             the x position
     * @param y             the y position
     * @return              a new Pointer object
     */
    public static Pointer create(final int id,
                                 @NonNull final PointerType pointerType,
                                 @NonNull final PointerEventType eventType,
                                 final float x,
                                 final float y) {
        return new Pointer(id, pointerType, eventType, x, y);
    }

    /**
     * Converts this pointer to a cancel pointer.
     */
    @NonNull
    Pointer toCancel() {
        return update(PointerEventType.kPointerCancel, x, y);
    }

    /**
     * Updates and returns this pointer object with new data.
     *
     * @param eventType     the event type
     * @param x             the x position
     * @param y             the y position
     * @return this
     */
    public Pointer update(@NonNull final PointerEventType eventType,
                       final float x,
                       final float y) {
        this.eventType = eventType;
        this.x = x;
        this.y = y;
        return this;
    }

    /**
     * Translates this pointer object with offsets.
     *
     * @param offsetX the x offset
     * @param offsetY the y offset
     */
    public void translate(int offsetX, int offsetY) {
        x += offsetX;
        y += offsetY;
    }

    /**
     * @return the id for the pointer.
     */
    public int getId() {
        return id;
    }

    /**
     * @return the pointer type (mouse or touch).
     */
    @NonNull
    public PointerType getPointerType() {
        return pointerType;
    }

    /**
     * @return the event type (down, move, up or cancel).
     */
    @NonNull
    public PointerEventType getPointerEventType() {
        return eventType;
    }

    /**
     * @return the x coordinate
     */
    public float getX() {
        return x;
    }

    /**
     * @return the y coordinate
     */
    public float getY() {
        return y;
    }

    /**
     * @return true if the pointers are the same type and id.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Pointer)) {
            return false;
        }
        Pointer other = (Pointer) o;
        return id == other.id
                && this.pointerType == other.pointerType
                && this.eventType == other.eventType
                && this.x == other.x
                && this.y == other.y;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, pointerType, eventType, x, y);
    }

    @SuppressWarnings("DefaultLocale")
    @NonNull
    public String toString() {
        return String.format("Pointer[id=%d, type=%s, eventType=%s, x=%f, y=%f]", id, pointerType, eventType, x, y);
    }
}
