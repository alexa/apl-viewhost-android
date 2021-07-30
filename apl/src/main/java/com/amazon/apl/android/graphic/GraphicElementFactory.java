/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.graphic;

import androidx.annotation.NonNull;
import android.util.Log;

import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.enums.GraphicElementType;

/**
 * Creates corresponding GraphicElement class given a native handle to the corresponding Core
 * object.
 */
public class GraphicElementFactory {
    private static final String TAG = "GraphicElementFactory";

    private GraphicElementFactory() {
    }

    @NonNull
    public static GraphicElement createGraphicElement(@NonNull final GraphicElementMap map,
                                                      final long handle,
                                                      RenderingContext renderingContext) {
        GraphicElementType type = GraphicElementType.valueOf(nGetType(handle));
        switch (type) {
            case kGraphicElementTypeGroup:
                return GraphicGroupElement.create(map, handle, renderingContext);
            case kGraphicElementTypePath:
                return GraphicPathElement.create(map, handle, renderingContext);
            case kGraphicElementTypeText:
                return GraphicTextElement.create(map, handle, renderingContext);
            default: {
                Log.e(TAG, "Invalid child type: " + type);
                throw new IllegalArgumentException("Argument is not a GraphicElement");
            }
        }
    }

    private static native int nGetType(long handle);
}
