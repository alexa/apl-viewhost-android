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

    private static native long nCreate();
    private static native String nGetLogId(long _handle);
}
