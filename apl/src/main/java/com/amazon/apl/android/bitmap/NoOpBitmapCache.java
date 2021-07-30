/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.bitmap;

import android.content.res.Configuration;
import android.graphics.Bitmap;

public class NoOpBitmapCache implements IBitmapCache {

    @Override
    public void putBitmap(BitmapKey key, Bitmap bitmap) {

    }

    @Override
    public Bitmap getBitmap(BitmapKey key) {
        return null;
    }

    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public void clear() {

    }
}
