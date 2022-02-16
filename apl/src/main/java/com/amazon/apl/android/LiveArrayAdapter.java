/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.alexaext.ILiveDataUpdateCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * Temporary adapter for LiveArray manipulation with new Extension API.
 */
public class LiveArrayAdapter extends LiveDataAdapter implements List<Object> {
    private ArrayList<Object> mBackingArray;

    private static class LiveArrayException extends RuntimeException {
        public LiveArrayException(String message) {
            super(message);
        }
    }

    static public LiveArrayAdapter create(ILiveDataUpdateCallback updateCallback, String uri, String name) {
        return new LiveArrayAdapter(updateCallback, uri, name);
    }

    /**
     * Construct an empty LiveArray.
     */
    private LiveArrayAdapter(ILiveDataUpdateCallback updateCallback, String uri, String name) {
        super(updateCallback, uri, name);
        mBackingArray = new ArrayList<>();
    }

    @Override
    public JSONObject getLiveDataDefinition() {
        JSONObject result = new JSONObject();
        try {
            result.put("type", "any[]");
            result.put("name", mName);
        } catch (JSONException ex) {}

        return result;
    }

    @Override
    public boolean add(Object element) {
        try {
            JSONArray operations = new JSONArray();
            JSONObject operation = new JSONObject();
            operation.put("type", "Insert");
            operation.put("index", mBackingArray.size());
            operation.put("item", element);
            operation.put("count", 1);
            operations.put(operation);
            if (sendUpdate(operations)) {
                mBackingArray.add(element);
            }
        } catch (JSONException ex) {}
        return false;
    }

    @Override
    public void add(int index, Object element) {
        mBackingArray.add(index, element);
        try {
            JSONArray operations = new JSONArray();
            JSONObject operation = new JSONObject();
            operation.put("type", "Insert");
            operation.put("index", index);
            operation.put("item", element);
            operations.put(operation);
            if (!sendUpdate(operations))
                throw new LiveArrayException("Insert at index="+index+" should not fail");

        } catch (JSONException ex) {}
    }

    @Override
    public boolean addAll(@NonNull Collection<?> collection) {
        int pos = mBackingArray.size();
        if (!mBackingArray.addAll(collection))
            return false;

        try {
            JSONArray operations = new JSONArray();
            JSONArray elements = new JSONArray();
            for (Object element : collection) {
                elements.put(element);
            }

            JSONObject operation = new JSONObject();
            operation.put("type", "Insert");
            operation.put("index", pos);
            operation.put("item", elements);
            operations.put(operation);

            return sendUpdate(operations);
        } catch (JSONException ex) {}

        return false;
    }

    @Override
    public boolean addAll(int index, @NonNull Collection<?> collection) {
        if (!mBackingArray.addAll(index, collection))
            return false;

        try {
            JSONArray operations = new JSONArray();
            JSONArray elements = new JSONArray();
            for (Object element : collection) {
                elements.put(element);
            }

            JSONObject operation = new JSONObject();
            operation.put("type", "Insert");
            operation.put("index", index);
            operation.put("item", elements);
            operations.put(operation);

            return sendUpdate(operations);
        } catch (JSONException ex) {}

        return false;
    }

    @Override
    public void clear() {
        mBackingArray.clear();
        try {
            JSONArray operations = new JSONArray();
            JSONObject operation = new JSONObject();
            operation.put("type", "Clear");
            operations.put(operation);
            sendUpdate(operations);
        } catch (JSONException ex) {}
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
        try {
            JSONArray operations = new JSONArray();
            JSONObject operation = new JSONObject();
            operation.put("type", "Remove");
            operation.put("index", index);
            operations.put(operation);
            sendUpdate(operations);
        } catch (JSONException ex) {}
        return result;
    }

    @Override
    public boolean remove(@Nullable Object element) {
        int index = mBackingArray.indexOf(element);
        if (index < 0)
            return false;

        mBackingArray.remove(index);
        try {
            JSONArray operations = new JSONArray();
            JSONObject operation = new JSONObject();
            operation.put("type", "Remove");
            operation.put("index", index);
            operation.put("count", 1);
            operations.put(operation);
            sendUpdate(operations);
        } catch (JSONException ex) {}
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
        try {
            JSONArray operations = new JSONArray();
            JSONObject operation = new JSONObject();
            operation.put("type", "Update");
            operation.put("index", index);
            operation.put("item", element);
            operations.put(operation);
            return sendUpdate(operations);
        } catch (JSONException ex) {}

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
            if (fromIndex < 0 || toIndex > LiveArrayAdapter.this.size() || fromIndex > toIndex)
                throw new IndexOutOfBoundsException();

            mFromIndex = fromIndex;
            mToIndex = toIndex;
        }

        @Override
        public Object get(int index) {
            if (index < 0 || index >= size())
                throw new IndexOutOfBoundsException();

            return LiveArrayAdapter.this.get(mFromIndex + index);
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
            if (index < 0 || index > LiveArrayAdapter.this.size())
                throw new IndexOutOfBoundsException();

            mIndex = index;
        }

        @Override
        public boolean hasNext() {
            return mIndex < LiveArrayAdapter.this.size();
        }

        @Override
        public Object next() {
            if (mIndex >= LiveArrayAdapter.this.size())
                throw new NoSuchElementException();
            Object result = LiveArrayAdapter.this.get(mIndex);
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
            return LiveArrayAdapter.this.get(mIndex);
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
}
