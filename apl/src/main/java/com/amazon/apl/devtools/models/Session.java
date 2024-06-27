/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models;

import static com.amazon.apl.android.Session.setSensitiveLoggingEnabled;

import android.util.Log;

import com.amazon.apl.devtools.controllers.DTConnection;
import com.amazon.apl.devtools.models.common.Event;
import com.amazon.apl.devtools.models.common.SessionModel;
import com.amazon.apl.devtools.util.IdGenerator;


/**
 * Session is a unique mapping between a DTConnection and a Target.
 * Each DTConnection can only have 1 Session attached to a specific Target.
 * For example, if there are 5 Targets, then a DTConnection can only have 5 Sessions, 1 per Target.
 */
public final class Session<T extends Target> extends SessionModel {
    private static final String TAG = Session.class.getSimpleName();
    private static final IdGenerator sessionIdGenerator = new IdGenerator();
    private final DTConnection mConnection;
    private final T mTarget;
    private boolean isPerformanceEnabled;
    private boolean isLogEnabled;
    private boolean isNetworkEnabled;

    public Session(DTConnection connection, T target) {
        super(sessionIdGenerator.generateId("session"));
        Log.i(TAG, "Creating session");
        mConnection = connection;
        mTarget = target;
        connection.registerSession(this);
        target.registerSession(this);
    }

    public DTConnection getConnection() {
        return mConnection;
    }

    public T getTarget() {
        return mTarget;
    }


    public void destroy() {
        mConnection.unregisterSession(getSessionId());
        mTarget.unregisterSession(getSessionId());
    }

    public boolean isPerformanceEnabled() {
        return isPerformanceEnabled;
    }

    public void setPerformanceEnabled(boolean performanceEnabled) {
        isPerformanceEnabled = performanceEnabled;
    }

    public boolean isLogEnabled() {
        return isLogEnabled;
    }

    public void setLogEnabled(boolean isLogEnabled) {
        setSensitiveLoggingEnabled(isLogEnabled);
        this.isLogEnabled = isLogEnabled;
        //TO DO: after Network.enable implemented, send old log entries after reconnect
    }

    public boolean isNetworkEnabled() {
        return isNetworkEnabled;
    }

    public void setNetworkEnabled(boolean isNetworkEnabled) {
        this.isNetworkEnabled = isNetworkEnabled;
    }

    public <TEvent extends Event> void sendEvent(TEvent event) {
        mConnection.sendEvent(event);
    }

    public void clearLog() {
        mTarget.clearLog();
    }
}
