/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.primitives;

import com.amazon.apl.viewhost.primitives.transcoder.KeyedContainerTranscoder;
import com.amazon.apl.viewhost.primitives.transcoder.SingleValueTranscoder;
import com.amazon.apl.viewhost.primitives.transcoder.Transcoder;
import com.amazon.apl.viewhost.primitives.transcoder.UnkeyedContainerTranscoder;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nullable;

/**
 * Transcode into JSONObject or JSONArray
 *
 * TODO: This is a partial implementation. Full implementation will be needed
 * for handling core-generated transcodable/decodable payloads.
 */
public class JsonTranscoder implements Transcoder {
    @Nullable
    private JSONObject mObject;

    @Nullable
    private JSONArray mArray;

    /**
     * @return The JSON object which is the result of transcoding. This can
     *         return null if the underlying object is an array or transcoding failed.
     */
    @Nullable
    public JSONObject getJsonObject() {
        return mObject;
    }

    /**
     * Method called by JsonDecodable to directly extract the underlying object
     *
     * @param value Pass-through JSON object
     */
    public void setJsonObject(@Nullable JSONObject value) {
        mObject = value;
    }

    /**
     * @return The JSON array which is the result of transcoding. This can
     *         return null if the underlying array is an array or transcoding failed.
     */
    @Nullable
    public JSONArray getJsonArray() {
        return mArray;
    }

    /**
     * Method called by JsonDecodable to directly extract the underlying array
     *
     * @param value Pass-through JSON array
     */
    public void setJsonArray(@Nullable JSONArray value) {
        mArray = value;
    }

    @Override
    public void transcodeNull() {

    }

    @Override
    public void transcode(boolean value) {

    }

    @Override
    public void transcode(float value) {

    }

    @Override
    public void transcode(double value) {

    }

    @Override
    public void transcode(int value) {

    }

    @Override
    public void transcode(long value) {

    }

    @Override
    public void transcodeUnsigned(int value) {

    }

    @Override
    public void transcodeUnsigned(long value) {

    }

    @Override
    public void transcode(String value) {

    }

    @Override
    public SingleValueTranscoder transcodeSingleValue() {
        return null;
    }

    @Override
    public KeyedContainerTranscoder transcodeKeyedContainer() {
        return null;
    }

    @Override
    public UnkeyedContainerTranscoder transcodeUnkeyedContainer() {
        return null;
    }
}
