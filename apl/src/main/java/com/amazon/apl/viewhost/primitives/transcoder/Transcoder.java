/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.primitives.transcoder;

/**
 * Defines the contract for transcoders that can convert data from one representation to another.
 * The owner of the data uses this contract to "describe" the data to be transcoded.
 */
public interface Transcoder extends SingleValueTranscoder {
    /**
     * Transcode a single value.
     * @return the transcoder to use to describe the value
     */
    SingleValueTranscoder transcodeSingleValue();

    /**
     * Transcode a keyed container (e.g. a map).
     *
     * @return the transcoder to use to describe the value
     */
    KeyedContainerTranscoder transcodeKeyedContainer();

    /**
     * Transcode an unkeyed container (e.g. an array).
     *
     * @return the transcoder to use to describe the value
     */
    UnkeyedContainerTranscoder transcodeUnkeyedContainer();
}
