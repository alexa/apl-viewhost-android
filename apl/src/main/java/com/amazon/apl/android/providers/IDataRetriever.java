/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.providers;

/**
 * Interface for Data Retrievers.
 */
public interface IDataRetriever {
    /**
     * Fetches the data from the source file or url and calls
     * the {@link Callback}
     * @param source
     * @param callback
     */
    void fetch(String source, Callback callback);

    /**
     * Cancels all fetch requests.
     */
    void cancelAll();

    /**
     * The Callback interface
     */
    interface Callback{
        /**
         * Callback method to call in case of success.
         * @param response
         */
        void success(String response);

        /**
         * Callback method to call in case of error.
         */
        void error();
    }
}
