/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.alexaext;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.alexa.android.extension.discovery.BuildConfig;
import com.amazon.alexa.android.extension.discovery.ExtensionDiscovery;
import com.amazon.alexa.android.extension.discovery.ExtensionMultiplexClient;

/**
 * Extension discovery provider. Allows to request any dynamic extensions.
 */
public class DiscoveryExtensionsProvider implements IExtensionProvider {
    private static final String TAG = "DiscoveryPrvdr";
    private final ExtensionMultiplexClient mMultiplexClient;

    /**
     * Create a provider.
     *
     * @param context The Android Context.
     */
    public DiscoveryExtensionsProvider(Context context) {
        mMultiplexClient = new ExtensionMultiplexClient(() -> context);
    }

    @Override
    @Nullable
    public ExtensionProxy getExtension(@NonNull final String uri) {
        if (mMultiplexClient.isKilled()) {
            if (BuildConfig.DEBUG) {
                throw new IllegalStateException("DiscoveryExtensionProvider was finished.");
            } else {
                Log.wtf(TAG, "Client is dead. Unable to provide extension for uri: " + uri);
                return null;
            }
        }

        final ExtensionDiscovery.ExtensionPresence presence = mMultiplexClient.hasExtension(uri);
        switch (presence) {
            case PRESENT:
                return new RemoteExtensionProxy(uri, new RemoteProxyDelegate(mMultiplexClient));
            case DEFERRED:
                return new RemoteExtensionProxy(uri, new DeferredRemoteProxyDelegate(mMultiplexClient));
            default:
                return null;
        }
    }

    @Override
    public boolean hasExtension(String uri) {
        if (mMultiplexClient.isKilled()) {
            if (BuildConfig.DEBUG) {
                throw new IllegalStateException("DiscoveryExtensionProvider was finished.");
            } else {
                Log.wtf(TAG, "Client is dead. Unable to find extension for uri: " + uri);
                return false;
            }
        }
        return mMultiplexClient.hasExtension(uri) != ExtensionDiscovery.ExtensionPresence.NOT_PRESENT;
    }

    @Override
    public void finish() {
        mMultiplexClient.kill();
    }
}
