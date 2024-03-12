/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.document;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.amazon.apl.android.APLLayout;
import com.amazon.apl.android.DocumentSession;
import com.amazon.apl.android.Frame;
import com.amazon.apl.android.views.APLAbsoluteLayout;
import com.amazon.apl.viewhost.DocumentHandle;
import com.amazon.apl.viewhost.PreparedDocument;
import com.amazon.apl.viewhost.Viewhost;
import com.amazon.apl.viewhost.config.DocumentOptions;
import com.amazon.apl.viewhost.config.ViewhostConfig;
import com.amazon.apl.viewhost.internal.DocumentHandleImpl;
import com.amazon.apl.viewhost.internal.DocumentState;
import com.amazon.apl.viewhost.internal.DocumentStateChangeListener;
import com.amazon.apl.viewhost.primitives.JsonStringDecodable;
import com.amazon.apl.viewhost.request.FinishDocumentRequest;
import com.amazon.apl.viewhost.request.PrepareDocumentRequest;
import com.amazon.apl.viewhost.request.RenderDocumentRequest;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

// This test requires a view to be displayed thus must be done via androidTest.
public class ViewhostTest extends AbstractDocViewTest {

    private static final String SIMPLE_DOC = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.0\"," +
            "  \"mainTemplate\": {" +
            "    \"item\":" +
            "    {" +
            "      \"type\": \"Frame\"" +
            "    }" +
            "  }" +
            "}";

    @Test
    public void testViewhost_prepareAndRender() throws InterruptedException {
        CountDownLatch displayedLatch = new CountDownLatch(1);
        CountDownLatch inflatedLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(1);
        final DocumentHandle[] mHandle = new DocumentHandle[1];

        activityRule.getScenario().onActivity(activity -> {
            APLLayout aplLayout = activity.findViewById(com.amazon.apl.android.test.R.id.apl);
            DocumentOptions documentOptions = DocumentOptions.builder().build();
            ViewhostConfig viewhostConfig = ViewhostConfig.builder()
                    .defaultDocumentOptions(documentOptions)
                    .build();
            Viewhost mViewhost = Viewhost.create(viewhostConfig);
            PrepareDocumentRequest request = PrepareDocumentRequest.builder()
                    .document(new JsonStringDecodable(SIMPLE_DOC))
                    .documentSession(DocumentSession.create())
                    .documentOptions(documentOptions)
                    .build();
            mViewhost.bind(aplLayout);

            mViewhost.registerStateChangeListener(new DocumentStateChangeListener() {
                @Override
                public void onDocumentStateChanged(DocumentState state, DocumentHandle handle) {
                    if (state == DocumentState.INFLATED) {
                        inflatedLatch.countDown();
                        assertTrue(((DocumentHandleImpl) handle).getRootContext().getTopComponent() instanceof Frame);
                    }
                    if (state == DocumentState.DISPLAYED) {
                        displayedLatch.countDown();
                        assertTrue(aplLayout.getChildAt(0) instanceof APLAbsoluteLayout);
                    }
                    if (state == DocumentState.FINISHED) {
                        finishLatch.countDown();
                    }
                }
            });
            // Prepare and render
            PreparedDocument preparedDocument = mViewhost.prepare(request);
            mHandle[0] = mViewhost.render(preparedDocument);
            assertNotNull(mHandle[0]);
        });
        assertTrue(inflatedLatch.await(5, TimeUnit.SECONDS));
        assertTrue(displayedLatch.await(5, TimeUnit.SECONDS));

        // Finish
        FinishDocumentRequest finishRequest = FinishDocumentRequest.builder().build();
        boolean result = mHandle[0].finish(finishRequest);
        assertTrue(finishLatch.await(1, TimeUnit.SECONDS));
        assertTrue(result);
    }

    @Test
    public void testViewhost_render() throws InterruptedException {
        CountDownLatch displayedLatch = new CountDownLatch(1);
        CountDownLatch inflatedLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(1);
        final DocumentHandle[] mHandle = new DocumentHandle[1];

        activityRule.getScenario().onActivity(activity -> {
            APLLayout aplLayout = activity.findViewById(com.amazon.apl.android.test.R.id.apl);
            DocumentOptions documentOptions = DocumentOptions.builder().build();
            ViewhostConfig viewhostConfig = ViewhostConfig.builder()
                    .defaultDocumentOptions(documentOptions)
                    .build();
            Viewhost mViewhost = Viewhost.create(viewhostConfig);
            RenderDocumentRequest request = RenderDocumentRequest.builder()
                    .document(new JsonStringDecodable(SIMPLE_DOC))
                    .documentSession(DocumentSession.create())
                    .documentOptions(documentOptions)
                    .build();
            mViewhost.bind(aplLayout);

            mViewhost.registerStateChangeListener(new DocumentStateChangeListener() {
                @Override
                public void onDocumentStateChanged(DocumentState state, DocumentHandle handle) {
                    if (state == DocumentState.INFLATED) {
                        inflatedLatch.countDown();
                        assertTrue(((DocumentHandleImpl) handle).getRootContext().getTopComponent() instanceof Frame);
                    }
                    if (state == DocumentState.DISPLAYED) {
                        displayedLatch.countDown();
                        assertTrue(aplLayout.getChildAt(0) instanceof APLAbsoluteLayout);
                    }
                    if (state == DocumentState.FINISHED) {
                        finishLatch.countDown();
                    }
                }
            });
            mHandle[0] = mViewhost.render(request);
            assertNotNull(mHandle[0]);
        });
        assertTrue(inflatedLatch.await(5, TimeUnit.SECONDS));
        assertTrue(displayedLatch.await(5, TimeUnit.SECONDS));
        // Finish
        FinishDocumentRequest finishRequest = FinishDocumentRequest.builder().build();
        boolean result = mHandle[0].finish(finishRequest);
        assertTrue(finishLatch.await(1, TimeUnit.SECONDS));
        assertTrue(result);
    }
}
