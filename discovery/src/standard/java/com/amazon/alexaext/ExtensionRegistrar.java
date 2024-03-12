/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.alexaext;

import android.util.Log;

import androidx.annotation.NonNull;

import com.amazon.common.BoundObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Interface that defines access to the extensions supported by the runtime. Effectively a collection of directly registered local (built-in)
 */
public class ExtensionRegistrar extends BoundObject {

    private final static String TAG = "ExtensionRegistrar";
    private final Map<String, ExtensionProxy> mProxies;
    private final Set<IExtensionProvider> mProviders;

    public ExtensionRegistrar() {
        final long handle = nCreate();
        bind(handle);
        mProxies = new HashMap<>();
        mProviders = new HashSet<>();
    }

    /**
     * Add a specific ExtensionProvider.
     * @param provider Provider implementation.
     * @return this for chaining.
     */
    public ExtensionRegistrar addProvider(IExtensionProvider provider) {
        mProviders.add(provider);
        return this;
    }

    /**
     * Register an extension. Called by the runtime to register a known extension.
     *
     * @param proxy The extension proxy.
     * @return This object for chaining.
     */
    public ExtensionRegistrar registerExtension(ExtensionProxy proxy) {
        mProxies.put(proxy.getUri(), proxy);
        return this;
    }

    /**
     * Explicitly closes all V1 open connections of RemoteExtensionProxy.
     * This method will further call onConnectionClosed of the ExtensionMultiplexClient.ConnectionCallback.
     */
    public void closeAllRemoteV1Connections(){
        Log.i(TAG, "Closing connections to all remote extensions");
        int count = 0;
        for (ExtensionProxy extensionProxy: mProxies.values()) {
            if (extensionProxy instanceof RemoteExtensionProxy) {
                RemoteExtensionProxy proxy = (RemoteExtensionProxy)extensionProxy;
                try {
                    proxy.disconnectV1();
                    count++;
                } catch (Throwable e) {
                    Log.e(TAG, "Exception caused while cleaning connections with cause: " + e);
                }
            }
        }
        Log.i(TAG, String.format("Closing connections to all remote extensions (count=%d)", count));
    }

    /**
     * Identifies the presence of an extension.  Called when a document has
     * requested an extension. This method returns true if an extension matching
     * the given uri has been registered.
     *
     * @param uri The requsted extension URI.
     * @return true if the extension is registered.
     */
    @NonNull
    @SuppressWarnings("unused")
    public boolean hasExtension(String uri) {
        if (mProxies.containsKey(uri)) return true;

        for (IExtensionProvider provider : mProviders) {
            if (provider.hasExtension(uri)) return true;
        }
        return false;
    }

    /**
     * Get a proxy to the extension.  Called when a document has requested
     * an extension.
     *
     * @param uri The extension URI.
     * @return An extension proxy of a registered extension, nullptr if the extension
     * was not registered.
     */
    public ExtensionProxy getExtension(String uri) {
        ExtensionProxy proxy = mProxies.get(uri);
        if (proxy != null) return proxy;

        for (IExtensionProvider provider : mProviders) {
            if (provider.hasExtension(uri)) {
                proxy = provider.getExtension(uri);
                if (proxy != null) {
                    mProxies.put(uri, proxy);
                    return proxy;
                }
            }
        }

        return null;
    }

    /**
     * @return list of existing extensions held by this registrar.
     * @deprecated Here only for LegacyExtension support. Should not be used.
     */
    @Deprecated
    public Collection<ExtensionProxy> getExtensions() {
        return mProxies.values();
    }

    @NonNull
    @SuppressWarnings("unused")
    private long createProxy(String uri) {
        ExtensionProxy proxy = getExtension(uri);
        if (proxy != null) {
            return proxy.getNativeHandle();
        }
        return 0;
    }

    private native long nCreate();
}
