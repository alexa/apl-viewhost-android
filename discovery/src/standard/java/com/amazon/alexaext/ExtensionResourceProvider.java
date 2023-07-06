/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.alexaext;

import android.content.Context;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.NonNull;

import com.amazon.alexaext.ResourceHolder.ViewDelegate;
import com.amazon.common.BoundObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Provider for access to shared resources between the extension and execution environment.
 */
public class ExtensionResourceProvider extends BoundObject {

    // Map of resource id to the rendering resource
    private final Map<String, ResourceHolder> mResources = new HashMap<>();


    public ExtensionResourceProvider() {
        final long handle = nCreate();
        bind(handle);
    }

    static ExtensionResourceProvider noOpInstance;

    public static ExtensionResourceProvider noOpInstance() {
        if (noOpInstance == null) {
            // TODO reconsider implications of BoundObject
            noOpInstance = new ExtensionResourceProvider() {
                @Override
                public Presenter selectPresenter(String type) {
                    return null;
                }
            };
        }
        return noOpInstance;
    }

    /**
     * The resource framework (from JNI) is requesting a resource to share with the extension.
     */
    @SuppressWarnings("unused")
    private long requestResource(String resourceId) {
        //todo may want to return ResourceHolder
        ResourceHolder holder = mResources.get(resourceId);
        if (holder == null)
            throw new IllegalStateException("Resource Not Available.");
        return holder.getNativeHandle();
    }

    public ResourceHolder getResource(String resourceId) {
        ResourceHolder holder = mResources.get(resourceId);
        if (holder == null)
            throw new IllegalStateException("Resource Not Available.");
        return holder;
    }


    public abstract class Presenter<R, V extends View> {
        public abstract ResourceHolder<R, V> createResourceHolder(String resourceId,
                                                                  Context context,
                                                                  ResourceHolder.Callback callback);

        public void bindResource(ResourceHolder holder, R resource) {
            holder.setResource(resource);
            // make the resource available to the extension
            associateResource(holder);
            // notify any resource listener of state change
            ResourceHolder.Callback cb = holder.getCallback();
            if (cb != null) {
                cb.resourceReady(holder.resourceId());
            }
        }

        public void unbindResource(ResourceHolder holder) {
            // notify any resource listener of state change
            ResourceHolder.Callback cb = holder.getCallback();
            if (cb != null) {
                cb.resourceDestroyed(holder.resourceId());
            }
            disassociateResource(holder);
            holder.setResource(null);
        }

        public void bindView(ResourceHolder holder, V view) {
            holder.setView(view);
            // notify any resource listener of state change
            ResourceHolder.Callback cb = holder.getCallback();
            if (cb != null) {
                cb.viewReady(holder.resourceId(), view);
            }
        }

        public void unbindView(ResourceHolder holder) {
            // notify any resource listener of state change
            ResourceHolder.Callback cb = holder.getCallback();
            if (cb != null) {
                cb.viewDestroyed(holder.resourceId());
            }
            holder.setView(null);
        }

    } // Presenter


    final Presenter<SurfaceHolder, SurfaceView> surfacePresenter = new Presenter<SurfaceHolder, SurfaceView>() {

        @Override
        public ResourceHolder createResourceHolder(String resourceId, Context context,
                                                   ResourceHolder.Callback callback) {
            // Create a ResourceHolder and assign the lifecycle callback
            ResourceHolder<SurfaceHolder, SurfaceView> resourceHolder = new ResourceHolder<>(resourceId);
            resourceHolder.setCallback(callback);

            // Create the SurfaceView, update the resource state when the surface changes
            SurfaceView view = new SurfaceView(context);
            view.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public synchronized void surfaceCreated(SurfaceHolder holder) {
                    // Bind the resource, making access the available
                    resourceHolder.setResource(view.getHolder());
                    bindResource(resourceHolder, holder);
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                    // Do nothing
                }

                @Override
                public synchronized void surfaceDestroyed(SurfaceHolder holder) {
                    // Unbind the resource making it unavailable
                    unbindResource(resourceHolder);
                    unbindView(resourceHolder);
                }
            });
            // bind the view immediately
            bindView(resourceHolder, view);

            return resourceHolder;
        }
    }; // surfacePresenter


    final Presenter<ViewDelegate, View> customPresenter = new Presenter<ViewDelegate, View>() {

        @Override
        public ResourceHolder<ViewDelegate, View> createResourceHolder(@NonNull String resourceId,
                                                                       @NonNull Context context,
                                                                       ResourceHolder.Callback callback) {
            // Create a ResourceHolder and assign the lifecycle callback
            final ResourceHolder<ViewDelegate, View> resourceHolder = new ResourceHolder<>(resourceId);
            resourceHolder.setCallback(callback);
            final WeakReference<Context> weakContext = new WeakReference<>(context);

            // Create a delegate that allows the extension to inject the view
            ViewDelegate delegate = new ViewDelegate() {
                @Override
                public void setContentView(View view) {
                    bindView(resourceHolder, view);
                }

                @Override
                public Context getContext() {
                    return weakContext.get();
                }
            };
            bindResource(resourceHolder, delegate);

            return resourceHolder;
        }
    }; // customPresenter


    final Presenter<SurfaceHolder, SurfaceView> defaultPresenter = surfacePresenter;


    /**
     * Select a presenter based on the resource type.
     *
     * @param type The resource type.
     * @return The presenter for the resource type.
     * @throws IllegalStateException if no presenter is available.
     */
    public Presenter selectPresenter(String type) {

        // TODO supported list is registered by environment rather than case statement

        switch (type.toUpperCase()) {
            case "SURFACE":
                return surfacePresenter;
            case "CUSTOM":
                return customPresenter;
            case "DEFAULT":
            case "":
                return defaultPresenter;
            default:
                throw new IllegalStateException("The resource type is not supported: " + type);
        }
    }

    /**
     * Associate a rendering resource to a resource identifier. The resource is only available after
     * the environment owning the resource is created, and the system allocates the rendering resource.
     * Example resources include {@link android.view.SurfaceHolder}
     *
     * @param resource The the resource.
     */
    void associateResource(@NonNull ResourceHolder resource) {
        mResources.put(resource.resourceId(), resource);
    }

    /**
     * Remove the association of a resource identifier from a Component.  The resource is
     * typically destroyed as a result of normal lifecycle events, or error.
     * <p>
     * See associated {@link #associateResource(ResourceHolder)} for association.
     *
     * @param resource The the resource.
     */
    void disassociateResource(@NonNull ResourceHolder resource) {
        mResources.remove(resource.resourceId());
    }

    private native long nCreate();


}
