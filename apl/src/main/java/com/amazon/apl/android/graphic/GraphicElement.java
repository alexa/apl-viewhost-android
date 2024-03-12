/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.graphic;


import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.graphics.Matrix;
import android.util.Log;

import com.amazon.common.BoundObject;
import com.amazon.apl.android.AVGFilter;
import com.amazon.apl.android.primitive.GraphicFilters;
import com.amazon.apl.android.scaling.IMetricsTransform;
import com.amazon.apl.android.primitive.Gradient;
import com.amazon.apl.android.PropertyMap;
import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.enums.GraphicElementType;
import com.amazon.apl.enums.GraphicPropertyKey;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyFilters;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyTransform;

/**
 * A single element of a graphic.  This may be a group of other elements, a path element,
 * or the overall container. This class is instantiated internally by the Graphic class.
 */

public abstract class GraphicElement extends BoundObject {
    private static final String TAG = "GraphicElement";

    private final List<GraphicElement> mChildren;
    private final RenderingContext mRenderingContext;
    public final PropertyMap<GraphicElement, GraphicPropertyKey> mProperties;
    private GraphicElementMap mGraphicElementMap;
    private List<AVGFilter> mFilters;

    protected GraphicElement(GraphicElementMap map, long nativeHandle,
                             RenderingContext renderingContext) {
        bind(nativeHandle);
        mChildren = new ArrayList<>();
        mRenderingContext = renderingContext;
        mProperties = new PropertyMap<GraphicElement, GraphicPropertyKey>() {
            @NonNull
            @Override
            public GraphicElement getMapOwner() {
                return GraphicElement.this;
            }

            @Nullable
            @Override
            public IMetricsTransform getMetricsTransform() {
                return mRenderingContext.getMetricsTransform();
            }
        };
        mGraphicElementMap = map;
        mFilters = new ArrayList<>();
        createFilters();
        putInMap(map);
        nInflateChildren(getNativeHandle());
    }

    void putInMap(GraphicElementMap map) {
        map.put(this);
    }

    public GraphicContainerElement getRootContainer() {
        return mGraphicElementMap.getRoot();
    }

    /**
     * Gets the type of the graphic element
     *
     * @return the type of the graphic element.
     */
    @NonNull
    protected GraphicElementType getType() {
        return GraphicElementType.valueOf(nGetType(getNativeHandle()));
    }

    /**
     * Get the children of this graphic element.
     *
     * @return the children of the graphic element returned in drawing order where later children
     * are drawn on top of earlier children.
     */
    public List<GraphicElement> getChildren() {
        return mChildren;
    }

    /**
     * Get the rendering context of the graphic element.
     *
     * @return the rendering context of the graphic element
     */
    public RenderingContext getRenderingContext() {
        return mRenderingContext;
    }

    public int getUniqueId() {
        return nGetUniqueId(getNativeHandle());
    }

    /**
     * Add children to the graphic element.
     *
     * @param childElement the child element
     */
    @SuppressWarnings("unused")
    protected void addChildren(long childElement) {
        GraphicElement child = GraphicElementFactory.getOrCreateGraphicElement(mGraphicElementMap,
                childElement, mRenderingContext);
        mChildren.add(child);
    }

    @NonNull
    protected GraphicPattern getGraphicPattern(@NonNull final GraphicPropertyKey property) {
        return GraphicPattern.create(this, property, mGraphicElementMap, mRenderingContext);
    }

    @NonNull
    protected Gradient getGradient(@NonNull final GraphicPropertyKey property) {
        return Gradient.create(this, property);
    }

    public PropertyMap<GraphicElement, GraphicPropertyKey> getProperties() {
        return mProperties;
    }

    public void applyDirtyProperties(@NonNull Set<Integer> dirtyGraphicUniqueIds) {
        for (Integer graphicElementId : dirtyGraphicUniqueIds) {
            GraphicElement element = mGraphicElementMap.get(graphicElementId);
            if (element != null) {
                element.applyProperties();
            } else {
                Log.w(TAG, "Cannot find graphic for unique id: " + graphicElementId);
            }
        }
    }

    /**
     * Creates the corresponding filters associated with this AVG object
     */
    void createFilters() {
        for(GraphicFilters.GraphicFilter filter : mProperties.getGraphicFilters(kGraphicPropertyFilters)) {
            switch(filter.filterType()) {
                case kGraphicFilterTypeDropShadow:
                    mFilters.add(new AVGDropShadowFilter(filter.color(), filter.radius(), filter.horizontalOffset(), filter.verticalOffset()));
                    break;
            }
        }
    }

    /**
     * Applies the corresponding filters associated with this AVG object
     *
     * @param bitmap the bitmap containing the AVG object and previous filters applied
     * @param xScale the horizontal scale
     * @param yScale the vertical scale
     */
    void applyFilters(@NonNull Bitmap bitmap, float xScale, float yScale) {
        for(AVGFilter filter : mFilters) {
            filter.apply(bitmap, xScale, yScale);
        }
    }

    /**
     * Checks whether this AVG object has any filters associated with it
     *
     * @return true if yes, otherwise false
     */
    boolean containsFilters() {
        return mFilters.size() > 0;
    }

    /**
     * Update cached properties when Graphic is marked dirty.
     */
    abstract void applyProperties();

    // TODO eliminate need for inflating java objects and expose a JNI Iterator
    protected native void nInflateChildren(long nativeHandle);

    protected native HashSet nGetDirtyProperties(long nativeHandle);

    protected native int nGetType(long mNativeHandle);

    private native int nGetUniqueId(long mNativeHandle);
}
