/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.alexaext.ExtensionRegistrar;
import com.amazon.alexaext.ExtensionProxy;
import com.amazon.alexaext.ExtensionResourceProvider;
import com.amazon.apl.android.dependencies.ExtensionFilterParameters;
import com.amazon.apl.android.dependencies.IExtensionEventCallback;
import com.amazon.apl.android.dependencies.IExtensionImageFilterCallback;
import com.amazon.common.BoundObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Protected API. This class mediates message passing between Extension and the APL engine.
 */
class ExtensionMediator extends BoundObject implements IExtensionEventCallback,
        IExtensionImageFilterCallback, IDocumentLifecycleListener {

    ExtensionResourceProvider extensionResourceProvider;
    ExtensionRegistrar extensionRegistrar;
    APLExtensionExecutor mExecutor;
    private static final String TAG = "ExtensionMediator";
    private Map<String, IExtensionImageFilterCallback> mCallbacks = new HashMap<>();
    private IExtensionGrantRequestCallback mExtensionGrantRequestCallback;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private String mLogId;

    /**
     * Callback to be invoked when all extensions which is required were loaded.
     */
    public interface ExtensionMediatorCallback {
        void onLoaded();
    }

    public interface IExtensionGrantRequestCallback {
        boolean isExtensionGranted(final String uri);
    }

    private Runnable mOnCompleteCallback;

    public static ExtensionMediator create(@NonNull final ExtensionRegistrar registrar, @NonNull DocumentSession session) {
        ExtensionResourceProvider resourceProvider = new ExtensionResourceProvider();
        APLExtensionExecutor executor = new APLExtensionExecutor();
        ExtensionMediator mediator = new ExtensionMediator(registrar,
                resourceProvider, executor, session);
        mediator.extensionResourceProvider = resourceProvider;
        mediator.extensionRegistrar = registrar;
        return mediator;
    }

    private ExtensionMediator(ExtensionRegistrar provider, ExtensionResourceProvider resourceProvider, APLExtensionExecutor executor, DocumentSession session) {
        mExecutor = executor;
        final long handle = nCreate(provider.getNativeHandle(), resourceProvider.getNativeHandle(), executor.getNativeHandle(), session.getNativeHandle());
        bind(handle);
    }

    public void initializeExtensions(RootConfig rootConfig, Content content, @NonNull IExtensionGrantRequestCallback extensionGrantRequestCallback) {
        mExtensionGrantRequestCallback = extensionGrantRequestCallback;
        mLogId = rootConfig.getSession().getLogId();
        nInitializeExtensions(getNativeHandle(), rootConfig.getNativeHandle(), content.getNativeHandle());
    }

    public void loadExtensions(RootConfig rootConfig, Content content, Runnable onComplete) {
        mOnCompleteCallback = onComplete;
        nLoadExtensions(getNativeHandle(), rootConfig.getNativeHandle(), content.getNativeHandle());
    }

    // TODO: Not used. We will likely not need IExtensionEventCallback after resource holder stuff are in.
    @Override
    public void onExtensionEvent(String name, String uri, Map<String, Object> source, Map<String, Object> custom, IExtensionEventCallbackResult resultCallback) {}

    @SuppressWarnings("unused")
    private boolean isExtensionGranted(final String uri) {
        return mExtensionGrantRequestCallback.isExtensionGranted(uri);
    }

    @NonNull
    @SuppressWarnings("unused")
    private void onExtensionsLoaded() {
        if (mOnCompleteCallback != null) {
            mOnCompleteCallback.run();
            mOnCompleteCallback = null;
        } else {
            Log.wtf(TAG, "OnComplete already called for this document.");
        }
    }

    // Register image filters in the Root config.
    // TODO Deprecate, this is clumsy legacy integration.
    void registerImageFilters(ExtensionRegistrar provider, Content content, RootConfig config) {
        for (String extensionRequest : content.getExtensionRequests()) {
            ExtensionProxy extensionProxy = provider.getExtension(extensionRequest);
            if (extensionProxy != null) {
                if (extensionProxy instanceof LegacyLocalExtensionProxy) {
                    List<ExtensionFilterDefinition> definitions = ((LegacyLocalExtensionProxy) extensionProxy).getFilterDefinitions();
                    for (ExtensionFilterDefinition definition : definitions) {
                        config.registerExtensionFilter(definition);
                    }
                    if (!definitions.isEmpty()) {
                        mCallbacks.put(extensionProxy.getUri(), ((LegacyLocalExtensionProxy) extensionProxy).getFilterCallback());
                    }
                }
            }
        }
    }

    @NonNull
    @Override
    public Bitmap processImage(@Nullable Bitmap sourceBitmap, @Nullable Bitmap destinationBitmap, ExtensionFilterParameters params) {
        IExtensionImageFilterCallback cb = mCallbacks.get(params.getURI());
        if (cb == null) return sourceBitmap;
        return cb.processImage(sourceBitmap, destinationBitmap, params);
    }

    /// Replicate what runtime implementation currently does. Deprecate in future.
    @Override
    public synchronized void onDocumentRender(@NonNull final RootContext rootContext) {
        mExecutor.setRootContext(rootContext);
    }

    public void enable(boolean enabled) {
        nEnable(getNativeHandle(), enabled);
    }

    /**
     * Invoked by a viewhost when the session associated with this mediator (if it has been
     * previously set) has ended.
     */
    void onSessionEnded() {
        nOnSessionEnded(getNativeHandle());
    }

    private native long nCreate(long providerHandler_, long resourceProviderHandler_, long executorHandler_, long sessionHandler_);
    private static native void nInitializeExtensions(long mediatorHandler_, long rootConfigHandler_, long contentHandler_);
    private static native void nLoadExtensions(long mediatorHandler_, long rootConfigHandler_, long contentHandler_);
    private static native void nEnable(long mediatorHandler_, boolean enabled);
    private static native void nOnSessionEnded(long mediatorHandler_);
}
