/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.utils;

public enum TracePoint {
    // Inflation
    APL_CONTROLLER_RENDER_DOCUMENT("APLController", "renderDocument"),

    ROOT_CONTEXT_CREATE("RootContext", "create"),
    TEXT_MEASURE("TextMeasure", "measure"),
    APL_LAYOUT_INFLATE_COMPONENT_HIERARCHY("APLLayout", "inflateComponentHierarchy"),
    APL_LAYOUT_ON_LAYOUT("APLLayout", "onLayout"),
    APL_LAYOUT_ON_LAYOUT_FOR("APLLayout", "onLayout.for"),

    // Frame loop
    ROOT_CONTEXT_DO_FRAME("RootContext", "doFrame"),
    ROOT_CONTEXT_UPDATE_TIME("RootContext", "updateTime"),
    ROOT_CONTEXT_CLEAR_PENDING("RootContext", "clearPending"),
    ROOT_CONTEXT_HANDLE_DIRTY_PROPERTIES("RootContext", "handleDirtyProperties"),
    ROOT_CONTEXT_HANDLE_EVENTS("RootContext", "handleEvents"),
    ROOT_CONTEXT_NOTIFY_VISUAL_CONTEXT("RootContext", "notifyVisualContext"),
    ROOT_CONTEXT_NOTIFY_DATA_SOURCE_CONTEXT("RootContext", "notifyDataSourceContext"),

    // Viewhost updates
    ROOT_CONTEXT_ON_COMPONENT_CHANGE("RootContext", "onComponentChange"),

    COMPONENT_REQUEST_LAYOUT("Component", "requestLayout"),
    COMPONENT_ON_CHILDREN_CHANGED("Component", "onChildrenChanged"),
    TEXT_APPLY_LAYOUT("Text", "applyLayout"),
    IMAGE_INIT_IMAGE_LOAD("Image", "initImageLoad"),


    // Miscellany
    ROOT_CONTEXT_RE_INFLATE("RootContext", "reinflate"),
    APL_LAYOUT_HANDLE_TOUCH("APLLayout", "handleTouch"),
    APL_LAYOUT_HANDLE_CONFIGURATION_CHANGE("APLLayout", "handleConfigurationChange");

    private final String mClassName;
    private final String mMethodName;

    TracePoint(String className, String methodName) {
        mClassName = className;
        mMethodName = methodName;
    }

    public String getClassName() {
        return mClassName;
    }

    public String getMethodName() {
        return mMethodName;
    }
}
