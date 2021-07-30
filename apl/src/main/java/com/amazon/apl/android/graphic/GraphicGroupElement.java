/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.graphic;


import android.graphics.Matrix;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;


import com.amazon.apl.android.RenderingContext;

import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyClipPath;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyOpacity;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyTransform;

/**
 * Group element implementation.
 */
public class GraphicGroupElement extends GraphicElement {
    @Nullable
    private PathParser.PathDataNode[] mClipPathNodes = null;

    // mStackedMatrix is only used temporarily when drawing, it combines all
    // the parents' local matrices with the current one.
    private final Matrix mStackedMatrix = new Matrix();

    // mLocalMatrix is updated based on this group's transformation information
    private final Matrix mLocalMatrix = new Matrix();

    protected GraphicGroupElement(GraphicElementMap map, long nativeHandle, RenderingContext renderingContext) {
        super(map, nativeHandle, renderingContext);
        applyProperties();
    }

    static GraphicGroupElement create(GraphicElementMap map, long graphicHandle, RenderingContext renderingContext) {
        return new GraphicGroupElement(map, graphicHandle, renderingContext);
    }

    /**
     * Gets an overall opacity applied to this group.
     *
     * @return the value of opacity property.
     */
    float getOpacity() {
        return mProperties.getFloat(kGraphicPropertyOpacity);
    }

    /**
     * Gets the calculated transform function applied to the contents of the group
     *
     * @return The graphic element's 2D transform.
     */
    @NonNull
    Matrix getTransform() {
        return mProperties.getTransform(kGraphicPropertyTransform);
    }

    /**
     * Gets a standard path item that is not drawn. It clips the children of the group.
     *
     * @return the value of the clipPath of the group element.
     */
    @Nullable
    String getClipPath() {
        return mProperties.getString(kGraphicPropertyClipPath);
    }

    /**
     * @return the clip path nodes for this group.
     */
    @Nullable
    PathParser.PathDataNode[] getClipPathNodes() {
        return mClipPathNodes;
    }

    /**
     * @return the temporary matrix that combines this matrix with parents' local matrices
     */
    Matrix getStackedMatrix() {
        return mStackedMatrix;
    }

    /**
     * @return the matrix containing this group's transformation information
     */
    Matrix getLocalMatrix() {
        return mLocalMatrix;
    }

    /**
     * Update cached properties when Graphic is marked dirty.
     */
    void applyProperties() {
        applyClipPath();

        applyMatrix();
    }

    private void applyMatrix() {
        mLocalMatrix.set(getTransform());
    }

    private void applyClipPath() {
        final String clipPath = getClipPath();
        if (TextUtils.isEmpty(clipPath)) {
            mClipPathNodes = null;
        } else {
            mClipPathNodes = PathParser.createNodesFromPathData(getClipPath(), getRenderingContext());
        }
    }
}


