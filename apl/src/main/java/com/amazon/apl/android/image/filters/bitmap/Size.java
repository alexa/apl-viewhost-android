/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters.bitmap;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Size {
    public static final Size ZERO = create(0, 0);

    abstract int width();
    abstract int height();

    public static Size create(int width, int height) {
        return new AutoValue_Size(width, height);
    }

    public static Size create(Size other) {
        return create(other.width(), other.height());
    }
}
