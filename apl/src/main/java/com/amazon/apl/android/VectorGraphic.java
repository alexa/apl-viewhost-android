/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import androidx.annotation.Nullable;

import android.net.Uri;
import android.util.Log;

import com.amazon.apl.android.content.MediaCallbackContentRetrieverDecorator;
import com.amazon.apl.android.dependencies.IContentRetriever;
import com.amazon.apl.android.graphic.GraphicContainerElement;
import com.amazon.apl.android.primitive.UrlRequests;
import com.amazon.apl.enums.PropertyKey;
import com.amazon.apl.enums.VectorGraphicAlign;
import com.amazon.apl.enums.VectorGraphicScale;

import java.util.Set;


/**
 * Creates a APL Vector Graphic component.
 */
public class VectorGraphic extends Component{

    private static final String TAG = VectorGraphic.class.getName();

    private final IContentRetriever<Uri, String> mContentRetriever;
    private GraphicContainerElement mGraphicContainerElement;

    /**
     * VectorGraphic constructor.
     * {@inheritDoc}
     */
    VectorGraphic(long nativeHandle, String componentId, RenderingContext renderingContext) {
        super(nativeHandle, componentId, renderingContext);
        mContentRetriever = renderingContext.getAvgRetriever();
    }

    /**
     * Get the content retriever for avg sources.
     * @return
     */
    public IContentRetriever<Uri, String> getContentRetriever() {
        return MediaCallbackContentRetrieverDecorator.create(this.getViewPresenter(), mContentRetriever);
    }

    /**
     * @return The URL to download the image from.
     */
    @Nullable
    public final UrlRequests.UrlRequest getSourceRequest() {
        UrlRequests requests = mProperties.getUrlRequests(PropertyKey.kPropertySource);
        if (requests.size() == 0) return null;
        return requests.at(0);

    }

    public void updateGraphic(String content) {
        nUpdateGraphic(getNativeHandle(), content);
    }

    public Set<Integer> getDirtyGraphics() {
        return nGetDirtyGraphics(getNativeHandle());
    }

    private native void nUpdateGraphic(long handle, String content);

    /**
     * @return Alignment of the image within the containing box. Defaults to center.
     */
    public final VectorGraphicAlign getAlign() {
        return VectorGraphicAlign.valueOf(mProperties.getEnum(PropertyKey.kPropertyAlign));
    }

    /**
     * @return How the image will be resized to fit in the bounding box. Defaults to best-fit.
     */
    public final VectorGraphicScale getScale() {
        return VectorGraphicScale.valueOf(mProperties.getEnum(PropertyKey.kPropertyScale));
    }

    /**
     * @return if the graphic content has been loaded for this VectorGraphic yet
     */
    public boolean hasGraphic() {
        return mProperties.hasProperty(PropertyKey.kPropertyGraphic);
    }

    /**
     * @return the {@link GraphicContainerElement} that is the root for this AVG or null if the source
     * hasn't been loaded yet.
     */
    @Nullable
    public GraphicContainerElement getOrCreateGraphicContainerElement() {
        if (mGraphicContainerElement != null) {
            return mGraphicContainerElement;
        }

        if (!hasGraphic()) {
            Log.e(TAG, "Attempting to get root when no graphic is loaded.");
            return null;
        }

        mGraphicContainerElement = GraphicContainerElement.create(nGetGraphic(getNativeHandle(), PropertyKey.kPropertyGraphic.getIndex()), getRenderingContext());
        return mGraphicContainerElement;
    }

    /**
        Reset the cached root to AVG, so a new root can be created.
     */
    public void resetGraphicContainerElement() {
        mGraphicContainerElement = null;
    }

    private native long nGetGraphic(long nativeHandle, int propertyKey);

    private native Set<Integer> nGetDirtyGraphics(long nativeHandle);
}


