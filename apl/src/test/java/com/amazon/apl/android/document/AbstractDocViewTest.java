/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.document;

import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.robolectric.ActivityTest;

/**
 * This base class serves as initial setup for Espresso Tests.
 *
 * In order to inflate the document, you should invoke it:
 *
 *         onView(withId(com.amazon.apl.android.test.R.id.apl))
 *                 .perform(inflate(myDocument))
 *                 .check(hasRootContext());
 *
 * where it will populate `APLTestContext` where it contains a reference
 * of the real RootContext of the application.
 */
public abstract class AbstractDocViewTest extends ActivityTest {

    final static String BASE_DOC = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.4\"," +
            "  \"mainTemplate\": {" +
            "    \"parameters\": [\n" +
            "      \"%s\"\n" +
            "    ],\n" +
            "    \"item\":" +
            "    {" +
            "      \"id\": \"testcomp\" " +
            "      %s" +
            "    }" +
            "  }" +
            "  %s" +
            "}";

    public void inflate(String componentProps, String documentProps) {
        inflateWithOptions(componentProps, documentProps, APLOptions.builder());
    }

    public void inflateWithOptions(String document, APLOptions.Builder optionsBuilder) {
        inflate(document, "", "", "payload", "{}", optionsBuilder, null);
    }

    public void inflateWithOptions(String componentProps, String documentProps, APLOptions.Builder optionsBuilder) {
        inflate(componentProps, documentProps, "payload", "{}", optionsBuilder);
    }

    public void inflateWithOptions(String componentProps, String documentProps, APLOptions.Builder optionsBuilder, RootConfig rootConfig) {
        inflate(BASE_DOC, componentProps, documentProps, "payload", "{}", optionsBuilder, rootConfig);
    }

    public void inflate(String componentProps, String documentProps, String payloadId, String data, APLOptions.Builder optionsBuilder) {
        inflate(BASE_DOC, componentProps, documentProps, payloadId, data, optionsBuilder, null);
    }

    public void inflate(String document, String componentProps, String documentProps, String payloadId, String data, APLOptions.Builder optionsBuilder, RootConfig rootConfig) {
        if (componentProps.length() > 0) {
            componentProps =  ", " + componentProps;
        }
        if (documentProps.length() > 0) {
            documentProps =  ", " + documentProps;
        }

        inflate(String.format(document, payloadId, componentProps, documentProps), payloadId, data, optionsBuilder, rootConfig);
    }
}