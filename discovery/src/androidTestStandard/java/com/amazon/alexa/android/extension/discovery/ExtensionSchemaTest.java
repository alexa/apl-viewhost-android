package com.amazon.alexa.android.extension.discovery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import com.amazon.common.Consumer;
import com.amazon.alexaext.ExtensionSchema;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class ExtensionSchemaTest {

    private ExtensionSchema schema;

    @Before
    public void setup() {
        schema = ExtensionSchema.builder()
                .uri("example:uri:10")
                .event("TestEvent")
                .build();
    }

    @Test
    public void testSetUri() {
        schema.setUri("example:uri:10");
        JSONObject schemaJson = schema.getSchema();
        assertEquals("example:uri:10", schemaJson.optString("uri"));
    }

    @Test
    public void testAddDataType() {
        Map<String, String> properties = new HashMap<>();
        properties.put("prop1", "string");
        properties.put("prop2", "number");

        schema = ExtensionSchema.builder()
                .type("DataType1", new Consumer<ExtensionSchema.TypeSchemaBuilder>() {
                    @Override
                    public void accept(ExtensionSchema.TypeSchemaBuilder typeSchema) {
                        typeSchema
                                .property("prop1", new Consumer<ExtensionSchema.PropertySchemaBuilder>() {
                                    @Override
                                    public void accept(ExtensionSchema.PropertySchemaBuilder propertySchema) {
                                        propertySchema.type("string");
                                    }
                                })
                                .property("prop2", new Consumer<ExtensionSchema.PropertySchemaBuilder>() {
                                    @Override
                                    public void accept(ExtensionSchema.PropertySchemaBuilder propertySchema) {
                                        propertySchema.type("number");
                                    }
                                });
                    }
                })
                .build();

        JSONObject schemaJson = schema.getSchema();
        JSONArray types = schemaJson.optJSONArray("types");
        assertNotNull(types);
        assertEquals(1, types.length());

        JSONObject dataType = types.optJSONObject(0);
        assertEquals("DataType1", dataType.optString("name"));

        JSONObject props = dataType.optJSONObject("properties");
        assertEquals("string", props.optJSONObject("prop1").optString("type"));
        assertEquals("number", props.optJSONObject("prop2").optString("type"));
    }

    @Test
    public void testSchemaGeneration() throws JSONException {
        ExtensionSchema schema = ExtensionSchema.builder()
                .uri("alexaext:ExtensionA:10")
                .command("Command1", commandSchema -> {
                    commandSchema
                            .dataType("Command1Payload")
                            .allowFastMode(true)
                            .requireResponse(false);
                })
                .command("Command2", commandSchema -> {
                    commandSchema
                            .dataType("Command2Payload")
                            .allowFastMode(true)
                            .requireResponse(false);
                })
                .type("Command1Payload", typeSchema -> {
                    typeSchema
                            .property("Property1", propertySchema -> {
                                propertySchema
                                        .type("string")
                                        .required(true)
                                        .defaultValue("");
                            })
                            .property("Property2", propertySchema -> {
                                propertySchema
                                        .type("object")
                                        .required(true)
                                        .defaultValue(new JSONObject());
                            });
                })
                .type("Command2Payload", typeSchema -> {
                    typeSchema
                            .property("Property1", propertySchema -> {
                                propertySchema
                                        .type("string")
                                        .required(true)
                                        .defaultValue("");
                            });
                })
                .event("Event1")
                .build();

        JSONObject expectedSchema = new JSONObject(
                "{\n" +
                        "  \"type\": \"Schema\",\n" +
                        "  \"version\": \"1.0\",\n" +
                        "  \"uri\": \"alexaext:ExtensionA:10\",\n" +
                        "  \"types\": [\n" +
                        "    {\n" +
                        "      \"name\": \"Command1Payload\",\n" +
                        "      \"properties\": {\n" +
                        "        \"Property1\": {\n" +
                        "          \"type\": \"string\",\n" +
                        "          \"required\": true,\n" +
                        "          \"default\": \"\"\n" +
                        "        },\n" +
                        "        \"Property2\": {\n" +
                        "          \"type\": \"object\",\n" +
                        "          \"required\": true,\n" +
                        "          \"default\": {}\n" +
                        "        }\n" +
                        "      }\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"name\": \"Command2Payload\",\n" +
                        "      \"properties\": {\n" +
                        "        \"Property1\": {\n" +
                        "          \"type\": \"string\",\n" +
                        "          \"required\": true,\n" +
                        "          \"default\": \"\"\n" +
                        "        }\n" +
                        "      }\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"events\": [\n" +
                        "    {\n" +
                        "      \"name\": \"Event1\"\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"commands\": [\n" +
                        "    {\n" +
                        "      \"name\": \"Command1\",\n" +
                        "      \"requireResponse\": false,\n" +
                        "      \"allowFastMode\": true,\n" +
                        "      \"payload\": \"Command1Payload\"\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"name\": \"Command2\",\n" +
                        "      \"requireResponse\": false,\n" +
                        "      \"allowFastMode\": true,\n" +
                        "      \"payload\": \"Command2Payload\"\n" +
                        "    }\n" +
                        "  ],\n" +
                        "  \"liveData\": [],\n" +
                        "  \"components\": []\n" +
                        "}"
        );

        JSONObject actualSchema = schema.getSchema();

        assertEquals(normalizeJSON(expectedSchema).toString(), normalizeJSON(actualSchema).toString());
    }

    @Test
    public void testAddEvent() {
        schema = ExtensionSchema.builder()
                .event("Event1")
                .event("Event2", true)
                .event("Event3", false)
                .build();

        JSONObject schemaJson = schema.getSchema();
        JSONArray events = schemaJson.optJSONArray("events");
        assertNotNull(events);
        assertEquals(3, events.length());

        JSONObject event1 = events.optJSONObject(0);
        assertEquals("Event1", event1.optString("name"));
        assertFalse(event1.has("mode")); // No mode should be present

        JSONObject event2 = events.optJSONObject(1);
        assertEquals("Event2", event2.optString("name"));
        assertEquals("FAST", event2.optString("mode"));

        JSONObject event3 = events.optJSONObject(2);
        assertEquals("Event3", event3.optString("name"));
        assertEquals("NORMAL", event3.optString("mode"));
    }

    @Test
    public void testAddCommand() {
        schema = ExtensionSchema.builder()
                .command("Command1", new Consumer<ExtensionSchema.CommandSchemaBuilder>() {
                    @Override
                    public void accept(ExtensionSchema.CommandSchemaBuilder commandSchema) {
                        commandSchema.dataType("CommandPayload");
                    }
                })
                .command("Command2", new Consumer<ExtensionSchema.CommandSchemaBuilder>() {
                    @Override
                    public void accept(ExtensionSchema.CommandSchemaBuilder commandSchema) {
                        commandSchema
                                .dataType("CommandPayload")
                                .requireResponse(true);
                    }
                })
                .build();

        JSONObject schemaJson = schema.getSchema();
        JSONArray commands = schemaJson.optJSONArray("commands");
        assertNotNull(commands);
        assertEquals(2, commands.length());

        JSONObject command1 = commands.optJSONObject(0);
        assertEquals("Command1", command1.optString("name"));
        assertEquals("CommandPayload", command1.optString("payload"));
        assertEquals(false, command1.optBoolean("requireResponse"));

        JSONObject command2 = commands.optJSONObject(1);
        assertEquals("Command2", command2.optString("name"));
        assertEquals("CommandPayload", command2.optString("payload"));
        assertEquals(true, command2.optBoolean("requireResponse"));
    }

    @Test
    public void testAddComponent() {
        schema = ExtensionSchema.builder()
                .uri("example:uri:10")
                .event("TestEvent")
                .build();

        schema.addComponent("Component1");

        JSONObject schemaJson = schema.getSchema();
        JSONArray components = schemaJson.optJSONArray("components");
        assertNotNull(components);
        assertEquals(1, components.length());

        JSONObject component = components.optJSONObject(0);
        assertEquals("Component1", component.optString("name"));
        assertNotNull(component.optJSONObject("properties"));
        assertNotNull(component.optJSONArray("events"));
    }

    private static JSONObject normalizeJSON(JSONObject jsonObject) throws JSONException {
        JSONObject normalized = new JSONObject();
        TreeMap<String, Object> sortedMap = new TreeMap<>();

        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject) {
                value = normalizeJSON((JSONObject) value);
            } else if (value instanceof JSONArray) {
                value = normalizeJSONArray((JSONArray) value);
            }
            sortedMap.put(key, value);
        }

        for (Map.Entry<String, Object> entry : sortedMap.entrySet()) {
            normalized.put(entry.getKey(), entry.getValue());
        }

        return normalized;
    }

    private static JSONArray normalizeJSONArray(JSONArray jsonArray) throws JSONException {
        JSONArray sortedArray = new JSONArray();
        for (int i = 0; i < jsonArray.length(); i++) {
            Object value = jsonArray.get(i);
            if (value instanceof JSONObject) {
                value = normalizeJSON((JSONObject) value);
            } else if (value instanceof JSONArray) {
                value = normalizeJSONArray((JSONArray) value);
            }
            sortedArray.put(value);
        }
        return sortedArray;
    }
}
