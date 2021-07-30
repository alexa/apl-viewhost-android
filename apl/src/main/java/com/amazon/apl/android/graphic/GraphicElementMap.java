/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.graphic;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import android.util.SparseArray;

/**
 * This class maintains a map of unique child ids to GraphicElements.
 */
class GraphicElementMap {
    private final SparseArray<GraphicElement> mUniqueIdToGraphicElementMap = new SparseArray<>();
    private GraphicContainerElement mRoot;

    /**
     * Puts a {@link GraphicElement} in the map of unique ids to GraphicElements.
     *
     * @param element   GraphicElement to put
     */
    void put(@NonNull GraphicElement element) {
        mUniqueIdToGraphicElementMap.put(element.getUniqueId(), element);
    }

    /**
     * Get the {@link GraphicElement} with the id returned from {@link GraphicElement#getUniqueId()}.
     *
     * @param uniqueId the unique id
     * @return the graphic element if added, null otherwise
     */
    @Nullable
    GraphicElement get(int uniqueId) {
        return mUniqueIdToGraphicElementMap.get(uniqueId);
    }

    void setRoot(@NonNull GraphicContainerElement root) {
        mRoot = root;
    }

    @NonNull
    GraphicContainerElement getRoot() {
        return mRoot;
    }
}
