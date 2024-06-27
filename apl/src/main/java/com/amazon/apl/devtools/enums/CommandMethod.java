/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.enums;

import androidx.annotation.NonNull;

public enum CommandMethod {
    EMPTY(""),
    TARGET_GET_TARGETS(CommandMethod.TARGET_GET_TARGETS_TEXT),
    TARGET_ATTACH_TO_TARGET(CommandMethod.TARGET_ATTACH_TO_TARGET_TEXT),
    TARGET_DETACH_FROM_TARGET(CommandMethod.TARGET_DETACH_FROM_TARGET_TEXT),
    VIEW_SET_DOCUMENT(CommandMethod.VIEW_SET_DOCUMENT_TEXT),
    VIEW_CAPTURE_IMAGE(CommandMethod.VIEW_CAPTURE_IMAGE_TEXT),
    VIEW_EXECUTE_COMMANDS(CommandMethod.VIEW_EXECUTE_COMMANDS_TEXT),
    LIVE_DATA_UPDATE(CommandMethod.LIVE_DATA_UPDATE_TEXT),
    PERFORMANCE_GET_METRICS(CommandMethod.PERFORMANCE_GET_METRICS_TEXT),
    PERFORMANCE_ENABLE(CommandMethod.PERFORMANCE_ENABLE_TEXT),
    PERFORMANCE_DISABLE(CommandMethod.PERFORMANCE_DISABLE_TEXT),
    MEMORY_GET_MEMORY(CommandMethod.MEMORY_GET_MEMORY_TEXT),
    FRAMEMETRICS_RECORD(CommandMethod.FRAMEMETRICS_RECORD_TEXT),
    FRAMEMETRICS_STOP(CommandMethod.FRAMEMETRICS_STOP_TEXT),
    LOG_ENABLE(CommandMethod.LOG_ENABLE_TEXT),
    LOG_DISABLE(CommandMethod.LOG_DISABLE_TEXT),
    LOG_CLEAR(CommandMethod.LOG_CLEAR_TEXT),
    DOCUMENT_GET_MAIN_PACKAGE(CommandMethod.DOCUMENT_GET_MAIN_PACKAGE_TEXT),
    DOCUMENT_GET_PACKAGE_LIST(CommandMethod.DOCUMENT_GET_PACKAGE_LIST_TEXT),
    DOCUMENT_GET_PACKAGE(CommandMethod.DOCUMENT_GET_PACKAGE_TEXT),
    DOCUMENT_GET_VISUAL_CONTEXT(CommandMethod.DOCUMENT_GET_VISUAL_CONTEXT_TEXT),
    DOCUMENT_GET_DOM(CommandMethod.DOCUMENT_GET_DOM_TEXT),
    DOCUMENT_GET_SCENE_GRAPH(CommandMethod.DOCUMENT_GET_SCENE_GRAPH_TEXT),
    DOCUMENT_GET_ROOT_CONTEXT(CommandMethod.DOCUMENT_GET_ROOT_CONTEXT_TEXT),
    DOCUMENT_GET_CONTEXT(CommandMethod.DOCUMENT_GET_CONTEXT_TEXT),
    DOCUMENT_HIGHLIGHT_COMPONENT(CommandMethod.DOCUMENT_HIGHLIGHT_COMPONENT_TEXT),
    DOCUMENT_HIDE_HIGHLIGHT(CommandMethod.DOCUMENT_HIDE_HIGHLIGHT_TEXT),
    INPUT_TOUCH(CommandMethod.INPUT_TOUCH_TEXT),
    INPUT_CANCEL(CommandMethod.INPUT_CANCEL_TEXT),
    NETWORK_ENABLE(CommandMethod.NETWORK_ENABLE_TEXT),
    NETWORK_DISABLE(CommandMethod.NETWORK_DISABLE_TEXT),
    SYSTEM_INFO_GET_APPLICATION_PROCESSOR_USAGE(CommandMethod.SYSTEM_INFO_GET_APPLICATION_PROCESSOR_USAGE_TEXT),
    SYSTEM_INFO_GET_ENVIRONMENT_PROCESSOR_USAGE(CommandMethod.SYSTEM_INFO_GET_ENVIRONMENT_PROCESSOR_USAGE_TEXT),
    SYSTEM_INFO_GET_ENVIRONMENT_MEMORY(CommandMethod.SYSTEM_INFO_GET_ENVIRONMENT_MEMORY_TEXT);

    private static final String TARGET_GET_TARGETS_TEXT = "Target.getTargets";
    private static final String TARGET_ATTACH_TO_TARGET_TEXT = "Target.attachToTarget";
    private static final String TARGET_DETACH_FROM_TARGET_TEXT = "Target.detachFromTarget";
    private static final String VIEW_SET_DOCUMENT_TEXT = "View.setDocument";
    private static final String VIEW_CAPTURE_IMAGE_TEXT = "View.captureImage";
    private static final String VIEW_EXECUTE_COMMANDS_TEXT = "View.executeCommands";
    private static final String LIVE_DATA_UPDATE_TEXT = "LiveData.update";
    private static final String PERFORMANCE_GET_METRICS_TEXT = "Performance.getMetrics";
    private static final String PERFORMANCE_ENABLE_TEXT = "Performance.enable";
    private static final String PERFORMANCE_DISABLE_TEXT = "Performance.disable";
    private static final String MEMORY_GET_MEMORY_TEXT = "Memory.getMemory";
    private static final String FRAMEMETRICS_RECORD_TEXT = "FrameMetrics.record";
    private static final String FRAMEMETRICS_STOP_TEXT = "FrameMetrics.stop";
    private static final String LOG_ENABLE_TEXT = "Log.enable";
    private static final String LOG_DISABLE_TEXT = "Log.disable";
    private static final String LOG_CLEAR_TEXT = "Log.clear";
    private static final String DOCUMENT_GET_MAIN_PACKAGE_TEXT = "Document.getMainPackage";
    private static final String DOCUMENT_GET_PACKAGE_LIST_TEXT = "Document.getPackageList";
    private static final String DOCUMENT_GET_PACKAGE_TEXT = "Document.getPackage";
    private static final String DOCUMENT_GET_VISUAL_CONTEXT_TEXT = "Document.getVisualContext";
    private static final String DOCUMENT_GET_DOM_TEXT = "Document.getDOM";
    private static final String DOCUMENT_GET_SCENE_GRAPH_TEXT = "Document.getSceneGraph";
    private static final String DOCUMENT_GET_ROOT_CONTEXT_TEXT = "Document.getRootContext";
    private static final String DOCUMENT_GET_CONTEXT_TEXT = "Document.getContext";
    private static final String DOCUMENT_HIGHLIGHT_COMPONENT_TEXT = "Document.highlightComponent";
    private static final String DOCUMENT_HIDE_HIGHLIGHT_TEXT = "Document.hideHighlight";
    private static final String INPUT_TOUCH_TEXT = "Input.touch";
    private static final String INPUT_CANCEL_TEXT = "Input.cancel";
    private static final String NETWORK_ENABLE_TEXT = "Network.enable";
    private static final String NETWORK_DISABLE_TEXT = "Network.disable";
    private static final String SYSTEM_INFO_GET_APPLICATION_PROCESSOR_USAGE_TEXT = "SystemInfo.getApplicationProcessorUsage";
    private static final String SYSTEM_INFO_GET_ENVIRONMENT_PROCESSOR_USAGE_TEXT = "SystemInfo.getEnvironmentProcessorUsage";
    private static final String SYSTEM_INFO_GET_ENVIRONMENT_MEMORY_TEXT = "SystemInfo.getEnvironmentMemory";

    private final String mCommandMethodText;

    CommandMethod(String commandMethodText) {
        mCommandMethodText = commandMethodText;
    }

    @NonNull
    @Override
    public String toString() {
        return mCommandMethodText;
    }
}
