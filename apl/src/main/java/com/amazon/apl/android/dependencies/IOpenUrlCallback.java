/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.dependencies;

/**
 * Callback for OpenURL command
 */
public interface IOpenUrlCallback {
    interface IOpenUrlCallbackResult {
        /**
         * If the open url call succeeded call this back with
         * @param succeeded `true` if succeeded. If false is returned, then the command
         *                  fails, which dispatches the `onFail` event in the OpenURL command
         */
        void onResult(boolean succeeded);
    }


    /**
     * Called when an APL doc requests to open a url through
     * OpenURL command
     * @param url The url to open
     * @param resultCallback Callback when open url either succeeded or failed
     */
    void onOpenUrl(String url, IOpenUrlCallbackResult resultCallback);
}
