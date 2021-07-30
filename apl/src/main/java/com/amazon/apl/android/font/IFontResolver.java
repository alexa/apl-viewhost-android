/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.font;

import android.graphics.Typeface;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


/**
 * Defines the font resolver interface, used for obtaining font {@link Typeface} information
 * from the OS.
 */
public interface IFontResolver {

    /**
     * Finds a specified font.
     *
     * @param key           The key for the font name, weight, etc.
     * @return  The requested typeface, or a null if requested font is unavailable.
     */
    @Nullable
    Typeface findFont(@NonNull FontKey key);

    void initialize();

}
