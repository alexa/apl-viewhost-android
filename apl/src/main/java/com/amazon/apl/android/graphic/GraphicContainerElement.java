/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.graphic;


import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.enums.GraphicLayoutDirection;

import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyHeightActual;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyLang;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyLayoutDirection;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyViewportHeightActual;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyViewportWidthActual;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyWidthActual;

/**
 * The top level (root) object of an {@link com.amazon.apl.android.graphic.AlexaVectorDrawable}
 * object.
 */
public class GraphicContainerElement extends GraphicGroupElement {

    private GraphicContainerElement(GraphicElementMap map, long nativeHandle,
                                    RenderingContext renderingContext) {
        super(map, nativeHandle, renderingContext);
    }

    /**
     * Create a new GraphicContainerElement.
     * @param graphicHandle the native handle to the GraphicContainer.
     * @return a new GraphicContainerElement.
     */
    public static GraphicContainerElement create(long graphicHandle,
                                                 RenderingContext renderingContext) {
        return new GraphicContainerElement(new GraphicElementMap(), graphicHandle, renderingContext);
    }

    @Override
    void putInMap(GraphicElementMap map) {
        super.putInMap(map);
        map.setRoot(this);
    }

    /**
     * Get the actual height of the container element.
     * @return actual height of container element.
     */
    float getHeightActual() {
        return mProperties.getDimension(kGraphicPropertyHeightActual).value();
    }

    /**
     * Get the actual width of the container element.
     * @return actual width of container element.
     */
    float getWidthActual() {
        return mProperties.getDimension(kGraphicPropertyWidthActual).value();
    }

    /**
     * Get the actual view port height  of the container element. Not a dimension.
     * @return actual view port height of container element.
     */
    float getViewportHeightActual() {
        return mProperties.getFloat(kGraphicPropertyViewportHeightActual);
    }

    /**
     * Get the actual view port width  of the container element. Not a dimension.
     * @return actual view port width of container element.
     */
    float getViewportWidthActual() {
        return mProperties.getFloat(kGraphicPropertyViewportWidthActual);
    }

    /**
     * @return The name of the font language. Defaults to "".
     * For example to select the japanese characters of the "Noto Sans CJK" font family set this to "ja-JP"
     */
    @NonNull
    String getFontLanguage() {
        return mProperties.getString(kGraphicPropertyLang);
    }

    /**
     * @return the layout direction
     */
    GraphicLayoutDirection getLayoutDirection() {
        return GraphicLayoutDirection.valueOf(mProperties.getEnum(kGraphicPropertyLayoutDirection));
    }

    @Override
    float getOpacity() {
        return 1.0f;
    }

    @Override
    void applyProperties() {
        // No container properties to apply
    }

    @Override
    void createFilters() {
        // No container filters to create
    }

    @Override
    void applyFilters(Bitmap bitmap, float xScale, float yScale) {
        // No container filters to apply
    }
}
