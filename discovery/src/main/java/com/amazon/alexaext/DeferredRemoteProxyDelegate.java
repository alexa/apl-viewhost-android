package com.amazon.alexaext;

import androidx.annotation.NonNull;

import com.amazon.alexa.android.extension.discovery.ExtensionMultiplexClient;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link BaseRemoteProxyDelegate} for out of process (deferred) extensions.
 */
class DeferredRemoteProxyDelegate extends BaseRemoteProxyDelegate {
    // Messages to extension.
    //
    // The messages cache is required as Deferred extensions provide registration information
    // instantly. The framework treats the extension as registered and may start passing messages
    // to it. Since by definition the extension has deferred binding (after rendering pass is complete),
    // the messages passed by framework to the extension might get lost if not cached. The messages
    // are passed to the extension when the extension service is bound (onConnect).
    @NonNull
    private final List<String> mOutboundMessages = new ArrayList<>();

    DeferredRemoteProxyDelegate(@NonNull final ExtensionMultiplexClient multiplexClient) {
        super(multiplexClient);
    }

    @Override
    boolean onProxyInitialize(@NonNull final String uri) {
        return true;
    }

    boolean onRequestRegistration(@NonNull final String uri, final String request) {
        // Deferred extensions pass registration result right away, so the underlying framwork
        // can start passing messages
        onMessageInternal(uri, mMultiplexClient.getExtensionDefinition(uri));

        return super.onRequestRegistration(uri, request);
    }

    @Override
    public synchronized void onConnect(@NonNull final String uri) {
        super.onConnect(uri);

        // Send deferred messages to the extension
        for (String message : mOutboundMessages) {
            sendMessage(uri, message);
        }

        mOutboundMessages.clear();
    }

    @Override
    synchronized boolean sendMessage(@NonNull final String uri, final String message) {
        // Deferred extensions would save outbound messages to the extension until the connection
        // is established.
        if (!mConnected) {
            mOutboundMessages.add(message);
            return true;
        }

        return super.sendMessage(uri, message);
    }

    @Override
    void onFocusGained(final @NonNull String uri) {
        connect(uri);
        super.onFocusGained(uri);
    }
}
