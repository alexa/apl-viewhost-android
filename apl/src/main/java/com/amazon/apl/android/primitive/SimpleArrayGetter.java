/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.primitive;

abstract class SimpleArrayGetter<K extends IterableProperty<T>, T> {
    /**
     * @return an iterable property.
     */
    abstract K builder();
}
