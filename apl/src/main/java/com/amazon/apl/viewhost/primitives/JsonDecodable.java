/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.primitives;

import com.amazon.apl.viewhost.primitives.decoder.KeyedContainerDecoder;
import com.amazon.apl.viewhost.primitives.decoder.SingleValueDecoder;
import com.amazon.apl.viewhost.primitives.decoder.UnkeyedContainerDecoder;
import com.amazon.apl.viewhost.primitives.transcoder.Transcoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import javax.annotation.Nullable;

/**
 * Decodable implementation where the underlying information is stored in a JSONObject.
 */
public class JsonDecodable implements Decodable {
    @Nullable
    private JSONObject mObject;

    @Nullable
    private JSONArray mArray;

    public JsonDecodable(JSONObject object) {
        mObject = object;
    }

    public JsonDecodable(JSONArray array) {
        mArray = array;
    }

    class ObjectValueDecoder implements SingleValueDecoder {
        private JSONObject mValue;
        private String mKey;

        ObjectValueDecoder(JSONObject value, String key) {
            mValue = value;
            mKey = key;
        }

        @Override
        public boolean decodeNull() {
            return mValue.isNull(mKey);
        }

        @Override
        public Boolean decodeBoolean() {
            try {
                return mValue.getBoolean(mKey);
            } catch (JSONException e) {
                return null;
            }
        }

        @Override
        public Float decodeFloat() {
            try {
                return (float)mValue.getDouble(mKey);
            } catch (JSONException e) {
                return null;
            }
        }

        @Override
        public Double decodeDouble() {
            try {
                return mValue.getDouble(mKey);
            } catch (JSONException e) {
                return null;
            }
        }

        @Override
        public Integer decodeInteger() {
            try {
                return mValue.getInt(mKey);
            } catch (JSONException e) {
                return null;
            }
        }

        @Override
        public Long decodeLong() {
            try {
                return mValue.getLong(mKey);
            } catch (JSONException e) {
                return null;
            }
        }

        @Override
        public String decodeString() {
            try {
                return mValue.getString(mKey);
            } catch (JSONException e) {
                return null;
            }
        }
    }

    class ArrayValueDecoder implements SingleValueDecoder {
        private JSONArray mArray;
        private int mIndex;

        ArrayValueDecoder(JSONArray array, int index) {
            mArray = array;
            mIndex = index;
        }

        @Override
        public boolean decodeNull() {
            return mArray.isNull(mIndex);
        }

        @Override
        public Boolean decodeBoolean() {
            try {
                return mArray.getBoolean(mIndex);
            } catch (JSONException e) {
                return null;
            }
        }

        @Override
        public Float decodeFloat() {
            try {
                return (float)mArray.getDouble(mIndex);
            } catch (JSONException e) {
                return null;
            }
        }

        @Override
        public Double decodeDouble() {
            try {
                return mArray.getDouble(mIndex);
            } catch (JSONException e) {
                return null;
            }
        }

        @Override
        public Integer decodeInteger() {
            try {
                return mArray.getInt(mIndex);
            } catch (JSONException e) {
                return null;
            }
        }

        @Override
        public Long decodeLong() {
            try {
                return mArray.getLong(mIndex);
            } catch (JSONException e) {
                return null;
            }
        }

        @Override
        public String decodeString() {
            try {
                return mArray.getString(mIndex);
            } catch (JSONException e) {
                return null;
            }
        }
    }

    class ArrayDecoder implements UnkeyedContainerDecoder {
        private JSONArray mArray;
        private int mIndex;

        ArrayDecoder(JSONArray array) {
            mArray = array;
            mIndex = 0;
        }

        @Override
        public int size() {
            return mArray.length();
        }

        @Override
        public boolean atEnd() {
            return mIndex >= mArray.length();
        }

        @Override
        public int index() {
            return mIndex;
        }

        @Override
        public SingleValueDecoder decodeSingleValue() {
            if (atEnd()) return null;
            return new ArrayValueDecoder(mArray, mIndex++);
        }

        @Override
        public KeyedContainerDecoder decodeKeyedContainer() {
            if (atEnd()) return null;
            try {
                ObjectDecoder decoder = new ObjectDecoder(mArray.getJSONObject(mIndex));
                mIndex++;
                return decoder;
            } catch (JSONException e) {
                return null;
            }
        }

        @Override
        public UnkeyedContainerDecoder decodeUnkeyedContainer() {
            if (atEnd()) return null;
            try {
                ArrayDecoder decoder = new ArrayDecoder(mArray.getJSONArray(mIndex));
                mIndex++;
                return decoder;
            } catch (JSONException e) {
                return null;
            }
        }
    }

    class ObjectDecoder implements KeyedContainerDecoder {
        private JSONObject mValue;

        ObjectDecoder(JSONObject value) {
            mValue = value;
        }

        @Override
        public int size() {
            return mValue.length();
        }

        @Override
        public boolean hasKey(String key) {
            return mValue.has(key);
        }

        @Override
        public Iterator<String> keys() {
            return mValue.keys();
        }

        @Override
        public SingleValueDecoder decodeSingleValue(String key) {
            return new ObjectValueDecoder(mValue, key);
        }

        @Override
        public KeyedContainerDecoder decodeKeyedContainer(String key) {
            try {
                return new ObjectDecoder(mValue.getJSONObject(key));
            } catch (JSONException e) {
                return null;
            }
        }

        @Override
        public UnkeyedContainerDecoder decodeUnkeyedContainer(String key) {
            try {
                return new ArrayDecoder(mValue.getJSONArray(key));
            } catch (JSONException e) {
                return null;
            }
        }
    }

    @Override
    public SingleValueDecoder decodeSingleValue() {
        // Top-level standalone value not supported
        return null;
    }

    @Override
    public KeyedContainerDecoder decodeKeyedContainer() {
        if (mObject == null) {
            return null;
        }
        return new ObjectDecoder(mObject);
    }

    @Override
    public UnkeyedContainerDecoder decodeUnkeyedContainer() {
        if (mArray == null) {
            return null;
        }

        return new ArrayDecoder(mArray);
    }

    @Override
    public boolean transcode(Transcoder transcoder) {
        // For efficiency, we have a special bypass to extract the underlying JSON
        if (transcoder instanceof JsonTranscoder) {
            if (mObject != null) {
                ((JsonTranscoder)transcoder).setJsonObject(mObject);
                return true;
            } else if (mArray != null) {
                ((JsonTranscoder)transcoder).setJsonArray(mArray);
                return true;
            }
        }

        // Otherwise the transcoder is not implemented yet
        return false;
    }
}
