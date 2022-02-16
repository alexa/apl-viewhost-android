/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.VectorGraphic;
import com.amazon.apl.android.graphic.APLVectorGraphicView;
import com.amazon.apl.android.graphic.AlexaVectorDrawable;
import com.amazon.apl.android.primitive.UrlRequests;
import com.amazon.apl.enums.PropertyKey;

public class VectorGraphicViewAdapter extends ComponentViewAdapter<VectorGraphic, APLVectorGraphicView> {
    private static final String TAG = "VectorGraphicViewAdptr";
    private static VectorGraphicViewAdapter INSTANCE;

    private VectorGraphicViewAdapter() {
        super();
        putPropertyFunction(PropertyKey.kPropertyGraphic, this::applyDirtyGraphics);
    }

    public static VectorGraphicViewAdapter getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new VectorGraphicViewAdapter();
        }
        return INSTANCE;
    }

    @Override
    public APLVectorGraphicView createView(Context context, IAPLViewPresenter presenter) {
        return new APLVectorGraphicView(context, presenter);
    }

    @Override
    void applyPadding(VectorGraphic component, APLVectorGraphicView view) {
        setPaddingFromBounds(component, view, false);
    }

    @Override
    public void applyAllProperties(VectorGraphic component, APLVectorGraphicView view) {
        super.applyAllProperties(component, view);
        view.setScale(component.getScale());
        view.setAlign(component.getAlign());
        view.setScaleType(ImageView.ScaleType.MATRIX);

        if (component.hasGraphic()) {
            createVectorDrawable(component, view);
        } else {
            UrlRequests.UrlRequest source = component.getSourceRequest();
            if (!TextUtils.isEmpty(source.url())) {
                component.getContentRetriever().fetchV2(Uri.parse(source.url()), source.headers(),
                        (request, result) -> view.post(() -> component.updateGraphic(result)),
                        (request, message, errorCode) -> Log.e(TAG, "Unable to open source " + message + " with errorCode " + errorCode));
            } else {
                Log.e(TAG, "Not a proper vector graphic source");
            }
        }
    }

    void createVectorDrawable(VectorGraphic component, APLVectorGraphicView view) {
        AlexaVectorDrawable vectorDrawable = AlexaVectorDrawable.create(component.getOrCreateGraphicContainerElement());
        view.setImageDrawable(vectorDrawable);
    }

    void applyDirtyGraphics(VectorGraphic component, APLVectorGraphicView view) {
        AlexaVectorDrawable vectorDrawable = (AlexaVectorDrawable) view.getDrawable();
        if (vectorDrawable == null) {
            createVectorDrawable(component, view);
        } else {
            vectorDrawable.updateDirtyGraphics(component.getDirtyGraphics());
        }
    }
}
