/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.primitives;

import com.amazon.apl.viewhost.primitives.decoder.KeyedContainerDecoder;
import com.amazon.apl.viewhost.primitives.decoder.SingleValueDecoder;
import com.amazon.apl.viewhost.primitives.decoder.UnkeyedContainerDecoder;

/**
 * Defines a contract for objects that can be externally decoded from their internal representation.
 * This contract assumes that the decoder understands the payload of the object being decoded and
 * can drive the decoding process.
 */
public interface Decodable extends Transcodable {
    /**
     * Attempts to decode the object as a single value. If the attempt succeeds, a valid decoder is
     * returned.  If the attempt fails, @c nullptr is returned.
     *
     * @return A single value decoder, or @c nullptr if the underlying object is not decodable as a
     * single value.
     */
    SingleValueDecoder decodeSingleValue();

    /**
     * Attempts to decode the object as a keyed container. If the attempt succeeds, a valid decoder
     * is returned.  If the attempt fails, @c nullptr is returned.
     *
     * @return A single value decoder, or @c nullptr if the underlying object is not decodable as a
     * keyed container.
     */
    KeyedContainerDecoder decodeKeyedContainer();

    /**
     * Attempts to decode the object as an unkeyed container. If the attempt succeeds, a valid
     * decoder is returned.  If the attempt fails, @c nullptr is returned.
     *
     * @return A single value decoder, or @c nullptr if the underlying object is not decodable as an
     * unkeyed container
     */
    UnkeyedContainerDecoder decodeUnkeyedContainer();
}
