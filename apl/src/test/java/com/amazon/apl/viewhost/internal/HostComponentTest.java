/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.viewhost.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Handler;
import android.view.View;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amazon.alexaext.ExtensionRegistrar;
import com.amazon.alexaext.IExtensionProvider;
import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.DocumentSession;
import com.amazon.apl.android.ExtensionMediator.IExtensionGrantRequestCallback;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.dependencies.IMediaPlayer;
import com.amazon.apl.android.document.AbstractDocUnitTest;
import com.amazon.apl.android.media.RuntimeMediaPlayerFactory;
import com.amazon.apl.android.providers.AbstractMediaPlayerProvider;
import com.amazon.apl.viewhost.DocumentHandle;
import com.amazon.apl.viewhost.PreparedDocument;
import com.amazon.apl.viewhost.Viewhost;
import com.amazon.apl.viewhost.config.DocumentOptions;
import com.amazon.apl.viewhost.config.EmbeddedDocumentFactory;
import com.amazon.apl.viewhost.config.EmbeddedDocumentResponse;
import com.amazon.apl.viewhost.config.ViewhostConfig;
import com.amazon.apl.viewhost.internal.message.notification.DocumentStateChangedImpl;
import com.amazon.apl.viewhost.message.BaseMessage;
import com.amazon.apl.viewhost.primitives.JsonStringDecodable;
import com.amazon.apl.viewhost.request.ExecuteCommandsRequest;
import com.amazon.apl.viewhost.request.ExecuteCommandsRequest.ExecuteCommandsCallback;
import com.amazon.apl.viewhost.request.PrepareDocumentRequest;
import com.amazon.apl.viewhost.utils.CapturingMessageHandler;
import com.amazon.apl.viewhost.utils.ManualExecutor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.HashMap;

@RunWith(AndroidJUnit4.class)
public class HostComponentTest extends AbstractDocUnitTest {

    private static final String DOC = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"2022.3\",\n" +
            "  \"mainTemplate\": {\n" +
            "    \"item\": {\n" +
            "      \"type\": \"Container\",\n" +
            "      \"items\": [\n" +
            "        {\n" +
            "          \"type\": \"Host\",\n" +
            "          \"source\": \"documentA\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"type\": \"Host\",\n" +
            "          \"source\": \"documentB\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  }\n" +
            "}\n";
    // Test content
    private static final String EMBEDDED_DOC_1 = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"1.0\",\n" +
            "  \"theme\": \"auto\",\n" +
            "  \"mainTemplate\": {\n" +
            "    \"items\": [\n" +
            "      {\n" +
            "        \"type\": \"Video\",\n" +
            "        \"height\": \"100%\",\n" +
            "        \"width\": \"100%\",\n" +
            "        \"id\": \"VideoPlayer\",\n" +
            "        \"source\": [\n" +
            "          {\n" +
            "            \"description\": \"The first video clip to play\",\n" +
            "            \"repeatCount\": 0,\n" +
            "            \"url\": \"dummy-url-1\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"description\": \"The second video clip to play\",\n" +
            "            \"url\": \"dummy-url-2\",\n" +
            "            \"repeatCount\": -1\n" +
            "          },\n" +
            "          {\n" +
            "            \"description\": \"This video clip will only be reached by a command\",\n" +
            "            \"url\": \"dummy-url-3\",\n" +
            "            \"repeatCount\": 2\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";
    private static final String EMBEDDED_DOC_WITH_EXTENSION = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"1.7\",\n" +
            "  \"extensions\": [\n" +
            "    {\n" +
            "      \"uri\": \"example:local:10\",\n" +
            "      \"name\": \"Local\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"mainTemplate\": {\n" +
            "    \"parameters\": [\n" +
            "      \"payload\"\n" +
            "    ],\n" +
            "    \"item\": {\n" +
            "      \"type\": \"Container\",\n" +
            "      \"width\": \"100%\",\n" +
            "      \"items\": [\n" +
            "        {\n" +
            "          \"type\": \"Container\",\n" +
            "          \"direction\": \"row\",\n" +
            "          \"items\": [\n" +
            "            {\n" +
            "              \"type\": \"Text\",\n" +
            "              \"width\": \"auto\",\n" +
            "              \"text\": \"${environment.extension.Local ? 'Local' : 'No Local'}\"\n" +
            "            }\n" +
            "          ]\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  }\n" +
            "}";

    private static final String CONTROL_MEDIA_COMMAND = "[{\n" +
            "  \"type\": \"ControlMedia\",\n" +
            "  \"componentId\": \"VideoPlayer\",\n" +
            "  \"command\": \"%s\",\n" +
            "  \"value\": %d\n" +
            "}]";

    private HashMap<String, DocumentHandle> mEmbeddedDocuments;

    @Mock
    private Handler mCoreWorker;
    @Mock
    IMediaPlayer mediaPlayerMock;
    @Mock
    ExecuteCommandsCallback mCallback;
    @Mock
    private IExtensionProvider mExtensionProvider;
    @Mock
    DocumentOptions mDocumentOptions;
    @Mock
    IExtensionGrantRequestCallback mExtensionGrantRequestCallback;

    private CapturingMessageHandler mMessageHandler;
    private Viewhost mViewhost;
    private ManualExecutor mRuntimeInteractionWorker;

    @Before
    public void setup() {
        when(mCoreWorker.post(any(Runnable.class))).thenAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        });
        initializeAndLoad();
    }

    private void initializeAndLoad() {
        mEmbeddedDocuments = new HashMap<>();
        mRuntimeInteractionWorker = new ManualExecutor();
        mMessageHandler = new CapturingMessageHandler();

        AbstractMediaPlayerProvider provider = new AbstractMediaPlayerProvider() {
            @Override
            public View createView(Context context) {
                return new View(context);
            }

            @Override
            public IMediaPlayer createPlayer(Context context, View view) {
                return mediaPlayerMock;
            }
        };
        mRootConfig = RootConfig.create("Unit Test", "1.0")
                .allowOpenUrl(true).mediaPlayerFactory(new RuntimeMediaPlayerFactory(provider));
        ExtensionRegistrar extensionRegistrar = new ExtensionRegistrar().addProvider(mExtensionProvider);
        // Create new viewhost for handling embedded documents
        ViewhostConfig config = ViewhostConfig.builder()
                .messageHandler(mMessageHandler)
                .extensionRegistrar(extensionRegistrar)
                .build();
        mViewhost = new ViewhostImpl(config, mRuntimeInteractionWorker, mCoreWorker);
        EmbeddedDocumentFactory factory = new EmbeddedDocumentFactoryTest(mViewhost);
        mRootConfig.setDocumentManager(factory, mCoreWorker);
        loadDocument(DOC, APLOptions.builder()
                .embeddedDocumentFactory(factory)
                .viewhost(mViewhost)
                .build());
    }

    @Test
    public void testExecuteCommand() {
        assertNotNull(mEmbeddedDocuments.get("documentA"));
        ExecuteCommandsRequest request = ExecuteCommandsRequest.builder()
                .commands(new JsonStringDecodable(getCommand("play", 0)))
                .callback(mCallback)
                .build();
        assertTrue(mEmbeddedDocuments.get("documentA").executeCommands(request));
    }

    private static String getCommand(final String command, final int value) {
        return String.format(CONTROL_MEDIA_COMMAND, command, value);
    }

    @Test
    public void testDocumentStateChangedNotified() {
        assertDocumentInflatedAndPreparedNotificationSent();

        mRootContext.finishDocument();

        assertDocumentFinishedNotificationSent();
    }

    private void assertDocumentFinishedNotificationSent() {
        assertFalse(mRuntimeInteractionWorker.isEmpty());
        mRuntimeInteractionWorker.flush();
        assert(mMessageHandler.queue.size() > 0);

        int documentStateChangedNotificationFinished = 0;
        while (!mMessageHandler.queue.isEmpty()) {
            BaseMessage message = mMessageHandler.queue.poll();
            if (message instanceof DocumentStateChangedImpl) {
                if (DocumentState.FINISHED.toString().equals(((DocumentStateChangedImpl) message).getState())) {
                    documentStateChangedNotificationFinished++;
                }
            }
        }

        assertEquals(2, documentStateChangedNotificationFinished);
    }

    private void assertDocumentInflatedAndPreparedNotificationSent() {
        assertFalse(mRuntimeInteractionWorker.isEmpty());
        mRuntimeInteractionWorker.flush();
        assert(mMessageHandler.queue.size() > 0);
        int documentStateChangeNotificationInflated = 0;
        int documentStateChangeNotificationPrepared = 0;
        while (!mMessageHandler.queue.isEmpty()) {
            BaseMessage message = mMessageHandler.queue.poll();
            if (message instanceof DocumentStateChangedImpl) {
                if (DocumentState.INFLATED.toString().equals(((DocumentStateChangedImpl) message).getState())) {
                    documentStateChangeNotificationInflated++;
                } else if (DocumentState.PREPARED.toString().equals(((DocumentStateChangedImpl) message).getState())) {
                    documentStateChangeNotificationPrepared++;
                }
            }
        }
        //There are 2 documents and we send 2 state changed(PREPARED and INFLATED) notification per doc
        assertEquals(2, documentStateChangeNotificationInflated);
        assertEquals(2, documentStateChangeNotificationPrepared);
    }


    private class EmbeddedDocumentFactoryTest implements EmbeddedDocumentFactory {
        private final Viewhost mViewhost;
        EmbeddedDocumentFactoryTest(Viewhost viewhost) {
            this.mViewhost = viewhost;
        }
        @Override
        public void onDocumentRequested(EmbeddedDocumentRequest request) {
            JsonStringDecodable document = null;
            if ("documentA".equals(request.getSource())) {
                document = new JsonStringDecodable(EMBEDDED_DOC_1);
            } else {
                document = new JsonStringDecodable(EMBEDDED_DOC_WITH_EXTENSION);
            }
            when(mDocumentOptions.getExtensionGrantRequestCallback()).thenReturn(mExtensionGrantRequestCallback);
            PrepareDocumentRequest prepareDocumentRequest = PrepareDocumentRequest.builder()
                    .document(document)
                    .documentSession(DocumentSession.create())
                    .build();

            PreparedDocument preparedDocument = mViewhost.prepare(prepareDocumentRequest);
            assertNotNull(preparedDocument.getHandle());
            mEmbeddedDocuments.put(request.getSource(), preparedDocument.getHandle());
            ((EmbeddedDocumentRequestImpl)request).setIsVisualContextConnected(true);

            EmbeddedDocumentResponse response = EmbeddedDocumentResponse.builder()
                    .preparedDocument(preparedDocument)
                    .visualContextAttached(false)
                    .build();
            request.resolve(response);
            assertNotNull(((DocumentHandleImpl) mEmbeddedDocuments.get(request.getSource())).getDocumentContext());
            if ("documentB".equals(request.getSource())) {
                assertNotNull(((DocumentHandleImpl) mEmbeddedDocuments.get("documentB")).getExtensionMediator());
            }
        }
    }
}
