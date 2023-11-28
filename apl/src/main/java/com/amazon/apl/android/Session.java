/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import com.amazon.common.BoundObject;

/**
 * Document logging session. Limited at the moment, but ultimately provides a possibility to provide
 * logs that could be sent to the experience developer (parsing/execution errors/etc).
 */
public class Session extends BoundObject {
    /**
     * Creates a default Session.
     */
    public Session() {
        bind(nCreate());
    }

    /**
     * @return APL instance log ID. May be used to uniquely identify logs emitted by APL engine instance.
     */
    public String getLogId() { return nGetLogId(getNativeHandle()); }

    /**
     * Enable publishing sensitive session information to the device logs. For example,
     * user-generated output from the Log command, which could contain arbitrary information from
     * the APL context. This is a security liability and should not be enabled in production builds
     * of apps unless explicitly intended.
     *
     * Note:
     * - This setting apples to all of the application's view hosts instances.
     * - This is not affected by the state of the debuggable flag in the application's manifest.
     */
    public static void setSensitiveLoggingEnabled(boolean enabled) {
        nSetDebuggingEnabled(enabled);
    }

    private static native long nCreate();
    private static native String nGetLogId(long _handle);
    private static native void nSetDebuggingEnabled(boolean enabled);
}
