/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.primitives.decoder;

/**
 * Specialized decoder for unkeyed containers (e.g. arrays).
 */
public interface UnkeyedContainerDecoder {
    /**
     * @return the total number of elements within the container.
     */
    int size();

    /**
     * @return @c true if the last element of the container has been decoded, @c false otherwise.
     */
    boolean atEnd();

    /**
     * @return The index of the next element to be decoded in the container (initially 0 for a new
     * decoder).
     */
    int index();

    /**
     * Attempts to decode the next element of the container as a single value.
     *
     * @return A valid decoder, or @c nullptr if the next element cannot be decoded as a single
     * value.
     */
    SingleValueDecoder decodeSingleValue();

    /**
     * Attempts to decode the next element of the container as a keyed container
     *
     * @return A valid decoder, or @c nullptr if the next element cannot be decoded as a keyed
     * container.
     */
    KeyedContainerDecoder decodeKeyedContainer();

    /**
     * Attempts to decode the next element of the container as a nested unkeyed container
     *
     * @return A valid decoder, or @c nullptr if the next element cannot be decoded as an unkeyed
     * container.
     */
    UnkeyedContainerDecoder decodeUnkeyedContainer();
}
