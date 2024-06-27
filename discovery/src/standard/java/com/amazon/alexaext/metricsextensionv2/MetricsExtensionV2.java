/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.alexaext.metricsextensionv2;

import com.amazon.common.BoundObject;
import com.amazon.alexaext.ActivityDescriptor;
import com.amazon.alexaext.ExtensionExecutor;

/**
 * The MetricsExtensionV2 class.
 * Allows runtime to initiate {@link com.amazon.alexaext.metricsExtensionV2.AplMetricsExtensionV2} methods via JNI
 */
public abstract class MetricsExtensionV2 extends BoundObject {
    public MetricsExtensionV2(DestinationFactory destinationFactory, ExtensionExecutor extensionExecutor) {
        final long handle = nCreate(destinationFactory.getNativeHandle(), extensionExecutor.getNativeHandle());
        bind(handle);
    }

    protected String createRegistrationInternal(ActivityDescriptor activity, String registrationRequest) {
        return nCreateRegistration(getNativeHandle(),
                activity.getURI(),
                activity.getSession().getId(),
                activity.getActivityId(),
                registrationRequest);
    }

    protected boolean invokeCommandInternal(ActivityDescriptor activity, String command) {
        return nInvokeCommand(getNativeHandle(), activity.getURI(), activity.getSession().getId(), activity.getActivityId(), command);
    }

    protected void onUnregisteredInternal(ActivityDescriptor activity) {
        nOnUnregistered(getNativeHandle(), activity.getURI(), activity.getSession().getId(), activity.getActivityId());
    }

    private static native long nCreate(long destinationFactoryHandle, long extensionExecutorHandle);
    private static native String nCreateRegistration(long nativeHandle,
            String uri_,
            String sessionId_,
            String activityId_,
            String registrationRequest_);
    private static native boolean nInvokeCommand(long nativeHandle, String uri_, String sessionId_, String activityId_, String command_);
    private static native void nOnUnregistered(long nativeHandle, String uri_, String sessionId_, String activityId_);
}
