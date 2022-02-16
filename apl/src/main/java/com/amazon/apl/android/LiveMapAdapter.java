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

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Temporary adapter for LiveMap manipulation with new Extension API.
 */
public class LiveMapAdapter extends LiveDataAdapter implements Map<String,Object> {
    private final Map<String,Object> mBackingMap = new HashMap<>();

    private LiveMapAdapter(ILiveDataUpdateCallback updateCallback, String uri, String name, Map<String, Object> defaults) {
        super(updateCallback, uri, name);
        mBackingMap.putAll(defaults);
    }

    @Override
    public JSONObject getTypeDefinition() {
        JSONObject result = new JSONObject();
        try {
            result.put("name", mName + "Type");
            JSONObject properties = new JSONObject();
            for (Map.Entry<String, Object> def : mBackingMap.entrySet()) {
                JSONObject propDef = new JSONObject();
                propDef.put("type", "any");
                propDef.put("default", def.getValue());
                properties.put(def.getKey(), propDef);
            }
            result.put("properties", properties);
        } catch (JSONException ex) {}

        return result;
    }

    /**
     * Construct an empty LiveMap
     * @return the LiveMap
     */
    static public LiveMapAdapter create(ILiveDataUpdateCallback updateCallback, String uri, String name, Map<String, Object> defaults) {
        return new LiveMapAdapter(updateCallback, uri, name, defaults);
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
        Object result = mBackingMap.put(key, value);
        try {
            JSONArray operations = new JSONArray();
            JSONObject operation = new JSONObject();
            operation.put("type", "Set");
            operation.put("key", key);
            operation.put("item", value);
            operations.put(operation);

            if (!sendUpdate(operations)) return null;
        } catch (JSONException ex) {
            return null;
        }

        return result;
    }

    @Override
    @Nullable
    public Object remove(@Nullable Object key) {
        Object result = mBackingMap.remove(key);
        if (result == null) return null;
        try {
            JSONArray operations = new JSONArray();
            JSONObject operation = new JSONObject();
            operation.put("type", "Remove");
            operation.put("key", key);
            operations.put(operation);
            if (!sendUpdate(operations)) return null;
        } catch (JSONException ex) {
            return null;
        }
        return result;
    }

    @Override
    public void putAll(@NonNull Map<? extends String, ? extends Object> m) {
        mBackingMap.putAll(m);
        try {
            JSONArray operations = new JSONArray();
            for (Entry<? extends String, ? extends Object> entry: m.entrySet()) {
                JSONObject operation = new JSONObject();
                operation.put("type", "Set");
                operation.put("key", entry.getKey());
                operation.put("item", entry.getValue());
                operations.put(operation);
            }
            sendUpdate(operations);
        } catch (JSONException ex) {}
    }

    public void clear() {
        try {
            JSONArray operations = new JSONArray();
            for (String key : mBackingMap.keySet()) {
                JSONObject operation = new JSONObject();
                operation.put("type", "Remove");
                operation.put("key", key);
                operations.put(operation);
            }
            sendUpdate(operations);
        } catch (JSONException ex) {}
        mBackingMap.clear();
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
        if (!(o instanceof LiveMapAdapter)) {
            return false;
        }
        LiveMapAdapter other = (LiveMapAdapter) o;
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
            return LiveMapAdapter.this.size();
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
                LiveMapAdapter.this.remove(curr);
            }
        }
    }

    /**
     * KeySet view of Map.
     */
    private class KeySet extends AbstractSet<String> {
        @Override
        public int size() {
            return LiveMapAdapter.this.size();
        }

        @NonNull
        @Override
        public Iterator<String> iterator() {
            return new KeyIterator();
        }

        private class KeyIterator implements Iterator<String> {
            private Iterator<Entry<String,Object>> iterator = LiveMapAdapter.this.entrySet().iterator();

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
            return LiveMapAdapter.this.size();
        }

        @Override
        public Iterator<Object> iterator() {
            return new ValueIterator();
        }

        private class ValueIterator implements Iterator<Object> {
            private Iterator<Entry<String,Object>> iterator = LiveMapAdapter.this.entrySet().iterator();

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
    private class LiveEntry implements Entry<String,Object> {
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
            Entry other = (Entry) o;
            return Objects.equals(mKey, other.getKey()) &&
                    Objects.equals(mValue, other.getValue());
        }

        @Override
        public int hashCode() {
            return Objects.hash(mKey, mValue);
        }
    }
}
