/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.viewhost.primitives;

import com.amazon.apl.viewhost.primitives.transcoder.Transcoder;

/**
 * Defines a payload that can describe itself to a transcoder instance.
 */
public interface Transcodable {
    /**
     * Transcodes this value using the specified transcoder. This payload will describe itself by
     * calling appropriate methods on the provided transcoder.
     *
     * @param transcoder The transcoder to use
     * @return @c true if there was a value to transcode, @c false otherwise
     */
    boolean transcode(Transcoder transcoder);
}
