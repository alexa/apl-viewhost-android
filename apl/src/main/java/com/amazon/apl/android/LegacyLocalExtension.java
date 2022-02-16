/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import androidx.annotation.Nullable;

import com.amazon.alexaext.ILiveDataUpdateCallback;
import com.amazon.apl.android.dependencies.IExtensionEventCallback;

import java.util.Map;

/**
 * Combined "legacy" local extension base.
 */
@Deprecated
public abstract class LegacyLocalExtension implements ILegacyLocalExtension {
    private ISendExtensionEventCallback mEventCallback;

    @Override
    public boolean initialize(ISendExtensionEventCallback eventCallback, ILiveDataUpdateCallback liveDataCallback) {
        mEventCallback = eventCallback;
        return true;
    }

    /**
     * Invoke extension event handler.
     * @param name Event name.
     * @param parameters Parameters to provide.
     * @return true if event handled, false otherwise.
     */
    public boolean sendExtensionEvent(String name, Map<String, Object> parameters) {
        return mEventCallback.sendExtensionEvent(name, parameters);
    }

    @Nullable
    @Override
    public IExtensionEventCallback getCallback() {
        return this;
    }
}
