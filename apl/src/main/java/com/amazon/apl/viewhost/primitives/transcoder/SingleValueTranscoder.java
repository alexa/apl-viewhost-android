/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.primitives.transcoder;

/**
 * Defines the contract for transcoders that can convert data from one representation to another.
 * The owner of the data uses this contract to "describe" the data to be transcoded.
 */
public interface SingleValueTranscoder {
    /**
     * Transcode a null value.
     */
    void transcodeNull();

    /**
     * Transcode a boolean value.
     *
     * @param value The value to transcode
     */
    void transcode(boolean value);

    /**
     * Transcode a single precision floating-point value.
     *
     * @param value The value to transcode
     */
    void transcode(float value);

    /**
     * Transcode a double precision floating-point value.
     *
     * @param value The value to transcode
     */
    void transcode(double value);

    /**
     * Transcode a signed 32-bit integer value.
     *
     * @param value The value to transcode
     */
    void transcode(int value);

    /**
     * Transcode a signed 64-bit integer value.
     *
     * @param value The value to transcode
     */
    void transcode(long value);

    /**
     * Transcode an unsigned 32-bit integer value.
     *
     * @param value The value to transcode
     */
    void transcodeUnsigned(int value);

    /**
     * Transcode an unsigned 64-bit integer value.
     *
     * @param value The value to transcode
     */
    void transcodeUnsigned(long value);

    /**
     * Transcode a string value.
     *
     * @param value The value to transcode
     */
    void transcode(String value);
};
