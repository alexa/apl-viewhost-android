/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.extension;


import com.amazon.apl.android.APLViewhostTest;
import com.amazon.apl.android.Content;
import com.amazon.apl.android.ExtensionClient;
import com.amazon.apl.android.RootConfig;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExtensionClientTest extends APLViewhostTest {

    // Test content
    private final String mTestDoc = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.0\"," +
            "  \"extensions\": [" +
            "    {\n" +
            "      \"name\": \"A\"," +
            "      \"uri\": \"alexaext:myext:10\"" +
            "    }" +
            "  ]," +
            "  \"mainTemplate\": {" +
            "    \"item\": {" +
            "      \"type\": \"Text\"" +
            "    }" +
            "  }" +
            "}";

    private String mUri = "alexaext:myext:10";

    @Test
    public void testCreate() {
        ExtensionClient client = ExtensionClient.create(RootConfig.create(), mUri);
        assertTrue(client.isBound());
    }

    @Test
    public void testRegistrationRequest_fromContent() throws Content.ContentException, JSONException {
        ExtensionClient client = ExtensionClient.create(RootConfig.create(), mUri);
        Content content = Content.create(mTestDoc);
        String registrationRequest = client.createRegistrationRequest(content);
        JSONObject jsonObject = new JSONObject(registrationRequest);
        assertEquals(mUri, jsonObject.getString("uri"));
        assertEquals(JSONObject.NULL, jsonObject.opt("settings"));
        assertEquals(JSONObject.NULL, jsonObject.opt("flags"));
        assertEquals("1.0", jsonObject.getString("version"));
        assertEquals("Register", jsonObject.getString("method"));
    }

    @Test
    public void testRegistrationRequest_fromSettings() throws JSONException {
        Map<String, Object> settings = new HashMap<>();
        settings.put("foo", "bar");
        String registrationRequest = ExtensionClient.createRegistrationRequest(mUri, settings);
        JSONObject jsonObject = new JSONObject(registrationRequest);
        assertEquals(mUri, jsonObject.getString("uri"));
        JSONObject settingsJson = jsonObject.optJSONObject("settings");
        assertEquals("bar", settingsJson.getString("foo"));
        assertEquals(JSONObject.NULL, jsonObject.opt("flags"));
        assertEquals("1.0", jsonObject.getString("version"));
        assertEquals("Register", jsonObject.getString("method"));
    }

    @Test
    public void testRegistrationRequest_fromSettingsWithFlags() throws JSONException {
        Map<String, Object> settings = new HashMap<>();
        settings.put("foo", "bar");
        Object flags = new Integer(1);
        String registrationRequest = ExtensionClient.createRegistrationRequest(mUri, settings, flags);
        JSONObject jsonObject = new JSONObject(registrationRequest);
        assertEquals(mUri, jsonObject.getString("uri"));
        JSONObject settingsJson = jsonObject.optJSONObject("settings");
        assertEquals("bar", settingsJson.getString("foo"));
        assertEquals(1.0, jsonObject.opt("flags"));
        assertEquals("1.0", jsonObject.getString("version"));
        assertEquals("Register", jsonObject.getString("method"));
    }

    @Test
    public void testRegistrationRequest_nullSettings() throws JSONException {
        String registrationRequest = ExtensionClient.createRegistrationRequest(mUri, null);
        JSONObject jsonObject = new JSONObject(registrationRequest);
        assertEquals(mUri, jsonObject.getString("uri"));
        assertEquals(JSONObject.NULL, jsonObject.opt("settings"));
        assertEquals(JSONObject.NULL, jsonObject.opt("flags"));
        assertEquals("1.0", jsonObject.getString("version"));
        assertEquals("Register", jsonObject.getString("method"));
    }

    @Test
    public void testRegistrationRequest_emptySettings() throws JSONException {
        String registrationRequest = ExtensionClient.createRegistrationRequest(mUri, new HashMap<>());
        JSONObject jsonObject = new JSONObject(registrationRequest);
        assertEquals(mUri, jsonObject.getString("uri"));
        assertEquals("{}", jsonObject.getString("settings"));
        assertEquals(JSONObject.NULL, jsonObject.opt("flags"));
        assertEquals("1.0", jsonObject.getString("version"));
        assertEquals("Register", jsonObject.getString("method"));
    }
}
