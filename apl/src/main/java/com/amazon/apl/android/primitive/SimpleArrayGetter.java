/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.primitive;

public abstract class SimpleArrayGetter<K extends IterableProperty<T>, T> {
    /**
     * @return an iterable property.
     */
    public abstract K builder();
}
