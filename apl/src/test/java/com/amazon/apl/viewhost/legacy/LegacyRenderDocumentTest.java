/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.viewhost.legacy;

import com.amazon.apl.android.APLController;
import com.amazon.apl.viewhost.AbstractLegacyViewhostTest;

import org.junit.Test;

public class LegacyRenderDocumentTest extends AbstractLegacyViewhostTest {
    private static final String SIMPLE_DOCUMENT = "{" +
        "  \"type\": \"APL\"," +
        "  \"version\": \"2024.2\"," +
        "  \"onMount\": {" +
        "    \"type\": \"SendEvent\"," +
        "    \"arguments\": [" +
        "      \"${viewport.pixelWidth}x${viewport.pixelHeight}\"," +
        "      \"${viewport.dpi}\"" +
        "    ]" +
        "  }," +
        "  \"mainTemplate\": {" +
        "    \"items\": {" +
        "      \"type\": \"Text\"," +
        "      \"text\": \"Hello, World!\"," +
        "      \"color\": \"white\"," +
        "      \"textAlign\": \"center\"," +
        "      \"textAlignVertical\": \"center\"" +
        "    }" +
        "  }" +
        "}";

    @Test
    public void testBasicDocumentRendering() {
        APLController.builder()
                .aplDocument(SIMPLE_DOCUMENT)
                .rootConfig(mRootConfig)
                .aplOptions(mAplOptionsBuilder.build())
                .aplLayout(mAplLayout)
                .disableAsyncInflate(true)
                .render();

        assertSendEvent("640x480", 160);
    }

    @Test
    public void testBasicDocumentAsyncRendering() {
        APLController.builder()
                .aplDocument(SIMPLE_DOCUMENT)
                .rootConfig(mRootConfig)
                .aplOptions(mAplOptionsBuilder.build())
                .aplLayout(mAplLayout)
                .disableAsyncInflate(false)
                .render();

        assertSendEvent("640x480", 160);
    }
}
