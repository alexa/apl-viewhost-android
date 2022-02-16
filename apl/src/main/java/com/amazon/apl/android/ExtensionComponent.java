/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import androidx.annotation.NonNull;

import com.amazon.apl.enums.ExtensionComponentResourceState;

import static com.amazon.apl.enums.PropertyKey.kPropertyResourceId;
import static com.amazon.apl.enums.PropertyKey.kPropertyResourceState;
import static com.amazon.apl.enums.PropertyKey.kPropertyResourceType;

/**
 * ExtensionComponent is defined by the extension and requested by the document.  The
 * display of an ExtensionComponent is dependent on the resource type defined by the extension.
 * For example, a "Surface" for remote rendering may defined as the resource type.
 */
public class ExtensionComponent extends Component {
    private static final String TAG = ExtensionComponent.class.getSimpleName();

    /**
     * Component constructor.
     *
     * @param nativeHandle The native handle to bind.
     * @param componentId  The unique string id of the component.
     * @param renderingContext the rendering context
     */
    protected ExtensionComponent(long nativeHandle, String componentId, @NonNull RenderingContext renderingContext) {
        super(nativeHandle, componentId, renderingContext);
    }

    /**
     * @return The extension URI associated with this component.
     */
    public String getUri() {
        return nGetUri(getNativeHandle());
    }

    /**
     * @return The resource identifier associated with the component.
     */
    public String getResourceId() {
        return mProperties.getString(kPropertyResourceId);
    }

    /**
     * @return The resource type identifier associated with the component.
     */
    public String getResourceType() {
        return mProperties.getString(kPropertyResourceType);
    }

    /**
     * @return The state of the extension resource used in rendering.
     */
    public ExtensionComponentResourceState getResourceState() {
        return ExtensionComponentResourceState.valueOf(mProperties.getEnum(kPropertyResourceState));
    }

    /**
     * Update the state of the component as a result of a resource state change.
     */
    public void updateResourceState(ExtensionComponentResourceState state) {
        nUpdateExtensionResourceState(getNativeHandle(), state.getIndex());
    }


    private static native String nGetUri(long nativeHandle);
    private static native void nUpdateExtensionResourceState(long nativeHandle, int state);

}
