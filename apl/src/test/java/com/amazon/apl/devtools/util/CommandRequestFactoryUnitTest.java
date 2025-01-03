/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazon.apl.devtools.controllers.impl.DTConnection;
import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.executers.DocumentCommandRequest;
import com.amazon.apl.devtools.executers.FrameMetricsDisableCommandRequest;
import com.amazon.apl.devtools.executers.FrameMetricsEnableCommandRequest;
import com.amazon.apl.devtools.executers.MemoryGetMemoryCommandRequest;
import com.amazon.apl.devtools.executers.NetworkDisableCommandRequest;
import com.amazon.apl.devtools.executers.NetworkEnableCommandRequest;
import com.amazon.apl.devtools.executers.PerformanceDisableCommandRequest;
import com.amazon.apl.devtools.executers.PerformanceEnableCommandRequest;
import com.amazon.apl.devtools.executers.TargetAttachToTargetCommandRequest;
import com.amazon.apl.devtools.executers.TargetGetTargetsCommandRequest;
import com.amazon.apl.devtools.executers.ViewCaptureImageCommandRequest;
import com.amazon.apl.devtools.executers.ViewExecuteCommandsCommandRequest;
import com.amazon.apl.devtools.executers.ViewSetDocumentCommandRequest;
import com.amazon.apl.devtools.executers.LogClearCommandRequest;
import com.amazon.apl.devtools.executers.LogDisableCommandRequest;
import com.amazon.apl.devtools.executers.LogEnableCommandRequest;
import com.amazon.apl.devtools.models.Session;
import com.amazon.apl.devtools.models.Target;
import com.amazon.apl.devtools.models.common.Request;
import com.amazon.apl.devtools.models.common.Response;
import com.amazon.apl.devtools.models.error.DTException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class CommandRequestFactoryUnitTest {
    @Mock
    private DTConnection mConnection;

    @Mock
    private CommandMethodUtil mCommandMethodUtil;


    private CommandRequestFactory mCommandRequestFactory;

    @Before
    public void setup() {
        mConnection = mock(DTConnection.class);
        mCommandMethodUtil = mock(CommandMethodUtil.class);
        TargetCatalog targetCatalog = mock(TargetCatalog.class);
        CommandRequestValidator commandRequestValidator = mock(CommandRequestValidator.class);
        Target target = mock(Target.class);
        Session session = mock(Session.class);

        // These methods are called during validation while constructing a command request object
        when(targetCatalog.has(any())).thenReturn(true);
        when(targetCatalog.get(any())).thenReturn(target);
        when(mConnection.getSession(any())).thenReturn(session);

        mCommandRequestFactory = new CommandRequestFactory(targetCatalog, mCommandMethodUtil,
                commandRequestValidator);
    }

    @Test
    public void createCommandRequest_forTargetGetTargets_returnsCorrectRequestObject() {
        try {
            JSONObject obj = new JSONObject()
                    .put("method", CommandMethod.TARGET_GET_TARGETS.toString())
                    .put("id", 100);
            when(mCommandMethodUtil.parseMethod(any())).thenReturn(
                    CommandMethod.TARGET_GET_TARGETS);
            Request<? extends Response> request = mCommandRequestFactory.createCommandRequest(obj,
                    mConnection);
            assertTrue(request instanceof TargetGetTargetsCommandRequest);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createCommandRequest_forTargetAttachToTarget_returnsCorrectRequestObject() {
        try {
            JSONObject obj = new JSONObject()
                    .put("method", CommandMethod.TARGET_ATTACH_TO_TARGET.toString())
                    .put("id", 100)
                    .put("params", new JSONObject()
                            .put("targetId", "target100"));
            when(mCommandMethodUtil.parseMethod(any())).thenReturn(
                    CommandMethod.TARGET_ATTACH_TO_TARGET);
            Request<? extends Response> request = mCommandRequestFactory.createCommandRequest(obj,
                    mConnection);
            assertTrue(request instanceof TargetAttachToTargetCommandRequest);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createCommandRequest_forViewSetDocument_documentOnly_returnsCorrectRequestObject() {
        try {
            JSONObject obj = new JSONObject()
                    .put("method", CommandMethod.VIEW_SET_DOCUMENT.toString())
                    .put("id", 100)
                    .put("sessionId", "session100")
                    .put("params", new JSONObject()
                            .put("document", new JSONObject()));
            when(mCommandMethodUtil.parseMethod(any())).thenReturn(
                    CommandMethod.VIEW_SET_DOCUMENT);
            Request<? extends Response> request = mCommandRequestFactory.createCommandRequest(obj,
                    mConnection);
            assertTrue(request instanceof ViewSetDocumentCommandRequest);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createCommandRequest_forViewSetDocument_documentAndDatasources_returnsCorrectRequestObject() {
        try {
            JSONObject obj = new JSONObject()
                    .put("method", CommandMethod.VIEW_SET_DOCUMENT.toString())
                    .put("id", 100)
                    .put("sessionId", "session100")
                    .put("params", new JSONObject()
                            .put("document", new JSONObject()
                                    .put("document", new JSONObject())
                                    .put("datasources", new JSONObject())));

            when(mCommandMethodUtil.parseMethod(any())).thenReturn(
                    CommandMethod.VIEW_SET_DOCUMENT);
            Request<? extends Response> request = mCommandRequestFactory.createCommandRequest(obj,
                    mConnection);
            assertTrue(request instanceof ViewSetDocumentCommandRequest);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createCommandRequest_forViewSetDocument_renderDocumentDirective_returnsCorrectRequestObject() {
        try {
            JSONObject obj = new JSONObject()
                    .put("method", CommandMethod.VIEW_SET_DOCUMENT.toString())
                    .put("id", 100)
                    .put("sessionId", "session100")
                    .put("params", new JSONObject()
                            .put("document", new JSONObject()
                                    .put("name", "RenderDocument")
                                    .put("payload", new JSONObject()
                                            .put("document", new JSONObject())
                                            .put("datasources", new JSONObject()))));
            when(mCommandMethodUtil.parseMethod(any())).thenReturn(
                    CommandMethod.VIEW_SET_DOCUMENT);
            Request<? extends Response> request = mCommandRequestFactory.createCommandRequest(obj,
                    mConnection);
            assertTrue(request instanceof ViewSetDocumentCommandRequest);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createCommandRequest_forPerformanceEnable_returnsCorrectRequestObject() {
        try {
            JSONObject obj = new JSONObject()
                    .put("method", CommandMethod.PERFORMANCE_ENABLE.toString())
                    .put("id", 100)
                    .put("sessionId", "session100");
            when(mCommandMethodUtil.parseMethod(any())).thenReturn(
                    CommandMethod.PERFORMANCE_ENABLE);
            Request<? extends Response> request = mCommandRequestFactory.createCommandRequest(obj,
                    mConnection);
            assertTrue(request instanceof PerformanceEnableCommandRequest);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createCommandRequest_forPerformanceDisable_returnsCorrectRequestObject() {
        try {
            JSONObject obj = new JSONObject()
                    .put("method", CommandMethod.PERFORMANCE_DISABLE.toString())
                    .put("id", 100)
                    .put("sessionId", "session100");
            when(mCommandMethodUtil.parseMethod(any())).thenReturn(
                    CommandMethod.PERFORMANCE_DISABLE);
            Request<? extends Response> request = mCommandRequestFactory.createCommandRequest(obj,
                    mConnection);
            assertTrue(request instanceof PerformanceDisableCommandRequest);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createCommandRequest_forViewCaptureImage_returnsCorrectRequestObject() {
        try {
            JSONObject obj = new JSONObject()
                    .put("method", CommandMethod.VIEW_CAPTURE_IMAGE.toString())
                    .put("id", 100)
                    .put("sessionId", "session100");
            when(mCommandMethodUtil.parseMethod(any())).thenReturn(
                    CommandMethod.VIEW_CAPTURE_IMAGE);
            Request<? extends Response> request = mCommandRequestFactory.createCommandRequest(obj,
                    mConnection);
            assertTrue(request instanceof ViewCaptureImageCommandRequest);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createCommandRequest_forViewExecuteCommands_returnsCorrectRequestObject() {
        try {
            JSONObject obj = new JSONObject()
                    .put("method", CommandMethod.VIEW_EXECUTE_COMMANDS.toString())
                    .put("id", 100)
                    .put("sessionId", "session100")
                    .put("params", new JSONObject()
                            .put("commands", new JSONArray()));
            when(mCommandMethodUtil.parseMethod(any())).thenReturn(
                    CommandMethod.VIEW_EXECUTE_COMMANDS);
            Request<? extends Response> request = mCommandRequestFactory.createCommandRequest(obj,
                    mConnection);
            assertTrue(request instanceof ViewExecuteCommandsCommandRequest);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createCommandRequest_forLogEnable_returnsCorrectRequestObject() {
        try {
            JSONObject obj = new JSONObject()
                    .put("method", CommandMethod.LOG_ENABLE.toString())
                    .put("id", 100)
                    .put("sessionId", "session100");
            when(mCommandMethodUtil.parseMethod(any())).thenReturn(
                    CommandMethod.LOG_ENABLE);
            Request<? extends Response> request = mCommandRequestFactory.createCommandRequest(obj,
                    mConnection);
            assertTrue(request instanceof LogEnableCommandRequest);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createCommandRequest_forLogDisable_returnsCorrectRequestObject() {
        try {
            JSONObject obj = new JSONObject()
                    .put("method", CommandMethod.LOG_DISABLE.toString())
                    .put("id", 100)
                    .put("sessionId", "session100");
            when(mCommandMethodUtil.parseMethod(any())).thenReturn(
                    CommandMethod.LOG_DISABLE);
            Request<? extends Response> request = mCommandRequestFactory.createCommandRequest(obj,
                    mConnection);
            assertTrue(request instanceof LogDisableCommandRequest);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createCommandRequest_forLogClear_returnsCorrectRequestObject() {
        try {
            JSONObject obj = new JSONObject()
                    .put("method", CommandMethod.LOG_CLEAR.toString())
                    .put("id", 100)
                    .put("sessionId", "session100");
            Session mockSession = mock(Session.class);
            when(mockSession.isLogEnabled()).thenReturn(true);
            when(mConnection.getSession("session100")).thenReturn(mockSession);
            when(mCommandMethodUtil.parseMethod(any())).thenReturn(
                    CommandMethod.LOG_CLEAR);
            Request<? extends Response> request = mCommandRequestFactory.createCommandRequest(obj,
                    mConnection);
            assertTrue(request instanceof LogClearCommandRequest);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createCommandRequest_forBadJObject_throws() {
        try {
            JSONObject obj = new JSONObject();
            mCommandRequestFactory.createCommandRequest(obj, mConnection);
            fail("CommandMethod should throw exception");
        } catch (Exception e) {
            assertTrue(e instanceof JSONException);
        }
    }

    @Test
    public void createCommandRequest_forUnimplementedMethod_throws() {
        try {
            JSONObject obj = new JSONObject()
                    .put("method", "CommandMethod.Empty")
                    .put("id", 100);
            when(mCommandMethodUtil.parseMethod(any())).thenReturn(CommandMethod.EMPTY);
            mCommandRequestFactory.createCommandRequest(obj, mConnection);
            fail("CommandMethod should throw exception");
        } catch (Exception e) {
            assertTrue(e instanceof DTException);
        }
    }

    @Test
    public void createCommandRequest_forMemoryGetMemory_returnsCorrectRequestObject() {
        try {
            JSONObject obj = new JSONObject()
                    .put("method", CommandMethod.MEMORY_GET_MEMORY.toString())
                    .put("id", 100)
                    .put("sessionId", "session100");
            when(mCommandMethodUtil.parseMethod(any())).thenReturn(
                    CommandMethod.MEMORY_GET_MEMORY);
            Request<? extends Response> request = mCommandRequestFactory.createCommandRequest(obj,
                    mConnection);
            assertTrue(request instanceof MemoryGetMemoryCommandRequest);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createCommandRequest_forDocumentGetMainPackage_returnsCorrectRequestObject() {
        try {
            JSONObject obj = new JSONObject()
                    .put("method", CommandMethod.DOCUMENT_GET_MAIN_PACKAGE.toString())
                    .put("id", 100)
                    .put("sessionId", "session100");
            when(mCommandMethodUtil.parseMethod(any())).thenReturn(
                    CommandMethod.DOCUMENT_GET_MAIN_PACKAGE);
            Request<? extends Response> request = mCommandRequestFactory.createCommandRequest(obj,
                    mConnection);
            assertTrue(request instanceof DocumentCommandRequest);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createCommandRequest_forDocumentGetPackageList_returnsCorrectRequestObject() {
        try {
            JSONObject obj = new JSONObject()
                    .put("method", CommandMethod.DOCUMENT_GET_PACKAGE_LIST.toString())
                    .put("id", 100)
                    .put("sessionId", "session100");
            when(mCommandMethodUtil.parseMethod(any())).thenReturn(
                    CommandMethod.DOCUMENT_GET_PACKAGE_LIST);
            Request<? extends Response> request = mCommandRequestFactory.createCommandRequest(obj,
                    mConnection);
            assertTrue(request instanceof DocumentCommandRequest);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createCommandRequest_forDocumentGetPackage_returnsCorrectRequestObject() {
        try {
            JSONObject obj = new JSONObject()
                    .put("method", CommandMethod.DOCUMENT_GET_PACKAGE.toString())
                    .put("id", 100)
                    .put("sessionId", "session100");
            when(mCommandMethodUtil.parseMethod(any())).thenReturn(
                    CommandMethod.DOCUMENT_GET_PACKAGE);
            Request<? extends Response> request = mCommandRequestFactory.createCommandRequest(obj,
                    mConnection);
            assertTrue(request instanceof DocumentCommandRequest);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createCommandRequest_forDocumentGetVisualContext_returnsCorrectRequestObject() {
        try {
            JSONObject obj = new JSONObject()
                    .put("method", CommandMethod.DOCUMENT_GET_VISUAL_CONTEXT.toString())
                    .put("id", 100)
                    .put("sessionId", "session100");
            when(mCommandMethodUtil.parseMethod(any())).thenReturn(
                    CommandMethod.DOCUMENT_GET_VISUAL_CONTEXT);
            Request<? extends Response> request = mCommandRequestFactory.createCommandRequest(obj,
                    mConnection);
            assertTrue(request instanceof DocumentCommandRequest);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createCommandRequest_forDocumentGetDom_returnsCorrectRequestObject() {
        try {
            JSONObject obj = new JSONObject()
                    .put("method", CommandMethod.DOCUMENT_GET_DOM.toString())
                    .put("id", 100)
                    .put("sessionId", "session100");
            when(mCommandMethodUtil.parseMethod(any())).thenReturn(
                    CommandMethod.DOCUMENT_GET_DOM);
            Request<? extends Response> request = mCommandRequestFactory.createCommandRequest(obj,
                    mConnection);
            assertTrue(request instanceof DocumentCommandRequest);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createCommandRequest_forDocumentGetSceneGraph_returnsCorrectRequestObject() {
        try {
            JSONObject obj = new JSONObject()
                    .put("method", CommandMethod.DOCUMENT_GET_SCENE_GRAPH.toString())
                    .put("id", 100)
                    .put("sessionId", "session100");
            when(mCommandMethodUtil.parseMethod(any())).thenReturn(
                    CommandMethod.DOCUMENT_GET_SCENE_GRAPH);
            Request<? extends Response> request = mCommandRequestFactory.createCommandRequest(obj,
                    mConnection);
            assertTrue(request instanceof DocumentCommandRequest);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createCommandRequest_forDocumentGetRootContext_returnsCorrectRequestObject() {
        try {
            JSONObject obj = new JSONObject()
                    .put("method", CommandMethod.DOCUMENT_GET_ROOT_CONTEXT.toString())
                    .put("id", 100)
                    .put("sessionId", "session100");
            when(mCommandMethodUtil.parseMethod(any())).thenReturn(
                    CommandMethod.DOCUMENT_GET_ROOT_CONTEXT);
            Request<? extends Response> request = mCommandRequestFactory.createCommandRequest(obj,
                    mConnection);
            assertTrue(request instanceof DocumentCommandRequest);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createCommandRequest_forDocumentGetContext_returnsCorrectRequestObject() {
        try {
            JSONObject obj = new JSONObject()
                    .put("method", CommandMethod.DOCUMENT_GET_CONTEXT.toString())
                    .put("id", 100)
                    .put("sessionId", "session100");
            when(mCommandMethodUtil.parseMethod(any())).thenReturn(
                    CommandMethod.DOCUMENT_GET_CONTEXT);
            Request<? extends Response> request = mCommandRequestFactory.createCommandRequest(obj,
                    mConnection);
            assertTrue(request instanceof DocumentCommandRequest);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createCommandRequest_forDocumentHighlightComponent_returnsCorrectRequestObject() {
        try {
            JSONObject obj = new JSONObject()
                    .put("method", CommandMethod.DOCUMENT_HIGHLIGHT_COMPONENT.toString())
                    .put("id", 100)
                    .put("sessionId", "session100");
            when(mCommandMethodUtil.parseMethod(any())).thenReturn(
                    CommandMethod.DOCUMENT_HIGHLIGHT_COMPONENT);
            Request<? extends Response> request = mCommandRequestFactory.createCommandRequest(obj,
                    mConnection);
            assertTrue(request instanceof DocumentCommandRequest);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createCommandRequest_forDocumentHideHighlight_returnsCorrectRequestObject() {
        try {
            JSONObject obj = new JSONObject()
                    .put("method", CommandMethod.DOCUMENT_HIDE_HIGHLIGHT.toString())
                    .put("id", 100)
                    .put("sessionId", "session100");
            when(mCommandMethodUtil.parseMethod(any())).thenReturn(
                    CommandMethod.DOCUMENT_HIDE_HIGHLIGHT);
            Request<? extends Response> request = mCommandRequestFactory.createCommandRequest(obj,
                    mConnection);
            assertTrue(request instanceof DocumentCommandRequest);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createCommandRequest_forNetworkEnable_returnsCorrectRequestObject() {
        try {
            JSONObject obj = new JSONObject()
                    .put("method", CommandMethod.NETWORK_ENABLE.toString())
                    .put("id", 100)
                    .put("sessionId", "session100");
            when(mCommandMethodUtil.parseMethod(any())).thenReturn(
                    CommandMethod.NETWORK_ENABLE);
            Request<? extends Response> request = mCommandRequestFactory.createCommandRequest(obj,
                    mConnection);
            assertTrue(request instanceof NetworkEnableCommandRequest);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createCommandRequest_forNetworkDisable_returnsCorrectRequestObject() {
        try {
            JSONObject obj = new JSONObject()
                    .put("method", CommandMethod.NETWORK_DISABLE.toString())
                    .put("id", 100)
                    .put("sessionId", "session100");
            when(mCommandMethodUtil.parseMethod(any())).thenReturn(
                    CommandMethod.NETWORK_DISABLE);
            Request<? extends Response> request = mCommandRequestFactory.createCommandRequest(obj,
                    mConnection);
            assertTrue(request instanceof NetworkDisableCommandRequest);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createCommandRequest_forFrameMetricsEnable_returnsCorrectRequestObject() {
        try {
            JSONObject obj = new JSONObject()
                    .put("method", CommandMethod.FRAMEMETRICS_ENABLE.toString())
                    .put("id", 100)
                    .put("sessionId", "session100");
            when(mCommandMethodUtil.parseMethod(any())).thenReturn(
                    CommandMethod.FRAMEMETRICS_ENABLE);
            Request<? extends Response> request = mCommandRequestFactory.createCommandRequest(obj,
                    mConnection);
            assertTrue(request instanceof FrameMetricsEnableCommandRequest);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void createCommandRequest_forFrameMetricsDisable_returnsCorrectRequestObject() {
        try {
            JSONObject obj = new JSONObject()
                    .put("method", CommandMethod.FRAMEMETRICS_DISABLE.toString())
                    .put("id", 100)
                    .put("sessionId", "session100");
            when(mCommandMethodUtil.parseMethod(any())).thenReturn(
                    CommandMethod.FRAMEMETRICS_DISABLE);
            Request<? extends Response> request = mCommandRequestFactory.createCommandRequest(obj,
                    mConnection);
            assertTrue(request instanceof FrameMetricsDisableCommandRequest);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
