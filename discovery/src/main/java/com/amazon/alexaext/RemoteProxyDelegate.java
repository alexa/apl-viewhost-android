package com.amazon.alexaext;

import androidx.annotation.NonNull;

import com.amazon.alexa.android.extension.discovery.ExtensionMultiplexClient;

/**
 * A {@link BaseRemoteProxyDelegate} for in-process extensions.
 */
class RemoteProxyDelegate extends BaseRemoteProxyDelegate {
    RemoteProxyDelegate(ExtensionMultiplexClient multiplexClient) {
        super(multiplexClient);
    }

    @Override
    boolean onProxyInitialize(@NonNull final String uri) {
        connect(uri);
        return true;
    }
}
