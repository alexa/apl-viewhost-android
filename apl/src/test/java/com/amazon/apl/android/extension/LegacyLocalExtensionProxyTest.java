/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.extension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.alexaext.ActivityDescriptor;
import com.amazon.alexaext.SessionDescriptor;
import com.amazon.apl.android.ExtensionCommandDefinition;
import com.amazon.apl.android.ExtensionEventHandler;
import com.amazon.apl.android.ExtensionFilterDefinition;
import com.amazon.apl.android.LegacyLocalExtension;
import com.amazon.apl.android.LegacyLocalExtensionProxy;
import com.amazon.apl.android.dependencies.IExtensionEventCallback;
import com.amazon.apl.android.dependencies.IExtensionImageFilterCallback;
import com.amazon.apl.android.providers.IExtension;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LegacyLocalExtensionProxyTest extends ViewhostRobolectricTest {
    public static final String URI = "example:extension:10";
    public static final String LEGACY_EXTENSION_URI = "exampleLegacy:extension:10";

    private IExtension oldBuildIn;
    private TestLegacyLocalExtension mLegacyLocalExtension;

    public static class TestLegacyLocalExtension extends LegacyLocalExtension {

        static final String SETTINGS_KEY = "settingsKey";
        private String mSetting;

        public String getSetting() {
            return mSetting;
        }

        @Override
        public void onExtensionEvent(String name, String uri, Map<String, Object> source, Map<String, Object> custom, IExtensionEventCallbackResult resultCallback) {

        }

        @NonNull
        @Override
        public String getUri() {
            return LEGACY_EXTENSION_URI;
        }

        @Override
        public void applySettings(Map<String, Object> settings) {
            mSetting = (String) settings.get(SETTINGS_KEY);
        }
    }

    public static class TestExtension implements IExtension {
        @NonNull
        @Override
        public String getUri() {
            return URI;
        }

        @Nullable
        @Override
        public IExtensionEventCallback getCallback() {
            return null;
        }

        @Nullable
        @Override
        public Object getEnvironment() {
            Map<String, Object> environment = new HashMap<>();
            environment.put("some", "variable");
            List<String> envArray = new ArrayList<>();
            envArray.add("One");
            envArray.add("Two");
            environment.put("collection", envArray);
            return environment;
        }

        @NonNull
        @Override
        public List<ExtensionCommandDefinition> getCommandDefinitions() {
            List<ExtensionCommandDefinition> commandDefinitions = new ArrayList<>();
            commandDefinitions.add(
                    new ExtensionCommandDefinition(URI, "Hello")
                            .property("arg1", 42, false)
                            .property("arg2", "potato", true)
                            .requireResolution(true)
                            .allowFastMode(false));
            return commandDefinitions;
        }

        @NonNull
        @Override
        public List<ExtensionFilterDefinition> getFilterDefinitions() {
            return null;
        }

        @NonNull
        @Override
        public List<ExtensionEventHandler> getEventHandlers() {
            List<ExtensionEventHandler> handlerDefinitions = new ArrayList<>();
            handlerDefinitions.add(new ExtensionEventHandler(URI, "Goodnight"));
            return handlerDefinitions;
        }

        @Nullable
        @Override
        public IExtensionImageFilterCallback getFilterCallback() {
            return null;
        }
    }

    public static class ProxyWrapper extends LegacyLocalExtensionProxy {
        public ProxyWrapper(IExtension extension) {
            super(extension);
        }

        @Override
        public void registrationResult(ActivityDescriptor activity, String result) {
            registrationResult = result;
        }

        public String registrationResult;
    }

    @Before
    public void setUp() {
        oldBuildIn = new TestExtension();
        mLegacyLocalExtension = new TestLegacyLocalExtension();
    }

    @After
    public void teardown() {}

    @Test
    public void testRegistrationMessage() {
        ProxyWrapper proxy = new ProxyWrapper(oldBuildIn);
        assertTrue(proxy.requestRegistrationNative(new ActivityDescriptor(URI, new SessionDescriptor("TEST"), "TEST"), ""));

        assertEquals(
                "{" +
                        "\"method\":\"RegisterSuccess\"," +
                        "\"version\":\"1.0\"," +
                        "\"token\":\"<AUTO_TOKEN>\"," +
                        "\"uri\":\"example:extension:10\"," +
                        "\"environment\":{" +
                        "\"some\":\"variable\"," +
                        "\"collection\":[\"One\",\"Two\"]" +
                        "}," +
                        "\"schema\":{" +
                        "\"type\":\"Schema\"," +
                        "\"version\":\"1.0\"," +
                        "\"uri\":\"example:extension:10\"," +
                        "\"commands\":[{" +
                        "\"name\":\"Hello\"," +
                        "\"requireResponse\":true," +
                        "\"allowFastMode\":false," +
                        "\"payload\":\"HelloPayload\"" +
                        "}]," +
                        "\"events\":[{" +
                        "\"name\":\"Goodnight\"" +
                        "}]," +
                        "\"types\":[{" +
                        "\"name\":\"HelloPayload\"," +
                        "\"properties\":{" +
                        "\"arg2\":{\"type\":\"any\",\"required\":true,\"default\":\"potato\"}," +
                        "\"arg1\":{\"type\":\"any\",\"required\":false,\"default\":42}" +
                        "}" +
                        "}]" +
                        "}}", proxy.registrationResult);
    }

    @Test
    public void testOnExtensionEvent() throws JSONException {
        IExtension extension = mock(IExtension.class);
        IExtensionEventCallback callback = mock(IExtensionEventCallback.class);
        when(extension.getCallback()).thenReturn(callback);
        when(extension.getUri()).thenReturn(URI);

        JSONObject payload = new JSONObject();
        payload.put("test", "test");

        JSONObject command = new JSONObject();
        command.put("name", "testCommand");
        command.put("payload", payload);

        LegacyLocalExtensionProxy proxy = new LegacyLocalExtensionProxy(extension);

        proxy.invokeCommandNative(new ActivityDescriptor(URI, new SessionDescriptor("TEST"), "TEST"), command.toString());

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);

        verify(callback).onExtensionEvent(eq("testCommand"), eq(URI), any(Map.class), captor.capture(), any());

        assertEquals(1, captor.getValue().size());
        assertTrue(captor.getValue().containsKey("test"));
        assertEquals("test", captor.getValue().get("test"));
    }

    @Test
    public void testSendExtensionEvent_nullActivity_eventNotHandled() {
        IExtension extension = mock(IExtension.class);
        IExtensionEventCallback callback = mock(IExtensionEventCallback.class);
        when(extension.getCallback()).thenReturn(callback);
        when(extension.getUri()).thenReturn(URI);

        LegacyLocalExtensionProxy proxy = spy(new LegacyLocalExtensionProxy(extension));
        proxy.sendExtensionEvent("ABC", new HashMap<>());

        verify(proxy, times(0)).invokeExtensionEventHandler(any(), any());
    }

    @Test
    public void testSendExtensionEvent_setActivity_eventHandled() {
        IExtension extension = mock(IExtension.class);
        IExtensionEventCallback callback = mock(IExtensionEventCallback.class);
        when(extension.getCallback()).thenReturn(callback);
        when(extension.getUri()).thenReturn(URI);

        LegacyLocalExtensionProxy proxy = spy(new LegacyLocalExtensionProxy(extension));
        proxy.setActivity(new ActivityDescriptor(URI, new SessionDescriptor("TEST"), "TEST"));
        proxy.sendExtensionEvent("ABC", new HashMap<>());

        verify(proxy).invokeExtensionEventHandler(any(), any());
    }

    @Test
    public void testApplySettings() {
        ProxyWrapper proxy = new ProxyWrapper(mLegacyLocalExtension);
        String request = String.format("{\"settings\": {\"%s\": \"settingsValue\"}}", TestLegacyLocalExtension.SETTINGS_KEY);
        assertTrue(proxy.requestRegistrationNative(new ActivityDescriptor(LEGACY_EXTENSION_URI, new SessionDescriptor("TEST"), "TEST"), request));
        assertEquals("settingsValue", mLegacyLocalExtension.getSetting());
    }
}
