/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.primitives.decoder;

/**
 * Specialized decoder for single primitive values.
 */
public interface SingleValueDecoder {
    /**
     * Attempts to decode a null value.
     *
     * @return true if null was decoded or false if decoding was not successful.
     */
    boolean decodeNull();

    /**
     * Attempts to decode a boolean value. Null is returned if decoding was not successful.
     *
     * @return the decoded value or null.
     */
    Boolean decodeBoolean();

    /**
     * Attempts to decode a single-precision floating-point value. Null is returned if decoding was
     * not successful.
     *
     * @return the decoded value or null.
     */
    Float decodeFloat();

    /**
     * Attempts to decode a double-precision floating-point value. Null is returned if decoding was
     * not successful.
     *
     * @return the decoded value or null.
     */
    Double decodeDouble();

    /**
     * Attempts to decode a 32-bit signed integer value. Null is returned if decoding was not
     * successful.
     *
     * @return the decoded value or null.
     */
    Integer decodeInteger();

    /**
     * Attempts to decode a 64-bit signed integer value. Null is returned if decoding was not
     * successful.
     *
     * @return the decoded value or null.
     */
    Long decodeLong();

    /**
     * Attempts to decode a string value. Null is returned if decoding was not successful. There is
     * no guarantee that other value types can be decoded as strings in addition to their own type.
     * For example, if the underlying object contains an integer value, calling @c decodeString will
     * typically return null.
     *
     * @return the decoded value or null.
     */
    String decodeString();
};
