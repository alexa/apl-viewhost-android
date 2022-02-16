/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import androidx.annotation.NonNull;

/**
 * No-op component that takes up space but otherwise does nothing
 */
public class NoOpComponent extends Component {
    /**
     * NoOp constructor
     * {@inheritDoc}
     */
    NoOpComponent(long nativeHandle, String componentId, @NonNull RenderingContext renderingContext) {
        super(nativeHandle, componentId, renderingContext);
    }
}
