/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import androidx.annotation.NonNull;

/**
 * Component for Container/TouchWrapper/ScrollView/Sequence/GridSequence/Pager.
 * See {@link <a https://developer.amazon.com/docs/alexa-presentation-language/apl-container.html>
 * APL Container Specification </a>}
 */
public class MultiChildComponent extends Component {
    /**
     * MultiChild constructor
     * {@inheritDoc}
     */
    MultiChildComponent(long nativeHandle, String componentId, @NonNull RenderingContext renderingContext) {
        super(nativeHandle, componentId, renderingContext);
    }
}
