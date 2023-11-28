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

    /**
     * Returns a GraphicElement corresponding to the native graphic element. If the element exists
     * already in the provided map, return it, otherwise create a new GraphicElement.
     *
     * @param map The cache of graphic elements within the GraphicContainerElement
     * @param handle The native handle of the graphic element
     * @param renderingContext The rendering context
     */
    @NonNull
    public static GraphicElement getOrCreateGraphicElement(@NonNull final GraphicElementMap map,
                                                           final long handle,
                                                           RenderingContext renderingContext) {
        int id = nGetUniqueId(handle);
        GraphicElement cachedElement = map.get(id);
        if (cachedElement != null) {
            // According to the unique ID (from core), this graphic element already has a
            // representation in this container, so return it from cache.
            return cachedElement;
        }

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
    private static native int nGetUniqueId(long handle);
}
