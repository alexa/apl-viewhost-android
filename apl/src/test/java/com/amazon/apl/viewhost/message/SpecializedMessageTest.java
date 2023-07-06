/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.viewhost.message;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.viewhost.DocumentHandle;
import com.amazon.apl.viewhost.internal.message.action.ActionMessageImpl;
import com.amazon.apl.viewhost.internal.message.action.FetchDataRequestImpl;
import com.amazon.apl.viewhost.internal.message.action.ReportRuntimeErrorRequestImpl;
import com.amazon.apl.viewhost.internal.message.action.SendUserEventRequestImpl;
import com.amazon.apl.viewhost.internal.message.notification.NotificationMessageImpl;
import com.amazon.apl.viewhost.message.action.ActionMessage;
import com.amazon.apl.viewhost.message.action.FetchDataRequest;
import com.amazon.apl.viewhost.message.action.OpenURLRequest;
import com.amazon.apl.viewhost.message.action.ReportRuntimeErrorRequest;
import com.amazon.apl.viewhost.message.action.SendUserEventRequest;
import com.amazon.apl.viewhost.message.notification.DataSourceContextChanged;
import com.amazon.apl.viewhost.message.notification.DocumentStateChanged;
import com.amazon.apl.viewhost.message.notification.NotificationMessage;
import com.amazon.apl.viewhost.message.notification.VisualContextChanged;
import com.amazon.apl.viewhost.primitives.JsonDecodable;
import com.amazon.apl.viewhost.primitives.JsonStringDecodable;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;


@RunWith(AndroidJUnit4.class)
public class SpecializedMessageTest extends ViewhostRobolectricTest {
    @Mock
    private DocumentHandle mDocumentHandle;
    @Mock
    private ActionMessageImpl.ResponseListener mResponseListener;
    private ExampleSpecializedMessageHandler mMessageHandler;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mMessageHandler = new ExampleSpecializedMessageHandler();
    }

    @Test
    public void testSpecializedHandlerDefaultBehavior() {
        SpecializedMessageHandler handler = new SpecializedMessageHandler();

        assertFalse(handler.handleDocumentStateChanged(null));
        assertFalse(handler.handleVisualContextChanged(null));
        assertFalse(handler.handleDataSourceContextChanged(null));
        assertFalse(handler.handleFetchDataRequest(null));
        assertFalse(handler.handleOpenURLRequest(null));
        assertFalse(handler.handleSendUserEventRequest(null));
        assertFalse(handler.handleReportRuntimeErrorRequest(null));

        NotificationMessage notificationMessage = new NotificationMessageImpl(1, mDocumentHandle,
                "Unknown", null);
        assertFalse(handler.handleNotification(notificationMessage));

        ActionMessage actionMessage = new ActionMessageImpl(2, mDocumentHandle, "Unknown", null,
                mResponseListener);
        assertFalse(handler.handleAction(actionMessage));
    }

    @Test
    public void testActionMessageCallbacksFailFastWithoutListener() {
        ActionMessage actionMessage = new ActionMessageImpl(2, mDocumentHandle, "Unknown", null, null);
        assertFalse(actionMessage.succeed());
        assertFalse(actionMessage.succeed(new JsonStringDecodable("{}")));
        assertFalse(actionMessage.fail("Reason"));
    }

    @Test
    public void testDataSourceContextChanged() throws JSONException {
        NotificationMessage notificationMessage = new NotificationMessageImpl(123,
                mDocumentHandle, "DataSourceContextChanged", null);

        assertTrue(mMessageHandler.handleNotification(notificationMessage));
        assertTrue(mMessageHandler._message instanceof DataSourceContextChanged);
        DataSourceContextChanged message = (DataSourceContextChanged) mMessageHandler._message;

        assertEquals(123, message.getId());
        assertEquals(mDocumentHandle, message.getDocument());
    }

    @Test
    public void testDocumentStateChangedWithoutReason() throws JSONException {
        JSONObject payload = new JSONObject();
        payload.put("state", "INFLATED");

        NotificationMessage notificationMessage = new NotificationMessageImpl(123,
                mDocumentHandle, "DocumentStateChanged", new JsonDecodable(payload));

        assertTrue(mMessageHandler.handleNotification(notificationMessage));
        assertTrue(mMessageHandler._message instanceof DocumentStateChanged);
        DocumentStateChanged message = (DocumentStateChanged) mMessageHandler._message;

        assertEquals(123, message.getId());
        assertEquals(mDocumentHandle, message.getDocument());
        assertEquals("INFLATED", message.getState());
        assertNull(message.getReason());
    }

    @Test
    public void testDocumentStateChangedWithReason() throws JSONException {
        JSONObject payload = new JSONObject();
        payload.put("state", "ERROR");
        payload.put("reason", "Invalid JSON");

        NotificationMessage notificationMessage = new NotificationMessageImpl(456,
                mDocumentHandle, "DocumentStateChanged", new JsonDecodable(payload));

        assertTrue(mMessageHandler.handleNotification(notificationMessage));
        assertTrue(mMessageHandler._message instanceof DocumentStateChanged);
        DocumentStateChanged message = (DocumentStateChanged) mMessageHandler._message;

        assertEquals(456, message.getId());
        assertEquals(mDocumentHandle, message.getDocument());
        assertEquals("ERROR", message.getState());
        assertEquals("Invalid JSON", message.getReason());
    }

    @Test
    public void testVisualContextChanged() throws JSONException {
        NotificationMessage notificationMessage = new NotificationMessageImpl(123,
                mDocumentHandle, "VisualContextChanged", null);

        assertTrue(mMessageHandler.handleNotification(notificationMessage));
        assertTrue(mMessageHandler._message instanceof VisualContextChanged);
        VisualContextChanged message = (VisualContextChanged) mMessageHandler._message;

        assertEquals(123, message.getId());
        assertEquals(mDocumentHandle, message.getDocument());
    }

    @Test
    public void testSendUserEventRequest() {
        Object[] arguments = new String[]{"one", "two", "three"};

        Map<String, Object> source = new HashMap<>();
        source.put("src1", "valueB");
        source.put("src2", 88);

        Map<String, Object> components = new HashMap<>();
        components.put("comp1", "valueA");
        components.put("comp2", new String[]{"apple", "pear"});

        Map<String, Object> flags = new HashMap<>();
        flags.put("flag1", "valueC");
        flags.put("flag2", 99);

        Map<String, Object> nestedFlag = new HashMap<>();
        nestedFlag.put("nestedkey", "nestedvalue");
        flags.put("flag3", nestedFlag);

        SendUserEventRequestImpl.Payload payload = new SendUserEventRequestImpl.Payload();
        payload.arguments = arguments;
        payload.components = components;
        payload.source = source;
        payload.flags = flags;

        ActionMessage actionMessage = new ActionMessageImpl(1, mDocumentHandle,
                "SendUserEventRequest", payload, mResponseListener);

        assertTrue(mMessageHandler.handleAction(actionMessage));
        assertTrue(mMessageHandler._message instanceof SendUserEventRequest);
        SendUserEventRequest message = (SendUserEventRequest) mMessageHandler._message;

        assertEquals(1, message.getId());
        assertEquals(mDocumentHandle, message.getDocument());
        assertArrayEquals(arguments, message.getArguments());
        assertEquals(source, message.getSource());
        assertEquals(components, message.getComponents());
        assertEquals(flags, message.getFlags());

        assertTrue(message.succeed());
        verify(mResponseListener, times(1)).onSuccess(null);

        assertTrue(message.fail("My reason"));
        verify(mResponseListener, times(1)).onFailure("My reason");
    }

    @Test
    public void testReportRuntimeErrorRequest() {
        Object[] errors = new String[]{"failA", "failB", "failC"};

        ReportRuntimeErrorRequestImpl.Payload payload = new ReportRuntimeErrorRequestImpl.Payload();
        payload.errors = errors;

        ActionMessage actionMessage = new ActionMessageImpl(88, mDocumentHandle,
                "ReportRuntimeErrorRequest", payload, mResponseListener);

        assertTrue(mMessageHandler.handleAction(actionMessage));
        assertTrue(mMessageHandler._message instanceof ReportRuntimeErrorRequest);
        ReportRuntimeErrorRequest message = (ReportRuntimeErrorRequest) mMessageHandler._message;

        assertEquals(88, message.getId());
        assertEquals(mDocumentHandle, message.getDocument());
        assertArrayEquals(errors, message.getErrors());

        assertTrue(message.succeed());
        verify(mResponseListener, times(1)).onSuccess(null);

        assertTrue(message.fail("My reason"));
        verify(mResponseListener, times(1)).onFailure("My reason");
    }

    @Test
    public void testOpenURLRequest() throws JSONException {
        JSONObject payload = new JSONObject();
        payload.put("source", "http://source");

        ActionMessage actionMessage = new ActionMessageImpl(2, mDocumentHandle, "OpenURLRequest",
                new JsonDecodable(payload), mResponseListener);

        assertTrue(mMessageHandler.handleAction(actionMessage));
        assertTrue(mMessageHandler._message instanceof OpenURLRequest);
        OpenURLRequest message = (OpenURLRequest) mMessageHandler._message;

        assertEquals(2, message.getId());
        assertEquals(mDocumentHandle, message.getDocument());
        assertEquals("http://source", message.getSource());

        assertTrue(message.succeed());
        verify(mResponseListener, times(1)).onSuccess(null);

        assertTrue(message.fail("My reason"));
        verify(mResponseListener, times(1)).onFailure("My reason");
    }

    @Test
    public void testFetchDataRequest() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("param1", "valueA");
        parameters.put("param2", 99);

        FetchDataRequestImpl.Payload payload = new FetchDataRequestImpl.Payload();
        payload.type = "DYNAMIC_INDEX_LIST";
        payload.parameters = parameters;

        ActionMessage actionMessage = new ActionMessageImpl(1, mDocumentHandle, "FetchDataRequest"
                , payload, mResponseListener);

        assertTrue(mMessageHandler.handleAction(actionMessage));
        assertTrue(mMessageHandler._message instanceof FetchDataRequest);
        FetchDataRequest message = (FetchDataRequest) mMessageHandler._message;

        assertEquals(1, message.getId());
        assertEquals(mDocumentHandle, message.getDocument());
        assertEquals("DYNAMIC_INDEX_LIST", message.getDataType());
        assertEquals(parameters, message.getParameters());

        assertTrue(message.succeed());
        verify(mResponseListener, times(1)).onSuccess(null);

        assertTrue(message.fail("My reason"));
        verify(mResponseListener, times(1)).onFailure("My reason");
    }

    class ExampleSpecializedMessageHandler extends SpecializedMessageHandler {
        public BaseMessage _message;

        @Override
        public boolean handleDataSourceContextChanged(final DataSourceContextChanged message) {
            _message = message;
            return true;
        }

        @Override
        public boolean handleDocumentStateChanged(final DocumentStateChanged message) {
            _message = message;
            return true;
        }

        @Override
        public boolean handleVisualContextChanged(final VisualContextChanged message) {
            _message = message;
            return true;
        }

        @Override
        public boolean handleOpenURLRequest(final OpenURLRequest message) {
            _message = message;
            return true;
        }

        @Override
        public boolean handleReportRuntimeErrorRequest(final ReportRuntimeErrorRequest message) {
            _message = message;
            return true;
        }

        @Override
        public boolean handleSendUserEventRequest(final SendUserEventRequest message) {
            _message = message;
            return true;
        }

        @Override
        public boolean handleFetchDataRequest(final FetchDataRequest message) {
            _message = message;
            return true;
        }
    }
}
