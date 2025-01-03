/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.viewhost.rendering;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.amazon.apl.android.DocumentSession;
import com.amazon.apl.viewhost.AbstractUnifiedViewhostTest;
import com.amazon.apl.viewhost.message.action.SendUserEventRequest;
import com.amazon.apl.viewhost.primitives.JsonStringDecodable;
import com.amazon.apl.viewhost.request.RenderDocumentRequest;

import org.junit.Test;
import org.robolectric.util.TestRunnable;

public class RenderDocumentTest extends AbstractUnifiedViewhostTest {
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
    public void testViewHostMessageQueue() {
        assertTrue(mAplLayout.isShown());

        TestRunnable runnable = new TestRunnable();
        assertTrue(mAplLayout.post(runnable));
        assertFalse(runnable.wasRun);

        runUntil(() -> runnable.wasRun);
    }

    @Test
    public void testBasicDocumentRendering() {
        RenderDocumentRequest renderDocumentRequest = RenderDocumentRequest.builder()
                .document(new JsonStringDecodable(SIMPLE_DOCUMENT))
                .documentSession(DocumentSession.create())
                .build();

        mViewhost.render(renderDocumentRequest);
        assertSendEvent("640x480", 160);
    }
}
