/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import static android.os.Looper.getMainLooper;

import androidx.annotation.NonNull;

import com.amazon.alexaext.ActivityDescriptor;
import com.amazon.alexaext.ExtensionProxy;
import com.amazon.alexaext.ExtensionRegistrar;
import com.amazon.alexaext.IExtensionProvider;
import com.amazon.alexaext.ILiveDataUpdateCallback;
import com.amazon.apl.android.ExtensionMediator.ILoadExtensionCallback;
import com.amazon.apl.android.providers.IExtension;
import com.amazon.apl.android.providers.ITelemetryProvider;
import com.amazon.apl.android.robolectric.ViewhostRobolectricTest;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

public class ExtensionMediatorTest extends ViewhostRobolectricTest {
    // Test content
    private final String mTestDoc = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.0\"," +
            "  \"extensions\": [" +
            "    {\n" +
            "      \"name\": \"A\"," +
            "      \"uri\": \"alexaext:myextA:10\"" +
            "    }," +
            "    {\n" +
            "      \"name\": \"B\"," +
            "      \"uri\": \"alexaext:myextB:10\"" +
            "    }" +
            "  ]," +
            "  \"mainTemplate\": {" +
            "    \"item\": {" +
            "      \"type\": \"Text\"" +
            "    }" +
            "  }" +
            "}";

    private ILoadExtensionCallback mLoadExtensionCallback;
    private CountDownLatch mCallbackLatch;
    private AtomicInteger mLoadOnSuccessCallbackCount;
    private AtomicInteger mLoadOnFailureCallbackCount;
    @Mock
    private ITelemetryProvider mTelemetryProvider;
    @Mock
    private APLOptions mOptions;
    @Mock
    private Content.CallbackV2 mContentCallbackV2;
    @Mock
    private ExtensionRegistrar mExtensionRegistrar;
    @Mock
    private IExtensionProvider mExtensionProvider;

    @Mock
    private Session mSession;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        when(mOptions.getTelemetryProvider()).thenReturn(mTelemetryProvider);
        Collection<ExtensionProxy> remoteExtensionProxies = Arrays.asList();
        when(mExtensionRegistrar.getExtensions()).thenReturn(remoteExtensionProxies);

        mCallbackLatch = new CountDownLatch(1);

        mLoadOnSuccessCallbackCount = new AtomicInteger();
        mLoadOnFailureCallbackCount = new AtomicInteger();

        mLoadExtensionCallback = new ExtensionMediator.ILoadExtensionCallback() {
            @Override
            public Runnable onSuccess() {
                return () -> {
                    mCallbackLatch.countDown();
                    mLoadOnSuccessCallbackCount.incrementAndGet();
                };
            }

            @Override
            public Runnable onFailure() {
                return () -> {
                    mCallbackLatch.countDown();
                    mLoadOnFailureCallbackCount.incrementAndGet();
                };
            }
        };
    }

    @Test
    public void testCreate() {
        ExtensionMediator mediator = ExtensionMediator.create(mExtensionRegistrar, DocumentSession.create());
        assertTrue(mediator.isBound());
    }

    @Test
    public void testLoadExtensions_calls_onExtensionsLoadedCallback_only_once() throws InterruptedException {
        loadExtensions();
        mCallbackLatch.await(2, TimeUnit.SECONDS);
        assertEquals(0, mCallbackLatch.getCount());
        assertEquals(1, mLoadOnSuccessCallbackCount.get());
    }

    @Test
    public void testInitializeExtensions_specific_grantedExtensions_loads_only_those_extensions() throws InterruptedException {
        Content content = Content.create(mTestDoc, mOptions, mContentCallbackV2, mSession);
        RootConfig rootConfig = RootConfig.create();
        ExtensionRegistrar extensionRegistrar = new ExtensionRegistrar().addProvider(mExtensionProvider);
        ExtensionMediator mediator = ExtensionMediator.create(extensionRegistrar, DocumentSession.create());
        mediator.initializeExtensions(rootConfig, content, (uri) -> {
            if ("alexaext:myextA:10".equals(uri)) {
                return true;
            }
            return false;
        });
        mediator.loadExtensions(rootConfig, content, mLoadExtensionCallback);
        mCallbackLatch.await(2, TimeUnit.SECONDS);
        verify(mExtensionProvider).hasExtension("alexaext:myextA:10");
        verify(mExtensionProvider, never()).hasExtension("alexaext:myextB:10");
    }

    @Test
    public void testInitializeExtensions_without_rootConfig_specific_grantedExtensions_loads_only_those_extensions() throws InterruptedException {
        Content content = Content.create(mTestDoc, mOptions, mContentCallbackV2, mSession);
        ExtensionRegistrar extensionRegistrar = new ExtensionRegistrar().addProvider(mExtensionProvider);
        ExtensionMediator mediator = ExtensionMediator.create(extensionRegistrar, DocumentSession.create());
        mediator.initializeExtensions(new HashMap<>(), content, (uri) -> {
            if ("alexaext:myextA:10".equals(uri)) {
                return true;
            }
            return false;
        });
        mediator.loadExtensions(new HashMap<>(), content, mLoadExtensionCallback);
        mCallbackLatch.await(2, TimeUnit.SECONDS);
        verify(mExtensionProvider).hasExtension("alexaext:myextA:10");
        verify(mExtensionProvider, never()).hasExtension("alexaext:myextB:10");
    }

    @Test
    public void testExecutor() throws InterruptedException {
        // given
        RootConfig rootConfig = RootConfig.create();
        Content content = Content.create(mTestDoc, mOptions, mContentCallbackV2, mSession);
        TestLiveDataLocalExtension extension = spy(new TestLiveDataLocalExtension());
        LegacyLocalExtensionProxy legacyLocalExtensionProxy = new LegacyLocalExtensionProxy(extension);
        ExtensionRegistrar extensionRegistrar = new ExtensionRegistrar().addProvider(mExtensionProvider);
        extensionRegistrar.registerExtension(legacyLocalExtensionProxy);
        ExtensionMediator mediator = ExtensionMediator.create(extensionRegistrar, DocumentSession.create());

        // when
        mediator.initializeExtensions(rootConfig, content, uri -> true);
        mediator.loadExtensions(rootConfig, content, mLoadExtensionCallback);

        mCallbackLatch.await(2, TimeUnit.SECONDS);

        shadowOf(getMainLooper()).idle();

        // then: should not segfault in JNI native code
        verify(extension).initialize(any(), any());
        verify(extension).onRegistered(eq(TestLiveDataLocalExtension.URI), anyString());
    }

    @Test
    public void testExecutorWithoutRootConfig() throws InterruptedException {
        // given
        Content content = Content.create(mTestDoc, mOptions, mContentCallbackV2, mSession);
        TestLiveDataLocalExtension extension = spy(new TestLiveDataLocalExtension());
        LegacyLocalExtensionProxy legacyLocalExtensionProxy = new LegacyLocalExtensionProxy(extension);
        ExtensionRegistrar extensionRegistrar = new ExtensionRegistrar().addProvider(mExtensionProvider);
        extensionRegistrar.registerExtension(legacyLocalExtensionProxy);
        ExtensionMediator mediator = ExtensionMediator.create(extensionRegistrar, DocumentSession.create());

        // when
        mediator.initializeExtensions(new HashMap<>(), content, uri -> true);
        mediator.loadExtensions(new HashMap<>(), content, mLoadExtensionCallback);

        mCallbackLatch.await(2, TimeUnit.SECONDS);

        shadowOf(getMainLooper()).idle();

        // then: should not segfault in JNI native code
        verify(extension).initialize(any(), any());
        verify(extension).onRegistered(eq(TestLiveDataLocalExtension.URI), anyString());
    }

    private ExtensionMediator loadExtensions() {
        Content content = Content.create(mTestDoc, mOptions, mContentCallbackV2, mSession);
        RootConfig rootConfig = RootConfig.create();
        ExtensionMediator mediator = ExtensionMediator.create(mExtensionRegistrar, DocumentSession.create());
        mediator.initializeExtensions(rootConfig, content, null);
        mediator.loadExtensions(rootConfig, content, mLoadExtensionCallback);
        return mediator;
    }


    private final String requiredExtension = "{" +
            "  \"type\": \"APL\"," +
            "  \"version\": \"1.0\"," +
            "  \"extensions\": [" +
            "    {\n" +
            "      \"name\": \"A\"," +
            "      \"uri\": \"alexaext:myextA:10\"," +
            "      \"required\": true" +
            "    }" +
            "  ]," +
            "  \"mainTemplate\": {" +
            "    \"item\": {" +
            "      \"type\": \"Text\"" +
            "    }" +
            "  }" +
            "}";

    @Test
    public void test_loadExtensions_requiredExtension_registeredAndGranted_callsOnSuccessCallback() throws InterruptedException {
        // given
        Content content = Content.create(requiredExtension, mOptions, mContentCallbackV2, mSession);
        ExtensionRegistrar extensionRegistrar = new ExtensionRegistrar();
        TestLiveDataLocalExtension extension = new TestLiveDataLocalExtension();
        LegacyLocalExtensionProxy legacyLocalExtensionProxy = new LegacyLocalExtensionProxy(extension);
        extensionRegistrar.registerExtension(legacyLocalExtensionProxy);
        ExtensionMediator mediator = ExtensionMediator.create(extensionRegistrar, DocumentSession.create());
        // when
        mediator.initializeExtensions(new HashMap<>(), content, uri -> true);
        mediator.loadExtensions(new HashMap<>(), content, mLoadExtensionCallback);
        mCallbackLatch.await(2, TimeUnit.SECONDS);
        shadowOf(getMainLooper()).idle();
        // then
        assertEquals(0, mCallbackLatch.getCount());
        assertEquals(1, mLoadOnSuccessCallbackCount.get());
    }

    @Test
    public void test_loadExtensions_requiredExtension_notRegistered_callsOnFailCallback() throws InterruptedException {
        // given that the required extension is not registered in the ExtensionRegistrar
        Content content = Content.create(requiredExtension, mOptions, mContentCallbackV2, mSession);
        ExtensionRegistrar extensionRegistrar = new ExtensionRegistrar();
        ExtensionMediator mediator = ExtensionMediator.create(extensionRegistrar, DocumentSession.create());
        // when callback grants this extension
        mediator.initializeExtensions(new HashMap<>(), content, uri -> true);
        mediator.loadExtensions(new HashMap<>(), content, mLoadExtensionCallback);
        mCallbackLatch.await(2, TimeUnit.SECONDS);
        // then onFailure callback should be called
        assertEquals(0, mCallbackLatch.getCount());
        assertEquals(1, mLoadOnFailureCallbackCount.get());
    }

    @Test
    public void test_loadExtensions_requiredExtension_notGranted_callsOnFailCallback() throws InterruptedException {
        // given
        Content content = Content.create(requiredExtension, mOptions, mContentCallbackV2, mSession);
        ExtensionRegistrar extensionRegistrar = new ExtensionRegistrar();
        TestLiveDataLocalExtension extension = spy(new TestLiveDataLocalExtension());
        LegacyLocalExtensionProxy legacyLocalExtensionProxy = new LegacyLocalExtensionProxy(extension);
        extensionRegistrar.registerExtension(legacyLocalExtensionProxy);
        ExtensionMediator mediator = ExtensionMediator.create(extensionRegistrar, DocumentSession.create());
        // when callback doesn't grant this extension
        mediator.initializeExtensions(new HashMap<>(), content, uri -> false);
        mediator.loadExtensions(new HashMap<>(), content, mLoadExtensionCallback);
        mCallbackLatch.await(2, TimeUnit.SECONDS);
        // then onFailure callback should be called
        assertEquals(0, mCallbackLatch.getCount());
        assertEquals(1, mLoadOnFailureCallbackCount.get());
    }

    @Test
    public void test_loadExtensions_requiredExtension_failsRegistration_callsOnFailCallback() throws InterruptedException {
        // given a proxy that fails registration
        ProxyWithFailingRegistration legacyLocalExtensionProxy = new ProxyWithFailingRegistration(new TestLiveDataLocalExtension());
        ExtensionRegistrar extensionRegistrar = new ExtensionRegistrar().registerExtension(legacyLocalExtensionProxy);
        ExtensionMediator mediator = ExtensionMediator.create(extensionRegistrar, DocumentSession.create());
        Content content = Content.create(requiredExtension, mOptions, mContentCallbackV2, mSession);
        // when
        mediator.initializeExtensions(new HashMap<>(), content, uri -> true);
        mediator.loadExtensions(new HashMap<>(), content, mLoadExtensionCallback);
        mCallbackLatch.await(2, TimeUnit.SECONDS);
        // then onFailure callback should be called
        assertEquals(0, mCallbackLatch.getCount());
        assertEquals(1, mLoadOnFailureCallbackCount.get());
    }

    class ProxyWithFailingRegistration extends LegacyLocalExtensionProxy {
        public ProxyWithFailingRegistration(IExtension extension) {
            super(extension);
        }

        @Override
        protected boolean requestRegistration(ActivityDescriptor activity, String request) {
            return false;
        };
    }

    public static class TestLiveDataLocalExtension extends LegacyLocalExtension {

        private static final String URI = "alexaext:myextA:10";
        private LiveMapAdapter mLiveMapAdapter;

        @Override
        public void onExtensionEvent(String name, String uri, Map<String, Object> source,
                                     Map<String, Object> custom,
                                     IExtensionEventCallbackResult resultCallback) { }

        @NonNull
        @Override
        public String getUri() {
            return URI;
        }

        @Override
        public boolean initialize(ISendExtensionEventCallback eventCallback, ILiveDataUpdateCallback liveDataCallback) {
            mLiveMapAdapter = LiveMapAdapter.create(liveDataCallback, URI, "liveData", Collections.EMPTY_MAP);
            return true;
        }

        @Override
        public List<LiveDataAdapter> getLiveData() {
            return Collections.singletonList(mLiveMapAdapter);
        }

        @Override
        public void onRegistered(String uri, String token) {
            mLiveMapAdapter.put("key", "value");
        }
    }
}
