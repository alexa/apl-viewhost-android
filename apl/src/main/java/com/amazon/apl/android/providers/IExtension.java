/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.providers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazon.apl.android.ExtensionCommandDefinition;
import com.amazon.apl.android.ExtensionEventHandler;
import com.amazon.apl.android.ExtensionFilterDefinition;
import com.amazon.apl.android.dependencies.IExtensionEventCallback;
import com.amazon.apl.android.dependencies.IExtensionImageFilterCallback;

import java.util.Collections;
import java.util.List;

/**
 * An apl extension that a runtime may configure
 */
public interface IExtension {

    /**
     * The extension URI (see {@link com.amazon.apl.android.RootConfig#registerExtension(String)}
     * @return the extension name
     */
    @NonNull String getUri();

    /**
     * The extension event callback that's called when extension events occur (see {@link com.amazon.apl.android.events.ExtensionEvent}).
     * @return the extension callback
     */
    @Nullable
    default IExtensionEventCallback getCallback() {
        return null;
    }

    /**
     * Provide environment information see {@link com.amazon.apl.android.RootConfig#registerExtensionEnvironment(String, Object)}}
     * @return the environment information
     */
    @Nullable
    default Object getEnvironment() {
        return null;
    }

    /**
     * Provide the extension commands see {@link com.amazon.apl.android.RootConfig#registerExtensionCommand(ExtensionCommandDefinition)}
     * @return a list of commands to register
     */
    @NonNull
    default List<ExtensionCommandDefinition> getCommandDefinitions() {
        return Collections.emptyList();
    }

    /**
     * Provide the extension filters see {@link com.amazon.apl.android.RootConfig#registerExtensionFilter(ExtensionFilterDefinition)}
     * @return a list of filters to register
     */
    @NonNull
    default List<ExtensionFilterDefinition> getFilterDefinitions() {
        return Collections.emptyList();
    }

    /**
     * Provide the event handlers see {@link com.amazon.apl.android.RootConfig#registerExtensionEventHandler(ExtensionEventHandler)}
     *
     * @return a list of event handlers to register
     */
    @NonNull
    default List<ExtensionEventHandler> getEventHandlers() {
        return Collections.emptyList();
    }

    /**
     * Provide the callback for image filter processing.
     *
     * @return an filter callback, or null.
     */
    @Nullable
    default IExtensionImageFilterCallback getFilterCallback() {
        return null;
    }
}
