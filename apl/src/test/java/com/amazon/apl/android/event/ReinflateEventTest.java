/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.event;

import static junit.framework.TestCase.assertTrue;

import android.view.View;

import com.amazon.apl.android.APLJSONData;
import com.amazon.apl.android.APLOptions;
import com.amazon.apl.android.Content;
import com.amazon.apl.android.ExtensionMediator;
import com.amazon.apl.android.IAPLViewPresenter;
import com.amazon.apl.android.RootConfig;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.configuration.ConfigurationChange;
import com.amazon.apl.android.dependencies.IContentRetriever;
import com.amazon.apl.android.dependencies.IDataSourceContextListener;
import com.amazon.apl.android.dependencies.IPackageLoader;
import com.amazon.apl.android.dependencies.ISendEventCallbackV2;
import com.amazon.apl.android.dependencies.IVisualContextListener;
import com.amazon.apl.android.document.AbstractDocUnitTest;
import com.amazon.apl.android.providers.AbstractMediaPlayerProvider;
import com.amazon.apl.android.scaling.ViewportMetrics;
import com.amazon.apl.android.utils.APLTrace;
import com.amazon.apl.devtools.models.network.IDTNetworkRequestHandler;
import com.amazon.apl.enums.ScreenShape;
import com.amazon.apl.enums.ViewportMode;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ReinflateEventTest extends AbstractDocUnitTest {
    @Mock
    private ISendEventCallbackV2 mMockSendEventCallback;
    @Mock
    private AbstractMediaPlayerProvider<View> mMockMediaPlayerProvider;
    @Mock
    private IDataSourceContextListener mMockDataSourceContextListener;
    @Mock
    private IVisualContextListener mMockVisualContextListener;
    @Mock
    private IPackageLoader mPackageLoader;
    @Mock
    private ExtensionMediator mMediator;
    @Mock
    private IDTNetworkRequestHandler mDTNetworkRequestHandler;

    private static final String REINFLATE_DOC = "{\n" +
            "    \"type\": \"APL\",\n" +
            "    \"version\": \"1.5\",\n" +
            "    \"mainTemplate\": {\n" +
            "        \"item\": {\n" +
            "            \"type\": \"Frame\",\n" +
            "            \"id\": \"frame\",\n" +
            "            \"width\": 100,\n" +
            "            \"height\": 100,\n" +
            "            \"backgroundColor\": \"green\"\n" +
            "        }\n" +
            "    }\n," +
            "    \"onConfigChange\": [\n" +
            "        {\n" +
            "            \"type\": \"SendEvent\",\n" +
            "            \"sequencer\": \"ConfigSendEvent\",\n" +
            "            \"arguments\": [ \"reinflating the APL document\"]\n" +
            "        },\n" +
            "        {\n" +
            "            \"type\": \"Reinflate\"\n" +
            "        }\n" +
            "    ]\n" +
            "}";
    private static String REINFLATE_WITH_CONDITIONALIMPORTS = "{\n" +
            "  \"type\": \"APL\",\n" +
            "  \"version\": \"1.6\",\n" +
            "  \"theme\": \"dark\",\n" +
            "  \"import\": [\n" +
            "    {\n" +
            "      \"when\": \"${environment.key == 'value'}\",\n" +
            "      \"name\": \"test-package\",\n" +
            "      \"version\": \"1.0\"\n" +
            "    }\n" +
            "  ],\n" +
            "    \"onConfigChange\": [\n" +
            "        {\n" +
            "            \"type\": \"SendEvent\",\n" +
            "            \"sequencer\": \"ConfigSendEvent\",\n" +
            "            \"arguments\": [ \"reinflating the APL document\"]\n" +
            "        },\n" +
            "        {\n" +
            "            \"type\": \"Reinflate\"\n" +
            "        }\n" +
            "    ],\n" +
            "  \"mainTemplate\": {\n" +
            "    \"items\": [\n" +
            "      {\n" +
            "        \"type\": \"Container\",\n" +
            "        \"items\": [\n" +
            "          {\n" +
            "            \"type\": \"Frame\"\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";
    private final String mTestPackage = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.0\"" +
            "}";
    private ViewportMetrics mMetrics;
    private APLOptions mAplOptions;

    @Before
    public void setup() {
        doAnswer(invocation -> {
            Content.ImportRequest request = invocation.getArgument(0);
            IContentRetriever.SuccessCallback<Content.ImportRequest, APLJSONData> successCallback = invocation.getArgument(1);
            if ("test-package".equals(request.getPackageName())) {
                successCallback.onSuccess(request, APLJSONData.create(mTestPackage));
            }
            return null;
        }).when(mPackageLoader).fetch(any(), any(), any());

        mMetrics = ViewportMetrics.builder()
                .width(1280)
                .height(720)
                .dpi(160)
                .shape(ScreenShape.RECTANGLE)
                .theme("dark")
                .mode(ViewportMode.kViewportModeHub)
                .build();


        mAplOptions = APLOptions.builder()
                .mediaPlayerProvider(mMockMediaPlayerProvider)
                .sendEventCallbackV2(mMockSendEventCallback)
                .packageLoader(mPackageLoader)
                .dataSourceContextListener(mMockDataSourceContextListener)
                .visualContextListener(mMockVisualContextListener)
                .build();
    }
    @Test
    public void testReinflateWithConditionalImports() throws InterruptedException {
        CountDownLatch contentComplete = new CountDownLatch(1);
        mRootConfig = buildRootConfig();
        mRootConfig.extensionMediator(mMediator);
        Content.create(REINFLATE_WITH_CONDITIONALIMPORTS, mAplOptions, new Content.CallbackV2() {
            @Override
            public void onPackageLoaded(Content content) {
                super.onComplete(content);
            }
            @Override
            public void onError(Exception e) {
                Assert.fail(e.getCause().getMessage());
            }
            @Override
            public void onComplete(Content content) {
                Content spyContent = spy(content);
                assertTrue(content.isReady());
                mAPLPresenter = mock(IAPLViewPresenter.class);
                when(mAPLPresenter.getAPLTrace()).thenReturn(mock(APLTrace.class));
                when(mAPLPresenter.getOrCreateViewportMetrics()).thenReturn(mMetrics);
                when(mMetricsRecorder.createCounter(anyString())).thenReturn(mCounter);
                when(mMetricsRecorder.startTimer(anyString(), any())).thenReturn(mTimer);
                mRootContext = spy(RootContext.create(mMetrics, spyContent, mRootConfig, mAplOptions, mAPLPresenter, mMetricsRecorder, mFluidityIncidentReporter));

                assertNotNull(mRootContext);


                mRootContext.initTime();
                mTime = 100;
                mRootContext.onTick(mTime);

                doAnswer(invocation -> {
                    ExtensionMediator.ILoadExtensionCallback callback = invocation.getArgument(2);
                    Runnable runnable = callback.onSuccess();
                    runnable.run();
                    return null;
                }).when(mMediator).loadExtensions(any(RootConfig.class), any(Content.class), any(ExtensionMediator.ILoadExtensionCallback.class));

                // Dummy config
                ConfigurationChange configChangeToNonTrueValue = mRootContext.createConfigurationChange().environmentValue("key", "shouldBeFalse")
                        .build();

                mRootContext.handleConfigurationChange(configChangeToNonTrueValue);

                update(100);

                verify(mRootContext).reinflate();

                verify(spyContent, never()).resolve(any(Content.CallbackV2.class), any(RootConfig.class));
                verify(mRootContext.getViewPresenter()).reinflate();

                // Dummy config
                ConfigurationChange configChange = mRootContext.createConfigurationChange().environmentValue("key", "value")
                        .build();

                mRootContext.handleConfigurationChange(configChange);

                update(100);

                verify(mRootContext, times(2)).reinflate();
                verify(spyContent).resolve(any(Content.CallbackV2.class), any(RootConfig.class));
                verify(mRootContext.getViewPresenter(), times(2)).reinflate();

                contentComplete.countDown();
            }
        }, mRootConfig, mDTNetworkRequestHandler);
        Assert.assertTrue(contentComplete.await(1, TimeUnit.SECONDS));
    }
    @Test
    public void testReinflate_rootContextReinflate_invoked() throws JSONException {

        loadDocument(REINFLATE_DOC, mAplOptions, mMetrics);

        // Dummy config
        ConfigurationChange configChange = mRootContext.createConfigurationChange()
                .build();

        mRootContext.handleConfigurationChange(configChange);

        update(100);

        String[] arguments = new String[]{"reinflating the APL document"};
        Map<String, Object> components = new HashMap<>();
        Map<String, Object> sources = new HashMap<>();
        sources.put("handler", "ConfigChange");
        sources.put("uid", null);
        sources.put("id", null);
        sources.put("source", "Document");
        sources.put("type", "Document");
        sources.put("value", null);
        verify(mMockSendEventCallback).onSendEvent(arguments, components, sources, null);

        // Verify that the document is paused and document lifecycle onDocumentPaused() is executed.
        InOrder inOrder = Mockito.inOrder(mRootContext, mMockDataSourceContextListener, mMockVisualContextListener, mAPLPresenter, mMockMediaPlayerProvider);
        inOrder.verify(mRootContext).reinflate();
        inOrder.verify(mMockMediaPlayerProvider).releasePlayers();
        inOrder.verify(mRootContext).pauseDocument();
        inOrder.verify(mAPLPresenter).onDocumentPaused();
        ArgumentCaptor<JSONObject> visualContextCaptor = ArgumentCaptor.forClass(JSONObject.class);
        inOrder.verify(mMockVisualContextListener).onVisualContextUpdate(visualContextCaptor.capture());
        // The line below will throw an exception if the frame is not present in the visual context.
        assertEquals("frame", visualContextCaptor.getValue().getString("id"));
        inOrder.verify(mMockDataSourceContextListener).onDataSourceContextUpdate(any());
        inOrder.verify(mAPLPresenter).reinflate();
        // Verify that repeated calls to pauseDocument() and resumeDocument() are ignored until initTime() is called.
        reset(mAPLPresenter);
        verifyPauseAndResumeIgnored();
        // Verify that a onTick() call does not post the callback again.
        mRootContext.onTick(1);
        verifyPauseAndResumeIgnored();
        // Verify that after initTime() is called, resumeDocument() and pauseDocument() are processed normally.
        mRootContext.initTime();
        inOrder.verify(mRootContext).resumeDocument();
        inOrder.verify(mAPLPresenter).onDocumentResumed();
        mRootContext.pauseDocument();
        verify(mAPLPresenter).onDocumentPaused();
    }

    private void verifyPauseAndResumeIgnored() {
        mRootContext.pauseDocument();
        verify(mAPLPresenter, never()).onDocumentPaused();
        mRootContext.resumeDocument();
        verify(mAPLPresenter, never()).onDocumentResumed();
    }

    // TODO: Add integration tests corresponding to Scaling cases.
}
