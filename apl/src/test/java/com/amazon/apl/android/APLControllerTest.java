/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.content.Context;
import android.os.Looper;

import com.amazon.alexaext.ExtensionRegistrar;
import com.amazon.apl.android.dependencies.IUserPerceivedFatalCallback;
import com.amazon.apl.android.extension.IExtensionRegistration;
import com.amazon.apl.android.metrics.IMetricsRecorder;
import com.amazon.apl.android.providers.AbstractMediaPlayerProvider;

import static com.amazon.apl.android.APLController.initializeAPL;
import static com.amazon.apl.android.APLController.setLibraryFuture;

import androidx.test.platform.app.InstrumentationRegistry;

import com.amazon.apl.android.bitmap.IBitmapPool;
import com.amazon.apl.android.dependencies.IContentCompleteCallback;
import com.amazon.apl.devtools.DevToolsProvider;
import com.amazon.apl.devtools.models.ViewTypeTarget;
import com.amazon.apl.android.font.CompatFontResolver;
import com.amazon.apl.android.font.TypefaceResolver;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.providers.ITtsPlayerProvider;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;
import com.amazon.apl.devtools.models.network.IDTNetworkRequestHandler;
import com.amazon.apl.enums.DisplayState;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.robolectric.Robolectric;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

public class APLControllerTest extends ViewhostRobolectricTest {

    @Mock
    private RootContext mRootContext;
    @Mock
    private Content mContent;
    @Mock
    private Action mAction;
    @Mock
    private APLLayout mAplLayout;
    @Mock
    private APLOptions mOptions;
    @Mock
    private IAPLViewPresenter mViewPresenter;
    @Mock
    private RootConfig mRootConfig;
    @Mock
    private AbstractMediaPlayerProvider mMediaPlayerProvider;
    @Mock
    private ExtensionMediator mExtensionMediator;
    @Mock
    private ITtsPlayerProvider mTtsPlayerProvider;
    @Mock
    private Future<Boolean> mLibraryFuture;
    @Mock
    private IContentCompleteCallback mContentCompleteCallback;
    @Mock
    private DocumentSession mDocumentSession;
    @Mock
    private ITelemetryProvider mTelemetryProvider;
    @Mock
    private IUserPerceivedFatalCallback mUserPerceivedFatalCallback;
    @Mock
    private ViewTypeTarget mViewTypeTarget;
    @Mock
    private DevToolsProvider mDevToolsProvider;
    @Mock
    private Session mAPLSession;
    private Context mContext;
    @Mock
    private RuntimeConfig mRuntimeConfig;
    @Mock
    private IExtensionRegistration mExtensionRegistration;
    @Mock
    private ExtensionRegistrar mExtensionRegistrar;
    @Mock
    private IMetricsRecorder mMetricsRecorder;

    private APLController mController;

    @Before
    public void setup() {
        mController = new APLController(mRootContext, mContent);
        when(mContent.getMetricsRecorder()).thenReturn(mMetricsRecorder);
        when(mOptions.getTelemetryProvider()).thenReturn(mTelemetryProvider);
        when(mOptions.getUserPerceivedFatalCallback()).thenReturn(mUserPerceivedFatalCallback);
        when(mRootContext.executeCommands(any())).thenReturn(mAction);
        when(mRootContext.getOptions()).thenReturn(mOptions);
        when(mRootContext.getRootConfig()).thenReturn(mRootConfig);
        when(mRootConfig.getSession()).thenReturn(mAPLSession);
        when(mAplLayout.getPresenter()).thenReturn(mViewPresenter);
        when(mAplLayout.getDevToolsProvider()).thenReturn(mDevToolsProvider);
        when(mDevToolsProvider.getDTView()).thenReturn(mViewTypeTarget);
        when(mOptions.getUserPerceivedFatalCallback()).thenReturn(mUserPerceivedFatalCallback);
        try {
            when(mLibraryFuture.get(
                    any(long.class),
                    any(TimeUnit.class)
            )).thenReturn(true);
        } catch (ExecutionException | TimeoutException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testRunnablesQueuedAreInvokedOnRender() {
        mController = new APLController(null, mContent);

        mController.executeCommands("commands", null);
        mController.updateDataSource("type", "data", null);
        mController.invokeExtensionEventHandler("uri", "name", new HashMap<>(), false, null);
        mController.cancelExecution();

        mController.onDocumentRender(mRootContext);
        mController.onDocumentDisplayed(System.currentTimeMillis());
        Robolectric.flushForegroundThreadScheduler();

        InOrder inOrder = inOrder(mRootContext);
        inOrder.verify(mRootContext).getOptions();
        inOrder.verify(mRootContext).getMetricsRecorder();
        inOrder.verify(mRootContext).executeCommands(eq("commands"));
        inOrder.verify(mRootContext).updateDataSource(eq("type"), eq("data"));
        inOrder.verify(mRootContext).invokeExtensionEventHandler(eq("uri"), eq("name"), any(), eq(false));
        inOrder.verify(mRootContext).cancelExecution();
        verifyNoMoreInteractions(mRootContext);

        mController.finishDocument();
        verify(mRootContext).finishDocument();
    }

    @Test
    public void testRunnablesNotInvokedAfterFinish() {
        mController = new APLController(mRootContext, mContent);
        mController.onDocumentFinish();

        mController.executeCommands("commands", null);
        mController.updateDataSource("type", "data", null);
        mController.invokeExtensionEventHandler("uri", "name", new HashMap<>(), false, null);
        mController.cancelExecution();
        mController.finishDocument();

        verifyNoInteractions(mRootContext);
    }

    @Test
    public void testMultipleCommandsInvokedInOrder() {
        mController = new APLController(null, mContent);

        mController.executeCommands("a", null);
        mController.executeCommands("b", null);

        mController.onDocumentRender(mRootContext);
        mController.onDocumentDisplayed(System.currentTimeMillis());
        Robolectric.flushForegroundThreadScheduler();

        InOrder inOrder = inOrder(mRootContext);
        inOrder.verify(mRootContext).executeCommands(eq("a"));
        inOrder.verify(mRootContext).executeCommands(eq("b"));
    }

    @Test
    public void testInvokedOnOtherThread() throws InterruptedException {
        mController = new APLController(mRootContext, mContent);

        CountDownLatch inner = new CountDownLatch(1);
        CountDownLatch outer = new CountDownLatch(1);
        Thread thread = new Thread(() -> {
            mController.executeCommands("commands", action -> {
                assertEquals(mAction, action);
                inner.countDown();
            });
            outer.countDown();
        });
        thread.start();
        verifyNoInteractions(mRootContext);

        mController.onDocumentDisplayed(System.currentTimeMillis());
        outer.await();
        shadowOf(Looper.getMainLooper()).idle();
        verify(mRootContext).executeCommands(eq("commands"));
        inner.await();
    }

    @Test
    public void testActionCallbackInvoked() throws InterruptedException {
        mController.onDocumentDisplayed(System.currentTimeMillis());
        CountDownLatch latch = new CountDownLatch(1);
        mController.executeCommands("commands", action -> {
            assertEquals(mAction, action);
            latch.countDown();
        });
        latch.await();
    }

    @Test
    public void testLifecycleEvents() {
        // Verify first resumeDocument() does not do anything
        mController.resumeDocument();
        verify(mRootContext, never()).resumeDocument();
        // Document is displayed. After this all RootContext runnables should execute.
        mController.onDocumentDisplayed(System.currentTimeMillis());
        Robolectric.flushForegroundThreadScheduler();

        mController.resumeDocument();
        verify(mRootContext, times(2)).resumeDocument();

        mController.pauseDocument();
        verify(mRootContext).pauseDocument();

        mController.finishDocument();
        verify(mRootContext).finishDocument();
    }

    @Test
    public void testLifecycleEventsIgnoredAfterFinish() {
        mController.finishDocument();
        verify(mRootContext).finishDocument();

        mController.resumeDocument();


        mController.pauseDocument();

        verify(mRootContext, times(2)).getRootConfig();
        verifyNoMoreInteractions(mRootContext);
    }

    @Test
    public void testDisplayStateChanges() {
        mController.onDocumentDisplayed(System.currentTimeMillis());
        mController.updateDisplayState(DisplayState.kDisplayStateBackground);
        verify(mRootContext).updateDisplayState(eq(DisplayState.kDisplayStateBackground));

        mController.updateDisplayState(DisplayState.kDisplayStateForeground);
        verify(mRootContext).updateDisplayState(eq(DisplayState.kDisplayStateForeground));
    }

    @Test
    public void testOnDocumentDisplayedStopTimer() {
        APLOptions aplOptions = APLOptions.builder()
                .contentCompleteCallback(mContentCompleteCallback)
                .telemetryProvider(mTelemetryProvider).build();

        when(mTelemetryProvider.createMetricId(anyString(), anyString(), eq(ITelemetryProvider.Type.COUNTER)))
                .thenReturn(1);

        APLController controller = (APLController)APLController.builder()
                .aplLayout(mAplLayout)
                .rootConfig(mRootConfig)
                .aplDocument("{}")
                .aplOptions(aplOptions)
                .contentCreator(((aplDocument, options, callbackV2, rootConfig, dtNetworkRequestHandler) -> {callbackV2.onComplete(mContent); return mContent;}))
                .documentSession(mDocumentSession)
                .render();

        controller.onDocumentRender(mRootContext);

        controller.onDocumentDisplayed(System.currentTimeMillis());
        verify(mTelemetryProvider).stopTimer(anyInt(), any(TimeUnit.class), anyLong());
    }

    @Test
    public void testUPFCallback_whenDoesNotInitializeApl_ContentCreationError() {
        // Setup
        try {
            when(mLibraryFuture.get(
                    any(long.class),
                    any(TimeUnit.class)
            )).thenReturn(false);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
        when(mOptions.getContentCompleteCallback()).thenReturn(mContentCompleteCallback);
        when(mOptions.getContentCompleteCallback()).thenReturn(mContentCompleteCallback);
        when(mRootConfig.getExtensionProvider()).thenReturn(mExtensionRegistrar);
        when(mOptions.getExtensionRegistration()).thenReturn(mExtensionRegistration);
        doNothing().when(mExtensionRegistration).registerExtensions(any(), any());
        mockStatic(ExtensionMediator.class);
        when(ExtensionMediator.create(any(), any())).thenReturn(mExtensionMediator);
        doAnswer(invocation -> {
            ExtensionMediator.ILoadExtensionCallback loadExtensionCallback = invocation.getArgument(2);
            loadExtensionCallback.onSuccess().run();
            return null;
        }).when(mExtensionMediator).loadExtensions(any(RootConfig.class), any(), any());
        setLibraryFuture(mLibraryFuture);

        // Test
        APLController controller = (APLController)APLController.builder()
                .aplLayout(mAplLayout)
                .rootConfig(mRootConfig)
                .aplDocument("{}")
                .aplOptions(mOptions)
                .contentCreator(((aplDocument, options, callbackV2, rootConfig, dtNetworkRequestHandler) -> {callbackV2.onComplete(mContent); return mContent;}))
                .documentSession(mDocumentSession)
                .render();

        // Verify
        verify(mUserPerceivedFatalCallback, times(1)).onFatalError(eq(UserPerceivedFatalReporter.UpfReason.APL_INITIALIZATION_FAILURE.toString()));
    }

    @Test
    public void testUPFCallback_whenDoesNotInitializeApl_ContentCreationComplete() {
        // Setup
        try {
            when(mLibraryFuture.get(
                    any(long.class),
                    any(TimeUnit.class)
            )).thenReturn(false);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
        when(mOptions.getContentCompleteCallback()).thenReturn(mContentCompleteCallback);
        when(mOptions.getContentCompleteCallback()).thenReturn(mContentCompleteCallback);
        when(mRootConfig.getExtensionProvider()).thenReturn(mExtensionRegistrar);
        when(mOptions.getExtensionRegistration()).thenReturn(mExtensionRegistration);
        doNothing().when(mExtensionRegistration).registerExtensions(any(), any());
        mockStatic(ExtensionMediator.class);
        when(ExtensionMediator.create(any(), any())).thenReturn(mExtensionMediator);
        doAnswer(invocation -> {
            ExtensionMediator.ILoadExtensionCallback loadExtensionCallback = invocation.getArgument(2);
            loadExtensionCallback.onSuccess().run();
            return null;
        }).when(mExtensionMediator).loadExtensions(any(RootConfig.class), any(), any());
        setLibraryFuture(mLibraryFuture);

        // Test
        APLController controller = (APLController)APLController.builder()
                .aplLayout(mAplLayout)
                .rootConfig(mRootConfig)
                .aplDocument("{}")
                .aplOptions(mOptions)
                .contentCreator(((aplDocument, options, callbackV2, rootConfig, dtNetworkRequestHandler) -> {callbackV2.onComplete(mContent); return mContent;}))
                .documentSession(mDocumentSession)
                .render();

        // Verify
        verify(mUserPerceivedFatalCallback, times(1)).onFatalError(eq(UserPerceivedFatalReporter.UpfReason.APL_INITIALIZATION_FAILURE.toString()));
    }

    @Test
    public void testUPFCallback_whenInitializeApl_ContentCreationError() {
        // Setup
        initializeAPL(mContext, mRuntimeConfig);
        setLibraryFuture(mLibraryFuture);

        // Test
        APLController controller = (APLController)APLController.builder()
                .aplLayout(mAplLayout)
                .rootConfig(mRootConfig)
                .aplDocument("{}")
                .aplOptions(mOptions)
                .contentCreator(((aplDocument, options, callbackV2, rootConfig, dtNetworkRequestHandler) -> {callbackV2.onError(new Exception()); return null;}))
                .documentSession(mDocumentSession)
                .render();

        // Verify
        verify(mUserPerceivedFatalCallback, times(1)).onFatalError(eq(UserPerceivedFatalReporter.UpfReason.CONTENT_CREATION_FAILURE.toString()));
    }

    @Test
    public void testRequiredExtensionLoadingFailure_whenExtensionCallbackReportsFailure() {
        // Setup
        initializeAPL(mContext, mRuntimeConfig);
        when(mOptions.getContentCompleteCallback()).thenReturn(mContentCompleteCallback);
        when(mRootConfig.getExtensionProvider()).thenReturn(mExtensionRegistrar);
        when(mOptions.getExtensionRegistration()).thenReturn(mExtensionRegistration);
        doNothing().when(mExtensionRegistration).registerExtensions(any(), any());
        mockStatic(ExtensionMediator.class);
        when(ExtensionMediator.create(any(), any())).thenReturn(mExtensionMediator);
        doAnswer(invocation -> {
            ExtensionMediator.ILoadExtensionCallback loadExtensionCallback = invocation.getArgument(2);
            loadExtensionCallback.onFailure().run();

            return null;
        }).when(mExtensionMediator).loadExtensions(any(RootConfig.class), any(), any());
        setLibraryFuture(mLibraryFuture);

        // Test
        APLController controller = (APLController)APLController.builder()
                .aplLayout(mAplLayout)
                .rootConfig(mRootConfig)
                .aplDocument("{}")
                .aplOptions(mOptions)
                .contentCreator(((aplDocument, options, callbackV2, rootConfig, dtNetworkRequestHandler) -> {callbackV2.onComplete(mContent); return mContent;}))
                .documentSession(mDocumentSession)
                .disableAsyncInflate(true)
                .render();

        // Verify
        verify(mUserPerceivedFatalCallback, times(1)).onFatalError(eq(UserPerceivedFatalReporter.UpfReason.REQUIRED_EXTENSION_LOADING_FAILURE.toString()));
    }

    @Test
    public void testDisplayStateChangesIgnoredAfterFinish() {
        mController.finishDocument();
        verify(mRootContext).finishDocument();

        mController.updateDisplayState(DisplayState.kDisplayStateHidden);

        verify(mRootContext, times(2)).getRootConfig();
        verifyNoMoreInteractions(mRootContext);
    }

    @Test
    public void testExposesUnderlyingDocVersion() {
        when(mContent.getAPLVersion()).thenReturn("1.7");

        assertEquals(APLVersionCodes.APL_1_7, mController.getDocVersion());

        mController.finishDocument();
        verify(mRootContext).finishDocument();
        verify(mRootContext, times(2)).getRootConfig();

        mController.getDocVersion();

        verifyNoMoreInteractions(mRootContext);
    }

    @Test
    public void testRenderV2_addsDocumentLifecycleListeners() {
        ITelemetryProvider mockTelemetry = mock(ITelemetryProvider.class);
        APLOptions aplOptions = APLOptions.builder()
                .telemetryProvider(mockTelemetry).build();

        APLController.builder()
                .aplLayout(mAplLayout)
                .rootConfig(mRootConfig)
                .aplDocument("{}")
                .aplOptions(aplOptions)
                .contentCreator(((aplDocument, options, callbackV2, rootConfig, dtNetworkRequestHandler) -> mContent))
                .documentSession(mDocumentSession)
                .render();

        verify(mViewPresenter).addDocumentLifecycleListener(mockTelemetry);
    }

    @Test
    public void testRenderV2_addsRootConfigLifecycleListeners() {
        Collection<IDocumentLifecycleListener> listeners = new LinkedList<>();
        listeners.add(mExtensionMediator);
        listeners.add(mTtsPlayerProvider);
        listeners.add(mMediaPlayerProvider);
        listeners.add(null);
        when(mRootConfig.getDocumentLifecycleListeners()).thenReturn(listeners);
        ITelemetryProvider mockTelemetry = mock(ITelemetryProvider.class);
        APLOptions aplOptions = APLOptions.builder()
                .telemetryProvider(mockTelemetry).build();

        APLController.builder()
                .aplLayout(mAplLayout)
                .rootConfig(mRootConfig)
                .aplDocument("{}")
                .aplOptions(aplOptions)
                .contentCreator(((aplDocument, options, callbackV2, rootConfig, dtNetworkRequestHandler) -> mContent))
                .documentSession(mDocumentSession)
                .render();

        verify(mViewPresenter).addDocumentLifecycleListener(mockTelemetry);
        verify(mViewPresenter).addDocumentLifecycleListener(mExtensionMediator);
        verify(mViewPresenter).addDocumentLifecycleListener(mMediaPlayerProvider);
        verify(mViewPresenter).addDocumentLifecycleListener(mTtsPlayerProvider);
    }

    @Test
    public void testRenderV2_contentCompleteCallbackOnComplete() {
        RuntimeConfig runtimeConfig = RuntimeConfig.builder().
                fontResolver(new CompatFontResolver()).
                bitmapPool(mock(IBitmapPool.class)).
                build();
        TypefaceResolver.getInstance().initialize(InstrumentationRegistry.getInstrumentation().getContext(),
                runtimeConfig);

        APLController.initializeAPL(InstrumentationRegistry.getInstrumentation().getContext(), runtimeConfig);
        ITelemetryProvider mockTelemetry = mock(ITelemetryProvider.class);
        setLibraryFuture(mLibraryFuture);
        APLOptions aplOptions = APLOptions.builder()
                .contentCompleteCallback(mContentCompleteCallback)
                .telemetryProvider(mockTelemetry).build();


        APLController.builder()
                .aplLayout(mAplLayout)
                .rootConfig(mRootConfig)
                .aplDocument("{}")
                .aplOptions(aplOptions)
                .contentCreator(((aplDocument, options, callbackV2, rootConfig, dtNetworkRequestHandler) -> {callbackV2.onComplete(mContent); return mContent;}))
                .documentSession(mDocumentSession)
                .render();

        verify(mViewPresenter).addDocumentLifecycleListener(mockTelemetry);
        verify(mContentCompleteCallback).onComplete();
    }

    @Test
    public void testExecuteCommandsCallback_is_called_when_action_is_null() throws InterruptedException {
        // Setup RootContext to return null action
        when(mRootContext.executeCommands(any(String.class))).thenReturn(null);
        IAPLController.ExecuteCommandsCallback callback = mock(IAPLController.ExecuteCommandsCallback.class);
        // Act
        mController.onDocumentDisplayed(System.currentTimeMillis());
        mController.executeCommands("commands", callback);
        // Verify that callback is still called with null action
        verify(callback).onExecuteCommands(null);
    }

    @Test
    public void waitForAPLInitialize_futureSet_futureCheckedTrueReturned() {
        final AtomicBoolean futureValChecked = new AtomicBoolean(false);

        Future f = new Future() {
            @Override
            public boolean cancel(boolean b) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public Object get() throws ExecutionException, InterruptedException {
                futureValChecked.set(true);
                return true;
            }

            @Override
            public Object get(long l, TimeUnit timeUnit) throws ExecutionException, InterruptedException, TimeoutException {
                futureValChecked.set(true);
                return true;
            }
        };

        setLibraryFuture(f);

        assertTrue(APLController.waitForInitializeAPLToComplete(null));
        assertTrue(futureValChecked.get());
    }

    @Test
    public void waitForAPLInitialize_initializeAPLNotCalledFirst_falseReturned() {
        // mimic initializeAPL not being called
        setLibraryFuture(null);

        assertFalse(APLController.waitForInitializeAPLToComplete(mock(ITelemetryProvider.class)));
    }
}
