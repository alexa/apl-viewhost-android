/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import androidx.annotation.VisibleForTesting;
import android.util.Log;

import com.amazon.apl.enums.ImageCount;

/**
 * Declare a custom filter for use in Image components.  The name of the filter should
 * be unique and not overlap with any existing filters.  A sample registration is:
 * <p>
 * rootConfig.registerExtensionFilter(
 * ExtensionFilterDefinition("MyURI", "CannyEdgeDetector", ExtensionFilterDefinition::ONE)
 * .property("min", 0.1, kBindingTypeNumber)
 * .property("max", 0.9, kBindingTypeNumber);
 * </p>
 * <p>
 * This filter may now be used in an Image component filter list:
 *
 * {
 *   "type": "Image",
 *   "filters": [
 *     {
 *       "type": "MyURI:CannyEdgeDetector",
 *       "min": 0.2,
 *       "max": 0.8,
 *       "source": 2
 *     }
 *   ]
 * </p>
 * <p>
 * The filter will satisfy
 *
 *   filter.getType()                                     == kFilterTypeExtension
 *   filter.getValue(kFilterPropertyExtensionURI)         == "MyURI"
 *   filter.getValue(kFilterPropertyName)                 == "CannyEdgeDetector"
 *   filter.getValue(kFilterPropertySource)               == 2
 *   filter.getValue(kFilterPropertyExtension).get("min") == 0.2
 *   filter.getValue(kFilterPropertyExtension).get("max") == 0.8
 *
 * A custom filter will have the following properties:
 *
 *   kFilterPropertyExtension       Map of string -> Object (includes properties like source, destination)
 *   kFilterPropertyExtensionURI    URI of the extension
 *   kFilterPropertyName            Name of the extension command
 *   kFilterPropertySource          If ImageCount == ONE || TWO
 *   kFilterPropertyDestination     If ImageCount == TWO
 * </p>
 */
public class ExtensionFilterDefinition extends BoundObject {
    private final static String TAG = "ExtensionFilter";

    /**
     * Standard constructor
     *
     * @param name The name of the filter.
     */
    public ExtensionFilterDefinition(String uri, String name, ImageCount imageCount) {
        long handle = nCreate(uri, name, imageCount.getIndex());
        if (handle != 0) {
            bind(handle);
        }
    }

    /**
     * Add a named property. The property names "when", "type", "source" and "destination" are reserved.
     *
     * @param property The property to add
     * @return This object for chaining.
     */
    public ExtensionFilterDefinition property(String property, Object defvalue) {
        //TODO pull the failure and log from core
        if (property.equalsIgnoreCase("when") ||
                property.equalsIgnoreCase("type") ||
                property.equalsIgnoreCase("source") ||
                property.equalsIgnoreCase("destination")) {
            Log.w(TAG, "Unable to register property '" + property
                    + "' in custom filter " + getName());
        } else
            nProperty(getNativeHandle(), property, defvalue);
        return this;
    }

    /**
     * @return The URI of the extension
     */
    @VisibleForTesting
    public String getURI() {
        return nGetURI(getNativeHandle());
    }

    /**
     * @return The name of the filter
     */
    @VisibleForTesting
    public String getName() {
        return nGetName(getNativeHandle());
    }

    /**
     * @return The count of properties in this custom filter.
     */
    @VisibleForTesting
    public int getPropertyCount() {
        return nGetPropertyCount(getNativeHandle());
    }

    /**
     * @return A property value in this custom filter, null if the property doesn't exist.
     */
    @VisibleForTesting
    public <T> T getPropertyValue(String name) {
        return (T) nGetPropertyValue(getNativeHandle(), name);
    }

    private static native long nCreate(String uri, String name, int imageCount);

    private static native void nProperty(long nativeHandle, String property, Object defvalue);

    private static native String nGetURI(long nativeHandle);

    private static native String nGetName(long nativeHandle);

    private static native Object nGetPropertyValue(long nativeHandle, String name);

    private static native int nGetPropertyCount(long nativeHandle);
}
