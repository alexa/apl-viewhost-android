/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.extension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import com.amazon.apl.android.APLViewhostTest;
import com.amazon.apl.android.ExtensionCommandDefinition;
import com.amazon.apl.android.ExtensionEventHandler;
import com.amazon.apl.android.ExtensionFilterDefinition;
import com.amazon.apl.android.LegacyLocalExtension;
import com.amazon.apl.android.LegacyLocalExtensionProxy;
import com.amazon.apl.android.dependencies.IExtensionEventCallback;
import com.amazon.apl.android.dependencies.IExtensionImageFilterCallback;
import com.amazon.apl.android.providers.IExtension;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class LegacyLocalExtensionProxyTest extends APLViewhostTest {
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
        public void registrationResult(String uri, String result) {
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
    @SmallTest
    public void testRegistrationMessage() {
        ProxyWrapper proxy = new ProxyWrapper(oldBuildIn);
        assertTrue(proxy.requestRegistration(URI, ""));

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
    @SmallTest
    public void testApplySettings() {
        ProxyWrapper proxy = new ProxyWrapper(mLegacyLocalExtension);
        String request = String.format("{\"settings\": {\"%s\": \"settingsValue\"}}", TestLegacyLocalExtension.SETTINGS_KEY);
        assertTrue(proxy.requestRegistration(LEGACY_EXTENSION_URI, request));
        assertEquals("settingsValue", mLegacyLocalExtension.getSetting());
    }
}
