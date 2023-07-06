/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.alexaext;

import android.util.Pair;

import androidx.annotation.NonNull;

import com.amazon.alexa.android.extension.discovery.ExtensionMultiplexClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final List<Pair<ActivityDescriptor, String>> mOutboundMessages = new ArrayList<>();

    // State changes outside of connection
    @NonNull
    private final Set<SessionDescriptor> mStartedSessions = new HashSet<>();

    @NonNull
    private final Set<ActivityDescriptor> mRegisteredActivities = new HashSet<>();

    private enum ExtensionDisplayState {
        FOREGROUND,
        BACKGROUND,
        HIDDEN
    };

    @NonNull
    private final Map<ActivityDescriptor, ExtensionDisplayState> mDisplayStates = new HashMap<>();


    DeferredRemoteProxyDelegate(@NonNull final ExtensionMultiplexClient multiplexClient) {
        super(multiplexClient);
    }

    @Override
    boolean onProxyInitialize(@NonNull final String uri) {
        return true;
    }

    boolean onRequestRegistration(@NonNull final ActivityDescriptor activity, final String request) {
        mCachedDescriptor = activity;

        // Deferred extensions pass registration result right away, so the underlying framwork
        // can start passing messages
        onMessageInternal(activity.getURI(), mMultiplexClient.getExtensionDefinition(activity.getURI()));

        return super.onRequestRegistration(activity, request);
    }

    @Override
    public synchronized void onConnect(@NonNull final String uri) {
        super.onConnect(uri);

        for (SessionDescriptor session : mStartedSessions) {
            super.onSessionStartedInternal(session);
        }
        mStartedSessions.clear();

        for (ActivityDescriptor activityDescriptor : mRegisteredActivities) {
            super.onRegisteredInternal(activityDescriptor);
        }
        mRegisteredActivities.clear();

        for (Map.Entry<ActivityDescriptor, ExtensionDisplayState> state : mDisplayStates.entrySet()) {
            switch (state.getValue()) {
                case FOREGROUND:
                    super.onForegroundInternal(state.getKey());
                    break;
                case BACKGROUND:
                    super.onBackgroundInternal(state.getKey());
                    break;
                case HIDDEN:
                    super.onHiddenInternal(state.getKey());
                    break;
            }
        }
        mDisplayStates.clear();

        // Send deferred messages to the extension
        for (Pair<ActivityDescriptor,String> p : mOutboundMessages) {
            sendMessage(p.first, p.second);
        }

        mOutboundMessages.clear();
    }

    @Override
    synchronized boolean sendMessage(@NonNull final ActivityDescriptor activity, final String message) {
        // Deferred extensions would save outbound messages to the extension until the connection
        // is established.
        if (!mConnected) {
            mOutboundMessages.add(Pair.create(activity, message));
            return true;
        }

        return super.sendMessage(activity, message);
    }

    @Override
    synchronized void onSessionStartedInternal(@NonNull SessionDescriptor session) {
        if (!mConnected) {
            mStartedSessions.add(session);
        } else {
            super.onSessionStartedInternal(session);
        }
    }

    @Override
    synchronized void onSessionEndedInternal(@NonNull SessionDescriptor session) {
        if (!mConnected) {
            mStartedSessions.remove(session);
        } else {
            super.onSessionEndedInternal(session);
        }
    }

    @Override
    synchronized void onRegisteredInternal(@NonNull ActivityDescriptor activity) {
        if (!mConnected) {
            mRegisteredActivities.add(activity);
        } else {
            super.onRegisteredInternal(activity);
        }
    }

    @Override
    synchronized void onUnregisteredInternal(@NonNull ActivityDescriptor activity) {
        if (!mConnected) {
            mRegisteredActivities.remove(activity);
        } else {
            super.onUnregisteredInternal(activity);
        }
    }

    @Override
    synchronized void onForegroundInternal(@NonNull ActivityDescriptor activity) {
        // Deferred extensions funky like that.
        if (!mConnected) {
            connect(activity.getURI());
            mDisplayStates.put(activity, ExtensionDisplayState.FOREGROUND);
        } else {
            super.onForegroundInternal(activity);
        }
    }

    @Override
    synchronized void onBackgroundInternal(@NonNull ActivityDescriptor activity) {
        if (!mConnected) {
            mDisplayStates.put(activity, ExtensionDisplayState.BACKGROUND);
        } else {
            super.onBackgroundInternal(activity);
        }
    }

    @Override
    synchronized void onHiddenInternal(@NonNull ActivityDescriptor activity) {
        if (!mConnected) {
            mDisplayStates.put(activity, ExtensionDisplayState.HIDDEN);
        } else {
            super.onHiddenInternal(activity);
        }
    }
}
