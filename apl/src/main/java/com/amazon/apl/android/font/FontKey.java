/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.font;

import com.google.auto.value.AutoValue;

/**
 * Provides an identifier for font requests.
 */
@AutoValue
public abstract class FontKey {
    /** FontKey properties */
    public abstract String getFamily();
    public abstract int getWeight();   // Weight of the font from 100-900.
    public abstract boolean isItalic();
    public abstract String getLanguage();


    public static com.amazon.apl.android.font.FontKey.Builder build(String family, int weight) {
        return new AutoValue_FontKey.Builder().
                family(family).
                weight(weight).
                italic(false).
                language("");
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract com.amazon.apl.android.font.FontKey.Builder family(String family);
        public abstract com.amazon.apl.android.font.FontKey.Builder weight(int weight);
        public abstract com.amazon.apl.android.font.FontKey.Builder italic(boolean italic);
        public abstract com.amazon.apl.android.font.FontKey.Builder language(String language);
        public abstract com.amazon.apl.android.font.FontKey build();
    }
}
