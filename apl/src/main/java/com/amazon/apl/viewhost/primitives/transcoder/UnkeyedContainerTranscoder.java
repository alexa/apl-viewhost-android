/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.primitives.transcoder;

/**
 * Specialized transcoder for unkeyed containers. Methods defined by this contract will be invoked
 * repeatedly when multiple elements are present.
 */
public interface UnkeyedContainerTranscoder extends SingleValueTranscoder {
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
};
