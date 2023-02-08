/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters.blender;

import android.os.Build;

import com.amazon.apl.enums.BlendMode;

public class BlenderFactory {

    public static Blender getBlender(BlendMode mode) {
        // The Android Canvas class supports blend modes beginning at API 29.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return new CanvasBlender(mode);
        } else {
            switch (mode) {
                case kBlendModeHue:
                case kBlendModeLuminosity:
                case kBlendModeColor:
                case kBlendModeSaturation:
                    return new NonSeparableBlender(mode);
                default:
                    return new SeparableBlender(mode);
            }
        }
    }
}
