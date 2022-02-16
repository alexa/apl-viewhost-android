/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.graphic;

import androidx.annotation.NonNull;

import com.amazon.common.BoundObject;
import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.enums.GraphicPropertyKey;

import java.util.ArrayList;
import java.util.List;

/**
 * AVG GraphicPattern JNI binding class.
 */
public class GraphicPattern {
    private final RenderingContext mRenderingContext;
    private final List<GraphicElement> mItems;
    private final float mHeight;
    private final float mWidth;

    private GraphicPattern(RenderingContext renderingContext, float width, float height, List<GraphicElement> items) {
        mRenderingContext = renderingContext;
        mWidth = width;
        mHeight = height;
        mItems = items;
    }

    /**
     * Creates a GraphicPattern.
     * @param boundObject       the bound object (should be a GraphicElement)
     * @param propertyKey       the property key (should be Fill or Stroke)
     * @param graphicElementMap the graphic element map
     * @param renderingContext  the rendering context
     * @return
     */
    static GraphicPattern create(BoundObject boundObject, GraphicPropertyKey propertyKey, GraphicElementMap graphicElementMap, RenderingContext renderingContext) {
        final float width = nGetWidth(boundObject.getNativeHandle(), propertyKey.getIndex());
        final float height = nGetHeight(boundObject.getNativeHandle(), propertyKey.getIndex());
        final long[] itemHandles = nGetItems(boundObject.getNativeHandle(), propertyKey.getIndex());
        final List<GraphicElement> items = new ArrayList<>(itemHandles.length);
        for (int i = 0; i < itemHandles.length; i++) {
            items.add(i, GraphicElementFactory.createGraphicElement(graphicElementMap, itemHandles[i], renderingContext));
        }
        return new GraphicPattern(renderingContext, width, height, items);
    }

    RenderingContext getRenderingContext() {
        return mRenderingContext;
    }

    /**
     * Width of the pattern.
     */
    public float getWidth() {
        return mWidth;
    }

    /**
     * Height of the pattern.
     */
    public float getHeight() {
        return mHeight;
    }

    /**
     * GraphicElement items in drawing order where later items appear on top of earlier items.
     */
    @NonNull
    public List<GraphicElement> getItems() {
        return mItems;
    }

    private static native float nGetWidth(long componentHandle, int propertyKey);
    private static native float nGetHeight(long componentHandle, int propertyKey);
    private static native long[] nGetItems(long componentHandle, int propertyKey);
}