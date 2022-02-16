/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.component;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.amazon.alexaext.ExtensionResourceProvider;
import com.amazon.alexaext.ResourceHolder;
import com.amazon.apl.android.ExtensionComponent;
import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.views.APLExtensionView;
import com.amazon.apl.enums.ExtensionComponentResourceState;

import java.util.Objects;

/**
 * Model - View adapter for Extension Components.  The adapter is stateless and is responsible
 * for reflecting the Component state in the associated View.  (Presenter pattern)
 */
public class ExtensionComponentViewAdapter extends ComponentViewAdapter<ExtensionComponent, APLExtensionView> {

    private static final String TAG = "ExtensionCompViewAdpt";


    private static ExtensionComponentViewAdapter INSTANCE;


    private ExtensionComponentViewAdapter() {
        super();
    }

    /**
     * @return The singleton view adapter.
     */
    public static ExtensionComponentViewAdapter getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ExtensionComponentViewAdapter();
        }
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public APLExtensionView createView(Context context, IAPLViewPresenter presenter) {
        // Use a simple frame as a container for the extension driven resource to be inserted
        return new APLExtensionView(context, presenter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void applyPadding(ExtensionComponent component, APLExtensionView view) {
        setPaddingFromBounds(component, view, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void applyAllProperties(ExtensionComponent component, APLExtensionView view) {
        super.applyAllProperties(component, view);
        // add the resource to the frame layout
        setResource(component, view);
    }

    /**
     * Set a content view based on the resource type defined by the extension.
     * Default "Surface" uses a SurfaceView shared with the extension.
     * "Custom" is a View provided by the extension.
     */
    private void setResource(@NonNull ExtensionComponent component, @NonNull FrameLayout ercView) {
        Objects.requireNonNull(component);
        Objects.requireNonNull(ercView);

        // Create a callback for resource and view updates
        // register for lifecycle changes in the resource
        ResourceHolder.Callback callback = new ResourceHolder.Callback() {
            @Override
            public void resourceReady(String resourceId) {
                component.updateResourceState(ExtensionComponentResourceState.kResourceReady);
            }

            @Override
            public void resourceDestroyed(String resourceId) {
                component.updateResourceState(ExtensionComponentResourceState.kResourceReady);
            }

            @Override
            public void viewReady(String resourceId, View view) {
                // add view of the resource to the view tree
                ercView.addView(view);
            }

            @Override
            public void viewDestroyed(String resourceId) {
                // add view of the resource to the view tree
                ercView.removeAllViews();
            }
        };
        // Create create a resource holder to manage the associations between the view, resource, callback
        // The holder is managed by the provider.
        ExtensionResourceProvider provider = component.getRenderingContext().getResourceProvider();
        ExtensionResourceProvider.Presenter presenter =
                provider.selectPresenter(component.getResourceType());
        ResourceHolder holder = presenter.createResourceHolder(component.getResourceId(),
                component.getViewPresenter().getContext(), callback);

    }



}
