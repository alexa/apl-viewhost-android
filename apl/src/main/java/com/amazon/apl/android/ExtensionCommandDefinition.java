/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;


import android.util.Log;

import com.amazon.common.BoundObject;

import java.util.HashSet;
import java.util.Set;


/**
 * Define a custom document-level command.  The name of the command should be
 * unique and not overlap with any macros or existing commands. A sample registration
 * is:
 * <p>
 * rootConfig.registerExtensionCommand(
 * ExtensionCommandDefinition("MyURI", "ChangeVolume").allowFastMode(true)
 * .property("volume", 3, false)
 * .property("channel", "all", false)
 * );
 * <p>
 * This command may now be used from within APL:
 * <p>
 * "onPress": {
 * "type": "MyURI:ChangeVolume",
 * "volume": 7
 * }
 * <p>
 * When this command fires, it will be returned as an ExtensionEvent to the RootContext.  The custom
 * command will have the following values: kEventPropertyExtension, kEventPropertyName, kEventPropertySource,
 * and kEventPropertyCustom.
 * <p>
 * For example, the above "ChangeVolume" custom command will satisfy:
 * <p>
 * event.getType() == kEventTypeCustom
 * event.getValue(kEventPropertyName) == Object("ChangeVolume")
 * event.getValue(kEventPropertyExtensionURI) == Object("MyURI")
 * event.getValue(kEventPropertySource).get("type") == Object("TouchWrapper")
 * event.getValue(kEventPropertyCustom).get("volume") == Object(7)
 * event.getValue(kEventPropertyCustom).get("channel") == Object("all")
 * <p>
 * kEventPropertyExtensionURI is the URI of the extension
 * kEventPropertyName is the name of the extension assigned by the APL document
 * kEventPropertySource is a map of the source object that generated the event.
 * See the SendEvent command for a description of the source fields.
 * kEventPropertyCustom value is a map of the user-specified properties listed at registration time.
 */
public class ExtensionCommandDefinition extends BoundObject {

    private static final String TAG = "ExtCommandDef";

    private Set<String> mProperties = new HashSet<>();

    /**
     * Standard constructor
     *
     * @param name The name of the command.
     */
    public ExtensionCommandDefinition(String uri, String name) {
        long handle = nCreate(uri, name);
        if (handle != 0) {
            bind(handle);
        }
    }


    /**
     * Configure if this command can run in fast mode.  When the command runs in fast mode the
     * "requireResolution" property is ignored (fast mode commands do not support action resolution).
     *
     * @param allowFastMode If true, this command can run in fast mode.
     * @return This object for chaining
     */
    public ExtensionCommandDefinition allowFastMode(boolean allowFastMode) {
        nAllowFastMode(getNativeHandle(), allowFastMode);
        return this;
    }


    /**
     * Configure if this command (in normal mode) will return an action pointer
     * that must be resolved by the view host before the next command in the sequence is executed.
     *
     * @param requireResolution If true, this command will provide an action pointer (in normal mode).
     * @return This object for chaining.
     */
    public ExtensionCommandDefinition requireResolution(boolean requireResolution) {
        nRequireResolution(getNativeHandle(), requireResolution);
        return this;
    }


    /**
     * Add a named property. The property names "when" and "type" are reserved.
     *
     * @param property The property to add
     * @return This object for chaining.
     */
    public ExtensionCommandDefinition property(String property, Object defvalue, boolean required) {
        //TODO pull the failure and log from core
        if (property.equalsIgnoreCase("when") || property.equalsIgnoreCase("type"))
            Log.w(TAG, "Unable to register property '" + property
                    + "' in custom command " + getName());
        else {
            mProperties.add(property);
            nProperty(getNativeHandle(), property, defvalue, required);
        }
        return this;
    }


    /**
     * Add a named array-ified property. The property will be converted into an array of values. The names "when"
     * and "type" are reserved.
     *
     * @param property The property to add
     * @param required If true and the property is not provided, the command will not execute.
     * @return This object for chaining.
     */
    public ExtensionCommandDefinition arrayProperty(String property, boolean required) {
        //TODO pull the failure and log from core
        if (property.equalsIgnoreCase("when") || property.equalsIgnoreCase("type"))
            Log.w(TAG, "Unable to register array-ified property '" + property
                    + "' in custom command " + getName());
        else {
            mProperties.add(property);
            nArrayProperty(getNativeHandle(), property, required);
        }
        return this;
    }


    /**
     * @return The URI of the extension
     */
    public String getURI() {
        return nGetURI(getNativeHandle());
    }

    /**
     * @return The name of the command
     */
    public String getName() {
        return nGetName(getNativeHandle());
    }

    /**
     * @return Set of properties command requires.
     */
    Set<String> getProperties() {
        return mProperties;
    }


    /**
     * @return True if this command can execute in fast mode
     */
    public boolean getAllowFastMode() {
        return nGetAllowFastMode(getNativeHandle());
    }


    /**
     * @return True if this command will return an action pointer that must be
     * resolved.  Please note that a command running in fast mode will
     * never wait to be resolved.
     */
    public boolean getRequireResolution() {
        return nGetRequireResolution(getNativeHandle());
    }


    /**
     * @return A property value in this custom command, null if the property doesn't exist.
     */
    public <T> T getPropertyValue(String name) {
        return (T) nGetPropertyValue(getNativeHandle(), name);
    }

    /**
     * @return false if the property is not required, or does not exist.
     */
    public boolean isPropertyRequired(String name) {
        return nIsPropertyRequired(getNativeHandle(), name);
    }

    /**
     * @return The count of properties in this custom command.
     */
    public int getPropertyCount() {
        return nGetPropertyCount(getNativeHandle());
    }


    private static native long nCreate(String uri, String name);

    private static native void nAllowFastMode(long nativeHandle, boolean allowFastMode);

    private static native void nRequireResolution(long nativeHandle, boolean requireResolution);

    private static native void nProperty(long nativeHandle, String property, Object defvalue, boolean required);

    private static native void nArrayProperty(long nativeHandle, String property, boolean required);

    private static native String nGetURI(long nativeHandle);

    private static native String nGetName(long nativeHandle);

    private static native boolean nGetAllowFastMode(long nativeHandle);

    private static native boolean nGetRequireResolution(long nativeHandle);

    private static native Object nGetPropertyValue(long nativeHandle, String name);

    private static native int nGetPropertyCount(long nativeHandle);

    private static native boolean nIsPropertyRequired(long nativeHandle, String name);
}
