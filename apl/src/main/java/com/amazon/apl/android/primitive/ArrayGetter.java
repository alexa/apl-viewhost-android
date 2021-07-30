/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.primitive;

import com.amazon.apl.android.BoundObject;
import com.amazon.apl.enums.APLEnum;

/**
 * Helper class for abstracting retrieving the size and other objects from an APLObject that is an Array.
 * @param <K> An object that is iterable (i.e. {@link Filters}.
 * @param <T> The value in the array.
 */
abstract class ArrayGetter<K extends IterableProperty<T>, T> {
    protected final BoundObject mBoundObject;
    protected final APLEnum mPropertyKey;

    ArrayGetter(BoundObject boundObject, APLEnum propertyKey) {
        mBoundObject = boundObject;
        mPropertyKey = propertyKey;
    }

    /**
     * @return pointer to owner of this array.
     */
    long getNativeHandle() {
        return mBoundObject.getNativeHandle();
    }

    /**
     * @return index for property pointing to this array.
     */
    int getIndex() {
        return mPropertyKey.getIndex();
    }

    /**
     * @return the number of items in the array.
     */
    int size() {
        return nSize(getNativeHandle(), getIndex());
    }

    /**
     * @return an iterable property.
     */
    abstract K builder();

    /**
     * Retrieve the object at the index. This is deferred to subclasses so that they may call the
     * appropriate jni methods.
     * @param index the index
     * @return the object at the index.
     */
    abstract T get(int index);

    private static native int nSize(long componentHandle, int propertyKey);
}
