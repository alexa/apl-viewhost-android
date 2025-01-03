package com.amazon.alexaext;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.amazon.common.Consumer;

import java.util.Map;

/**
 * The ExtensionMessage class provides a set of classes and helper methods for creating
 * and manipulating JSON messages used in the communication between extensions and the execution
 * environment. These messages include registration requests, registration responses (success or failure),
 * commands, command results (success or failure), events, and live data updates.
 */
public class ExtensionMessage {
    private static final String TAG = "ExtensionMessage";
    private static String SCHEMA_VERSION = "1.0";

    /**
     * Represents a registration request message sent by an extension to the execution environment.
     */
    public static class RegistrationRequest {
        private final JSONObject message;

        public RegistrationRequest() {
            message = new JSONObject();
            putValue(message, "method", "Register");
            putValue(message, "version", SCHEMA_VERSION);
        }

        public RegistrationRequest settings(JSONObject settings) {
            putValue(message, "settings", settings);
            return this;
        }

        public RegistrationRequest flags(JSONObject flags) {
            putValue(message, "flags", flags);
            return this;
        }

        public JSONObject getMessage() {
            return message;
        }
    }

    /**
     * Represents a registration failure message sent by the execution environment to the extension.
     */
    public static class RegistrationFailure {
        private final JSONObject message;

        public RegistrationFailure(String uri) {
            message = new JSONObject();
            putValue(message, "method", "RegisterFailure");
            putValue(message, "version", SCHEMA_VERSION);
            putValue(message, "uri", uri);
        }

        public RegistrationFailure errorCode(int errorCode) {
            putValue(message, "code", errorCode);
            return this;
        }

        public RegistrationFailure errorMessage(String errorMessage) {
            putValue(message, "message", errorMessage);
            return this;
        }

        public JSONObject getMessage() {
            return message;
        }
    }

    /**
     * Represents a registration success message sent by the execution environment to the extension.
     */
    public static class RegistrationSuccess {
        private final JSONObject message;

        public RegistrationSuccess() {
            message = new JSONObject();
            putValue(message, "version", SCHEMA_VERSION);
            putValue(message, "method", "RegisterSuccess");
        }

        public RegistrationSuccess token(String token) {
            putValue(message, "token", token);
            return this;
        }

        public RegistrationSuccess environment(Map<String, String> environment) {
            putValue(message, "environment", new JSONObject(environment));
            return this;
        }

        public RegistrationSuccess schema(Consumer<ExtensionSchema.ExtensionSchemaBuilder> schemaBuilderConsumer) {
            ExtensionSchema.ExtensionSchemaBuilder schemaBuilder = ExtensionSchema.builder();
            schemaBuilderConsumer.accept(schemaBuilder);
            putValue(message, "schema", schemaBuilder.build().getSchema());
            return this;
        }


        public JSONObject getMessage() {
            return message;
        }
    }

    /**
     * Represents a command message sent by the execution environment to the extension.
     */
    public static class Command {
        private final JSONObject message;
        private final JSONObject payload;

        public Command() {
            message = new JSONObject();
            payload = new JSONObject();
            putValue(message, "method", "Command");
            putValue(message, "version", SCHEMA_VERSION);
            putValue(message, "payload", payload);
        }

        public Command id(int id) {
            putValue(message, "id", id);
            return this;
        }

        public Command name(String name) {
            putValue(message, "name", name);
            return this;
        }

        public Command property(String key, Object value) {
            putValue(payload, key, value);
            return this;
        }

        public JSONObject getMessage() {
            return message;
        }
    }

    /**
     * Represents a command success message sent by the extension to the execution environment.
     */
    public static class CommandSuccess {
        private final JSONObject message;

        public CommandSuccess() {
            message = new JSONObject();
            putValue(message, "method", "CommandSuccess");
            putValue(message, "version", SCHEMA_VERSION);
        }

        public CommandSuccess id(int id) {
            putValue(message, "id", id);
            return this;
        }

        public CommandSuccess result(JSONObject result) {
            putValue(message, "result", result);
            return this;
        }

        public JSONObject getMessage() {
            return message;
        }
    }

    /**
     * Represents a command failure message sent by the extension to the execution environment.
     */
    public static class CommandFailure {
        private final JSONObject message;

        public CommandFailure() {
            message = new JSONObject();
            putValue(message, "method", "CommandFailure");
            putValue(message, "version", SCHEMA_VERSION);
        }

        public CommandFailure id(int id) {
            putValue(message, "id", id);
            return this;
        }

        public CommandFailure errorCode(int errorCode) {
            putValue(message, "code", errorCode);
            return this;
        }

        public CommandFailure errorMessage(String errorMessage) {
            putValue(message, "message", errorMessage);
            return this;
        }

        public JSONObject getMessage() {
            return message;
        }
    }

    /**
     * Represents an event message sent by the extension to the execution environment.
     */
    public static class Event {
        private final JSONObject message;
        private final JSONObject payload;

        public Event(String targetUri) {
            message = new JSONObject();
            payload = new JSONObject();
            putValue(message, "method", "Event");
            putValue(message, "version", SCHEMA_VERSION);
            putValue(message, "target", targetUri);
            putValue(message, "payload", payload);
        }

        public Event name(String name) {
            putValue(message, "name", name);
            return this;
        }

        public Event resourceId(String resourceId) {
            putValue(message, "resourceId", resourceId);
            return this;
        }

        public Event property(String key, Object value) {
            putValue(payload, key, value);
            return this;
        }

        public JSONObject getMessage() {
            return message;
        }
    }

    /**
     * Represents a live data update message sent by the extension to the execution environment.
     */
    public static class LiveDataUpdate {
        private final JSONObject message;
        private final JSONArray operations;

        public LiveDataUpdate(String targetUri) {
            message = new JSONObject();
            operations = new JSONArray();
            putValue(message, "method", "LiveDataUpdate");
            putValue(message, "version", SCHEMA_VERSION);
            putValue(message, "target", targetUri);
            putValue(message, "operations", operations);
        }

        public LiveDataUpdate objectName(String name) {
            putValue(message, "name", name);
            return this;
        }

        public LiveDataUpdate liveDataArrayUpdate(JSONObject operation) {
            operations.put(operation);
            return this;
        }

        public LiveDataUpdate liveDataMapUpdate(JSONObject operation) {
            operations.put(operation);
            return this;
        }

        public JSONObject getMessage() {
            return message;
        }
    }

    /**
     * Represents a live data array operation  message sent by the extension to the execution environment.
     */
    public static class LiveDataArrayOperation {
        private final JSONObject operation;

        public LiveDataArrayOperation() {
            operation = new JSONObject();
        }

        public LiveDataArrayOperation type(String type) {
            putValue(operation, "type", type);
            return this;
        }

        public LiveDataArrayOperation item(Object value) {
            putValue(operation, "item", value);
            return this;
        }

        public LiveDataArrayOperation index(int index) {
            putValue(operation, "index", index);
            return this;
        }

        public LiveDataArrayOperation count(int count) {
            putValue(operation, "count", count);
            return this;
        }

        public JSONObject getOperation() {
            return operation;
        }
    }

    /**
     * Represents a live data map operation  message sent by the extension to the execution environment.
     */
    public static class LiveDataMapOperation {
        private final JSONObject operation;

        public LiveDataMapOperation() {
            operation = new JSONObject();
        }

        public LiveDataMapOperation type(String type) {
            putValue(operation, "type", type);
            return this;
        }

        public LiveDataMapOperation item(Object value) {
            putValue(operation, "item", value);
            return this;
        }

        public LiveDataMapOperation key(String key) {
            putValue(operation, "key", key);
            return this;
        }

        public JSONObject getOperation() {
            return operation;
        }
    }

    private static void putValue(JSONObject object, String key, Object value) {
        try {
            object.put(key, value);
        } catch (JSONException e) {
            Log.w(TAG, "Message processing failure.");
        }
    }
}