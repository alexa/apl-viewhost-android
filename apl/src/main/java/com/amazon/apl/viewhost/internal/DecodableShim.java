/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.internal;

import com.amazon.apl.viewhost.primitives.Decodable;
import com.amazon.apl.viewhost.primitives.decoder.KeyedContainerDecoder;
import com.amazon.apl.viewhost.primitives.decoder.SingleValueDecoder;
import com.amazon.apl.viewhost.primitives.decoder.UnkeyedContainerDecoder;
import com.amazon.apl.viewhost.primitives.transcoder.Transcoder;

/**
 * Internal shim to maintain the decodable contract where we want it eventually but where we
 * don't yet have full internal support for it yet (e.g. getting decodable payloads from core).
 */
public class DecodableShim implements Decodable {
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
}
