/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import androidx.annotation.NonNull;

import com.amazon.apl.android.scaling.IMetricsTransform;
import com.amazon.common.BoundObject;
import com.amazon.apl.enums.EventProperty;
import com.amazon.apl.enums.EventType;

/**
 * This class represents a single event sent from APL core. It holds the handle to the native peer.
 * It typically wraps a single command - for example, SendEvent or SpeakItem.
 */
public abstract class Event extends BoundObject {
    // The command properties associated with this event.
    @NonNull
    protected final PropertyMap<Event, EventProperty> mProperties;

    // The RootContext
    final protected RootContext mRootContext;

    private final IMetricsTransform mMetricsTransform;

    private boolean isTerminated = false;

    /**
     * Constructs the Event.
     *
     * @param nativeHandle Handle to the native event.
     */
    protected Event(long nativeHandle, RootContext rootContext) {
        bind(nativeHandle);
        // init to listen for terminate callback
        nInit(getNativeHandle());
        mRootContext = rootContext;
        mRootContext.addPending(this);
        mMetricsTransform = mRootContext.getMetricsTransform();
        mProperties = new PropertyMap<Event, EventProperty>() {
            @NonNull
            @Override
            public Event getMapOwner() {
                return Event.this;
            }

            @NonNull
            @Override
            public IMetricsTransform getMetricsTransform() {
                return mMetricsTransform;
            }
        };
    }

    private IMetricsTransform getMetricsTransform() {
        return mMetricsTransform;
    }

    /**
     * Execute the command.
     */
    abstract public void execute();


    /**
     * Terminate the event process.
     */
    public abstract void terminate();

    /**
     * Returns true if this event was terminated by either a `RootContext.cancelExecution`
     * or running `RootContext.executeCommands`.
     *
     * @return true if terminated
     */
    final public boolean isTerminated() {
        return isTerminated;
    }


    /**
     * @return The type of the event
     */
    final public EventType getType() {
        return EventType.valueOf(nGetType(getNativeHandle()));
    }

    /**
     * Retrieve a Command value from the event.
     *
     * @return The collection of Command values.
     */
    @NonNull
    final public PropertyMap<Event, EventProperty> getCommands() {
        return mProperties;
    }

    /**
     * The unique id of the component associated with this event. For the ScrollToIndex command this
     * is the actual component that the index points to.  In all other commands it is
     * what the componentId points to.
     *
     * @return The component associated with this event.
     */
    final public Component getComponent() {
        return mRootContext.getOrInflateComponentWithUniqueId(nGetComponentId(getNativeHandle()));
    }

    /**
     * Resolve the event action. This is expected to be called after the event has been processed.
     */
    final public void resolve() {
        nResolve(getNativeHandle());
        destroy();
    }

    final public void resolve(int arg) {
        nResolveArg(getNativeHandle(), arg);
        destroy();
    }

    /**
     * Resolve with a rect
     *
     * @param x      Rect x
     * @param y      Rect x
     * @param width  Rect width
     * @param height Rect height
     */
    final public void resolve(int x, int y, int width, int height) {
        final IMetricsTransform transform = getMetricsTransform();
        nResolveRect(getNativeHandle(),
                transform.toCore(x),
                transform.toCore(y),
                transform.toCore(width),
                transform.toCore(height));
    }


    @NonNull
    public String debug() {
        return String.format("%s type:%s comp:%s", super.toString(), getType(), getComponent().getUniqueId());
    }

    /**
     * Called by JNI when/if this event has been terminated
     */
    @SuppressWarnings("unused")
    private void onTerminate() {
        isTerminated = true;
        this.terminate();
        destroy();
    }

    /**
     * Remove reference to the event from the RootContext.
     */
    private void destroy() {
        mRootContext.removePending(this);
    }

    private native void nInit(long nativeHandle);

    private static native int nGetType(long nativeHandle);

    @NonNull
    private static native String nGetComponentId(long nativeHandle);

    private static native void nResolve(long nativeHandle);

    private static native void nResolveArg(long nativeHandle, int arg);

    private static native void nResolveRect(long nativeHandle, int x, int y, int width, int height);
}