/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.amazon.common.BoundObject;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Class supporting use of live maps in the top level data-binding context.
 *
 * LiveMaps are created and modified by Viewhost Runtimes.  For example:
 *
 *      // Before creating the root context:
 *      LiveMap myMap = LiveMap.create();
 *      rootConfig.liveData("MyLiveMap", myMap);
 *
 *      // After the root context has been created:
 *      myMap.put("MyValue", "Changed string object");
 *
 * Inside of the APL document the LiveMap may be used normally in data-binding
 * contexts.  For example:
 *
 *     {
 *       "type": "Text",
 *       "text": "The live object is currently '${MyLiveMap.MyValue}'"
 *     }
 */
public class LiveMap extends BoundObject implements Map<String,Object> {
    private final Map<String,Object> mBackingMap = new HashMap<>();

    /**
     * Construct an empty LiveMap
     * @return the LiveMap
     */
    static public  LiveMap create() {
        return new LiveMap();
    }

    /**
     * Construct a live map initialized by an Object map.
     *
     * Items are copied from the map.
     *
     * @param map The object map
     * @return The LiveMap
     */
    static public  LiveMap create(Map<String,Object> map) {
        LiveMap liveMap = new LiveMap();
        liveMap.putAll(map);
        return liveMap;
    }

    /**
     * Construct an empty LiveMap.
     */
    private LiveMap() {
        long handle = nCreate();
        bind(handle);
    }

    @Override
    public int size() {
        return mBackingMap.size();
    }

    @Override
    public boolean isEmpty() {
        return mBackingMap.isEmpty();
    }

    @Override
    public boolean containsKey(@Nullable Object key) {
        return mBackingMap.containsKey(key);
    }

    @Override
    public boolean containsValue(@Nullable Object value) {
        return mBackingMap.containsValue(value);
    }

    @Override
    public Object get(@Nullable Object key) {
        return mBackingMap.get(key);
    }

    @Override
    @Nullable
    public Object put(@NonNull String key, @NonNull Object value) {
        nSet(getNativeHandle(), key, value);
        return mBackingMap.put(key, value);
    }

    @Override
    @Nullable
    public Object remove(@Nullable Object key) {
        nRemove(getNativeHandle(), (String) key);
        return mBackingMap.remove(key);
    }

    @Override
    public void putAll(@NonNull Map<? extends String, ? extends Object> m) {
        mBackingMap.putAll(m);
        for (Entry<? extends String, ? extends Object> entry: m.entrySet()) {
            nSet(getNativeHandle(), entry.getKey(), entry.getValue());
        }
    }

    public void clear() {
        mBackingMap.clear();
        nClear(getNativeHandle());
    }

    @NonNull
    public Set<String> keySet() {
        return new KeySet();
    }

    @NonNull
    @Override
    public Collection<Object> values() {
        return new ValueCollection();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LiveMap)) {
            return false;
        }
        LiveMap other = (LiveMap) o;
        return mBackingMap.equals(other.mBackingMap);
    }

    @Override
    public int hashCode() {
        return mBackingMap.hashCode();
    }

    @NonNull
    @Override
    public Set<Entry<String,Object>> entrySet() {
        return new EntrySet();
    }

    /**
     * EntrySet view of Map.
     */
    private class EntrySet extends AbstractSet<Entry<String,Object>> {
        @Override
        public int size() {
            return LiveMap.this.size();
        }

        @Override
        public Iterator<Entry<String,Object>> iterator() {
            return new EntryIterator();
        }

        private class EntryIterator implements Iterator<Entry<String,Object>> {
            private Iterator<Entry<String,Object>> iterator = mBackingMap.entrySet().iterator();
            String curr;

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Entry<String, Object> next() {
                Entry<String,Object> next = iterator.next();
                curr = next.getKey();
                return new LiveEntry(next.getKey(), next.getValue());
            }

            @Override
            public void remove() {
                LiveMap.this.remove(curr);
            }
        }
    }

    /**
     * KeySet view of Map.
     */
    private class KeySet extends AbstractSet<String> {
        @Override
        public int size() {
            return LiveMap.this.size();
        }

        @NonNull
        @Override
        public Iterator<String> iterator() {
            return new KeyIterator();
        }

        private class KeyIterator implements Iterator<String> {
            private Iterator<Entry<String,Object>> iterator = LiveMap.this.entrySet().iterator();

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public String next() {
                return iterator.next().getKey();
            }

            @Override
            public void remove() {
                iterator.remove();
            }
        }
    }

    /**
     * ValueCollection view of Map.
     */
    private class ValueCollection extends AbstractCollection<Object> {
        @Override
        public int size() {
            return LiveMap.this.size();
        }

        @Override
        public Iterator<Object> iterator() {
            return new ValueIterator();
        }

        private class ValueIterator implements Iterator<Object> {
            private Iterator<Entry<String,Object>> iterator = LiveMap.this.entrySet().iterator();

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Object next() {
                return iterator.next().getValue();
            }

            @Override
            public void remove() {
                iterator.remove();
            }
        }
    }

    /**
     * LiveEntry supporting the setValue command.
     */
    private class LiveEntry implements Map.Entry<String,Object> {
        private final String mKey;
        private Object mValue;

        public LiveEntry(@NonNull String key, @NonNull Object value) {
            mKey = key;
            mValue = value;
        }

        @Override
        public String getKey() {
            return mKey;
        }

        @Override
        public Object getValue() {
            return mValue;
        }

        @Override
        public Object setValue(Object value) {
            return put(mKey, value);
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry other = (Map.Entry) o;
            return Objects.equals(mKey, other.getKey()) &&
                    Objects.equals(mValue, other.getValue());
        }

        @Override
        public int hashCode() {
            return Objects.hash(mKey, mValue);
        }
    }

    /**
     * Checks the size of the core peer.
     * @return the size of the core peer.
     */
    @VisibleForTesting
    public int innerSize() {
        return nSize(getNativeHandle());
    }

    /**
     * Checks whether the core peer is empty.
     * @return whether the core peer is empty.
     */
    @VisibleForTesting
    public boolean innerEmpty() {
        return nEmpty(getNativeHandle());
    }

    /**
     * Checks whether the core peer has a key
     * @param key the key.
     * @return true if the key is in the core peer.
     */
    @VisibleForTesting
    public boolean innerHas(String key) {
        return nHas(getNativeHandle(), key);
    }

    /**
     * Gets the value from the core peer.
     * @param key the key.
     * @return the value matching the key from the core peer.
     */
    @VisibleForTesting
    @Nullable
    public Object innerGet(String key) {
        return nGet(getNativeHandle(), key);
    }

    private static native long nCreate();
    private static native int nSize(long nativeHandle);
    private static native boolean nEmpty(long nativeHandle);
    private static native void nClear(long nativeHandle);
    private static native void nSet(long nativeHandle, String key, Object value);
    private static native Object nGet(long nativeHandle, String key);
    private static native boolean nHas(long nativeHandle, String key);
    private static native boolean nRemove(long nativeHandle, String key);
}
