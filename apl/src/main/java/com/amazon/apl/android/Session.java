/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import com.amazon.apl.android.dependencies.IAPLSessionListener;
import com.amazon.common.BoundObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Document logging session. Limited at the moment, but ultimately provides a possibility to provide
 * logs that could be sent to the experience developer (parsing/execution errors/etc).
 */
public class Session extends BoundObject {
    private final List<LogInfo> mLogList;
    private IAPLSessionListener mListener;

    public Session() {
        bind(nCreate());
        mLogList = Collections.synchronizedList(new ArrayList<>());
    }

    public enum LogEntryLevel {
        NONE, TRACE, DEBUG, INFO, WARN, ERROR, CRITICAL
    }

    public enum LogEntrySource {
        SESSION, VIEW, COMMAND
    }

    /**
     * @return APL instance log ID. May be used to uniquely identify logs emitted by APL engine instance.
     */
    public String getLogId() {
        return nGetLogId(getNativeHandle());
    }

    /**
     * Enable publishing sensitive session information to the device logs. For example,
     * user-generated output from the Log command, which could contain arbitrary information from
     * the APL context. This is a security liability and should not be enabled in production builds
     * of apps unless explicitly intended.
     * <p>
     * Note:
     * - This setting apples to all of the application's view hosts instances.
     * - This is not affected by the state of the debuggable flag in the application's manifest.
     */
    public static void setSensitiveLoggingEnabled(boolean enabled) {
        nSetDebuggingEnabled(enabled);
    }

    public void setAPLListener(IAPLSessionListener listener) {
        mListener = listener;
        reportExistingLogs();
    }

    private void reportExistingLogs() {
        for (LogInfo logInfo : mLogList) {
            mListener.write(logInfo.getLevel(), logInfo.getSource(), logInfo.getMessage(), logInfo.getArguments());
        }
    }


    private native long nCreate();

    private static native String nGetLogId(long _handle);

    private static native void nSetDebuggingEnabled(boolean enabled);

    public void write(LogEntryLevel level, LogEntrySource source, String message) {
        write(level, source, message, null);
    }

    /**
     * Override to provide special handling of session-related log entries.
     *
     * @param level The log entry's severity level
     * @param source The log entry's source
     * @param message The log entry's message
     * @param arguments Any additional arguments associated with log entry
     */
    public void write(LogEntryLevel level, LogEntrySource source, String message, Object[] arguments){
        if (mListener != null) {
            mListener.write(level, source, message, arguments);
        }
        mLogList.add(new LogInfo(level, source, message, arguments));
    }

    private static class LogInfo {
        private final LogEntryLevel mLevel;
        private final LogEntrySource mSource;
        private final String mMessage;
        private final Object[] mArguments;

        LogInfo(LogEntryLevel level, LogEntrySource source, String message, Object[] arguments) {
            mLevel = level;
            mSource = source;
            mMessage = message;
            mArguments = arguments;
        }

        public LogEntryLevel getLevel() {
            return mLevel;
        }

        public LogEntrySource getSource() {
            return mSource;
        }

        public String getMessage() {
            return mMessage;
        }

        public Object[] getArguments() {
            return mArguments;
        }
    }

}
