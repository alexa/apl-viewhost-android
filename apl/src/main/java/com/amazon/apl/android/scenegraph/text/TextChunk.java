/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.scenegraph.text;

import com.amazon.apl.android.primitive.StyledText;

/**
 * Simple access wrapper for a TextChunk (which is a wrapper around StyledText to start with)
 */
public class TextChunk {
    private final long mNativeHandle;

    private StyledText mStyledText;


    public TextChunk(long nativeHandle) {
        this.mNativeHandle = nativeHandle;
    }

    /**
     * @return Data to display in the text block.
     */
    public StyledText getText() {
        if (mStyledText == null)
            mStyledText = new StyledText(mNativeHandle);

        return mStyledText;
    }
}