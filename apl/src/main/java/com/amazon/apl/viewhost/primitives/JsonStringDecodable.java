/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.primitives;

import com.amazon.apl.viewhost.primitives.Decodable;
import com.amazon.apl.viewhost.primitives.decoder.KeyedContainerDecoder;
import com.amazon.apl.viewhost.primitives.decoder.SingleValueDecoder;
import com.amazon.apl.viewhost.primitives.decoder.UnkeyedContainerDecoder;
import com.amazon.apl.viewhost.primitives.transcoder.Transcoder;

/**
 * Shim for the decodable contract that wraps a JSON-encoded string.
 */
public class JsonStringDecodable implements Decodable {
    private final String mJsonString;

    public JsonStringDecodable(String jsonString) {
        mJsonString = jsonString;
    }

    @Override
    public boolean transcode(Transcoder transcoder) {
        return false;
    }

    @Override
    public SingleValueDecoder decodeSingleValue() {
        return null;
    }

    @Override
    public KeyedContainerDecoder decodeKeyedContainer() {
        return null;
    }

    @Override
    public UnkeyedContainerDecoder decodeUnkeyedContainer() {
        return null;
    }

    public String getString() {
        return mJsonString;
    }
}
