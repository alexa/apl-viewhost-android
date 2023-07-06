/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.primitives.decoder;

import java.util.Iterator;

/**
 * Specialized decoder for keyed containers (e.g. maps). Keyed containers do not allow repeated keys.
 */
public interface KeyedContainerDecoder {
    /**
     * @return the total number of key/value pairs in the container.
     */
    int size();

    /**
     * Determines whether a key/value pair exists with the specified key.
     *
     * @param key The key to look up
     * @return @c true if a key/value pair exists in the container for the specified key, @c false
     *            otherwise.
     */
    boolean hasKey(String key);

    /**
     * @return all keys from the container.
     */
    Iterator<String> keys();

    /**
     * Attempts to decode the value associated with the specified key as a single value.
     *
     * @param key The key to look up
     * @return a valid decoder, or @c null if either the key is not found or it cannot be decoded
     *         as a single value.
     */
    SingleValueDecoder decodeSingleValue(String key);

    /**
    /**
     * Attempts to decode the value associated with the specified key as a nested keyed container.
     *
     * @param key The key to look up
     * @return a valid decoder, or @c null if either the key is not found or it cannot be decoded
     *         as a keyed container.
     */
    KeyedContainerDecoder decodeKeyedContainer(String key);

    /**
     * Attempts to decode the value associated with the specified key as an unkeyed container.
     *
     * @param key The key to look up
     * @return a valid decoder, or @c null if either the key is not found or it cannot be decoded
     *         as an unkeyed container.
     */
    UnkeyedContainerDecoder decodeUnkeyedContainer(String key);
};
