/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.widget.FrameLayout;

/**
 * Per-child layout information associated with AbsoluteLayout.
 */
@SuppressWarnings("WeakerAccess")
public class APLLayoutParams extends FrameLayout.LayoutParams {
    //The horizontal, or X, location of the child within the view group.
    public final int x;
    //The vertical, or Y, location of the child within the view group.
    public final int y;

    /**
     * Creates a new set of layout parameters with the specified width,
     * height and location.
     *
     * @param width The
     */
    public APLLayoutParams(int width, int height, int x, int y) {
        //TODO see if core can return ints, unit of measureTextContent is pixel.
        super(width, height);
        this.x = x;
        this.y = y;
    }
}