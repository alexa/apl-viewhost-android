/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.util;

import static com.amazon.apl.devtools.enums.CommandMethod.DOCUMENT_GET_CONTEXT;
import static com.amazon.apl.devtools.enums.CommandMethod.DOCUMENT_GET_DOM;
import static com.amazon.apl.devtools.enums.CommandMethod.DOCUMENT_GET_MAIN_PACKAGE;
import static com.amazon.apl.devtools.enums.CommandMethod.DOCUMENT_GET_PACKAGE;
import static com.amazon.apl.devtools.enums.CommandMethod.DOCUMENT_GET_PACKAGE_LIST;
import static com.amazon.apl.devtools.enums.CommandMethod.DOCUMENT_GET_ROOT_CONTEXT;
import static com.amazon.apl.devtools.enums.CommandMethod.DOCUMENT_GET_SCENE_GRAPH;
import static com.amazon.apl.devtools.enums.CommandMethod.DOCUMENT_GET_VISUAL_CONTEXT;
import static com.amazon.apl.devtools.enums.CommandMethod.DOCUMENT_HIDE_HIGHLIGHT;
import static com.amazon.apl.devtools.enums.CommandMethod.DOCUMENT_HIGHLIGHT_COMPONENT;

import android.util.Log;

import com.amazon.apl.devtools.controllers.DTConnection;
import com.amazon.apl.devtools.enums.DTError;
import com.amazon.apl.devtools.executers.DocumentCommandRequest;
import com.amazon.apl.devtools.executers.FrameMetricsRecordCommandRequest;
import com.amazon.apl.devtools.executers.FrameMetricsStopCommandRequest;
import com.amazon.apl.devtools.executers.LiveDataUpdateCommandRequest;
import com.amazon.apl.devtools.executers.MemoryGetMemoryCommandRequest;
import com.amazon.apl.devtools.executers.NetworkDisableCommandRequest;
import com.amazon.apl.devtools.executers.NetworkEnableCommandRequest;
import com.amazon.apl.devtools.executers.PerformanceDisableCommandRequest;
import com.amazon.apl.devtools.executers.PerformanceEnableCommandRequest;
import com.amazon.apl.devtools.executers.PerformanceGetMetricsCommandRequest;
import com.amazon.apl.devtools.executers.TargetAttachToTargetCommandRequest;
import com.amazon.apl.devtools.executers.TargetGetTargetsCommandRequest;
import com.amazon.apl.devtools.executers.ViewCaptureImageCommandRequest;
import com.amazon.apl.devtools.executers.ViewExecuteCommandsCommandRequest;
import com.amazon.apl.devtools.executers.ViewSetDocumentCommandRequest;
import com.amazon.apl.devtools.executers.LogEnableCommandRequest;
import com.amazon.apl.devtools.executers.LogDisableCommandRequest;
import com.amazon.apl.devtools.executers.LogClearCommandRequest;
import com.amazon.apl.devtools.models.RequestHeader;
import com.amazon.apl.devtools.models.common.Request;
import com.amazon.apl.devtools.models.common.Response;
import com.amazon.apl.devtools.models.error.DTException;

import org.json.JSONException;
import org.json.JSONObject;

public final class CommandRequestFactory {
    private static final String TAG = CommandRequestFactory.class.getSimpleName();
    private final TargetCatalog mTargetCatalog;
    private final CommandMethodUtil mCommandMethodUtil;
    private final CommandRequestValidator mCommandRequestValidator;

    public CommandRequestFactory(TargetCatalog targetCatalog, CommandMethodUtil commandMethodUtil,
                                 CommandRequestValidator commandRequestValidator) {
        mTargetCatalog = targetCatalog;
        mCommandMethodUtil = commandMethodUtil;
        mCommandRequestValidator = commandRequestValidator;
    }

    // Using a wildcard because the response type associated with the returned request is unknown
    public Request<? extends Response> createCommandRequest(JSONObject obj, DTConnection connection)
            throws JSONException, DTException {
        Log.i(TAG, "Creating command request object");
        RequestHeader requestHeader = new RequestHeader(mCommandMethodUtil, obj);
        switch (requestHeader.getMethod()) {
            case TARGET_GET_TARGETS:
                return new TargetGetTargetsCommandRequest(mTargetCatalog, obj);
            case TARGET_ATTACH_TO_TARGET:
                return new TargetAttachToTargetCommandRequest(mTargetCatalog,
                        mCommandRequestValidator, obj, connection);
            case VIEW_SET_DOCUMENT:
                return new ViewSetDocumentCommandRequest(mCommandRequestValidator, obj, connection);
            case VIEW_CAPTURE_IMAGE:
                return new ViewCaptureImageCommandRequest(mCommandRequestValidator, obj,
                        connection);
            case VIEW_EXECUTE_COMMANDS:
                return new ViewExecuteCommandsCommandRequest(mCommandRequestValidator, obj,
                        connection);
            case LIVE_DATA_UPDATE:
                return new LiveDataUpdateCommandRequest(mCommandRequestValidator, obj,
                        connection);
            case PERFORMANCE_ENABLE:
                return new PerformanceEnableCommandRequest(mCommandRequestValidator, obj,
                        connection);
            case PERFORMANCE_DISABLE:
                return new PerformanceDisableCommandRequest(mCommandRequestValidator, obj,
                        connection);
            case PERFORMANCE_GET_METRICS:
                return new PerformanceGetMetricsCommandRequest(mCommandRequestValidator, obj,
                        connection);
            case MEMORY_GET_MEMORY:
                return new MemoryGetMemoryCommandRequest(obj);
            case FRAMEMETRICS_RECORD:
                return new FrameMetricsRecordCommandRequest(mCommandRequestValidator, obj, connection);
            case FRAMEMETRICS_STOP:
                return new FrameMetricsStopCommandRequest(mCommandRequestValidator, obj, connection);
            case LOG_ENABLE:
                return new LogEnableCommandRequest(mCommandRequestValidator, obj, connection);
            case LOG_DISABLE:
                return new LogDisableCommandRequest(mCommandRequestValidator, obj, connection);
            case LOG_CLEAR:
                return new LogClearCommandRequest(mCommandRequestValidator, obj, connection);
            case DOCUMENT_GET_MAIN_PACKAGE:
                return new DocumentCommandRequest(DOCUMENT_GET_MAIN_PACKAGE, mCommandRequestValidator, obj, connection);
            case DOCUMENT_GET_PACKAGE_LIST:
                return new DocumentCommandRequest(DOCUMENT_GET_PACKAGE_LIST, mCommandRequestValidator, obj, connection);
            case DOCUMENT_GET_PACKAGE:
                return new DocumentCommandRequest(DOCUMENT_GET_PACKAGE, mCommandRequestValidator, obj, connection);
            case DOCUMENT_GET_VISUAL_CONTEXT:
                return new DocumentCommandRequest(DOCUMENT_GET_VISUAL_CONTEXT, mCommandRequestValidator, obj, connection);
            case DOCUMENT_GET_DOM:
                return new DocumentCommandRequest(DOCUMENT_GET_DOM, mCommandRequestValidator, obj, connection);
            case DOCUMENT_GET_SCENE_GRAPH:
                return new DocumentCommandRequest(DOCUMENT_GET_SCENE_GRAPH, mCommandRequestValidator, obj, connection);
            case DOCUMENT_GET_ROOT_CONTEXT:
                return new DocumentCommandRequest(DOCUMENT_GET_ROOT_CONTEXT, mCommandRequestValidator, obj, connection);
            case DOCUMENT_GET_CONTEXT:
                return new DocumentCommandRequest(DOCUMENT_GET_CONTEXT, mCommandRequestValidator, obj, connection);
            case DOCUMENT_HIGHLIGHT_COMPONENT:
                return new DocumentCommandRequest(DOCUMENT_HIGHLIGHT_COMPONENT, mCommandRequestValidator, obj, connection);
            case DOCUMENT_HIDE_HIGHLIGHT:
                return new DocumentCommandRequest(DOCUMENT_HIDE_HIGHLIGHT, mCommandRequestValidator, obj, connection);
            case NETWORK_ENABLE:
                return new NetworkEnableCommandRequest(mCommandRequestValidator, obj, connection);
            case NETWORK_DISABLE:
                return new NetworkDisableCommandRequest(mCommandRequestValidator, obj, connection);
            default:
                throw new DTException(requestHeader.getId(),
                        DTError.METHOD_NOT_IMPLEMENTED.getErrorCode(), DTError.METHOD_NOT_IMPLEMENTED.getErrorMsg());
        }
    }
}
