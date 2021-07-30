/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.touch;

import android.view.MotionEvent;

import androidx.test.core.view.MotionEventBuilder;
import androidx.test.core.view.PointerCoordsBuilder;
import androidx.test.core.view.PointerPropertiesBuilder;

import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.enums.PointerEventType;
import com.amazon.apl.enums.PointerType;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PointerTrackerTest extends ViewhostRobolectricTest {

    private PointerTracker pointerTracker = new PointerTracker();

    @Test
    public void test_singleTap() {
        MotionEventStream stream = new MotionEventStream()
                .down(1, 2)
                .up(0, 3, 4);

        List<Pointer> expectedPointers = Arrays.asList(
                Pointer.create(0, PointerType.kTouchPointer, PointerEventType.kPointerDown, 1, 2),
                Pointer.create(0, PointerType.kTouchPointer, PointerEventType.kPointerUp, 3, 4)
        );

        assertEquals(expectedPointers, stream.translate());
        assertNull(pointerTracker.cancelPointer());
    }

    @Test
    public void test_downMoveUp() {
        MotionEventStream stream = new MotionEventStream()
                .down(1, 2)
                .move(0, 3, 4)
                .move(0, 5, 6)
                .up(0, 5, 6);

        List<Pointer> expectedPointers = Arrays.asList(
                Pointer.create(0, PointerType.kTouchPointer, PointerEventType.kPointerDown, 1, 2),
                Pointer.create(0, PointerType.kTouchPointer, PointerEventType.kPointerMove, 3, 4),
                Pointer.create(0, PointerType.kTouchPointer, PointerEventType.kPointerMove, 5, 6),
                Pointer.create(0, PointerType.kTouchPointer, PointerEventType.kPointerUp, 5, 6)
        );

        assertEquals(expectedPointers, stream.translate());
        assertNull(pointerTracker.cancelPointer());
    }

    @Test
    public void test_doubleTap() {
        MotionEventStream stream = new MotionEventStream()
                .down(1, 2)
                .up(0, 1, 2)
                .down(3, 4)
                .up(0, 3, 4);

        List<Pointer> expectedPointers = Arrays.asList(
                Pointer.create(0, PointerType.kTouchPointer, PointerEventType.kPointerDown, 1, 2),
                Pointer.create(0, PointerType.kTouchPointer, PointerEventType.kPointerUp, 1, 2),
                Pointer.create(0, PointerType.kTouchPointer, PointerEventType.kPointerDown, 3, 4),
                Pointer.create(0, PointerType.kTouchPointer, PointerEventType.kPointerUp, 3, 4)
        );

        assertEquals(expectedPointers, stream.translate());
        assertNull(pointerTracker.cancelPointer());
    }

    @Test
    public void test_cancel() {
        MotionEventStream stream = new MotionEventStream()
                .down(1, 2)
                .cancel(0, 3, 4);

        List<Pointer> expectedPointers = Arrays.asList(
                Pointer.create(0, PointerType.kTouchPointer, PointerEventType.kPointerDown, 1, 2),
                Pointer.create(0, PointerType.kTouchPointer, PointerEventType.kPointerCancel, 3, 4)
        );

        assertEquals(expectedPointers, stream.translate());
        assertNull(pointerTracker.cancelPointer());
    }

    @Test
    public void test_toCancelNullPointer() {
        assertNull(pointerTracker.cancelPointer());
    }

    @Test
    public void test_toCancel() {
        MotionEventStream stream = new MotionEventStream()
                .down(1, 2)
                .move(0, 3, 4);

        List<Pointer> expectedPointers = Arrays.asList(
                Pointer.create(0, PointerType.kTouchPointer, PointerEventType.kPointerDown, 1, 2),
                Pointer.create(0, PointerType.kTouchPointer, PointerEventType.kPointerMove, 3, 4)
        );

        assertEquals(expectedPointers, stream.translate());
        assertEquals(Pointer.create(0, PointerType.kTouchPointer, PointerEventType.kPointerCancel, 3, 4), pointerTracker.cancelPointer());
        // Second cancel returns null
        assertNull(pointerTracker.cancelPointer());
    }

    public void test_toCancelWithoutPrimaryPointerReturnsNull() {
        MotionEventStream stream = new MotionEventStream()
                .down(1, 2)
                .up(0, 3, 4);

        List<Pointer> expectedPointers = Arrays.asList(
                Pointer.create(0, PointerType.kTouchPointer, PointerEventType.kPointerDown, 1, 2),
                Pointer.create(0, PointerType.kTouchPointer, PointerEventType.kPointerUp, 3, 4)
        );

        assertEquals(expectedPointers, stream.translate());
        assertNull(pointerTracker.cancelPointer());
    }

    @Test
    public void test_secondaryPointerReturnsMoves() {
        // finger 1 press -> finger 2 press -> move -> finger 2 lift -> finger 1 lift
        MotionEventStream stream = new MotionEventStream()
                .down(1, 2)
                .secondaryDown(1, new PointerData(0, 1, 2), new PointerData(1, 3, 4))
                .move(new PointerData(0, 3, 4), new PointerData(1, 9, 10))
                .secondaryUp(1, new PointerData(0, 1, 2), new PointerData(1, 9, 10))
                .up(0,5, 6);

        List<Pointer> expectedPointers = Arrays.asList(
                Pointer.create(0, PointerType.kTouchPointer, PointerEventType.kPointerDown, 1, 2),
                Pointer.create(0, PointerType.kTouchPointer, PointerEventType.kPointerMove, 1, 2),
                Pointer.create(0, PointerType.kTouchPointer, PointerEventType.kPointerMove, 3, 4),
                Pointer.create(0, PointerType.kTouchPointer, PointerEventType.kPointerMove, 1, 2),
                Pointer.create(0, PointerType.kTouchPointer, PointerEventType.kPointerUp, 5, 6)
        );

        assertEquals(expectedPointers, stream.translate());
        assertNull(pointerTracker.cancelPointer());
    }

    @Test
    public void test_firstPointerUpTriggersUp() {
        // finger 1 press -> finger 2 press -> finger 1 lift -> finger 2 move -> finger 2 lift
        MotionEventStream stream = new MotionEventStream()
                .down(1, 2)
                .secondaryDown(1, new PointerData(0, 3, 4), new PointerData(1, 5, 6))
                .secondaryUp(0, new PointerData(0, 7, 8), new PointerData(1, 9, 10))
                .move(1, 11, 12)
                .up(1, 11, 12);

        List<Pointer> expectedPointers = Arrays.asList(
                Pointer.create(0, PointerType.kTouchPointer, PointerEventType.kPointerDown, 1, 2),
                Pointer.create(0, PointerType.kTouchPointer, PointerEventType.kPointerMove, 3, 4),
                Pointer.create(0, PointerType.kTouchPointer, PointerEventType.kPointerUp, 7, 8),
                null,
                null
        );

        assertEquals(expectedPointers, stream.translate());
        assertNull(pointerTracker.cancelPointer());
    }

    /**
     * Represents a stream of motion events.
     */
    class MotionEventStream {
        static final int delta = 100;
        int startTime = 100;
        private List<MotionEvent> motionEvents = new ArrayList<>();

        private MotionEventStream() {

        }

        List<Pointer> translate() {
            List<Pointer> pointers = new ArrayList<>();
            for (MotionEvent event : motionEvents) {
                Pointer pointer = pointerTracker.trackAndGetPointer(event);
                if (pointer == null) {
                    pointers.add(null);
                } else {
                    pointers.add(new Pointer(pointer));
                }
            }
            return pointers;
        }

        MotionEventStream down(float x, float y) {
            return addEvent(MotionEvent.ACTION_DOWN, 0, new PointerData(0, x, y));
        }

        MotionEventStream up(int id, float x, float y) {
            return addEvent(MotionEvent.ACTION_UP, 0, new PointerData(id, x, y));
        }

        MotionEventStream move(int id, float x, float y) {
            return addEvent(MotionEvent.ACTION_MOVE, 0, new PointerData(id, x, y));
        }

        MotionEventStream cancel(int id, float x, float y) {
            return addEvent(MotionEvent.ACTION_CANCEL, 0, new PointerData(id, x, y));
        }

        MotionEventStream move(PointerData... pointers) {
            return addEvent(MotionEvent.ACTION_MOVE, 0, pointers);
        }

        MotionEventStream secondaryDown(int actionIndex, PointerData... pointers) {
            return addEvent(MotionEvent.ACTION_POINTER_DOWN, actionIndex, pointers);
        }

        MotionEventStream secondaryUp(int actionIndex, PointerData... pointers) {
            return addEvent(MotionEvent.ACTION_POINTER_UP, actionIndex, pointers);
        }

        private MotionEventStream addEvent(int action, int actionIndex, PointerData... pointers) {
            MotionEventBuilder builder = MotionEventBuilder.newBuilder()
                    .setAction(action)
                    .setActionIndex(actionIndex);

            for (PointerData data : pointers) {
                MotionEvent.PointerCoords pointerCoords = PointerCoordsBuilder.newBuilder()
                        .setCoords(data.x, data.y)
                        .build();

                MotionEvent.PointerProperties pointerProps = PointerPropertiesBuilder.newBuilder()
                        .setId(data.id)
                        .setToolType(MotionEvent.TOOL_TYPE_FINGER)
                        .build();

                builder.setPointer(pointerProps, pointerCoords);
            }
            motionEvents.add(builder.build());
            startTime += delta;
            return this;
        }
    }

    static class PointerData {
        final int id;
        final float x, y;

        PointerData(int id, float x, float y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }
    }
}
