/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.extension;

import androidx.annotation.NonNull;

import com.amazon.apl.android.Content;
import com.amazon.apl.android.RootConfig;

/**
 * TODO update this api when extensions are handled in a different level
 */
public interface IExtensionRegistration {

    /**
     * Register the extensions for this document.
     */
    @Deprecated
    default void registerExtensions(@NonNull Content content,
                            @NonNull RootConfig rootConfig,
                            @NonNull Callback callback) {
        callback.onExtensionsReady();
    }

    /**
     * Add extensions to the registration process. Useful for Legacy extensions that require to set
     * settings or register LiveData.
     */
    default void registerExtensions(@NonNull Content content, @NonNull RootConfig rootConfig) {}

    interface Callback {
        /**
         * Notify that all extensions have been registered.
         */
        void onExtensionsReady();
    }
}
