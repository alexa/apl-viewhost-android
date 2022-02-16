/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.alexaext;

import android.content.Context;
import android.view.View;

import com.amazon.common.BoundObject;

import java.lang.ref.WeakReference;

/**
 * Associates an Extension Resource, a View, and accessors for a resource shared between an extension
 * and the execution environment. A ResourceHolder contains only associations, and no logic.
 *
 * See {@link ExtensionResourceProvider.Presenter} for logic.
 *
 * @param <R> The resource type shared with the extension.
 * @param <V> The view type shared with the execution environment.
 */
public class ResourceHolder<R, V extends View> extends BoundObject {

    private WeakReference<R> wResource;
    private WeakReference<V> wView;
    private WeakReference<Callback> wCallback;

    /**
     * Callback for the execution environment notification of resource changes.
     */
    public interface Callback {
        void resourceReady(String resourceId);

        void resourceDestroyed(String resourceId);

        void viewReady(String resourceId, View view);

        void viewDestroyed(String resourceId);
    }

    /**
     * Allows for a view to be injected into the rendering Context.
     */
    public interface ViewDelegate {
        void setContentView(View view);
        Context getContext();
    }

    /**
     * Protected construction of a ResourceHolder.  Construction is the responsibility of
     * {@link ExtensionResourceProvider}.
     * @param resourceId The resource identifier.
     */
    ResourceHolder(String resourceId) {
        final long handle = nCreate(resourceId);
        bind(handle);
    }

    /**
     * @return The resource identifier.
     */
    public String resourceId() {
        return nResourceId(getNativeHandle());
    }

    /**
     * Callback that updates the execution environment of resource lifecycle.
     * @param callback
     */
    void setCallback(Callback callback) {
        wCallback = new WeakReference<>(callback);
    }

    /**
     * @return The execution environment callback.
     */
     Callback getCallback() {
        if (wCallback == null)
            return null;
        return wCallback.get();
    }


    /**
     * Get the resource by specifying facet type.
     *
     * @return The resource.
     * @throws IllegalStateException If the resource cannot be accessed.
     */
    @SuppressWarnings("unchecked")
    public <T> T getFacet(Class<T> type) {
        // This is a light impl of a facet.  Typically a facet would be stored in
        // an Map<Class,Object>.  In this case the resource is the only queryable facet, so it is
        // cast on return.
        T result = type.cast(getResource());
       return result;
    }

    /**
     * Get the resource.
     *
     * @return The resource.
     * @throws IllegalStateException If the resource cannot be accessed.
     */
     R getResource() {
        try {
            return wResource.get();
        } catch (Exception e) {
            throw new IllegalStateException("Resource failure: " + e.getMessage(), e);
        }
    }

    /**
     * Set the resource for the holder.
     * @param resource
     */
    void setResource(R resource) {
        wResource = new WeakReference<>(resource);
    }

    /**
     * Set the view associated to the resource. See {@link ExtensionResourceProvider.Presenter}
     * @param view The view.
     */
    void setView(V view) {
        wView = new WeakReference<>(view);
    }


    private native long nCreate(String resourceId);

    private static native String nResourceId(long nativeHandle);


}
