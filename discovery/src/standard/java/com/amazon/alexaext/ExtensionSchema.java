package com.amazon.alexaext;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.amazon.common.Consumer;

import java.util.List;
import java.util.Map;

/**
 * The ExtensionSchema class provides methods to create and modify a JSON schema that defines
 * the extension API exposed to the execution environment. The schema typically includes data types,
 * events, commands, and live data definitions for the extension.
 */
public class ExtensionSchema {

    private static final String TAG = "ExtensionSchema";
    private static String SCHEMA_VERSION = "1.0";

    private final JSONObject mSchema;

    /**
     * Constructs a new ExtensionSchema object.
     */
    public ExtensionSchema() {
        mSchema = createObject();
        putValue(mSchema, "type", "Schema");
        putValue(mSchema, "version", SCHEMA_VERSION);
        putValue(mSchema, "events", new JSONArray());
        putValue(mSchema, "types", new JSONArray());
        putValue(mSchema, "commands", new JSONArray());
        putValue(mSchema, "liveData", new JSONArray());
        putValue(mSchema, "components", new JSONArray());
    }

    /**
     * Returns the JSONObject representing the extension schema.
     *
     * @return The JSONObject representing the extension schema.
     */
    public JSONObject getSchema() {
        return mSchema;
    }

    /**
     * Sets the URI (Uniform Resource Identifier) for the extension schema.
     *
     * @param uri The URI for the extension schema.
     * @return The ExtensionSchema object for method chaining.
     */
    public ExtensionSchema setUri(String uri) {
        putValue(mSchema, "uri", uri);
        return this;
    }

    /**
     * Adds an event definition to the extension schema.
     *
     * @param name The name of the event.
     * @return The ExtensionSchema object for method chaining.
     */
    public ExtensionSchema addEvent(String name) {
        return addEvent(name, true);
    }

    /**
     * Adds an event definition to the extension schema with the specified fast mode setting.
     *
     * @param name     The name of the event.
     * @param fastMode A boolean indicating whether the event is in fast mode or not.
     * @return The ExtensionSchema object for method chaining.
     */
    public ExtensionSchema addEvent(String name, boolean fastMode) {
        JSONObject eventSchema = createEventSchema(name, fastMode);
        putArrayValue(mSchema, "events", eventSchema);
        return this;
    }

    /**
     * Adds a live data array definition to the extension schema.
     *
     * @param name     The name of the live data array.
     * @param dataType The data type of the live data array elements.
     * @return An EventHandlerBuilder instance for method chaining.
     */
    public EventHandlerBuilder addLiveDataArray(String name, String dataType) {
        JSONObject liveDataSchema = createLiveDataSchema(name, dataType, true);
        return new EventHandlerBuilder(liveDataSchema);
    }

    /**
     * Adds a live data map definition to the extension schema.
     *
     * @param name     The name of the live data map.
     * @param dataType The data type of the live data map values.
     * @return An EventHandlerBuilder instance for method chaining.
     */
    public EventHandlerBuilder addLiveDataMap(String name, String dataType) {
        JSONObject liveDataSchema = createLiveDataSchema(name, dataType, false);
        return new EventHandlerBuilder(liveDataSchema);
    }

    /**
     * Adds a component definition to the extension schema.
     *
     * @param name The name of the component.
     * @return The ExtensionSchema object for method chaining.
     */
    public ExtensionSchema addComponent(String name) {
        JSONObject componentSchema = createComponentSchema(name);
        putArrayValue(mSchema, "components", componentSchema);
        return this;
    }

    /**
     * Creates a JSONObject representing the payload definition for a command.
     *
     * @param dataType    The data type of the command payload
     * @param description The description of the command payload (can be null).
     * @return A JSONObject representing the payload definition.
     */
    public JSONObject createPayloadDefinition(String dataType, String description) {
        JSONObject payload = new JSONObject();
        putValue(payload, "type", dataType);
        if (description != null) {
            putValue(payload, "description", description);
        }
        return payload;
    }

    /**
     * Creates a JSONObject representing a data type schema with the given name and properties.
     *
     * @param name       The name of the data type.
     * @param properties A map containing the properties of the data type.
     * @return A JSONObject representing the data type schema.
     */

    private static JSONObject createTypeSchema(String name, Map<String, String> properties) {
        JSONObject typeSchema = createObject();
        putValue(typeSchema, "name", name);
        JSONObject propertiesObject = createObject();
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            putValue(propertiesObject, entry.getKey(), entry.getValue());
        }
        putValue(typeSchema, "properties", propertiesObject);
        return typeSchema;
    }

    /**
     * Creates a JSONObject representing an event schema with the given name and fast mode setting.
     *
     * @param name     The name of the event.
     * @param fastMode A boolean indicating whether the event is in fast mode or not.
     * @return A JSONObject representing the event schema.
     */
    private static JSONObject createEventSchema(String name, Boolean fastMode) {
        JSONObject eventSchema = createObject();
        putValue(eventSchema, "name", name);
        if (fastMode != null) {
            putValue(eventSchema, "mode", fastMode ? "FAST" : "NORMAL");
        }
        return eventSchema;
    }

    /**
     * Creates a JSONObject representing a live data schema with the given name, data type, and array flag.
     *
     * @param name     The name of the live data.
     * @param dataType The data type of the live data (can be null).
     * @param isArray  A boolean indicating whether the live data is an array or not.
     * @return A JSONObject representing the live data schema.
     */
    private static JSONObject createLiveDataSchema(String name, String dataType, boolean isArray) {
        JSONObject liveDataSchema = createObject();
        putValue(liveDataSchema, "name", name);
        if (dataType != null) {
            putValue(liveDataSchema, "type", isArray ? dataType + "[]" : dataType);
        }
        JSONObject eventsObject = createObject();
        putValue(liveDataSchema, "events", eventsObject);
        return liveDataSchema;
    }

    /**
     * Adds an event handler definition to a live data schema.
     *
     * @param liveDataSchema The JSONObject representing the live data schema.
     * @param operation      The operation for the event handler (e.g., "set", "add", "remove", "update").
     * @param eventHandler   The name of the event handler function.
     * @param properties     A list of property names for the event handler.
     */
    private static void addEventHandler(JSONObject liveDataSchema, String operation, String eventHandler, List<String> properties, boolean update, boolean collapse) {
        JSONObject eventHandlerObject = createObject();
        putValue(eventHandlerObject, "eventHandler", eventHandler);
        JSONArray propertiesArray = new JSONArray();
        for (String property : properties) {
            JSONObject propertyObject = createObject();
            putValue(propertyObject, "name", property);
            putValue(propertyObject, "update", update);
            putValue(propertyObject, "collapse", collapse);
            propertiesArray.put(propertyObject);
        }
        putValue(eventHandlerObject, "properties", propertiesArray);
        JSONObject eventsObject = getObject(liveDataSchema, "events");
        putValue(eventsObject, operation, eventHandlerObject);
    }

    /**
     * Creates a JSONObject representing a component schema with the given name.
     *
     * @param name The name of the component.
     * @return A JSONObject representing the component schema.
     */
    private static JSONObject createComponentSchema(String name) {
        JSONObject componentSchema = createObject();
        putValue(componentSchema, "name", name);
        putValue(componentSchema, "properties", createObject());
        putValue(componentSchema, "events", new JSONArray());
        return componentSchema;
    }

    /**
     * Creates a new empty JSONObject.
     *
     * @return A new empty JSONObject.
     */
    private static JSONObject createObject() {
        return new JSONObject();
    }

    /**
     * Puts a key-value pair into a JSONObject.
     *
     * @param object The JSONObject to put the value into.
     * @param key    The key for the value.
     * @param value  The value to put into the JSONObject.
     */
    private static void putValue(JSONObject object, String key, Object value) {
        try {
            object.put(key, value);
        } catch (JSONException e) {
            Log.w(TAG, "Failed to putValue for key " + key + ": " + e.getMessage());
        }
    }

    /**
     * Puts a JSONObject value into an array in another JSONObject.
     *
     * @param object The JSONObject containing the array.
     * @param key    The key of the array in the JSONObject.
     * @param value  The JSONObject value to put into the array.
     */
    private static void putArrayValue(JSONObject object, String key, JSONObject value) {
        try {
            object.getJSONArray(key).put(value);
        } catch (JSONException e) {
            Log.w(TAG, "Failed to putArrayValue for key " + key + ": " + e.getMessage());
        }
    }

    /**
     * Gets a JSONObject value from another JSONObject.
     *
     * @param object The JSONObject containing the value.
     * @param key    The key of the value in the JSONObject.
     * @return The JSONObject value, or an empty JSONObject if the key is not found or there is an error retrieving the value.
     */
    private static JSONObject getObject(JSONObject object, String key) {
        try {
            return object.getJSONObject(key);
        } catch (JSONException e) {
            Log.w(TAG, "Failed to get JSONObject for key " + key + ": " + e.getMessage());
            return createObject();
        }
    }

    public static class ExtensionSchemaBuilder {
        private final ExtensionSchema schema;

        private ExtensionSchemaBuilder() {
            schema = new ExtensionSchema();
        }

        public ExtensionSchemaBuilder version(String version) {
            putValue(schema.mSchema, "version", version);
            return this;
        }

        public ExtensionSchemaBuilder uri(String uri) {
            putValue(schema.mSchema, "uri", uri);
            return this;
        }

        public ExtensionSchemaBuilder command(String name, Consumer<CommandSchemaBuilder> consumer) {
            CommandSchemaBuilder commandSchemaBuilder = new CommandSchemaBuilder(name);
            consumer.accept(commandSchemaBuilder);
            putArrayValue(schema.mSchema, "commands", commandSchemaBuilder.build());
            return this;
        }

        public ExtensionSchemaBuilder type(String name, Consumer<TypeSchemaBuilder> consumer) {
            TypeSchemaBuilder typeSchemaBuilder = new TypeSchemaBuilder(name);
            consumer.accept(typeSchemaBuilder);
            putArrayValue(schema.mSchema, "types", typeSchemaBuilder.build());
            return this;
        }

        public ExtensionSchemaBuilder event(String name) {
            return event(name, null);
        }

        public ExtensionSchemaBuilder event(String name, Boolean fastMode) {
            putArrayValue(schema.mSchema, "events", createEventSchema(name, fastMode));
            return this;
        }

        public ExtensionSchemaBuilder addLiveDataArray(String name, String dataType) {
            JSONObject liveDataSchema = createLiveDataSchema(name, dataType, true);
            putArrayValue(schema.mSchema, "liveData", liveDataSchema);
            return this;
        }

        public ExtensionSchemaBuilder addLiveDataMap(String name, String dataType) {
            JSONObject liveDataSchema = createLiveDataSchema(name, dataType, false);
            putArrayValue(schema.mSchema, "liveData", liveDataSchema);
            return this;
        }

        public ExtensionSchemaBuilder addEventHandler(String operation, String eventHandler, List<String> properties, boolean update, boolean collapse) {
            JSONObject liveDataSchema = schema.mSchema.optJSONArray("liveData").optJSONObject(0);
            ExtensionSchema.addEventHandler(liveDataSchema, operation, eventHandler, properties, update, collapse);
            return this;
        }

        public ExtensionSchema build() {
            return schema;
        }
    }

    public static ExtensionSchemaBuilder builder() {
        return new ExtensionSchemaBuilder();
    }

    public static class CommandSchemaBuilder {
        private final JSONObject commandSchema;

        private CommandSchemaBuilder(String name) {
            commandSchema = createObject();
            putValue(commandSchema, "name", name);
        }

        public CommandSchemaBuilder dataType(String dataType) {
            putValue(commandSchema, "payload", dataType);
            return this;
        }

        public CommandSchemaBuilder allowFastMode(boolean allowFastMode) {
            putValue(commandSchema, "allowFastMode", allowFastMode);
            return this;
        }

        public CommandSchemaBuilder requireResponse(boolean requireResponse) {
            putValue(commandSchema, "requireResponse", requireResponse);
            return this;
        }

        private JSONObject build() {
            return commandSchema;
        }
    }

    public static class TypeSchemaBuilder {
        private final JSONObject typeSchema;
        private final JSONObject propertiesObject;

        private TypeSchemaBuilder(String name) {
            typeSchema = createObject();
            propertiesObject = createObject();
            putValue(typeSchema, "name", name);
        }

        public TypeSchemaBuilder property(String name, Consumer<PropertySchemaBuilder> consumer) {
            PropertySchemaBuilder propertySchemaBuilder = new PropertySchemaBuilder(name);
            consumer.accept(propertySchemaBuilder);
            putValue(propertiesObject, name, propertySchemaBuilder.build());
            return this;
        }

        public TypeSchemaBuilder property(String name, String value) {
            putValue(propertiesObject, name, value);
            return this;
        }

        private JSONObject build() {
            putValue(typeSchema, "properties", propertiesObject);
            return typeSchema;
        }
    }

    public static class PropertySchemaBuilder {
        private final JSONObject propertySchema;

        private PropertySchemaBuilder(String name) {
            propertySchema = createObject();
        }

        public PropertySchemaBuilder type(String type) {
            putValue(propertySchema, "type", type);
            return this;
        }

        public PropertySchemaBuilder required(boolean required) {
            putValue(propertySchema, "required", required);
            return this;
        }

        public PropertySchemaBuilder defaultValue(Object defaultValue) {
            putValue(propertySchema, "default", defaultValue);
            return this;
        }

        private JSONObject build() {
            return propertySchema;
        }
    }

    /**
     * Auxillary Builder class for chaining multiple Event Handlers to a live data schema
     * Calling build adds the completed live data schema to the overall schema.
     */
    public class EventHandlerBuilder {
        private final JSONObject liveDataSchema;

        private EventHandlerBuilder(JSONObject liveDataSchema) {
            this.liveDataSchema = liveDataSchema;
        }

        public EventHandlerBuilder addEventHandler(String operation, String eventHandler, List<String> properties, boolean update, boolean collapse) {
            ExtensionSchema.addEventHandler(liveDataSchema, operation, eventHandler, properties, update, collapse);
            return this;
        }

        public EventHandlerBuilder addEventHandler(String operation, String eventHandler, List<String> properties, boolean update) {
            addEventHandler(operation, eventHandler, properties, update, true);
            return this;
        }

        public ExtensionSchema build() {
            putArrayValue(mSchema, "liveData", liveDataSchema);
            return ExtensionSchema.this;
        }
    }

}
