/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models;

import android.util.Log;

import com.amazon.apl.devtools.enums.TargetType;
import com.amazon.apl.devtools.models.common.TargetModel;
import com.amazon.apl.devtools.models.log.LogEntry;
import com.amazon.apl.devtools.util.DependencyContainer;
import com.amazon.apl.devtools.util.IdGenerator;
import com.amazon.apl.devtools.util.TargetCatalog;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Target is an object that can be uniquely identified and are the subject of command requests.
 * There are different types of Targets.
 */
public abstract class Target extends TargetModel {
    private static final String TAG = Target.class.getSimpleName();
    private static final IdGenerator targetIdGenerator = new IdGenerator();
    private final Map<String, Session> mRegisteredSessionsMap = new HashMap<>();
    private final TargetCatalog mTargetCatalog;

    protected Target(TargetType type, String name) {
        super(targetIdGenerator.generateId("target"), type, name);
        Log.i(TAG, "Creating target of type " + getType() + " with target id " +
                getTargetId());
        mTargetCatalog = DependencyContainer.getInstance().getTargetCatalog();
    }

    public void registerToCatalog() {
        mTargetCatalog.add(this);
    }

    public void unregisterToCatalog() {
        mTargetCatalog.remove(this);
    }

    public void registerSession(Session session) {
        mRegisteredSessionsMap.put(session.getSessionId(), session);
        Log.i(TAG, "registerSession: Target with target id " + getTargetId() +
                " is attached to " + mRegisteredSessionsMap.size() + " sessions");
    }

    public void unregisterSession(String sessionId) {
        mRegisteredSessionsMap.remove(sessionId);
        Log.i(TAG, "unregisterSession: Target with target id " + getTargetId() +
                " is attached to " + mRegisteredSessionsMap.size() + " sessions");
    }

    public boolean hasSession(String sessionId) {
        return mRegisteredSessionsMap.containsKey(sessionId);
    }

    public Collection<String> getRegisteredSessionIds() {
        return mRegisteredSessionsMap.keySet();
    }

    public Collection<Session> getRegisteredSessions() {
        return mRegisteredSessionsMap.values();
    }

    public abstract List<LogEntry> getLogEntries();

    public abstract void clearLog();
}
