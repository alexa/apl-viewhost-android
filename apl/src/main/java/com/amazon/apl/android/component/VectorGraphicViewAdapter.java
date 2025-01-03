/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.component;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
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
        putPropertyFunction(PropertyKey.kPropertySource, this::applySource);
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

        applySource(component, view);
    }

    @Override
    public void applyAlpha(VectorGraphic component, APLVectorGraphicView view) {
        if (component.hasProperty(PropertyKey.kPropertyOpacity)) {
            final float opacity = component.getOpacity();
            // https://developer.android.com/topic/performance/hardware-accel#layers-anims
            if (component.getRenderingContext().isRuntimeHardwareAccelerationEnabled()) {
                if (opacity < 1.0 && view.getLayerType() != View.LAYER_TYPE_HARDWARE) { // set to HARDWARE if not already
                    view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                } else if (opacity == 1.0 && view.getLayerType() != View.LAYER_TYPE_NONE) { // set to default NONE if not already
                    view.setLayerType(View.LAYER_TYPE_NONE, null);
                }
            }
            view.setAlpha(opacity);
        }
    }

    private void applySource(VectorGraphic component, APLVectorGraphicView view) {

        if (component.hasGraphic()) {
            component.resetGraphicContainerElement();
            createVectorDrawable(component, view);
        } else {
            UrlRequests.UrlRequest source = component.getSourceRequest();
            if (!TextUtils.isEmpty(source.url())) {

                component.getContentRetriever().fetchV2(Uri.parse(source.url()), source.headers(),
                        (request, result) -> view.post(() -> updateGraphic(component, view, result)),
                        (request, message, errorCode) -> {
                            Log.e(TAG, "Unable to open source " + request.getPath() + " with error " + message);
                            view.post(() -> resetDrawableGraphics(component, view));
                        });
            } else {
                Log.e(TAG, "Not a proper vector graphic source");
                resetDrawableGraphics(component, view);
            }
        }

    }

    private void updateGraphic(VectorGraphic component, APLVectorGraphicView view, String graphicString) {
        component.resetGraphicContainerElement();
        component.updateGraphic(graphicString);
        createVectorDrawable(component, view);
    }

    void createVectorDrawable(VectorGraphic component, APLVectorGraphicView view) {
        AlexaVectorDrawable vectorDrawable = AlexaVectorDrawable.create(component.getOrCreateGraphicContainerElement());
        view.setImageDrawable(vectorDrawable);
    }

    void resetDrawableGraphics(VectorGraphic component, APLVectorGraphicView view) {
        component.resetGraphicContainerElement();
        view.setImageDrawable(null);
    }

    void applyDirtyGraphics(VectorGraphic component, APLVectorGraphicView view) {
        AlexaVectorDrawable vectorDrawable = (AlexaVectorDrawable) view.getDrawable();
        if (vectorDrawable == null) {
            if (component.hasGraphic())
                createVectorDrawable(component, view);
        } else {
            vectorDrawable.updateDirtyGraphics(component.getDirtyGraphics());
        }
    }
}
