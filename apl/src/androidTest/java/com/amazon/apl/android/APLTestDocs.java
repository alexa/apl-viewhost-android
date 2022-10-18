/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android;

public final class APLTestDocs {



    // Base document has a instance of the test Component as the top Component. The component
    // type is inserted as the "type": property. Required and optional properties can also be
    // inserted into the component properties. Additionally components like {@link VectorGraphic}
    // can have optional template properties.
    public final static String COMPONENT_BASE_DOC = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.0\"," +
            "  \"onConfigChange\": [\n" +
            "      {\n" +
            "          \"type\": \"SendEvent\",\n" +
            "          \"sequencer\": \"ConfigSendEvent\",\n" +
            "          \"arguments\": [ \"reinflating the APL document\"]\n" +
            "      },\n" +
            "      {\n" +
            "          \"type\": \"Reinflate\"\n" +
            "      }\n" +
            "  ],\n" +
            "  \"mainTemplate\": {" +
            "    \"item\":" +
            "    {" +
            "      \"id\": \"testcomp\", " +
            "      \"type\": \"%s\" %s" +
            "    }" +
            "  }" +
            "  %s" +
            "}";

    // Document has a instance of the test Component parented by a Frame. The component
    // type is inserted as the "type": property. Required and optional properties can also be
    // inserted into the component properties.
    @SuppressWarnings("SpellCheckingInspection")
    public final static String COMPONENT_PARENT_DOC = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.0\"," +
            "  \"mainTemplate\": {" +
            "    \"item\": {" +
            "      \"type\": \"Frame\"," +
            "     \"name\": \"parentcomp\"," +
            "      \"item\": { " +
            "        \"id\": \"testcomp\", " +
            "        \"type\": \"%s\" %s" +
            "      }" +
            "    }" +
            "  }" +
            "}";

    // Properties that are common to all components, but optional.
    public final static String COMPONENT_OPTIONAL_COMMON_PROPERTIES = "" +
            "\"accessibilityLabel\": \"Go APL\",\n" +
            "      \"description\": \"APL Description\",\n" +
            "      \"checked\": true,\n" +
            "      \"disabled\": true,\n" +
            "      \"display\": \"invisible\",\n" +
            "      \"height\": 111,\n" +
            "      \"width\": 642,\n" +
            "      \"maxHeight\": 123,\n" +
            "      \"minHeight\": 13,\n" +
            "      \"maxWidth\": 669,\n" +
            "      \"minWidth\": 64,\n" +
            "      \"opacity\": 0.5";


}
