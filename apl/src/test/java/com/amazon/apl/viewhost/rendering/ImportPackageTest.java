/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.viewhost.rendering;

import androidx.annotation.NonNull;

import com.amazon.apl.android.APLJSONData;
import com.amazon.apl.android.Content;
import com.amazon.apl.android.DocumentSession;
import com.amazon.apl.android.dependencies.IPackageLoader;
import com.amazon.apl.viewhost.AbstractUnifiedViewhostTest;
import com.amazon.apl.viewhost.primitives.JsonStringDecodable;
import com.amazon.apl.viewhost.request.RenderDocumentRequest;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ImportPackageTest extends AbstractUnifiedViewhostTest {
    /**
     * Package defines a layout, which has a Text component that emits a SendEvent once mounted.
     */
    private static final String HELLO_PACKAGE = "{" +
        "  \"type\": \"APL\"," +
        "  \"version\": \"2024.2\"," +
        "  \"layouts\": {" +
        "    \"HelloText\": {" +
        "      \"parameters\": [" +
        "        \"name\"" +
        "      ]," +
        "      \"item\": {" +
        "        \"type\": \"Text\"," +
        "        \"onMount\": {" +
        "          \"type\": \"SendEvent\"," +
        "          \"arguments\": [" +
        "            \"helloMounted\"" +
        "          ]" +
        "        }," +
        "        \"text\": \"Hello, ${name}!\"" +
        "      }" +
        "    }" +
        "  }" +
        "}";

    /**
     * Document that imports the above package in the traditional way.
     */
    private static final String BASIC_DOC_WITH_IMPORT = "{" +
        "  \"type\": \"APL\"," +
        "  \"version\": \"2024.2\"," +
        "  \"import\": [" +
        "    {" +
        "      \"name\": \"hello-layout\"," +
        "      \"version\": \"1.0.0\"" +
        "    }" +
        "  ]," +
        "  \"mainTemplate\": {" +
        "    \"items\": {" +
        "      \"type\": \"HelloText\"," +
        "      \"name\": \"World\"" +
        "    }" +
        "  }" +
        "}";

    /**
     * Document that imports the above package using ImportPackage instead.
     */
    private static final String BASIC_DOC_WITH_IMPORT_PACKAGE = "{" +
        "  \"type\": \"APL\"," +
        "  \"version\": \"2024.2\"," +
        "  \"onMount\": {" +
        "    \"type\": \"ImportPackage\"," +
        "    \"name\": \"hello-layout\"," +
        "    \"version\": \"1.0.0\"," +
        "    \"onLoad\": {" +
        "      \"type\": \"InsertItem\"," +
        "      \"componentId\": \"main\"," +
        "      \"item\": {" +
        "        \"type\": \"HelloText\"," +
        "        \"name\": \"World\"" +
        "      }" +
        "    }," +
        "    \"onFail\": {" +
        "      \"type\": \"SendEvent\"," +
        "      \"sequencer\": \"SEND_EVENT\"," +
        "      \"arguments\": [" +
        "        \"importFailed\"" +
        "      ]" +
        "    }" +
        "  }," +
        "  \"mainTemplate\": {" +
        "    \"item\": {" +
        "      \"type\": \"Container\"," +
        "      \"height\": \"100%\"," +
        "      \"width\": \"100%\"," +
        "      \"id\": \"main\"," +
        "      \"items\": [" +
        "        {" +
        "          \"type\": \"Frame\"," +
        "          \"height\": 100," +
        "          \"backgroundColor\": \"blue\"" +
        "        }" +
        "      ]" +
        "    }" +
        "  }" +
        "}";

    StaticPackageLoader mPackageLoader;

    @Override
    public void initialize() {
        mPackageLoader = new StaticPackageLoader();
        mViewhostConfigBuilder.IPackageLoader(mPackageLoader);
        super.initialize();
    }

    @Test
    public void testSimpleImport() {
        mPackageLoader.addPackage(Content.ImportRef.create("hello-layout", "1.0.0"), HELLO_PACKAGE);

        RenderDocumentRequest renderDocumentRequest = RenderDocumentRequest.builder()
                .document(new JsonStringDecodable(BASIC_DOC_WITH_IMPORT))
                .documentSession(DocumentSession.create())
                .build();

        mViewhost.render(renderDocumentRequest);

        assertSendEvent("helloMounted");
    }

    @Test
    public void testImportPackageSuccess() {
        mPackageLoader.addPackage(Content.ImportRef.create("hello-layout", "1.0.0"), HELLO_PACKAGE);

        RenderDocumentRequest renderDocumentRequest = RenderDocumentRequest.builder()
                .document(new JsonStringDecodable(BASIC_DOC_WITH_IMPORT_PACKAGE))
                .documentSession(DocumentSession.create())
                .build();

        mViewhost.render(renderDocumentRequest);

        assertSendEvent("helloMounted");
    }

    @Test
    public void testImportPackageFail() {
        // By not pre-populating the "hello-layout" package it will fail to import.

        RenderDocumentRequest renderDocumentRequest = RenderDocumentRequest.builder()
                .document(new JsonStringDecodable(BASIC_DOC_WITH_IMPORT_PACKAGE))
                .documentSession(DocumentSession.create())
                .build();

        mViewhost.render(renderDocumentRequest);

        assertSendEvent("importFailed");
    }

    /**
     * Setup a package loader in which packages are preloaded beforehand.
     */
    private static class StaticPackageLoader implements IPackageLoader {
        private final Map<Content.ImportRef, APLJSONData> mCache = new HashMap<>();

        public void addPackage(Content.ImportRef importRef, String contents) {
            mCache.put(importRef, APLJSONData.create(contents));
        }

        @Override
        public void fetch(@NonNull Content.ImportRequest request, @NonNull SuccessCallback<Content.ImportRequest, APLJSONData> successCallback, @NonNull FailureCallback<Content.ImportRequest> failureCallback) {
            final Content.ImportRef ref = request.getImportRef();
            APLJSONData result = mCache.get(ref);
            if (result != null) {
                successCallback.onSuccess(request, result);
            } else {
                failureCallback.onFailure(request, "Package not available");
            }
        }
    }
}
