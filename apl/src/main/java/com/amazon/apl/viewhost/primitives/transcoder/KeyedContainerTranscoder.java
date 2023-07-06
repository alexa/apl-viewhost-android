/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.primitives.transcoder;

/**
 * Specialized transcoder for keyed containers.
 */
public interface KeyedContainerTranscoder {
    /**
     * Transcode a key/value pair for the specified key and a single value. The returned transcoder
     * will be used to transcode the value.
     *
     * @param key The key to transcode
     * @return A transcoder to use when transcoding the value associated with the specified key.
     */
    SingleValueTranscoder transcodeSingleValue(String key);

    /**
     * Transcode a key/value pair for the specified key and an unkeyed container value. The returned
     * transcoder will be used to transcode the value.
     *
     * @param key The key to transcode
     * @return A transcoder to use when transcoding the value associated with the specified key.
     */
    UnkeyedContainerTranscoder transcodeUnkeyedContainer(String key);

    /**
     * Transcode a key/value pair for the specified key and a keyed container value. The returned
     * transcoder will be used to transcode the value.
     *
     * @param key The key to transcode
     * @return A transcoder to use when transcoding the value associated with the specified key.
     */
    KeyedContainerTranscoder transcodeKeyedContainer(String key);

    /**
     * Creates a transcoder for the specified key. The transcoder will be used to populate the value
     * associated with the specified key.
     *
     * @param key The key to transcode
     * @return A transcoder to use to populate the value associated with the specified key.
     */
    Transcoder transcode(String key);

    /*
     * Convenience methods, concrete implementations can override to provide more efficient
     * implements if needed
     */

    /**
     * Transcode a null value for the specified key.
     *
     * @param key the key to transcode
     */
    void transcodeNull(String key);

    /**
     * Transcode a boolean value for the specified key.
     *
     * @param key the key to transcode
     * @param value The value to transcode
     */
    void transcode(String key, boolean value);

    /**
     * Transcode a single precision floating-point value for the specified key.
     *
     * @param key the key to transcode
     * @param value The value to transcode
     */
    void transcode(String key, float value);

    /**
     * Transcode a double precision floating-point value for the specified key.
     *
     * @param key the key to transcode
     * @param value The value to transcode
     */
    void transcode(String key, double value);

    /**
     * Transcode a signed 64-bit integer value.
     *
     * @param key the key to transcode
     * @param value The value to transcode
     */
    void transcode(String key, long value);

    /**
     * Transcode a signed 32-bit integer value for the specified key.
     *
     * @param key the key to transcode
     * @param value The value to transcode
     */
    void transcode(String key, int value);

    /**
     * Transcode a string value for the specified key.
     *
     * @param key the key to transcode
     * @param value The value to transcode
     */
    void transcode(String key, String value);
}
