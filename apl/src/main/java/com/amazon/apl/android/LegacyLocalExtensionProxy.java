/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import com.amazon.alexaext.ExtensionProxy;
import com.amazon.apl.android.dependencies.IExtensionEventCallback;
import com.amazon.apl.android.dependencies.IExtensionImageFilterCallback;
import com.amazon.apl.android.providers.IExtension;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Proxy for "legacy" local (built-in) extensions.
 */
public class LegacyLocalExtensionProxy extends ExtensionProxy {
    private IExtension mExtension;
    private ILegacyLocalExtension mLegacyLocalExtension;

    public LegacyLocalExtensionProxy(IExtension extension) {
        super(extension.getUri());
        mExtension = extension;
        mLegacyLocalExtension = mExtension instanceof ILegacyLocalExtension ? (ILegacyLocalExtension)mExtension : null;
    }

    @Override
    protected boolean initializeInternal(String uri) {
        if (mLegacyLocalExtension != null) {
            return mLegacyLocalExtension.initialize(
                    this::sendExtensionEvent,
                    this::invokeLiveDataUpdate);
        }

        return true;
    }

    @Override
    protected boolean invokeCommandInternal(String uri, String command) {
        IExtensionEventCallback callback = mExtension.getCallback();
        if (callback == null) return false;

        try {
            JSONObject reader = new JSONObject(command);
            double id = reader.optDouble("id", 0);
            String name = reader.optString("name");
            JSONObject payload = reader.optJSONObject("payload");
            Map<String, Object> props = new HashMap<>();
            if (payload.length() > 0) {
                for (Iterator<String> it = payload.keys(); it.hasNext(); ) {
                    String key = it.next();
                    props.put(key, payload.opt(key));
                }
            }

            callback.onExtensionEvent(name, uri, new HashMap<>(), props, succeeded -> {
                JSONObject result = new JSONObject();
                try {
                    result.put("version", "1.0");
                    result.put("id", id);
                    if (succeeded) {
                        result.put("method", "CommandSuccess");
                    } else {
                        result.put("method", "CommandFailure");
                        // TODO: What code?
                        result.put("code", 7);
                        result.put("message", "Failed to execute command.");
                    }
                } catch (JSONException ex) {}

                commandResult(uri, result.toString());
            });

            return true;
        } catch (JSONException ex) {}

        return false;
    }

    @Override
    protected boolean sendMessageInternal(String uri, String command) {
        // TODO: Relevant to CustomComponents.
        return false;
    }

    @Override
    protected boolean requestRegistrationInternal(String uri, String request) {
        try {
            JSONObject reader = new JSONObject(request);
            JSONObject settings = reader.optJSONObject("settings");
            Map<String, Object> settingsMap = new HashMap<>();
            if (settings != null) {
                for (Iterator<String> it = settings.keys(); it.hasNext(); ) {
                    String key = it.next();
                    settingsMap.put(key, settings.opt(key));
                }
            }
            if (mLegacyLocalExtension != null) {
                mLegacyLocalExtension.applySettings(settingsMap);
            }
        } catch (JSONException ex) {}

        JSONObject registrationSuccess = new JSONObject();
        try {
            registrationSuccess.put("method", "RegisterSuccess");
            registrationSuccess.put("version", "1.0");
            registrationSuccess.put("token", "<AUTO_TOKEN>");
            registrationSuccess.put("uri", getUri());

            // Env
            Object envObject = mExtension.getEnvironment();
            if (envObject instanceof Map) {
                JSONObject env = new JSONObject((Map)envObject);
                registrationSuccess.put("environment", env);
            }

            // Schema
            JSONObject schema = new JSONObject();
            schema.put("type", "Schema");
            schema.put("version", "1.0");
            schema.put("uri", getUri());

            JSONArray types = new JSONArray();

            // Commands
            List<ExtensionCommandDefinition> commandDefinitions = mExtension.getCommandDefinitions();
            if (!commandDefinitions.isEmpty()) {
                JSONArray commands = new JSONArray();
                for (ExtensionCommandDefinition def : commandDefinitions) {
                    JSONObject command = new JSONObject();
                    command.put("name", def.getName());
                    command.put("requireResponse", def.getRequireResolution());
                    command.put("allowFastMode", def.getAllowFastMode());

                    if (def.getPropertyCount() > 0) {
                        String payloadName = def.getName() + "Payload";
                        JSONObject payloadDef = new JSONObject();
                        JSONObject propDefs = new JSONObject();
                        for (String prop : def.getProperties()) {
                            JSONObject propDef = new JSONObject();
                            propDef.put("type", "any");
                            propDef.put("required", def.isPropertyRequired(prop));
                            propDef.put("default", def.getPropertyValue(prop));
                            propDefs.put(prop, propDef);
                        }

                        payloadDef.put("name", payloadName);
                        payloadDef.put("properties", propDefs);

                        types.put(payloadDef);
                        command.put("payload", payloadName);
                    }

                    commands.put(command);
                }
                schema.put("commands", commands);
            }

            // Events
            List<ExtensionEventHandler> handlerDefs = mExtension.getEventHandlers();
            if (!handlerDefs.isEmpty()) {
                JSONArray events = new JSONArray();
                for (ExtensionEventHandler handlerDef : handlerDefs) {
                    JSONObject event = new JSONObject();
                    event.put("name", handlerDef.getName());

                    events.put(event);
                }
                schema.put("events", events);
            }

            // LiveData
            // TODO: It's semi-sane.
            if (mLegacyLocalExtension != null) {
                List<LiveDataAdapter> liveData = mLegacyLocalExtension.getLiveData();
                if (!liveData.isEmpty()) {
                    JSONArray live = new JSONArray();
                    for (LiveDataAdapter adapter : liveData) {
                        JSONObject type = adapter.getTypeDefinition();
                        if (type.length() > 0) {
                            types.put(type);
                        }
                        live.put(adapter.getLiveDataDefinition());
                    }
                    schema.put("liveData", live);
                }
            }

            if (types.length() > 0) {
                schema.put("types", types);
            }
            registrationSuccess.put("schema", schema);
            registrationResult(uri, registrationSuccess.toString());
            return true;
        } catch (org.json.JSONException e) {}
        return false;
    }

    @Override
    protected void onRegisteredInternal(String uri, String token) {
        if (mLegacyLocalExtension != null) {
            mLegacyLocalExtension.onRegistered(uri, token);
        }
    }

    @Override
    protected void onUnregisteredInternal(String uri, String token) {
        if (mLegacyLocalExtension != null) {
            mLegacyLocalExtension.onUnregistered(uri, token);
        }
    }

    /**
     * Invoke extension event handler.
     * @param name Event name.
     * @param parameters Parameters to provide.
     * @return true if event handled, false otherwise.
     */
    public boolean sendExtensionEvent(String name, Map<String, Object> parameters) {
        try {
            JSONObject event = new JSONObject();
            event.put("version", "1.0");
            event.put("method", "Event");
            event.put("target", getUri());
            event.put("name", name);

            if (!parameters.isEmpty()) {
                JSONObject payload = new JSONObject();
                for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                    payload.put(entry.getKey(), entry.getValue());
                }
                event.put("payload", payload);
            }

            return invokeExtensionEventHandler(getUri(), event.toString());
        } catch (JSONException ex) {}
        return false;
    }

    /**
     * Below are temporary for image filters support.
     */
    public List<ExtensionFilterDefinition> getFilterDefinitions() {
        return mExtension.getFilterDefinitions();
    }

    public IExtensionImageFilterCallback getFilterCallback() {
        return mExtension.getFilterCallback();
    }
}
