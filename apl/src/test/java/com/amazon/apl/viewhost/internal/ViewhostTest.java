/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.viewhost.internal;

import static com.amazon.apl.viewhost.config.EmbeddedDocumentFactory.EmbeddedDocumentRequest;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.amazon.alexaext.ExtensionRegistrar;
import com.amazon.alexaext.IExtensionProvider;
import com.amazon.apl.android.APLLayout;
import com.amazon.apl.android.Action;
import com.amazon.apl.android.Content;
import com.amazon.apl.android.DocumentSession;
import com.amazon.apl.android.ExtensionMediator;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.Session;
import com.amazon.apl.android.UserPerceivedFatalReporter;
import com.amazon.apl.android.dependencies.IAPLSessionListener;
import com.amazon.apl.android.dependencies.IDataSourceFetchCallback;
import com.amazon.apl.android.dependencies.IOpenUrlCallback;
import com.amazon.apl.android.dependencies.ISendEventCallbackV2;
import com.amazon.apl.android.dependencies.IUserPerceivedFatalCallback;
import com.amazon.apl.android.document.AbstractDocUnitTest;
import com.amazon.apl.android.events.DataSourceFetchEvent;
import com.amazon.apl.android.events.OpenURLEvent;
import com.amazon.apl.android.events.SendEvent;
import com.amazon.apl.android.extension.IExtensionRegistration;
import com.amazon.apl.android.scaling.ViewportMetrics;
import com.amazon.apl.android.shadow.ShadowBitmapRenderer;
import com.amazon.apl.android.utils.APLTrace;
import com.amazon.apl.devtools.DevToolsProvider;
import com.amazon.apl.devtools.models.ViewTypeTarget;
import com.amazon.apl.enums.DisplayState;
import com.amazon.apl.enums.ScreenShape;
import com.amazon.apl.enums.ViewportMode;
import com.amazon.apl.viewhost.DocumentHandle;
import com.amazon.apl.viewhost.PreparedDocument;
import com.amazon.apl.viewhost.TimeProvider;
import com.amazon.apl.viewhost.Viewhost;
import com.amazon.apl.viewhost.config.DocumentOptions;
import com.amazon.apl.viewhost.config.EmbeddedDocumentFactory;
import com.amazon.apl.viewhost.config.EmbeddedDocumentResponse;
import com.amazon.apl.viewhost.config.ViewhostConfig;
import com.amazon.apl.viewhost.example.ExampleDocumentFactory;
import com.amazon.apl.viewhost.internal.message.notification.DocumentStateChangedImpl;
import com.amazon.apl.viewhost.internal.message.notification.ScreenLockStatusChangedImpl;
import com.amazon.apl.viewhost.message.BaseMessage;
import com.amazon.apl.viewhost.message.Message;
import com.amazon.apl.viewhost.message.action.FetchDataRequest;
import com.amazon.apl.viewhost.message.action.OpenURLRequest;
import com.amazon.apl.viewhost.message.action.ReportRuntimeErrorRequest;
import com.amazon.apl.viewhost.message.action.SendUserEventRequest;
import com.amazon.apl.viewhost.message.notification.VisualContextChanged;
import com.amazon.apl.viewhost.primitives.JsonStringDecodable;
import com.amazon.apl.viewhost.request.ExecuteCommandsRequest;
import com.amazon.apl.viewhost.request.FinishDocumentRequest;
import com.amazon.apl.viewhost.request.PrepareDocumentRequest;
import com.amazon.apl.viewhost.request.RenderDocumentRequest;
import com.amazon.apl.viewhost.request.UpdateDataSourceRequest;
import com.amazon.apl.viewhost.request.UpdateDataSourceRequest.UpdateDataSourceCallback;
import com.amazon.apl.viewhost.utils.CapturingMessageHandler;
import com.amazon.apl.viewhost.utils.ManualExecutor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class ViewhostTest extends AbstractDocUnitTest {
    @Mock
    private DocumentHandle mDocumentHandle;
    @Mock
    private DocumentHandleImpl mDocumentHandleImpl;
    @Mock
    private DocumentContext mDocumentContext;

    @Mock
    private APLLayout.APLViewPresenterImpl mViewPresenter;

    @Mock
    private DevToolsProvider mDevToolsProvider;

    @Mock
    private IAPLSessionListener mAPLSessionListener;

    @Mock
    private ViewTypeTarget mDTView;

    private Viewhost mViewhost;

    private Viewhost mViewhost2;

    private Viewhost mViewhost3;
    private CapturingMessageHandler mMessageHandler;
    private ManualExecutor mRuntimeInteractionWorker;
    @Mock
    private IExtensionProvider mExtensionProvider;
    @Mock
    private TimeProvider mTimeProvider;
    private DocumentOptions mDocumentOptions;
    @Mock
    ExtensionMediator.IExtensionGrantRequestCallback mExtensionGrantRequestCallback;
    @Mock
    ExtensionMediator mMediator;
    @Mock
    protected ShadowBitmapRenderer mockShadowRenderer;
    @Mock
    private UpdateDataSourceCallback mCallback;

    @Mock
    private Handler mCoreWorker;

    @Mock
    private RootContext mRootContext;

    @Mock
    private Viewhost.ExtensionEventHandlerCallback mExtensionEventHandlerCallback;

    @Mock
    private Action mAction;

    @Mock
    private IUserPerceivedFatalCallback mUserPerceivedFatalCallback;

    @Mock
    private IExtensionRegistration mLegacyExtensionRegistration;

    private APLLayout mAplLayout;

    private ExtensionRegistrar mExtensionRegistrar;

    private static final String SIMPLE_DOC = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"2023.3\"," +
            "  \"mainTemplate\": {" +
            "    \"item\":" +
            "    {" +
            "      \"type\": \"Frame\"" +
            "    }" +
            "  }" +
            "}";
    private static final String SIMPLE_DOC_WITH_SEND_EVENT = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"1.1\",\n" +
            "  \"onMount\": [\n" +
            "    {\n" +
            "      \"type\": \"SendEvent\",\n" +
            "      \"arguments\": [\n" +
            "        \"primary\",\n" +
            "        \"document\"\n" +
            "      ]\n" +
            "    }\n" +
            "  ],\n" +
            "  \"mainTemplate\": {\n" +
            "    \"item\": {\n" +
            "      \"type\": \"Frame\"\n" +
            "    }\n" +
            "  }\n" +
            "}";
    private static final String INVALID_DOC = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"2023.3\"," +
            "  \"mainTemplate\": " +
            "    \"item\":" +
            "    {" +
            "      \"type\": \"Frame\"" +
            "    }" +
            "  }" +
            "}";
    private static final String SIMPLE_DOC_WITH_HOST_COMPONENT = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"2023.3\",\n" +
            "  \"mainTemplate\": {\n" +
            "    \"item\": {\n" +
            "      \"type\": \"Container\",\n" +
            "      \"items\": [\n" +
            "      \n" +
            "        {\n" +
            "          \"type\": \"Host\",\n" +
            "          \"source\": \"url\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  }\n" +
            "}\n";

    private static final String SIMPLE_DOC_WITH_PAYLOAD = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"2023.3\"," +
            "  \"mainTemplate\": {" +
            "    \"parameters\": [" +
            "      \"payload\"" +
            "    ]," +
            "    \"item\":" +
            "    {" +
            "      \"type\": \"Frame\"" +
            "    }" +
            "  }" +
            "}";
    private static final String SHOPPING_LIST_DOC = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"2023.2\"," +
            "  \"mainTemplate\": {" +
            "    \"parameters\": [" +
            "      \"shoppingListData\"" +
            "    ]," +
            "    \"items\": {" +
            "      \"type\": \"Sequence\"," +
            "      \"width\": \"100%\"," +
            "      \"height\": \"100%\"," +
            "      \"data\": \"${shoppingListData}\"," +
            "      \"items\": {" +
            "        \"type\": \"Text\"," +
            "        \"text\": \"${index + 1}. ${data.text}\"," +
            "        \"color\": \"white\"," +
            "        \"textAlign\": \"center\"," +
            "        \"textAlignVertical\": \"center\"" +
            "      }" +
            "    }" +
            "  }" +
            "}";
    private static final String SHOPPING_LIST_DATA = "{\n" +
            "  \"shoppingListData\" : {\n" +
            "    \"type\": \"dynamicIndexList\",\n" +
            "    \"listId\": \"shoppingListA\",\n" +
            "    \"startIndex\": 0,\n" +
            "    \"minimumInclusiveIndex\": 0,\n" +
            "    \"maximumExclusiveIndex\": 100,\n" +
            "    \"items\": []\n" +
            "  }\n" +
            "}";

    private static final String HELLO_WORLD = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"2023.2\"," +
            "  \"mainTemplate\": {" +
            "    \"items\": {" +
            "      \"type\": \"Text\"," +
            "      \"text\": \"Hello, World!\"," +
            "      \"entities\": [" +
            "        \"hello\"" +
            "      ]," +
            "      \"id\": \"text1\"," +
            "      \"color\": \"white\"," +
            "      \"textAlign\": \"center\"," +
            "      \"textAlignVertical\": \"center\"" +
            "    }" +
            "  }" +
            "}";

    private String mGoodbyeCommands;
    private ViewhostConfig mConfig;


    @Before
    public void setup() throws JSONException {
        ViewportMetrics metrics = ViewportMetrics.builder()
                .width(1280)
                .height(720)
                .dpi(160)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeHub)
                .build();
        
        mMessageHandler = new CapturingMessageHandler();
        mRuntimeInteractionWorker = new ManualExecutor();
        mAplLayout = spy(new APLLayout(RuntimeEnvironment.getApplication().getBaseContext(), null));
        mAplLayout.setAplViewPresenterForTesting(mViewPresenter);
        mExtensionRegistrar = new ExtensionRegistrar().addProvider(mExtensionProvider);

        mDocumentOptions = DocumentOptions.builder().extensionGrantRequestCallback(mExtensionGrantRequestCallback)
                .extensionRegistrar(mExtensionRegistrar)
                .userPerceivedFatalCallback(mUserPerceivedFatalCallback).build();
        when(mAplLayout.getPresenter()).thenReturn(mViewPresenter);
        when(mViewPresenter.getShadowRenderer()).thenReturn(mockShadowRenderer);
        when(mViewPresenter.getAPLTrace()).thenReturn(mock(APLTrace.class));
        when(mViewPresenter.getOrCreateViewportMetrics()).thenReturn(metrics);
        when(mMetricsRecorder.createCounter(anyString())).thenReturn(mCounter);
        when(mViewPresenter.metricsRecorder()).thenReturn(mMetricsRecorder);
        when(mViewPresenter.getDevToolsProvider()).thenReturn(mDevToolsProvider);
        when(mDevToolsProvider.getAPLSessionListener()).thenReturn(mAPLSessionListener);
        when(mDevToolsProvider.getDTView()).thenReturn(mDTView);
        when(mCoreWorker.getLooper()).thenReturn(Looper.getMainLooper());
        when(mCoreWorker.post(any(Runnable.class))).thenAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        });
        // Create new viewhost for handling embedded documents
        mConfig = ViewhostConfig.builder()
                .messageHandler(mMessageHandler)
                .extensionRegistrar(mExtensionRegistrar)
                .defaultDocumentOptions(mDocumentOptions)
                .timeProvider(mTimeProvider)
                .build();
        mViewhost = new ViewhostImpl(mConfig, mRuntimeInteractionWorker, mCoreWorker);
        mViewhost2 = new ViewhostImpl(mConfig, mRuntimeInteractionWorker, mCoreWorker);
        mViewhost3 = new ViewhostImpl(mConfig, mRuntimeInteractionWorker, mCoreWorker);

        // Create a command array JSON string, which changes the "Hello, World" text.
        mGoodbyeCommands = new JSONArray().put(new JSONObject()
                        .put("type", "SetValue")
                        .put("componentId", "text1")
                        .put("property", "text")
                        .put("value", "Goodbye!"))
                .toString();
    }

    @Test
    public void testDocumentFactory() {
        ExampleDocumentFactory factory = new ExampleDocumentFactory(mViewhost);

        // A ProvideDocument directive comes in before it's requested
        EmbeddedDocumentRequest requestA = mock(EmbeddedDocumentRequest.class);
        when(requestA.getSource()).thenReturn("uriA");
        factory.onProvideDocument("uriA", "documentA");
        factory.onDocumentRequested(requestA);
        verify(requestA, times(1)).resolve(any(PreparedDocument.class));

        // A ProvideDocument directive comes in after it's requested
        EmbeddedDocumentRequest requestB = mock(EmbeddedDocumentRequest.class);
        when(requestB.getSource()).thenReturn("uriB");
        factory.onDocumentRequested(requestB);
        factory.onProvideDocument("uriB", "documentB");
        verify(requestB, times(1)).resolve(any(PreparedDocument.class));
    }

    @Test
    public void testConstructor_background_coreWorker() {
        DocumentSession mockDocumentSession = spy(DocumentSession.create());
        PrepareDocumentRequest request = PrepareDocumentRequest.builder()
                .token("mytoken")
                .document(new JsonStringDecodable(SIMPLE_DOC))
                .data(new JsonStringDecodable("data"))
                .documentSession(mockDocumentSession)
                .documentOptions(DocumentOptions.builder().build())
                .build();
        Viewhost viewhost = new ViewhostImpl(mConfig);
        PreparedDocument preparedDocument = viewhost.prepare(request);
        DocumentHandle handle = preparedDocument.getHandle();
        assertNotNull(handle);
        ArgumentCaptor<Handler> handlerArgumentCaptor = ArgumentCaptor.forClass(Handler.class);
        verify(mockDocumentSession).setCoreWorker(handlerArgumentCaptor.capture());
        Thread thread = handlerArgumentCaptor.getValue().getLooper().getThread();
        assertEquals("BackgroundHandler", thread.getName());
        assertEquals(Thread.MAX_PRIORITY, thread.getPriority());
        assertNotNull(viewhost.render(preparedDocument));
    }

    @Test
    public void testPrepareDocument() {
        // Basic demonstration of a prepare document request, getting a document handle
        PrepareDocumentRequest request = PrepareDocumentRequest.builder()
                .token("mytoken")
                .document(new JsonStringDecodable(SIMPLE_DOC))
                .data(new JsonStringDecodable("data"))
                .documentSession(DocumentSession.create())
                .documentOptions(DocumentOptions.builder().build())
                .build();
        PreparedDocument preparedDocument = mViewhost.prepare(request);
        DocumentHandle handle = preparedDocument.getHandle();
        assertNotNull(handle);
        assertEquals("mytoken", preparedDocument.getToken());
        assertTrue(preparedDocument.hasToken());
        assertTrue(preparedDocument.isValid());
        assertTrue(preparedDocument.isReady());
        assertEquals(handle.getUniqueId(), preparedDocument.getUniqueID());

        assertNotNull(mViewhost.render(preparedDocument));

        // Could finish document given a handle
        FinishDocumentRequest finishRequest = FinishDocumentRequest.builder()
                .token("mytoken")
                .build();
        handle.finish(finishRequest);
    }

    @Test
    public void testPrepareDocument_reportUpfFatal_onContentCreationFailed() throws Content.ContentException {
        // Basic demonstration of a prepare document request, getting a document handle
        PrepareDocumentRequest request = PrepareDocumentRequest.builder()
                .token("mytoken")
                .document(new JsonStringDecodable(SIMPLE_DOC))
                .data(new JsonStringDecodable("data"))
                .documentSession(DocumentSession.create())
                .documentOptions(mDocumentOptions)
                .build();

        mockStatic(Content.class);
        Content.CallbackV2 callbackV2 = mock(Content.CallbackV2.class);

        doAnswer(invocation -> {
            Exception e = new Exception();
            ((Content.CallbackV2) invocation.getMock()).onError(e);
            return null;
        }).when(callbackV2).onError(any());

        when(Content.create(any(), any(), any(), any(Session.class), any(), eq(true), any())).then(invocation -> {
            Content.CallbackV2 callback = invocation.getArgument(2);
            callback.onError(new Exception());
            return null;
        });
        PreparedDocument preparedDocument = mViewhost.prepare(request);
        verify(mUserPerceivedFatalCallback, times(1)).onFatalError(eq(UserPerceivedFatalReporter.UpfReason.CONTENT_CREATION_FAILURE.toString()));
        verify(mUserPerceivedFatalCallback, times(0)).onFatalError(eq(UserPerceivedFatalReporter.UpfReason.REQUIRED_EXTENSION_LOADING_FAILURE.toString()));
    }

    @Test
    public void testRenderDocumentWithTimeProviderConfig() {
        prepareAndRender(SIMPLE_DOC, "");
        verify(mConfig.getTimeProvider()).getLocalTimeOffset();
    }

    @Test
    public void testPrepareDocument_reportUpfFatal_onRequiredExtensionFailure() throws Content.ContentException {
        ExtensionMediator extensionMediator = mock(ExtensionMediator.class);
        ExtensionRegistrar extensionRegistrar = mock(ExtensionRegistrar.class);
        ExtensionMediator.IExtensionGrantRequestCallback extensionGrantRequestCallback = mock(ExtensionMediator.IExtensionGrantRequestCallback.class);
        Content content = mock(Content.class);

        // Basic demonstration of a prepare document request, getting a document handle
        PrepareDocumentRequest request = PrepareDocumentRequest.builder()
                .token("mytoken")
                .document(new JsonStringDecodable(SIMPLE_DOC))
                .data(new JsonStringDecodable("data"))
                .documentSession(DocumentSession.create())
                .documentOptions(mDocumentOptions)
                .build();

        mockStatic(Content.class);
        Content.CallbackV2 callbackV2 = mock(Content.CallbackV2.class);

        doAnswer(invocation -> {
            ((Content.CallbackV2) invocation.getMock()).onComplete(content);
            return content;
        }).when(callbackV2).onComplete(any());

        when(Content.create(any(), any(), any(), any(Session.class), any(), eq(true), any())).then(invocation -> {
            Content.CallbackV2 callback = invocation.getArgument(2);
            callback.onComplete(content);
            return content;
        });

        mockStatic(ExtensionMediator.class);
        when(ExtensionMediator.create(any(), any())).thenReturn(extensionMediator);
        doAnswer(invocation -> {
            ExtensionMediator.ILoadExtensionCallback loadExtensionCallback = invocation.getArgument(2);
            loadExtensionCallback.onFailure().run();
            return null;
        }).when(extensionMediator).loadExtensions((Map<String, Object>) any(), any(), any());

        PreparedDocument preparedDocument = mViewhost.prepare(request);

        verify(mUserPerceivedFatalCallback, times(0)).onFatalError(eq(UserPerceivedFatalReporter.UpfReason.CONTENT_CREATION_FAILURE.toString()));
        verify(mUserPerceivedFatalCallback, times(1)).onFatalError(eq(UserPerceivedFatalReporter.UpfReason.REQUIRED_EXTENSION_LOADING_FAILURE.toString()));
    }

    @Test
    public void testPreparedDocument_withoutRender_FinishRequest() {
        PrepareDocumentRequest request = PrepareDocumentRequest.builder()
                .document(new JsonStringDecodable(SIMPLE_DOC))
                .documentSession(DocumentSession.create())
                .documentOptions(mDocumentOptions)
                .build();

        PreparedDocument preparedDocument = mViewhost.prepare(request);
        DocumentHandle handle = preparedDocument.getHandle();
        assertNotNull(handle);
        assertNull(((DocumentHandleImpl) handle).getRootContext());
        assertNull(((DocumentHandleImpl) handle).getDocumentContext());
        ((DocumentHandleImpl)handle).setExtensionMediator(mMediator);

        //finish request
        FinishDocumentRequest finishRequest = FinishDocumentRequest.builder()
                .build();
        boolean result = handle.finish(finishRequest);
        assertTrue(result);
        verify(mMediator).finish();
    }

    @Test
    public void testPreparedDocument_withoutRender_FinishRequest_InvalidToken() {
        PrepareDocumentRequest request = PrepareDocumentRequest.builder()
                .token("mytoken")
                .document(new JsonStringDecodable(SIMPLE_DOC))
                .documentSession(DocumentSession.create())
                .documentOptions(mDocumentOptions)
                .build();

        PreparedDocument preparedDocument = mViewhost.prepare(request);
        DocumentHandle handle = preparedDocument.getHandle();
        assertNotNull(handle);
        assertNull(((DocumentHandleImpl) handle).getRootContext());
        assertNull(((DocumentHandleImpl) handle).getDocumentContext());

        //finish request
        FinishDocumentRequest finishRequest = FinishDocumentRequest.builder()
                .token("tokenMistake")
                .build();
        boolean result = handle.finish(finishRequest);
        //result should be false when tokens are not matched
        assertFalse(result);
    }

    @Test
    public void testPreparedDocument_withoutRender_FinishRequest_noToken() {
        PrepareDocumentRequest request = PrepareDocumentRequest.builder()
                .token("mytoken")
                .document(new JsonStringDecodable(SIMPLE_DOC))
                .documentSession(DocumentSession.create())
                .documentOptions(mDocumentOptions)
                .build();

        PreparedDocument preparedDocument = mViewhost.prepare(request);
        DocumentHandle handle = preparedDocument.getHandle();
        assertNotNull(handle);
        assertNull(((DocumentHandleImpl) handle).getRootContext());
        assertNull(((DocumentHandleImpl) handle).getDocumentContext());

        //finish request
        FinishDocumentRequest finishRequest = FinishDocumentRequest.builder().build();
        boolean result = handle.finish(finishRequest);
        //result should be true when finishRequest do not have any token specified
        assertTrue(result);
    }

    @Test
    public void testPreparedDocument_Render_Finish() throws InterruptedException {
        CountDownLatch inflatedLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(1);
        mViewhost.registerStateChangeListener((state, handle) -> {
            if (state == DocumentState.INFLATED) {
                inflatedLatch.countDown();
            } else if (state == DocumentState.FINISHED) {
                finishLatch.countDown();
            }
        });
        DocumentHandle handle = prepareAndRender(SIMPLE_DOC, "");
        assertTrue(inflatedLatch.await(5, TimeUnit.SECONDS));

        //finish request
        FinishDocumentRequest finishRequest = FinishDocumentRequest.builder()
                .build();
        boolean result = handle.finish(finishRequest);
        assertTrue(result);
        assertTrue(finishLatch.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testHandlePayloadParameter() {
        PrepareDocumentRequest request = PrepareDocumentRequest.builder()
                .document(new JsonStringDecodable(SIMPLE_DOC_WITH_PAYLOAD))
                .data(new JsonStringDecodable(SHOPPING_LIST_DATA))
                .documentSession(DocumentSession.create())
                .documentOptions(DocumentOptions.builder().build())
                .build();

        PreparedDocument preparedDocument = mViewhost.prepare(request);

        Content content = ((DocumentHandleImpl)preparedDocument.getHandle()).getContent();

        Assert.assertTrue(content.isReady());
    }

    @Test
    public void testUpdateDisplayState() throws InterruptedException {
        CountDownLatch inflatedLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(1);
        mViewhost.registerStateChangeListener((state, handle) -> {
            if (state == DocumentState.INFLATED) {
                inflatedLatch.countDown();
            } else if (state == DocumentState.FINISHED) {
                finishLatch.countDown();
            }
        });
        DocumentHandleImpl handle = (DocumentHandleImpl)prepareAndRender(SIMPLE_DOC, "");
        inflatedLatch.await(5, TimeUnit.SECONDS);
        handle.getRootContext().resumeDocument();
        clearInvocations(mViewPresenter);

        mViewhost.updateDisplayState(DisplayState.kDisplayStateHidden);
        verify(mViewPresenter).onDocumentPaused();

        mViewhost.updateDisplayState(DisplayState.kDisplayStateForeground);
        verify(mViewPresenter).onDocumentResumed();

        // Switching to background shouldn't pause when at 2024.1 and above.
        mViewhost.updateDisplayState(DisplayState.kDisplayStateBackground);
        verifyNoMoreInteractions(mViewPresenter);
    }

    @Test
    public void testRenderPreparedDocumentError() throws InterruptedException {
        CountDownLatch preparedLatch = new CountDownLatch(1);
        CountDownLatch errorLatch = new CountDownLatch(1);
        mAplLayout.measure(View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY));
        mAplLayout.layout(0, 0, 640, 480);
        if (!mViewhost.isBound()) {
            mViewhost.bind(mAplLayout);
        }
        when(mAplLayout.getPresenter()).thenReturn(null);
        PrepareDocumentRequest request = PrepareDocumentRequest.builder()
                .document(new JsonStringDecodable(SIMPLE_DOC))
                .documentSession(DocumentSession.create())
                .documentOptions(DocumentOptions.builder().build())
                .build();
        mViewhost.registerStateChangeListener((state, handle) -> {
            if (state == DocumentState.PREPARED) {
                preparedLatch.countDown();
            } else if (state == DocumentState.ERROR) {
                errorLatch.countDown();
            }
        });
        PreparedDocument preparedDocument = mViewhost.prepare(request);
        DocumentHandle handle = mViewhost.render(preparedDocument);
        assertNotNull(handle);
        assertTrue(preparedLatch.await(1, TimeUnit.SECONDS));
        assertTrue(errorLatch.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testRenderPrepareDocument_viewUnbound_requestIgnored() {
        PrepareDocumentRequest request = PrepareDocumentRequest.builder()
                .document(new JsonStringDecodable(SIMPLE_DOC))
                .documentSession(DocumentSession.create())
                .documentOptions(DocumentOptions.builder().build())
                .build();
        PreparedDocument preparedDocument = mViewhost.prepare(request);
        DocumentHandle handle = mViewhost.render(preparedDocument);
        assertNotNull(handle);
        assertFalse(mViewhost.isBound());
        assertPrepared(handle);
    }

    @Test
    public void testRenderPreparedDocumentInflated() throws InterruptedException {
        Map<String, DocumentHandle> documentHandleMap = new HashMap<>();
        testRenderPreparedDocumentInflated(SIMPLE_DOC, documentHandleMap);
        assertEquals(1, documentHandleMap.size());
    }

    @Test
    public void testRenderPreparedDocumentWithEmbeddedDocsInflated() throws InterruptedException {
        Map<String, DocumentHandle> documentHandleMap = new HashMap<>();
        EmbeddedDocumentFactory factory = new EmbeddedDocumentFactoryTest(mViewhost, documentHandleMap);

        mDocumentOptions = DocumentOptions.builder().extensionGrantRequestCallback(mExtensionGrantRequestCallback)
                .extensionRegistrar(mExtensionRegistrar)
                .embeddedDocumentFactory(factory)
                .userPerceivedFatalCallback(mUserPerceivedFatalCallback).build();

        testRenderPreparedDocumentInflated(SIMPLE_DOC_WITH_HOST_COMPONENT, documentHandleMap);
        assertEquals(2, documentHandleMap.size());
    }

    private void testRenderPreparedDocumentInflated(String document, Map<String, DocumentHandle> documentHandleMap) throws InterruptedException {
        CountDownLatch inflatedLatch = new CountDownLatch(1);

        mViewhost.registerStateChangeListener((state, handle) -> {
            if (state == DocumentState.INFLATED) {
                inflatedLatch.countDown();
                documentHandleMap.put(handle.getUniqueId(), handle);
            }
        });
        prepareAndRender(document, "");
        assertTrue(inflatedLatch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testUpdateDataSourcePrimaryDocument() throws InterruptedException {
        String type = "dynamicIndexList";
        String payload = "{\n" +
                "   \"listId\":\"shoppingListA\",\n" +
                "   \"listVersion\":1,\n" +
                "   \"operations\":[\n" +
                "      {\n" +
                "         \"type\":\"DeleteMultipleItems\",\n" +
                "         \"index\":0,\n" +
                "         \"count\":999\n" +
                "      },\n" +
                "      {\n" +
                "         \"type\":\"InsertMultipleItems\",\n" +
                "         \"index\":0,\n" +
                "         \"items\":[\n" +
                "            {\n" +
                "               \"primaryText\":\"Updated item 1\"\n" +
                "            },\n" +
                "            {\n" +
                "               \"primaryText\":\"Updated item 2\"\n" +
                "            },\n" +
                "            {\n" +
                "               \"primaryText\":\"Updated item 3\"\n" +
                "            },\n" +
                "            {\n" +
                "               \"primaryText\":\"Updated item 4\"\n" +
                "            },\n" +
                "            {\n" +
                "               \"primaryText\":\"Updated item 5\"\n" +
                "            }\n" +
                "         ]\n" +
                "      }\n" +
                "   ]\n" +
                "}";
        UpdateDataSourceRequest updateDataSourceRequest = UpdateDataSourceRequest
                .builder()
                .data(new JsonStringDecodable(payload))
                .type(type)
                .callback(mCallback)
                .build();
        CountDownLatch updateDataSourceLatch = new CountDownLatch(1);
        mViewhost.registerStateChangeListener((state, handle) -> {
            if (state == DocumentState.INFLATED) {
                assertTrue(handle.updateDataSource(updateDataSourceRequest));
                updateDataSourceLatch.countDown();
            }
        });
        prepareAndRender(SHOPPING_LIST_DOC, SHOPPING_LIST_DATA);
        assertTrue(updateDataSourceLatch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testRenderDocument_callsPrepareAndRender() {
        // This test verifies that the correct calls are made since render(RenderDocumentRequest)
        // is essentially a chaining of a prepare(PrepareDocumentRequest) and render(PreparedDocument)
        ViewhostImpl mockViewhost = spy((ViewhostImpl) mViewhost);
        ArgumentCaptor<PrepareDocumentRequest> captor = ArgumentCaptor.forClass(PrepareDocumentRequest.class);

        RenderDocumentRequest renderDocumentRequest = RenderDocumentRequest.builder()
                .token("mytoken")
                .document(new JsonStringDecodable(SIMPLE_DOC))
                .data(new JsonStringDecodable("data"))
                .documentSession(DocumentSession.create())
                .documentOptions(DocumentOptions.builder().build())
                .build();
        mockViewhost.render(renderDocumentRequest);

        verify(mockViewhost).prepare(captor.capture());

        PrepareDocumentRequest prepareDocumentRequest = captor.getValue();
        assertEquals(prepareDocumentRequest.getToken(), renderDocumentRequest.getToken());
        assertEquals(prepareDocumentRequest.getDocument(), renderDocumentRequest.getDocument());
        assertEquals(prepareDocumentRequest.getData(), renderDocumentRequest.getData());
        assertEquals(prepareDocumentRequest.getDocumentSession(), renderDocumentRequest.getDocumentSession());
        assertEquals(prepareDocumentRequest.getDocumentOptions(), renderDocumentRequest.getDocumentOptions());

        verify(mockViewhost).render(any(PreparedDocument.class), any(Long.class));
    }

    @Test
    public void testRenderDocument_viewhostBound_inflatesAndFinishes() throws InterruptedException {
        CountDownLatch inflatedLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(1);
        mViewhost.registerStateChangeListener((state, handle) -> {
            if (state == DocumentState.INFLATED) {
                inflatedLatch.countDown();
            } else if (state == DocumentState.FINISHED) {
                finishLatch.countDown();
            }
        });

        DocumentHandle handle = render(SIMPLE_DOC, "");
        assertTrue(inflatedLatch.await(5, TimeUnit.SECONDS));

        // Finish
        FinishDocumentRequest finishRequest = FinishDocumentRequest.builder()
                .build();
        boolean result = handle.finish(finishRequest);
        assertTrue(result);
        assertTrue(finishLatch.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void testRenderDocument_inflated() throws InterruptedException {
        Map<String, DocumentHandle> documentHandleMap = new HashMap<>();
        testRenderDocument_inflated(SIMPLE_DOC, documentHandleMap);
        assertEquals(1, documentHandleMap.size());
    }

    @Test
    public void testRenderDocument_embeddedDoc_inflated() throws InterruptedException {
        Map<String, DocumentHandle> documentHandleMap = new HashMap<>();
        EmbeddedDocumentFactory factory = new EmbeddedDocumentFactoryTest(mViewhost, documentHandleMap);

        mDocumentOptions = DocumentOptions.builder().extensionGrantRequestCallback(mExtensionGrantRequestCallback)
                .extensionRegistrar(mExtensionRegistrar)
                .embeddedDocumentFactory(factory)
                .userPerceivedFatalCallback(mUserPerceivedFatalCallback).build();

        testRenderPreparedDocumentInflated(SIMPLE_DOC_WITH_HOST_COMPONENT, documentHandleMap);
        assertEquals(2, documentHandleMap.size());
    }

    private void testRenderDocument_inflated(String document, Map<String, DocumentHandle> documentHandleMap) throws InterruptedException {
        CountDownLatch inflatedLatch = new CountDownLatch(1);

        mViewhost.registerStateChangeListener((state, handle) -> {
            if (state == DocumentState.INFLATED) {
                inflatedLatch.countDown();
                documentHandleMap.put(handle.getUniqueId(), handle);
            }
        });

        render(document, "");
        assertTrue(inflatedLatch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testExecuteCommands() {
        // Basic demonstration of execute commands given a known document handle
        ExecuteCommandsRequest request =
                ExecuteCommandsRequest.builder()
                        .commands(new JsonStringDecodable("commands"))
                        .build();
        mDocumentHandle.executeCommands(request);
    }

    @Test
    public void testExampleMessage() {
        JsonStringDecodable payload = new JsonStringDecodable("test");
        Message message = new Message(mDocumentHandle, payload);

        assertEquals(mDocumentHandle, message.getDocument());
        assertEquals(payload, message.getPayload());
    }

    @Test
    public void testAllowsUnrelatedEventsToProceedWithoutOverriding() {
        SendEvent event = mock(SendEvent.class);
        updateDocumentMap(222, mDocumentHandleImpl, mDocumentContext);
        when(event.getDocumentContextId()).thenReturn((long) 333);
        assertTrue(((ViewhostImpl) mViewhost).interceptEventIfNeeded(event));
        verify(event, never()).overrideCallback(any(ISendEventCallbackV2.class));
    }

    @Test
    public void testUnrecognizedEventsPertainingToKnownDocuments() {
        SendEvent event = mock(SendEvent.class);
        updateDocumentMap(456, mDocumentHandleImpl, mDocumentContext);
        when(event.getDocumentContextId()).thenReturn((long) 456);
        assertTrue(((ViewhostImpl) mViewhost).interceptEventIfNeeded(event));
    }

    @Test
    public void testInterceptSendEventIfNeeded() {
        SendEvent event = mock(SendEvent.class);
        long key = 123;
        Object[] args = {"one", "two", "three"};
        updateDocumentMap(key, mDocumentHandleImpl, mDocumentContext);
        when(event.getDocumentContextId()).thenReturn(key);
        doAnswer(invocation -> {
            ISendEventCallbackV2 callback = invocation.getArgument(0);
            callback.onSendEvent(args, new HashMap<String, Object>(), new HashMap<String, Object>(), new HashMap<String, Object>());
            return null;
        }).when(event).overrideCallback(any(ISendEventCallbackV2.class));

        assertTrue(((ViewhostImpl) mViewhost).interceptEventIfNeeded(event));

        assertTrue(mRuntimeInteractionWorker.size() > 0);
        mRuntimeInteractionWorker.flush();

        assertEquals(1, mMessageHandler.queue.size());
        assertTrue(mMessageHandler.queue.peek() instanceof SendUserEventRequest);

        SendUserEventRequest message = (SendUserEventRequest) mMessageHandler.queue.poll();
        assertEquals(1, message.getId());
        assertEquals(mDocumentHandleImpl, message.getDocument());
        assertArrayEquals(args, message.getArguments());
        message.succeed();
    }

    @Test
    public void testInterceptDataSourceFetchEventIfNeeded() {
        long key = 123;
        updateDocumentMap(key, mDocumentHandleImpl, mDocumentContext);

        DataSourceFetchEvent event = mock(DataSourceFetchEvent.class);
        when(event.getDocumentContextId()).thenReturn(key);

        Map<String, Object> payload = new HashMap<>();
        payload.put("key1", "val1");
        payload.put("key2", "val2");

        doAnswer(invocation -> {
            IDataSourceFetchCallback callback = invocation.getArgument(0);
            callback.onDataSourceFetchRequest("dynamicIndexList", payload);
            return null;
        }).when(event).overrideCallback(any(IDataSourceFetchCallback.class));

        assertTrue(((ViewhostImpl) mViewhost).interceptEventIfNeeded(event));

        // Second event
        DataSourceFetchEvent event2 = mock(DataSourceFetchEvent.class);
        when(event2.getDocumentContextId()).thenReturn(key);
        doAnswer(invocation -> {
            IDataSourceFetchCallback callback = invocation.getArgument(0);
            callback.onDataSourceFetchRequest("dynamicTokenList", payload);
            return null;
        }).when(event2).overrideCallback(any(IDataSourceFetchCallback.class));

        assertTrue(((ViewhostImpl) mViewhost).interceptEventIfNeeded(event2));

        assertTrue(mRuntimeInteractionWorker.size() > 0);
        mRuntimeInteractionWorker.flush();

        assertEquals(2, mMessageHandler.queue.size());

        assertTrue(mMessageHandler.queue.peek() instanceof FetchDataRequest);
        FetchDataRequest message = (FetchDataRequest) mMessageHandler.queue.poll();
        assertEquals(1, message.getId());
        assertEquals("DYNAMIC_INDEX_LIST", message.getDataType());
        assertEquals(mDocumentHandleImpl, message.getDocument());
        assertEquals(payload, message.getParameters());
        message.succeed();

        assertTrue(mMessageHandler.queue.peek() instanceof FetchDataRequest);
        FetchDataRequest message2 = (FetchDataRequest) mMessageHandler.queue.poll();
        assertEquals(2, message2.getId());
        assertEquals("DYNAMIC_TOKEN_LIST", message2.getDataType());
        assertEquals(mDocumentHandleImpl, message2.getDocument());
        assertEquals(payload, message2.getParameters());
        message.fail("Could not publish event");
    }

    @Test
    public void testInterceptDataSourceFetchEventWithInvalidType() {
        long key = 123;
        updateDocumentMap(key, mDocumentHandleImpl, mDocumentContext);

        DataSourceFetchEvent event = mock(DataSourceFetchEvent.class);
        when(event.getDocumentContextId()).thenReturn(key);

        Map<String, Object> payload = new HashMap<>();
        payload.put("key1", "val1");
        payload.put("key2", "val2");

        doAnswer(invocation -> {
            IDataSourceFetchCallback callback = invocation.getArgument(0);
            callback.onDataSourceFetchRequest("invalidType", payload);
            return null;
        }).when(event).overrideCallback(any(IDataSourceFetchCallback.class));

        assertTrue(((ViewhostImpl) mViewhost).interceptEventIfNeeded(event));

        assertEquals(0, mRuntimeInteractionWorker.size());
        mRuntimeInteractionWorker.flush();
        assertEquals(0, mMessageHandler.queue.size());
    }

    @Test
    public void testInterceptOpenURLIfNeeded() {
        OpenURLEvent event = mock(OpenURLEvent.class);
        long key = 123;
        String source = "https://example.com/source.js";
        updateDocumentMap(key, mDocumentHandleImpl, mDocumentContext);
        when(event.getDocumentContextId()).thenReturn(key);

        IOpenUrlCallback.IOpenUrlCallbackResult callbackResult =
                mock(IOpenUrlCallback.IOpenUrlCallbackResult.class);

        doAnswer(invocation -> {
            IOpenUrlCallback callback = invocation.getArgument(0);
            callback.onOpenUrl(source, callbackResult);
            return null;
        }).when(event).overrideCallback(any(IOpenUrlCallback.class));

        assertTrue(((ViewhostImpl) mViewhost).interceptEventIfNeeded(event));

        assertTrue(mRuntimeInteractionWorker.size() > 0);
        mRuntimeInteractionWorker.flush();

        assertEquals(1, mMessageHandler.queue.size());
        assertTrue(mMessageHandler.queue.peek() instanceof OpenURLRequest);

        OpenURLRequest message = (OpenURLRequest) mMessageHandler.queue.poll();
        assertEquals(1, message.getId());
        assertEquals(mDocumentHandleImpl, message.getDocument());
        assertEquals(source, message.getSource());

        message.succeed();
        verify(callbackResult, times(1)).onResult(true);
    }

    @Test
    public void testInterceptOpenURLIfNeededWithFailedCallback() {
        OpenURLEvent event = mock(OpenURLEvent.class);
        long key = 123;
        String source = "https://example.com/source.js";
        updateDocumentMap(key, mDocumentHandleImpl, mDocumentContext);
        when(event.getDocumentContextId()).thenReturn(key);

        IOpenUrlCallback.IOpenUrlCallbackResult callbackResult =
                mock(IOpenUrlCallback.IOpenUrlCallbackResult.class);

        doAnswer(invocation -> {
            IOpenUrlCallback callback = invocation.getArgument(0);
            callback.onOpenUrl(source, callbackResult);
            return null;
        }).when(event).overrideCallback(any(IOpenUrlCallback.class));

        assertTrue(((ViewhostImpl) mViewhost).interceptEventIfNeeded(event));

        assertTrue(mRuntimeInteractionWorker.size() > 0);
        mRuntimeInteractionWorker.flush();

        assertEquals(1, mMessageHandler.queue.size());
        assertTrue(mMessageHandler.queue.peek() instanceof OpenURLRequest);

        OpenURLRequest message = (OpenURLRequest) mMessageHandler.queue.poll();
        assertEquals(1, message.getId());
        assertEquals(mDocumentHandleImpl, message.getDocument());
        assertEquals(source, message.getSource());

        message.fail("Reason for failure");
        verify(callbackResult, times(1)).onResult(false);
    }

    @Test
    public void testEventsDroppedWithoutMessageHandler() {
        DocumentOptions documentOptions = DocumentOptions.builder().properties(new HashMap<String, Object>() {{
            put("inflateOnMainThread", true);
        }}).build();
        ViewhostConfig config = ViewhostConfig.builder().defaultDocumentOptions(documentOptions).build();
        ViewhostImpl viewhost = new ViewhostImpl(config);

        when(mDocumentHandleImpl.getDocumentContext()).thenReturn(mDocumentContext);
        when(mDocumentContext.getId()).thenReturn((long) 123);
        viewhost.updateDocumentMap(mDocumentHandleImpl);

        SendEvent sendEvent = mock(SendEvent.class);
        when(sendEvent.getDocumentContextId()).thenReturn((long) 123);

        DataSourceFetchEvent dataSourceFetchEvent = mock(DataSourceFetchEvent.class);
        when(dataSourceFetchEvent.getDocumentContextId()).thenReturn((long) 123);

        OpenURLEvent openURLEvent = mock(OpenURLEvent.class);
        when(openURLEvent.getDocumentContextId()).thenReturn((long) 123);

        assertFalse(viewhost.interceptEventIfNeeded(sendEvent));
        assertFalse(viewhost.interceptEventIfNeeded(dataSourceFetchEvent));
        assertFalse(viewhost.interceptEventIfNeeded(openURLEvent));
    }

    @Test
    public void testCancelExecution() {
        ((ViewhostImpl) mViewhost).setTopDocumentHandleAndRootContext(mDocumentHandleImpl, mRootContext);
        mViewhost.cancelExecution();
        verify(mRootContext).cancelExecution();
    }


    @Test
    public void testInvokeExtensionEventHandler_nullRootContext() {
        ((ViewhostImpl) mViewhost).setTopDocumentHandleAndRootContext(null, null);

        mViewhost.invokeExtensionEventHandler("myextension:10", "MyExtension", new HashMap<>(), true, null);

        verify(mRootContext, times(0)).invokeExtensionEventHandler(any(String.class), any(String.class), any(Map.class), any(Boolean.class));
        assertTrue(mRuntimeInteractionWorker.size() == 0);
        // No NPE, test passes
    }
    @Test
    public void testInvokeExtensionEventHandler_nullCallback() {
        ((ViewhostImpl) mViewhost).setTopDocumentHandleAndRootContext(mDocumentHandleImpl, mRootContext);

        mViewhost.invokeExtensionEventHandler("myextension:10", "MyExtension", new HashMap<>(), true, null);

        verify(mRootContext).invokeExtensionEventHandler("myextension:10", "MyExtension", new HashMap<>(), true);
        assertTrue(mRuntimeInteractionWorker.size() == 0);
        // No NPE, test passes
    }

    @Test
    public void testInvokeExtensionEventHandler_nullAction_callsOnComplete() {
        ((ViewhostImpl) mViewhost).setTopDocumentHandleAndRootContext(mDocumentHandleImpl, mRootContext);
        when(mRootContext.invokeExtensionEventHandler(any(String.class), any(String.class), any(Map.class), any(Boolean.class))).thenReturn(null);

        mViewhost.invokeExtensionEventHandler("myextension:10", "MyExtension", new HashMap<>(), true, mExtensionEventHandlerCallback);

        verify(mRootContext).invokeExtensionEventHandler("myextension:10", "MyExtension", new HashMap<>(), true);
        assertTrue(mRuntimeInteractionWorker.size() == 1);
        mRuntimeInteractionWorker.flush();
        verify(mExtensionEventHandlerCallback, times(1)).onComplete();
        verify(mExtensionEventHandlerCallback, times(0)).onTerminated();
    }

    @Test
    public void testInvokeExtensionEventHandler_callback_action_addsCallbacksToAction() {
        ((ViewhostImpl) mViewhost).setTopDocumentHandleAndRootContext(mDocumentHandleImpl, mRootContext);
        when(mRootContext.invokeExtensionEventHandler(any(String.class), any(String.class), any(Map.class), any(Boolean.class))).thenReturn(mAction);

        mViewhost.invokeExtensionEventHandler("myextension:10", "MyExtension", new HashMap<>(), true, mExtensionEventHandlerCallback);

        verify(mRootContext).invokeExtensionEventHandler("myextension:10", "MyExtension", new HashMap<>(), true);

        assertTrue(mRuntimeInteractionWorker.size() == 0);

        // callback.onComplete() is called when action completed successfully
        ArgumentCaptor<Runnable> onCompleteRunnable = ArgumentCaptor.forClass(Runnable.class);
        verify(mAction).then(onCompleteRunnable.capture());
        onCompleteRunnable.getValue().run();
        assertTrue(mRuntimeInteractionWorker.size() == 1);
        mRuntimeInteractionWorker.flush();
        verify(mExtensionEventHandlerCallback, times(1)).onComplete();
        assertTrue(mRuntimeInteractionWorker.size() == 0);

        // callback.onTerminated() is called when action terminates
        ArgumentCaptor<Runnable> onTerminateRunnable = ArgumentCaptor.forClass(Runnable.class);
        verify(mAction).addTerminateCallback(onTerminateRunnable.capture());
        onTerminateRunnable.getValue().run();
        assertTrue(mRuntimeInteractionWorker.size() == 1);
        mRuntimeInteractionWorker.flush();
        verify(mExtensionEventHandlerCallback, times(1)).onTerminated();
        assertTrue(mRuntimeInteractionWorker.size() == 0);
    }


    @Test
    public void testRestoreDocumentSuccess() throws InterruptedException {
        DocumentHandle doc1 = prepareAndRender(SIMPLE_DOC, "");
        DocumentHandle doc2 = prepareAndRender(SIMPLE_DOC, "");
        assertTrue(mViewhost.restoreDocument(SavedDocument.builder().documentHandle(doc1).build()));

        assertTrue(mRuntimeInteractionWorker.size() > 0);
        mRuntimeInteractionWorker.flush();
        assert(mMessageHandler.queue.size() > 0);
        int doc1InflationCount = 0;
        while(!mMessageHandler.queue.isEmpty()) {
            BaseMessage message = mMessageHandler.queue.poll();
            if (message instanceof DocumentStateChangedImpl
                    && DocumentState.INFLATED.toString().equals(((DocumentStateChangedImpl) message).getState())
                    && doc1.getUniqueId().equals(message.getDocument().getUniqueId())) {
                doc1InflationCount++;
            }
        }
        assertEquals(2, doc1InflationCount);
    }

    @Test
    public void testRestoreDocumentInvalidDoc() throws InterruptedException {
        DocumentHandle invalidDoc = mViewhost.prepare( PrepareDocumentRequest.builder()
                .document(new JsonStringDecodable(INVALID_DOC))
                .documentSession(DocumentSession.create())
                .documentOptions(mDocumentOptions)
                .build()).getHandle();
        DocumentHandle doc2 = prepareAndRender(SIMPLE_DOC, "");
        assertFalse(mViewhost.restoreDocument(SavedDocument.builder().documentHandle(invalidDoc).build()));

        assertTrue(mRuntimeInteractionWorker.size() > 0);
        mRuntimeInteractionWorker.flush();
        assert(mMessageHandler.queue.size() > 0);
        int invalidDocInflationCount = 0;
        while(!mMessageHandler.queue.isEmpty()) {
            BaseMessage message = mMessageHandler.queue.poll();
            if (message instanceof DocumentStateChangedImpl
                    && DocumentState.INFLATED.toString().equals(((DocumentStateChangedImpl) message).getState())
                    && invalidDoc.getUniqueId().equals(message.getDocument().getUniqueId())) {
                invalidDocInflationCount++;
            }
        }
        assertEquals(0, invalidDocInflationCount);
    }

    @Test
    public void testReuseFinishDocumentRequestDropped() {
        Handler coreWorker = Mockito.mock(Handler.class);
        when(coreWorker.getLooper()).thenReturn(Looper.getMainLooper());
        List<Runnable> list = new ArrayList<>();
        when(coreWorker.post(any(Runnable.class))).thenAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            list.add(task);
            return null;
        });
        Viewhost viewhost = new ViewhostImpl(mConfig, mRuntimeInteractionWorker, coreWorker);
        PreparedDocument preparedDocument = viewhost.prepare(PrepareDocumentRequest.builder()
                .document(new JsonStringDecodable(SIMPLE_DOC))
                .documentSession(DocumentSession.create())
                .documentOptions(mDocumentOptions)
                .build());
        DocumentHandleImpl documentHandle = (DocumentHandleImpl) preparedDocument.getHandle();
        mAplLayout.measure(View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY));
        mAplLayout.layout(0, 0, 640, 480);
        if (!viewhost.isBound()) {
            viewhost.bind(mAplLayout);
        }
        //render
        viewhost.render(preparedDocument);
        list.remove(0).run();
        assertTrue(mRuntimeInteractionWorker.size() > 0);
        mRuntimeInteractionWorker.flush();
        assertMessageReceived(DocumentState.INFLATED, preparedDocument.getHandle().getUniqueId());

        //unbind
        viewhost.unBind();

        //finish the document, however core runnable not run.
        documentHandle.finish(FinishDocumentRequest.builder().build());

        //reuse finished prepared doc
        viewhost.bind(mAplLayout);

        //At this point we should have 2 items in the runnable queue for core
        assertEquals(2, list.size());
        list.remove(0).run();
        assertTrue(mRuntimeInteractionWorker.size() > 0);
        mRuntimeInteractionWorker.flush();
        assertMessageReceived(DocumentState.FINISHED, preparedDocument.getHandle().getUniqueId());
        assertEquals(DocumentState.FINISHED, documentHandle.getDocumentState());

        //run the second task
        list.remove(0).run();

        assertTrue(mRuntimeInteractionWorker.size() == 0);
        assertEquals(DocumentState.FINISHED, documentHandle.getDocumentState());

    }

    @Test
    public void testReusePreparedDocument() {
        PreparedDocument preparedDocument = mViewhost.prepare(PrepareDocumentRequest.builder()
                .document(new JsonStringDecodable(SIMPLE_DOC))
                .documentSession(DocumentSession.create())
                .documentOptions(mDocumentOptions)
                .build());
        DocumentHandleImpl documentHandle = (DocumentHandleImpl) preparedDocument.getHandle();
        mAplLayout.measure(View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY));
        mAplLayout.layout(0, 0, 640, 480);
        if (!mViewhost.isBound()) {
            mViewhost.bind(mAplLayout);
        }
        renderSuccess(preparedDocument);
        //RC after first render
        RootContext firstRootContext = documentHandle.getRootContext();
        assertEquals(DocumentState.INFLATED, documentHandle.getDocumentState());
        //all the messages were de-queued so queue should be empty
        assertTrue(mRuntimeInteractionWorker.size() == 0);

        //unbind
        mViewhost.unBind();

        //reuse finished prepared doc
        mViewhost.bind(mAplLayout);
        assertTrue(mRuntimeInteractionWorker.size() > 0);
        mRuntimeInteractionWorker.flush();
        assertMessageReceived(DocumentState.INFLATED, preparedDocument.getHandle().getUniqueId());
        assertEquals(DocumentState.INFLATED, documentHandle.getDocumentState());
        //asserting that the RC remains the same after document re-use
        assertEquals(firstRootContext, documentHandle.getRootContext());

        documentHandle.finish(FinishDocumentRequest.builder().build());
        finishSuccess(preparedDocument.getHandle());

        assertEquals(DocumentState.FINISHED, documentHandle.getDocumentState());

        //null response when document not valid
        DocumentHandle handle = mViewhost.render(preparedDocument);
        assertNull(handle);
    }

    @Test
    public void test_singleViewhost_legacyExtensionRegistration() {
        mConfig = ViewhostConfig.builder()
                .messageHandler(mMessageHandler)
                .extensionRegistrar(mExtensionRegistrar)
                .defaultDocumentOptions(mDocumentOptions)
                .legacyExtensionRegistration(mLegacyExtensionRegistration)
                .build();

        mViewhost = new ViewhostImpl(mConfig, mRuntimeInteractionWorker, mCoreWorker);

        PreparedDocument preparedDocument = mViewhost.prepare(PrepareDocumentRequest.builder()
                .document(new JsonStringDecodable(SIMPLE_DOC))
                .documentOptions(mDocumentOptions)
                .documentSession(DocumentSession.create())
                .build());

        DocumentHandleImpl handle = (DocumentHandleImpl) preparedDocument.getHandle();
        assertNotNull(handle);
        assertTrue(preparedDocument.isValid());
        assertTrue(preparedDocument.isReady());
        assertEquals(handle.getUniqueId(), preparedDocument.getUniqueID());

        verify(mLegacyExtensionRegistration).registerExtensions(any(Content.class), any(RootConfig.class));

        // Bind and render
        mAplLayout.measure(View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY));
        mAplLayout.layout(0, 0, 640, 480);
        if (!mViewhost.isBound()) {
            mViewhost.bind(mAplLayout);
        }
        assertNotNull(mViewhost.render(preparedDocument));

        assertNotNull(handle.getRootContext());
        assertNotNull(handle.getDocumentContext());
    }

    @Test
    public void test_multiViewhost_legacyExtensionRegistration() {
        mConfig = ViewhostConfig.builder()
                .messageHandler(mMessageHandler)
                .extensionRegistrar(mExtensionRegistrar)
                .defaultDocumentOptions(mDocumentOptions)
                .legacyExtensionRegistration(mLegacyExtensionRegistration)
                .build();

        mViewhost = new ViewhostImpl(mConfig, mRuntimeInteractionWorker, mCoreWorker);
        mViewhost2 = new ViewhostImpl(mConfig, mRuntimeInteractionWorker, mCoreWorker);

        // Prepare using VH1
        PreparedDocument preparedDocument = mViewhost.prepare(PrepareDocumentRequest.builder()
                .document(new JsonStringDecodable(SIMPLE_DOC))
                .documentOptions(mDocumentOptions)
                .documentSession(DocumentSession.create())
                .build());
        DocumentHandleImpl handle = (DocumentHandleImpl) preparedDocument.getHandle();
        assertNotNull(handle);
        assertTrue(preparedDocument.isValid());
        assertTrue(preparedDocument.isReady());
        assertEquals(handle.getUniqueId(), preparedDocument.getUniqueID());

        // Check for the callback
        verify(mLegacyExtensionRegistration).registerExtensions(any(Content.class), any(RootConfig.class));

        // Bind and render using VH2
        mAplLayout.measure(View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY));
        mAplLayout.layout(0, 0, 640, 480);
        if (!mViewhost2.isBound()) {
            mViewhost2.bind(mAplLayout);
        }
        mViewhost2.render(preparedDocument);

        assertNotNull(handle.getRootContext());
        assertNotNull(handle.getDocumentContext());
    }

    @Test
    public void testMultiViewhost_usingTwoViewhosts_documentReuseSucess() {
        //prepare using VH1
        PreparedDocument preparedDocument = mViewhost.prepare(PrepareDocumentRequest.builder()
                .document(new JsonStringDecodable(SIMPLE_DOC))
                .documentSession(DocumentSession.create())
                .documentOptions(mDocumentOptions)
                .build());
        DocumentHandleImpl handle = (DocumentHandleImpl) preparedDocument.getHandle();

        //set apl layout
        mAplLayout.measure(View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY));
        mAplLayout.layout(0, 0, 640, 480);

        //bind and render using VH2
        if (!mViewhost2.isBound()) {
            mViewhost2.bind(mAplLayout);
        }
        mViewhost2.render(preparedDocument);
        assertTrue(mRuntimeInteractionWorker.size() > 0);
        mRuntimeInteractionWorker.flush();
        assert(mMessageHandler.queue.size() > 0);

        assertNotNull(handle.getRootContext());
        assertNotNull(handle.getDocumentContext());

        assertMessageReceived(DocumentState.INFLATED, handle.getUniqueId());

        //all the messages were de-queued so queue should be empty
        assertTrue(mRuntimeInteractionWorker.size() == 0);

        //reuse using VH2 again
        mViewhost2.unBind();
        mViewhost2.bind(mAplLayout);
        assertTrue(mRuntimeInteractionWorker.size() > 0);
        mRuntimeInteractionWorker.flush();
        assert(mMessageHandler.queue.size() > 0);

        assertNotNull(handle.getRootContext());
        assertNotNull(handle.getDocumentContext());

        assertMessageReceived(DocumentState.INFLATED, handle.getUniqueId());
    }

    @Test
    public void testSendEventReceived() {
        //prepare using VH1
        PreparedDocument preparedDocument = mViewhost.prepare(PrepareDocumentRequest.builder()
                .document(new JsonStringDecodable(SIMPLE_DOC_WITH_SEND_EVENT))
                .documentSession(DocumentSession.create())
                .documentOptions(mDocumentOptions)
                .build());
        DocumentHandleImpl handle = (DocumentHandleImpl) preparedDocument.getHandle();

        //set apl layout
        mAplLayout.measure(View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY));
        mAplLayout.layout(0, 0, 640, 480);

        //bind and render using VH2
        if (!mViewhost.isBound()) {
            mViewhost.bind(mAplLayout);
        }
        mViewhost.render(preparedDocument);

        assertNotNull(handle.getRootContext());
        assertNotNull(handle.getDocumentContext());

       update(handle, 100);

        mRuntimeInteractionWorker.flush();
        assert(mMessageHandler.queue.size() > 0);

        boolean sendMessageReceived = false;
        while(!mMessageHandler.queue.isEmpty()) {
            BaseMessage message = mMessageHandler.queue.poll();
            if (message instanceof SendUserEventRequest
                    && handle.getUniqueId().equals(message.getDocument().getUniqueId())) {
                sendMessageReceived = true;
            }
        }
        assertTrue(sendMessageReceived);
    }


    @Test
    public void testMultiViewhost_sendEventReceived() {
        //prepare using VH1
        PreparedDocument preparedDocument = mViewhost.prepare(PrepareDocumentRequest.builder()
                .document(new JsonStringDecodable(SIMPLE_DOC_WITH_SEND_EVENT))
                .documentSession(DocumentSession.create())
                .documentOptions(mDocumentOptions)
                .build());
        DocumentHandleImpl handle = (DocumentHandleImpl) preparedDocument.getHandle();

        //set apl layout
        mAplLayout.measure(View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY));
        mAplLayout.layout(0, 0, 640, 480);

        //bind and render using VH2
        if (!mViewhost2.isBound()) {
            mViewhost2.bind(mAplLayout);
        }
        mViewhost2.render(preparedDocument);

        assertNotNull(handle.getRootContext());
        assertNotNull(handle.getDocumentContext());

        update(handle, 100);

        mRuntimeInteractionWorker.flush();
        assert(mMessageHandler.queue.size() > 0);

        boolean sendMessageReceived = false;
        while(!mMessageHandler.queue.isEmpty()) {
            BaseMessage message = mMessageHandler.queue.poll();
            if (message instanceof SendUserEventRequest
                    && handle.getUniqueId().equals(message.getDocument().getUniqueId())) {
                sendMessageReceived = true;
            }
        }
        assertTrue(sendMessageReceived);
    }

    private void update(DocumentHandleImpl handle, int time) {
        handle.getRootContext().initTime();
        if (handle.getRootContext() != null) {
            mTime += time * 1000000;
            handle.getRootContext().onTick(mTime);
        }
    }

    @Test
    public void testVisualContextChangeNotificationReceived() {
        //prepare and render using same VH
        PreparedDocument preparedDocument = mViewhost.prepare(PrepareDocumentRequest.builder()
                .document(new JsonStringDecodable(HELLO_WORLD))
                .documentSession(DocumentSession.create())
                .documentOptions(mDocumentOptions)
                .build());
        DocumentHandleImpl handle = (DocumentHandleImpl) preparedDocument.getHandle();

        //set apl layout
        mAplLayout.measure(View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY));
        mAplLayout.layout(0, 0, 640, 480);

        //bind and render using VH2
        if (!mViewhost.isBound()) {
            mViewhost.bind(mAplLayout);
        }
        mViewhost.render(preparedDocument);

        //flush all document state change notifications and clear the queue
        assertFalse(mRuntimeInteractionWorker.isEmpty());
        mRuntimeInteractionWorker.flush();
        mMessageHandler.queue.clear();

        assertTrue(handle.executeCommands(ExecuteCommandsRequest.builder().commands(new JsonStringDecodable(mGoodbyeCommands)).build()));

        update(handle, 100);
        assertFalse(mRuntimeInteractionWorker.isEmpty());
        mRuntimeInteractionWorker.flush();

        assertTrue(mMessageHandler.queue.peek() instanceof VisualContextChanged);
        VisualContextChanged message = (VisualContextChanged) mMessageHandler.queue.poll();
        assertEquals(handle, message.getDocument());
    }

    private final String DOC_WITH_SCREEN_LOCK = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"2024.3\",\n" +
            "  \"onMount\": [\n" +
            "    {\n" +
            "    \"type\": \"Idle\",\n" +
            "    \"delay\": 3000,\n" +
            "    \"screenLock\": true\n" +
            "    }\n" +
            "  ],\n" +
            "  \"mainTemplate\": {\n" +
            "    \"item\": {\n" +
            "      \"type\": \"Text\"\n" +
            "    }\n" +
            "  }\n" +
            "}";

    private final String DOC_WITH_SCREEN_LOCK_FALSE = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"2024.3\",\n" +
            "  \"onMount\": [\n" +
            "    {\n" +
            "      \"type\": \"Idle\",\n" +
            "      \"delay\": 3000,\n" +
            "      \"screenLock\": false\n" +
            "    }\n" +
            "  ],\n" +
            "  \"mainTemplate\": {\n" +
            "    \"item\": {\n" +
            "      \"type\": \"Text\"\n" +
            "    }\n" +
            "  }\n" +
            "}";

    @Test
    public void testScreenLockNotificationNotSent_whenScreenLockFalse() {
        DocumentHandleImpl handle = (DocumentHandleImpl) prepareAndRender(DOC_WITH_SCREEN_LOCK_FALSE, "");
        assertNotNull(handle);
        update(handle, 100);
        assertFalse(mRuntimeInteractionWorker.isEmpty());
        mRuntimeInteractionWorker.flush();
        boolean statusChanged = false;
        while(!mMessageHandler.queue.isEmpty()) {
            BaseMessage message = mMessageHandler.queue.poll();
            assertFalse(message instanceof ScreenLockStatusChangedImpl);
        }
    }

    @Test
    public void testScreenLockNotificationSent() {
        DocumentHandleImpl handle = (DocumentHandleImpl) prepareAndRender(DOC_WITH_SCREEN_LOCK, "");
        assertNotNull(handle);
        update(handle, 100);
        assertFalse(mRuntimeInteractionWorker.isEmpty());
        mRuntimeInteractionWorker.flush();
        boolean statusChanged = false;
        while(!mMessageHandler.queue.isEmpty()) {
            BaseMessage message = mMessageHandler.queue.poll();
            if (message instanceof ScreenLockStatusChangedImpl) {
                statusChanged = true;
                assertTrue(((ScreenLockStatusChangedImpl) message).hasScreenLockStatusChanged());
            }
        }
        assertTrue(statusChanged);
    }

    @Test
    public void testMultiViewhost_VisualContextChangeNotificationReceived() {
        //prepare using VH1
        Viewhost mViewhost1 = new ViewhostImpl(mConfig, mRuntimeInteractionWorker, mCoreWorker);
        PreparedDocument preparedDocument = mViewhost1.prepare(PrepareDocumentRequest.builder()
                .document(new JsonStringDecodable(HELLO_WORLD))
                .documentSession(DocumentSession.create())
                .documentOptions(mDocumentOptions)
                .build());
        DocumentHandleImpl handle = (DocumentHandleImpl) preparedDocument.getHandle();

        //release VH1 to test that changes work as expected even if VH is released.
        mViewhost1 = null;

        //set apl layout
        mAplLayout.measure(View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY));
        mAplLayout.layout(0, 0, 640, 480);

        //bind and render using VH2
        if (!mViewhost2.isBound()) {
            mViewhost2.bind(mAplLayout);
        }
        mViewhost2.render(preparedDocument);

        //flush all document state change notifications and clear the queue
        assertFalse(mRuntimeInteractionWorker.isEmpty());
        mRuntimeInteractionWorker.flush();
        mMessageHandler.queue.clear();

        assertTrue(handle.executeCommands(ExecuteCommandsRequest.builder().commands(new JsonStringDecodable(mGoodbyeCommands)).build()));

        update(handle, 100);
        assertFalse(mRuntimeInteractionWorker.isEmpty());
        mRuntimeInteractionWorker.flush();

        assertTrue(mMessageHandler.queue.peek() instanceof VisualContextChanged);
        VisualContextChanged message = (VisualContextChanged) mMessageHandler.queue.poll();
        assertEquals(handle, message.getDocument());
    }

    @Test
    public void testDataSourceErrors() {
        DocumentHandleImpl handle = (DocumentHandleImpl) prepareAndRender(SHOPPING_LIST_DOC, SHOPPING_LIST_DATA);
        assertDataSourceError(handle);
    }

    @Test
    public void testMultiViewhost_DataSourceErrors() {
        //prepare using VH1
        Viewhost viewhost = new ViewhostImpl(mConfig, mRuntimeInteractionWorker, mCoreWorker);
        PreparedDocument preparedDocument = viewhost.prepare(PrepareDocumentRequest.builder()
                .document(new JsonStringDecodable(SIMPLE_DOC_WITH_SEND_EVENT))
                .documentSession(DocumentSession.create())
                .documentOptions(mDocumentOptions)
                .build());
        DocumentHandleImpl handle = (DocumentHandleImpl) preparedDocument.getHandle();

        //destroy VH1
        viewhost = null;

        //set apl layout
        mAplLayout.measure(View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY));
        mAplLayout.layout(0, 0, 640, 480);

        //bind and render using VH2
        if (!mViewhost2.isBound()) {
            mViewhost2.bind(mAplLayout);
        }
        mViewhost2.render(preparedDocument);

        assertNotNull(handle.getRootContext());
        assertNotNull(handle.getDocumentContext());

        assertDataSourceError(handle);
    }

    private void assertDataSourceError(DocumentHandleImpl handle) {
        Map<String, Object> map = populateMapWithIncorrectData();
        sendShoppingList(handle, map);

        //It will internally call coreFrameUpdate in rootContext
        update(handle, 500);

        assertFalse(mRuntimeInteractionWorker.isEmpty());
        mRuntimeInteractionWorker.flush();
        assert(mMessageHandler.queue.size() > 0);

        int errorCount = 0;
        while (!mMessageHandler.queue.isEmpty()) {
            BaseMessage message = mMessageHandler.queue.poll();
            if (message instanceof ReportRuntimeErrorRequest) {
                errorCount++;
            }
        }

        assertEquals(1, errorCount);
    }

    private Map<String, Object> populateMapWithIncorrectData() {
        Map<String, Object> map = new HashMap<>();
        map.put("startIndex", 0);
        map.put("correlationToken", 101);
        map.put("listId", "wrongListId");
        map.put("count", 1);
        return map;
    }

    public void sendShoppingList(DocumentHandle handle, Map<String, Object> request) {
        JSONObject response = createResponse(request);
        UpdateDataSourceRequest updateDataSourceRequest = UpdateDataSourceRequest
                .builder()
                .data(new JsonStringDecodable(response.toString()))
                .callback(mCallback)
                .build() ;
        assertTrue(handle.updateDataSource(updateDataSourceRequest));
    }

    private JSONObject createResponse(Map<String, Object> request) {
        try {
            int count = (Integer) request.get("count");
            int startIndex = (Integer) request.get("startIndex");

            JSONArray items = new JSONArray();
            for (int i = startIndex; i < startIndex + count; i++) {
                JSONObject item = new JSONObject();
                item.put("text", "item" + i);
                items.put(item);
            }

            // Response payload structure comes from
            // https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-interface.html#sendindexlistdata-directive
            JSONObject response = new JSONObject();
            response.put("correlationToken", request.get("correlationToken"));
            response.put("listId", request.get("listId"));
            response.put("startIndex", request.get("startIndex"));
            response.put("items", items);
            response.put("minimumInclusiveIndex", 0);
            response.put("maximumExclusiveIndex", 100);
            response.put("type", "dynamicIndexList");
            return response;
        } catch (JSONException e) {
            fail("JSON exception " + e);
        }
        return null;
    }

    private void renderSuccess(PreparedDocument preparedDocument) {
        mViewhost.render(preparedDocument);
        assertTrue(mRuntimeInteractionWorker.size() > 0);
        mRuntimeInteractionWorker.flush();
        assertMessageReceived(DocumentState.INFLATED, preparedDocument.getHandle().getUniqueId());
    }

    private void finishSuccess(DocumentHandle documentHandle) {
        assertTrue(mRuntimeInteractionWorker.size() > 0);
        mRuntimeInteractionWorker.flush();
        assertMessageReceived(DocumentState.FINISHED, documentHandle.getUniqueId());
    }

    private void assertMessageReceived(DocumentState state, String documentUniqueId) {
        boolean stateFlag = false;
        int stateCount = 0;
        while(!mMessageHandler.queue.isEmpty()) {
            BaseMessage message = mMessageHandler.queue.poll();
            if (message instanceof DocumentStateChangedImpl
                    && state.toString().equals(((DocumentStateChangedImpl) message).getState())
                    && documentUniqueId.equals(message.getDocument().getUniqueId())) {
                stateFlag = true;
                stateCount++;
            }
        }
        assertTrue(stateFlag);
        assertEquals(1, stateCount);
    }

    @Test
    public void testIsBound() {
        assertFalse(mViewhost.isBound());
        mViewhost.bind(mAplLayout);
        assertTrue(mViewhost.isBound());
    }

    @Test
    public void testMultiViewhost_renderPreparedDocumentSuccess() {

        //prepare using VH1
        PrepareDocumentRequest request = PrepareDocumentRequest.builder()
                .document(new JsonStringDecodable(SIMPLE_DOC))
                .data(new JsonStringDecodable(""))
                .documentSession(DocumentSession.create())
                .documentOptions(mDocumentOptions)
                .build();

        PreparedDocument preparedDocument = mViewhost.prepare(request);
        DocumentHandle handle = preparedDocument.getHandle();
        assertNotNull(handle);
        //bind VH2
        mAplLayout.measure(View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY));
        mAplLayout.layout(0, 0, 640, 480);
        if (!mViewhost2.isBound()) {
            mViewhost2.bind(mAplLayout);
        }

        //render using VH2
        mViewhost2.render(preparedDocument);
        assertTrue(mRuntimeInteractionWorker.size() > 0);
        mRuntimeInteractionWorker.flush();
        assert(mMessageHandler.queue.size() > 0);

        assertNotNull(((DocumentHandleImpl) handle).getRootContext());
        assertNotNull(((DocumentHandleImpl) handle).getDocumentContext());

        int inflatedCount = 0;
        while(!mMessageHandler.queue.isEmpty()) {
            BaseMessage message = mMessageHandler.queue.poll();
            if (message instanceof DocumentStateChangedImpl
                    && DocumentState.INFLATED.toString().equals(((DocumentStateChangedImpl) message).getState())
                    && handle.getUniqueId().equals(message.getDocument().getUniqueId())) {
                inflatedCount++;
            }
        }
        assertEquals(1, inflatedCount);
    }

    private void updateDocumentMap(long key, DocumentHandleImpl documentHandle, DocumentContext documentContext) {
        when(documentHandle.getDocumentContext()).thenReturn(documentContext);
        when(documentHandle.isValid()).thenReturn(true);
        when(documentContext.getId()).thenReturn(key);
        ((ViewhostImpl) mViewhost).updateDocumentMap(documentHandle);
    }

    private class EmbeddedDocumentFactoryTest implements EmbeddedDocumentFactory {
        private final Viewhost mViewhost;
        private final Map<String, DocumentHandle> mMap;

        EmbeddedDocumentFactoryTest(Viewhost viewhost, Map<String, DocumentHandle> map) {
            mViewhost = viewhost;
            mMap = map;
        }

        @Override
        public void onDocumentRequested(EmbeddedDocumentRequest request) {
            PrepareDocumentRequest prepareDocumentRequest = PrepareDocumentRequest.builder()
                    .document(new JsonStringDecodable(SIMPLE_DOC))
                    .documentSession(DocumentSession.create())
                    .build();

            PreparedDocument preparedDocument = mViewhost.prepare(prepareDocumentRequest);
            assertNotNull(preparedDocument.getHandle());

            DocumentHandle handle = preparedDocument.getHandle();
            request.resolve(EmbeddedDocumentResponse.builder().preparedDocument(preparedDocument).visualContextAttached(false).build());

            assertNotNull(((DocumentHandleImpl) handle).getDocumentContext());
            assertNull(((DocumentHandleImpl) handle).getRootContext());
            mMap.put(handle.getUniqueId(), handle);
        }
    }

    private DocumentHandle render(String document, String data) {
        RenderDocumentRequest request = RenderDocumentRequest.builder()
                .document(new JsonStringDecodable(document))
                .data(new JsonStringDecodable(data))
                .documentSession(DocumentSession.create())
                .documentOptions(mDocumentOptions)
                .build();

        // Bind the Viewhost
        mAplLayout.measure(View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY));
        mAplLayout.layout(0, 0, 640, 480);
        mViewhost.bind(mAplLayout);

        DocumentHandle handle = mViewhost.render(request);
        assertNotNull(handle);
        assertNotNull(((DocumentHandleImpl) handle).getRootContext());
        assertNotNull(((DocumentHandleImpl) handle).getDocumentContext());
        return handle;
    }
    private DocumentHandle prepareAndRender(String document, String data) {
        PrepareDocumentRequest request = PrepareDocumentRequest.builder()
                .document(new JsonStringDecodable(document))
                .data(new JsonStringDecodable(data))
                .documentSession(DocumentSession.create())
                .documentOptions(mDocumentOptions)
                .build();

        mAplLayout.measure(View.MeasureSpec.makeMeasureSpec(640, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(480, View.MeasureSpec.EXACTLY));
        mAplLayout.layout(0, 0, 640, 480);
        if (!mViewhost.isBound()) {
            mViewhost.bind(mAplLayout);
        }
        PreparedDocument preparedDocument = mViewhost.prepare(request);
        DocumentHandle handle = mViewhost.render(preparedDocument);
        assertNotNull(handle);
        assertNotNull(((DocumentHandleImpl) handle).getRootContext());
        assertNotNull(((DocumentHandleImpl) handle).getDocumentContext());
        return handle;
    }

    private void assertPrepared(DocumentHandle handle) {
        assertTrue(mRuntimeInteractionWorker.size() > 0);
        mRuntimeInteractionWorker.flush();

        int prepareCount = 0;
        int errorCount = 0;
        int displayedCount = 0;
        int inflatedCount = 0;
        while(!mMessageHandler.queue.isEmpty()) {
            BaseMessage message = mMessageHandler.queue.poll();
            if (message instanceof DocumentStateChangedImpl
                    && DocumentState.PREPARED.toString().equals(((DocumentStateChangedImpl) message).getState())
                    && handle.getUniqueId().equals(message.getDocument().getUniqueId())) {
                prepareCount++;
            }
            if (message instanceof DocumentStateChangedImpl
                    && DocumentState.ERROR.toString().equals(((DocumentStateChangedImpl) message).getState())
                    && handle.getUniqueId().equals(message.getDocument().getUniqueId())) {
                errorCount++;
            }
            if (message instanceof DocumentStateChangedImpl
                    && DocumentState.DISPLAYED.toString().equals(((DocumentStateChangedImpl) message).getState())
                    && handle.getUniqueId().equals(message.getDocument().getUniqueId())) {
                displayedCount++;
            }
            if (message instanceof DocumentStateChangedImpl
                    && DocumentState.INFLATED.toString().equals(((DocumentStateChangedImpl) message).getState())
                    && handle.getUniqueId().equals(message.getDocument().getUniqueId())) {
                inflatedCount++;
            }
        }
        assertEquals(1, prepareCount);
        assertEquals(0, errorCount);
        assertEquals(0, displayedCount);
        assertEquals(0, inflatedCount);
    }
}
