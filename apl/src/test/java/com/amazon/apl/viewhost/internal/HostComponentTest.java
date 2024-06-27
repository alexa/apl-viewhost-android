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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amazon.alexaext.ExtensionRegistrar;
import com.amazon.alexaext.IExtensionProvider;
import com.amazon.apl.android.APLJSONData;
import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.Content;
import com.amazon.apl.android.DocumentSession;
import com.amazon.apl.android.ExtensionMediator;
import com.amazon.apl.android.ExtensionMediator.IExtensionGrantRequestCallback;
import com.amazon.apl.android.MultiChildComponent;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.Text;
import com.amazon.apl.android.configuration.ConfigurationChange;
import com.amazon.apl.android.dependencies.IContentRetriever;
import com.amazon.apl.android.dependencies.IMediaPlayer;
import com.amazon.apl.android.dependencies.IPackageLoader;
import com.amazon.apl.android.dependencies.IUserPerceivedFatalCallback;
import com.amazon.apl.android.document.AbstractDocUnitTest;
import com.amazon.apl.android.events.RefreshEvent;
import com.amazon.apl.android.media.RuntimeMediaPlayerFactory;
import com.amazon.apl.android.metrics.ICounter;
import com.amazon.apl.android.metrics.MetricsOptions;
import com.amazon.apl.android.providers.AbstractMediaPlayerProvider;
import com.amazon.apl.android.providers.ITelemetryProvider;
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
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class HostComponentTest extends AbstractDocUnitTest {

    // Main
    private static final String DOC = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"2022.3\",\n" +
            "  \"mainTemplate\": {\n" +
            "    \"item\": {\n" +
            "      \"type\": \"Container\",\n" +
            "      \"items\": [\n" +
            "        {\n" +
            "          \"type\": \"Host\",\n" +
            "          \"source\": \"embeddedWithVideo\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"type\": \"Host\",\n" +
            "          \"source\": \"embeddedWithExtension\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"type\": \"Host\",\n" +
            "          \"source\": \"embeddedWithConditionalImport\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  }\n" +
            "}\n";


    private static final String HOST_WITH_CONDITIONAL_EMBEDDED = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"2022.3\",\n" +
            "  \"mainTemplate\": {\n" +
            "    \"item\": {\n" +
            "      \"type\": \"Container\",\n" +
            "      \"items\": [\n" +
            "        {\n" +
            "          \"type\": \"Host\",\n" +
            "          \"source\": \"embeddedWithConditionalImport\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  }\n" +
            "}\n";

    // Embedded
    private static final String EMBEDDED_WITH_VIDEO = "{\n" +
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
    private static final String EMBEDDED_DOC_WITH_CONDITIONAL_IMPORTS = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"1.6\",\n" +
            "  \"theme\": \"dark\",\n" +
            "  \"import\": [\n" +
            "    {\n" +
            "      \"when\": \"${environment.key == 'value'}\",\n" +
            "      \"name\": \"test-package\",\n" +
            "      \"version\": \"1.2.0\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"onConfigChange\": [\n" +
            "    {\n" +
            "      \"type\": \"Reinflate\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"mainTemplate\": {\n" +
            "    \"items\": [\n" +
            "      {\n" +
            "        \"type\": \"Frame\"\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";
    private final String mTestPackage = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.0\"" +
            "}";

    private static final String CONTROL_MEDIA_COMMAND = "[{\n" +
            "  \"type\": \"ControlMedia\",\n" +
            "  \"componentId\": \"VideoPlayer\",\n" +
            "  \"command\": \"%s\",\n" +
            "  \"value\": %d\n" +
            "}]";

    private static final String HOST_WITH_REMOVEITEM = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"2023.1\",\n" +
            "  \"mainTemplate\": {\n" +
            "    \"items\": [{\n" +
            "      \"type\": \"Container\",\n" +
            "      \"width\": \"100%\",\n" +
            "      \"host\": \"100%\",\n" +
            "      \"items\": {\n" +
            "        \"type\": \"Host\",\n" +
            "        \"id\": \"host\",\n" +
            "        \"source\": \"embeddedWithVideo\",\n" +
            "        \"height\": \"100%\",\n" +
            "        \"width\": \"100%\"\n" +
            "      }\n" +
            "    }]\n" +
            "  },\n" +
            "  \"onMount\": {\n" +
            "    \"type\": \"RemoveItem\",\n" +
            "    \"componentId\": \"host\"\n" +
            "  }\n" +
            "}";

    private static final String HOST_SAME_DOC = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"2022.3\",\n" +
            "  \"mainTemplate\": {\n" +
            "    \"item\": {\n" +
            "      \"type\": \"Container\",\n" +
            "      \"items\": [\n" +
            "        {\n" +
            "          \"type\": \"Host\",\n" +
            "          \"source\": \"embeddedWithRemoteExtension\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"type\": \"Host\",\n" +
            "          \"source\": \"embeddedWithRemoteExtension\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"type\": \"Host\",\n" +
            "          \"source\": \"embeddedWithRemoteExtension\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  }\n" +
            "}\n";

    private static final String EMBEDDED_DOC_REMOTE_EXTENSION = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"1.7\",\n" +
            "  \"extensions\": [\n" +
            "    {\n" +
            "      \"uri\": \"example:remote:10\",\n" +
            "      \"name\": \"Remote\"\n" +
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
            "              \"text\": \"${environment.extension.Remote ? 'Remote' : 'No Remote'}\"\n" +
            "            }\n" +
            "          ]\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  }\n" +
            "}";

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
    @Mock
    IPackageLoader mPackageLoader;
    @Mock
    ExtensionMediator mMediator;
    @Mock
    private ITelemetryProvider mTelemetryProvider;

    private CapturingMessageHandler mMessageHandler;
    private Viewhost mViewhost;
    private APLOptions mOptions;
    private ManualExecutor mRuntimeInteractionWorker;
    @Mock
    private ICounter mCounter;

    @Before
    public void setup() {
        when(mMetricsRecorder.createCounter(anyString())).thenReturn(mCounter);
        doAnswer(invocation -> {
            Content.ImportRequest request = invocation.getArgument(0);
            IContentRetriever.SuccessCallback<Content.ImportRequest, APLJSONData> successCallback = invocation.getArgument(1);
            if ("test-package".equals(request.getPackageName())) {
                successCallback.onSuccess(request, APLJSONData.create(mTestPackage));
            }
            return null;
        }).when(mPackageLoader).fetch(any(), any(), any());
        doAnswer(invocation -> {
            ExtensionMediator.ILoadExtensionCallback callback = invocation.getArgument(2);
            callback.onSuccess().run();
            return null;
        }).when(mMediator).loadExtensions(any(Map.class), any(Content.class), any(ExtensionMediator.ILoadExtensionCallback.class));
        when(mCoreWorker.post(any(Runnable.class))).thenAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        });
        initialize();
    }

    private void initialize() {
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
                .IPackageLoader(mPackageLoader)
                .extensionRegistrar(extensionRegistrar)
                .defaultDocumentOptions(mDocumentOptions)
                .build();
        mViewhost = Mockito.spy(new ViewhostImpl(config, mRuntimeInteractionWorker, mCoreWorker));
        EmbeddedDocumentFactory factory = new EmbeddedDocumentFactoryTest(mViewhost);
        mRootConfig.setDocumentManager(factory, mCoreWorker, mTelemetryProvider);
        mOptions = APLOptions.builder()
                .embeddedDocumentFactory(factory)
                .viewhost(mViewhost)
                .packageLoader(mPackageLoader)
                .build();
    }

    private static final String HOST_DOC_WITH_PARAMETERS_AND_DATA = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"2022.2\",\n" +
            "  \"mainTemplate\": {\n" +
            "    \"item\": {\n" +
            "      \"type\": \"Container\",\n" +
            "      \"items\": [\n" +
            "        {\n" +
            "          \"type\": \"Host\",\n" +
            "          \"source\": \"embeddedDocWithParameter\",\n" +
            "          \"parameters\": {\n" +
            "            \"Location\": \"${data}\"\n" +
            "          }\n" +
            "        }],\n" +
            "      \"data\": [\n" +
            "        \"world\"\n" +
            "      ]\n" +
            "    }\n" +
            "  }\n" +
            "}";
    private static final String EMBEDDED_DOC_WITH_EXPLICIT_PARAMETER = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"2022.3\",\n" +
            "  \"mainTemplate\": {\n" +
            "    \"parameters\": [\"Location\",\"myDocumentData\"],\n" +
            "    \"items\": [\n" +
            "      {\n" +
            "        \"type\": \"Container\",\n" +
            "        \"items\": [\n" +
            "          {\n" +
            "            \"text\": \"Hello, ${Location} and ${myDocumentData.name}!\",\n" +
            "            \"type\": \"Text\",\n" +
            "            \"id\": \"id\"\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";
    private static final String EMBEDDED_DOC_DATA = "{\n" +
            "  \"myDocumentData\": {\n" +
            "    \"name\": \"Alexa\"\n" +
            "  }\n" +
            "}";
    @Test
    public void testExplicitParameterLoadSuccessful() {
        loadDocument(HOST_DOC_WITH_PARAMETERS_AND_DATA, mOptions);
        DocumentHandleImpl handle = (DocumentHandleImpl) mEmbeddedDocuments.get("embeddedDocWithParameter");
        assertNotNull(handle);
        assertTrue(handle.getContent().isReady());
        Text component = (Text) mRootContext.findComponentById("id");
        assertEquals(component.getText(), "Hello, world and Alexa!");
    }

    private static final String HOST_DOC_WITH_BACKGROUND = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"1.1\",\n" +
            "  \"mainTemplate\": {\n" +
            "    \"item\": {\n" +
            "      \"type\": \"Container\",\n" +
            "      \"items\": [\n" +
            "        {\n" +
            "          \"type\": \"Host\",\n" +
            "          \"source\": \"embeddedDocWithBackground\",\n" +
            "          \"id\" : \"hostId\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  }\n" +
            "}\n";
    private static final String EMBEDDED_DOC_WITH_BACKGROUND = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"1.1\",\n" +
            "  \"background\": \"blue\",\n" +
            "  \"mainTemplate\": {\n" +
            "    \"item\":\n" +
            "    {\n" +
            "      \"type\": \"Text\",\n" +
            "      \"text\": \"Hello\"\n" +
            "    }\n" +
            "  }\n" +
            "}\n";

    @Test
    public void testHostDocumentBackgroundLoadedSuccess() {
        loadDocument(HOST_DOC_WITH_BACKGROUND, mOptions);
        DocumentHandleImpl handle = (DocumentHandleImpl) mEmbeddedDocuments.get("embeddedDocWithBackground");
        assertNotNull(handle);
        assertTrue(handle.getContent().isReady());
        MultiChildComponent component = (MultiChildComponent) mRootContext.findComponentById("hostId");
        assertEquals(component.getBackgroundColor(), Color.BLUE);
    }

    @Test
    public void testExecuteCommand() {
        loadDocument(DOC, mOptions);
        DocumentHandle embeddedWithVideo = mEmbeddedDocuments.get("embeddedWithVideo");
        assertNotNull(embeddedWithVideo);
        ExecuteCommandsRequest request = ExecuteCommandsRequest.builder()
                .commands(new JsonStringDecodable(getCommand("play", 0)))
                .callback(mCallback)
                .build();
        assertTrue(embeddedWithVideo.executeCommands(request));
    }

    @Test
    public void testConditionalImport_Reinflate() {
        // When
        loadDocument(HOST_WITH_CONDITIONAL_EMBEDDED, mOptions);

        DocumentHandleImpl embeddedWithConditionalImport = (DocumentHandleImpl) mEmbeddedDocuments.get("embeddedWithConditionalImport");
        assertNotNull(embeddedWithConditionalImport);
        Content content = embeddedWithConditionalImport.getContent();

        // Expected before inflation
        verify(mMediator).loadExtensions(any(Map.class), any(Content.class), any(ExtensionMediator.ILoadExtensionCallback.class));
        assertTrue(embeddedWithConditionalImport.getDocumentContext() != null);
        assertTrue(content.isReady());

        // Re-inflate
        ConfigurationChange configChange = mRootContext.createConfigurationChange().environmentValue("key", "value")
                .build();
        mRootContext.handleConfigurationChange(configChange);
        update(100);

        // Re-inflate was handled as expected
        verify((ViewhostImpl)mViewhost).interceptEventIfNeeded(any(RefreshEvent.class));
        verify(mMediator, times(2)).loadExtensions(any(Map.class), any(Content.class), any(ExtensionMediator.ILoadExtensionCallback.class));
        assertTrue(embeddedWithConditionalImport.getDocumentContext() != null);
        assertTrue(content.isReady());
    }

    @Test
    public void testConditionalImport() {
        // When
        loadDocument(HOST_WITH_CONDITIONAL_EMBEDDED, mOptions);

        DocumentHandleImpl embeddedWithConditionalImport = (DocumentHandleImpl) mEmbeddedDocuments.get("embeddedWithConditionalImport");
        assertNotNull(embeddedWithConditionalImport);
        Content content = embeddedWithConditionalImport.getContent();

        // Refresh was called, and loadExtension was called
        verify(mMediator).loadExtensions(any(Map.class), any(Content.class), any(ExtensionMediator.ILoadExtensionCallback.class));
        assertTrue(content.isReady());
        assertTrue(embeddedWithConditionalImport.getDocumentContext() != null);
    }

    private static String getCommand(final String command, final int value) {
        return String.format(CONTROL_MEDIA_COMMAND, command, value);
    }

    @Test
    public void testDocumentStateChangedNotified() {
        loadDocument(DOC, mOptions);
        assertDocumentInflatedAndPreparedNotificationSent();

        mRootContext.finishDocument();

        assertDocumentFinishedNotificationSent(3);
    }

    @Test
    public void testHostComponentReleased_beforeEmbeddedDocumentSuccess_setsEmbeddedDocumentToFinished() {
        // given
        ControlledTestEmbeddedFactory factory = new ControlledTestEmbeddedFactory(mViewhost);
        mRootConfig.setDocumentManager(factory, mCoreWorker, mTelemetryProvider);
        mOptions = APLOptions.builder()
                .embeddedDocumentFactory(factory)
                .viewhost(mViewhost)
                .build();

        // when the Host is removed
        loadDocument(HOST_WITH_REMOVEITEM, mOptions);
        // before the success callback is invoked
        factory.resolveRequest();

        // then the embedded Document is set to a Finished state
        assertDocumentFinishedNotificationSent(1);
    }

    private void assertDocumentFinishedNotificationSent(int count) {
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
        assertEquals(count, documentStateChangedNotificationFinished);
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
        //There are 3 documents and we send 3 state changed(PREPARED and INFLATED) notification per doc
        assertEquals(3, documentStateChangeNotificationInflated);
        assertEquals(3, documentStateChangeNotificationPrepared);
    }

    @Test
    public void testMultipleHostsMultipleExtensions(){
        MultipleExtensionsDocumentFactoryTest factory = new MultipleExtensionsDocumentFactoryTest(mViewhost);
        mRootConfig.setDocumentManager(factory, mCoreWorker, mTelemetryProvider);
        mOptions = APLOptions.builder()
                .embeddedDocumentFactory(factory)
                .viewhost(mViewhost)
                .build();
        // assertions happen in the Factory so just load the document
        loadDocument(HOST_SAME_DOC, mOptions);
    }

    @Test
    public void testNullDocumentOptions() {
        ViewhostConfig config = ViewhostConfig.builder()
                .defaultDocumentOptions(null)
                .build();
        mViewhost = new ViewhostImpl(config, mRuntimeInteractionWorker, mCoreWorker);
        EmbeddedDocumentFactory factory = new NullDocumentOptionsTest(mViewhost);
        mRootConfig.setDocumentManager(factory, mCoreWorker, mTelemetryProvider);
        mOptions = APLOptions.builder()
                .embeddedDocumentFactory(factory)
                .viewhost(mViewhost)
                .build();
        loadDocument(DOC, mOptions);
    }

    private class NullDocumentOptionsTest implements EmbeddedDocumentFactory {
        private final Viewhost mViewhost;

        NullDocumentOptionsTest(Viewhost viewhost){
            mViewhost = viewhost;
        }

        @Override
        public void onDocumentRequested(EmbeddedDocumentRequest request) {
            JsonStringDecodable document = new JsonStringDecodable(EMBEDDED_WITH_VIDEO);;

            PrepareDocumentRequest prepareDocumentRequest = PrepareDocumentRequest.builder()
                    .document(document)
                    .documentSession(DocumentSession.create())
                    .documentOptions(null)
                    .build();

            PreparedDocument preparedDocument = mViewhost.prepare(prepareDocumentRequest);
            assertNotNull(preparedDocument.getHandle());
            mEmbeddedDocuments.put(request.getSource(), preparedDocument.getHandle());

            EmbeddedDocumentResponse response = EmbeddedDocumentResponse.builder()
                    .preparedDocument(preparedDocument)
                    .visualContextAttached(false)
                    .build();
            request.resolve(response);
            assertNotNull(((DocumentHandleImpl) mEmbeddedDocuments.get(request.getSource())).getDocumentContext());
        }
    }
    
    private class MultipleExtensionsDocumentFactoryTest implements EmbeddedDocumentFactory {
        private final Viewhost mViewhost;

        MultipleExtensionsDocumentFactoryTest(Viewhost viewhost){
            this.mViewhost = viewhost;
        }

        @Override
        public void onDocumentRequested(EmbeddedDocumentRequest request) {
            JsonStringDecodable document = null;
            if ("embeddedWithRemoteExtension".equals(request.getSource())) {
                document = new JsonStringDecodable(EMBEDDED_DOC_REMOTE_EXTENSION);
            }

            ExtensionRegistrar extensionRegistrar = new ExtensionRegistrar().addProvider(mExtensionProvider);

            when(mDocumentOptions.getExtensionGrantRequestCallback()).thenReturn(mExtensionGrantRequestCallback);
            when(mDocumentOptions.getExtensionRegistrar()).thenReturn(extensionRegistrar);

            PrepareDocumentRequest prepareDocumentRequest = PrepareDocumentRequest.builder()
                    .document(document)
                    .documentSession(DocumentSession.create())
                    .documentOptions(mDocumentOptions)
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

            // all embedded documents are embeddedWithRemoteExtension
            if ("embeddedWithRemoteExtension".equals(request.getSource())) {
                assertNotNull(((DocumentHandleImpl) mEmbeddedDocuments.get("embeddedWithRemoteExtension")).getExtensionMediator());
            }
        }
    }

    private class ControlledTestEmbeddedFactory implements EmbeddedDocumentFactory {
        private Runnable mRequestResolver;
        private final Viewhost mViewhost;

        ControlledTestEmbeddedFactory(Viewhost viewhost) {
            this.mViewhost = viewhost;
        }

        public void resolveRequest() {
            mRequestResolver.run();
        }

        @Override
        public void onDocumentRequested(EmbeddedDocumentRequest request) {
            JsonStringDecodable document = new JsonStringDecodable(EMBEDDED_WITH_VIDEO);
            PrepareDocumentRequest prepareDocumentRequest = PrepareDocumentRequest.builder()
                    .document(document)
                    .documentSession(DocumentSession.create())
                    .build();
            PreparedDocument preparedDocument = mViewhost.prepare(prepareDocumentRequest);
            mEmbeddedDocuments.put(request.getSource(), preparedDocument.getHandle());
            ((EmbeddedDocumentRequestImpl)request).setIsVisualContextConnected(true);

            EmbeddedDocumentResponse response = EmbeddedDocumentResponse.builder()
                    .preparedDocument(preparedDocument)
                    .visualContextAttached(false)
                    .build();

            mRequestResolver = () -> request.resolve(response);
        }
    }

    private class EmbeddedDocumentFactoryTest implements EmbeddedDocumentFactory {
        private final Viewhost mViewhost;
        EmbeddedDocumentFactoryTest(Viewhost viewhost) {
            this.mViewhost = viewhost;
        }
        @Override
        public void onDocumentRequested(EmbeddedDocumentRequest request) {
            JsonStringDecodable document = null;
            PrepareDocumentRequest.Builder builder = PrepareDocumentRequest.builder();
            if ("embeddedWithVideo".equals(request.getSource())) {
                document = new JsonStringDecodable(EMBEDDED_WITH_VIDEO);
            } else if ("embeddedWithExtension".equals(request.getSource())){
                document = new JsonStringDecodable(EMBEDDED_DOC_WITH_EXTENSION);
            } else if ("embeddedWithConditionalImport".equals(request.getSource())){
                document = new JsonStringDecodable(EMBEDDED_DOC_WITH_CONDITIONAL_IMPORTS);
            } else if ("embeddedDocWithParameter".equals(request.getSource())) {
                document = new JsonStringDecodable(EMBEDDED_DOC_WITH_EXPLICIT_PARAMETER);
                builder.data(new JsonStringDecodable(EMBEDDED_DOC_DATA));
            } else if ("embeddedDocWithBackground".equals(request.getSource())){
                document = new JsonStringDecodable(EMBEDDED_DOC_WITH_BACKGROUND);
            }

            ExtensionRegistrar extensionRegistrar = new ExtensionRegistrar().addProvider(mExtensionProvider);
            when(mDocumentOptions.getExtensionGrantRequestCallback()).thenReturn(mExtensionGrantRequestCallback);
            when(mDocumentOptions.getExtensionRegistrar()).thenReturn(extensionRegistrar);

            PrepareDocumentRequest prepareDocumentRequest = builder
                    .document(document)
                    .documentSession(DocumentSession.create())
                    .documentOptions(mDocumentOptions)
                    .build();

            PreparedDocument preparedDocument = mViewhost.prepare(prepareDocumentRequest);
            assertNotNull(preparedDocument.getHandle());

            DocumentHandleImpl documentHandle = (DocumentHandleImpl) preparedDocument.getHandle();
            documentHandle.setExtensionMediator(mMediator);

            documentHandle.setDocumentOptions(new DocumentOptions() {
                @Nullable
                @Override
                public IExtensionGrantRequestCallback getExtensionGrantRequestCallback() {
                    return uri -> true;
                }

                @Nullable
                @Override
                public ExtensionRegistrar getExtensionRegistrar() {
                    return null;
                }

                @Nullable
                @Override
                public Map<String, Object> getExtensionFlags() {
                    return null;
                }

                @Nullable
                @Override
                public ITelemetryProvider getTelemetryProvider() {
                    return null;
                }

                @Nullable
                @Override
                public MetricsOptions getMetricsOptions() {
                    return null;
                }

                @Nullable
                @Override
                public EmbeddedDocumentFactory getEmbeddedDocumentFactory() {
                    return null;
                }

                @Nullable
                @Override
                public IUserPerceivedFatalCallback getUserPerceivedFatalCallback() {
                    return null;
                }
            });

            mEmbeddedDocuments.put(request.getSource(), preparedDocument.getHandle());
            ((EmbeddedDocumentRequestImpl)request).setIsVisualContextConnected(true);

            EmbeddedDocumentResponse response = EmbeddedDocumentResponse.builder()
                    .preparedDocument(preparedDocument)
                    .visualContextAttached(false)
                    .build();
            request.resolve(response);
            assertNotNull(((DocumentHandleImpl) mEmbeddedDocuments.get(request.getSource())).getDocumentContext());

            if ("embeddedWithExtension".equals(request.getSource())) {
                assertNotNull(((DocumentHandleImpl) mEmbeddedDocuments.get("embeddedWithExtension")).getExtensionMediator());
            }
        }
    }
}
