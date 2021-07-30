/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.apl.android.bitmap.IBitmapFactory;
import com.amazon.apl.android.dependencies.IExtensionImageFilterCallback;
import com.amazon.apl.android.image.filters.bitmap.FilterResult;
import com.amazon.apl.android.primitive.Filters;

import java.util.List;
import java.util.concurrent.Future;

public class FilterOperationFactory {
    public static FilterOperation create(List<Future<FilterResult>> sourceFutures, @NonNull Filters.Filter filter, IBitmapFactory bitmapFactory, @NonNull RenderScriptWrapper renderScript, @Nullable IExtensionImageFilterCallback extensionImageFilterCallback) {
        switch (filter.filterType()) {
            case kFilterTypeBlend:
                return new BlendFilterOperation(sourceFutures, filter, bitmapFactory, renderScript);
            case kFilterTypeBlur:
                return new BlurFilterOperation(sourceFutures, filter, bitmapFactory, renderScript);
            case kFilterTypeColor:
                return new ColorFilterOperation(filter, bitmapFactory);
            case kFilterTypeGradient:
                return new GradientFilterOperation(filter, bitmapFactory);
            case kFilterTypeExtension:
                return new ExtensionFilterOperation(sourceFutures, filter, bitmapFactory, extensionImageFilterCallback);
            case kFilterTypeNoise:
                return new NoiseFilterOperation(sourceFutures, filter, bitmapFactory);
            case kFilterTypeGrayscale:
            case kFilterTypeSaturate:
                return new ColorMatrixFilterOperation(sourceFutures, filter, bitmapFactory, renderScript);
        }

        throw new IllegalArgumentException("Filter: " + filter.filterType() + " unknown.");
    }
}
