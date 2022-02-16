/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.amazon.common.BoundObject;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * Wrapper class for the APL Core LiveArray.
 *
 * To use a LiveArray, register the array with {@link RootConfig#liveData(String, LiveArray)}.
 * Make changes to the LiveArray from your UX thread.  Changes in the LiveArray will
 * propagate through the APL data-binding context and show up on the screen.
 */
public class LiveArray extends BoundObject implements List<Object> {
    /**
     * Store a copy of the array that is accessible to Java.  A copy of this
     * array is stored in attached C++ object.  The main purpose of the backing
     * array is so that users of a LiveArray can iterate over the array, search
     * for elements, and otherwise treat the LiveArray as a normal Java List.
     */
    private ArrayList<Object> mBackingArray;

    /**
     * Exception class for unrecoverable exceptions
     *
     * For most list overrides we try to perform the operation on the backing array
     * first.  The backing array will throw standard List exceptions for all common
     * errors.  Once the backing array has been modified we update the native APL object.
     * If that fails we have an unsynchronized LiveArray and no way to recover.  We
     * throw the LiveArrayException and expect to terminate.
     */
    private static class LiveArrayException extends RuntimeException {
        public LiveArrayException(String message) {
            super(message);
        }
    }

    /**
     * Construct an empty LiveArray
     * @return The live array
     */
    static public LiveArray create() {
        return new LiveArray();
    }

    /**
     * Construct a live array initialized by an Object array
     * @param array The object array
     * @return The live array
     */
    static public LiveArray create(Object[] array) {
        LiveArray liveArray = new LiveArray();
        Collections.addAll(liveArray.mBackingArray, array);
        nPushBackRange(liveArray.getNativeHandle(), array);
        return liveArray;
    }

    /**
     * Construct a live array initialized by a collection
     * @param collection The collection
     * @return The live array
     */
    static public LiveArray create(Collection<?> collection) {
        LiveArray liveArray = new LiveArray();
        liveArray.mBackingArray.addAll(collection);
        nPushBackRange(liveArray.getNativeHandle(), collection.toArray());
        return liveArray;
    }

    /**
     * Construct an empty LiveArray.
     */
    private LiveArray() {
        long handle = nCreate();
        bind(handle);
        mBackingArray = new ArrayList<>();
    }

    @Override
    public boolean add(Object element) {
        mBackingArray.add(element);
        nPushBack(getNativeHandle(), element);
        return true;
    }

    @Override
    public void add(int index, Object element) {
        mBackingArray.add(index, element);
        if (!nInsert(getNativeHandle(), index, element))
            throw new LiveArrayException("Insert at index="+index+" should not fail");
    }

    @Override
    public boolean addAll(@NonNull Collection<?> collection) {
        if (!mBackingArray.addAll(collection))
            return false;

        nPushBackRange(getNativeHandle(), collection.toArray());
        return true;
    }

    @Override
    public boolean addAll(int index, @NonNull Collection<?> collection) {
        if (!mBackingArray.addAll(index, collection))
            return false;

        if (!nInsertRange(getNativeHandle(), index, collection.toArray()))
            throw new LiveArrayException("Insert range at index="+index+" should not fail");

        return true;
    }

    @Override
    public void clear() {
        mBackingArray.clear();
        nClear(getNativeHandle());
    }

    @Override
    public boolean contains(@Nullable Object element) {
        return mBackingArray.contains(element);
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> collection) {
        return mBackingArray.containsAll(collection);
    }

    @Override
    public Object get(int index) {
        return mBackingArray.get(index);
    }

    @Override
    public int indexOf(@Nullable Object element) {
        return mBackingArray.indexOf(element);
    }

    @Override
    public boolean isEmpty() {
        return mBackingArray.isEmpty();
    }

    @NonNull
    @Override
    public Iterator<Object> iterator() {
        return new LiveArrayIterator(0);
    }

    @Override
    public int lastIndexOf(@Nullable Object element) {
        return mBackingArray.lastIndexOf(element);
    }

    @NonNull
    @Override
    public ListIterator<Object> listIterator() {
        return new LiveArrayIterator(0);
    }

    @NonNull
    @Override
    public ListIterator<Object> listIterator(int index) {
        return new LiveArrayIterator(index);
    }

    @Override
    public boolean retainAll(@NonNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @NonNull
    @Override
    public Object remove(int index) {
        Object result = mBackingArray.remove(index);
        if (!nRemove(getNativeHandle(), index, 1))
            throw new LiveArrayException("Illegal remove at index="+index);
        return result;
    }

    @Override
    public boolean remove(@Nullable Object element) {
        int index = mBackingArray.indexOf(element);
        if (index < 0)
            return false;

        mBackingArray.remove(index);
        if (!nRemove(getNativeHandle(), index, 1))
            throw new LiveArrayException("Illegal remove of element by value at index="+index);
        return true;
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> collection) {
        boolean changed = false;
        for (Object c : collection)
            while (remove(c))
                changed = true;

        return changed;
    }

    @Override
    public Object set(int index, Object element) {
        Object result = mBackingArray.set(index, element);
        if (!nUpdate(getNativeHandle(), index, element))
            throw new LiveArrayException("Illegal set at index="+index);
        return result;
    }

    @Override
    public int size() {
        return mBackingArray.size();
    }

    @NonNull
    @Override
    public List<Object> subList(int fromIndex, int toIndex) {
        return new LiveArraySubList(fromIndex, toIndex);
    }

    @Nullable
    @Override
    public Object[] toArray() {
        return mBackingArray.toArray();
    }

    @Override
    public <T> T[] toArray(@Nullable T[] a) {
        return mBackingArray.toArray(a);
    }


    /**
     * Internal class for supporting the sublist operation
     */
    private class LiveArraySubList extends AbstractList<Object> {
        int mFromIndex;
        int mToIndex;

        public LiveArraySubList(int fromIndex, int toIndex) {
            if (fromIndex < 0 || toIndex > LiveArray.this.size() || fromIndex > toIndex)
                throw new IndexOutOfBoundsException();

            mFromIndex = fromIndex;
            mToIndex = toIndex;
        }

        @Override
        public Object get(int index) {
            if (index < 0 || index >= size())
                throw new IndexOutOfBoundsException();

            return LiveArray.this.get(mFromIndex + index);
        }

        @Override
        public int size() {
            return mToIndex - mFromIndex;
        }
    }

    /**
     * Internal class for supporting the list iterator operation.
     * For the moment we enforce a non-modifiable iterator
     */
    private class LiveArrayIterator implements ListIterator<Object> {
        int mIndex;

        public LiveArrayIterator(int index) {
            if (index < 0 || index > LiveArray.this.size())
                throw new IndexOutOfBoundsException();

            mIndex = index;
        }

        @Override
        public boolean hasNext() {
            return mIndex < LiveArray.this.size();
        }

        @Override
        public Object next() {
            if (mIndex >= LiveArray.this.size())
                throw new NoSuchElementException();
            Object result = LiveArray.this.get(mIndex);
            mIndex += 1;
            return result;
        }

        @Override
        public boolean hasPrevious() {
            return mIndex > 0;
        }

        @Override
        public Object previous() {
            if (mIndex <= 0)
                throw new NoSuchElementException();
            mIndex -= 1;
            return LiveArray.this.get(mIndex);
        }

        @Override
        public int nextIndex() {
            return mIndex;
        }

        @Override
        public int previousIndex() {
            return mIndex - 1;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(Object o) {
            throw new UnsupportedOperationException();
        }
    }

    /*************************** Additional array manipulation methods ***************************/

    /**
     * Remove a range of objects from the array.
     * @param position The starting position at which to remove objects.
     * @param count The number of objects to remove.
     * @return True if the position was valid and at least one object was removed.
     */
    public void removeRange(int position, int count) {
        if (position < 0 || count <= 0 || position + count > mBackingArray.size())
            throw new IndexOutOfBoundsException();

        for (int i = 0 ; i < count ; i++)
            mBackingArray.remove(position);

        if (!nRemove(getNativeHandle(), position, count))
            throw new LiveArrayException("Unexpected removeRange position="+position+" count="+count);
    }

    /**
     * Update a range of objects to new values.  The position must fall within [0,size-array.length]
     * @param position The position to update.
     * @param collection An array of objects to update
     * @return True if the position was valid and at least one object was updated.
     */
    public void setRange(int position, @NonNull Collection<?> collection) {
        Object[] array = collection.toArray();
        if (position < 0 || position + array.length > mBackingArray.size())
            throw new IndexOutOfBoundsException();

        if (array.length > 0) {
            for (int i = 0; i < array.length; i++)
                mBackingArray.set(position + i, array[i]);

            if (!nUpdateRange(getNativeHandle(), position, array))
                throw new LiveArrayException("Unexpected update range error position="+position);
        }
    }

    /**
     * @return The size of the C++ array.  This method is for testing.
     */
    @VisibleForTesting
    public int innerSize() {
        return nSize(getNativeHandle());
    }

    /**
     * Retrieve an object from the C++ array.  This method is for testing.
     * @param position The index to retrieve.
     * @return The C++ object converted to Java
     */
    @VisibleForTesting
    public Object innerGet(int position) {
        if (position < 0 || position >= mBackingArray.size())
            throw new ArrayIndexOutOfBoundsException();

        return nAt(getNativeHandle(), position);
    }

    private static native long nCreate();
    private static native void nClear(long nativeHandle);
    private static native int nSize(long nativeHandle);
    private static native Object nAt(long nativeHandle, int position);
    private static native boolean nInsert(long nativeHandle, int position, Object value);
    private static native boolean nInsertRange(long nativeHandle, int position, Object[] array);
    private static native boolean nRemove(long nativeHandle, int position, int count);
    private static native boolean nUpdate(long nativeHandle, int position, Object value);
    private static native boolean nUpdateRange(long nativeHandle, int position, Object[] array);
    private static native void nPushBack(long nativeHandle, Object value);
    private static native void nPushBackRange(long nativeHandle, Object[] objectArray);
}
