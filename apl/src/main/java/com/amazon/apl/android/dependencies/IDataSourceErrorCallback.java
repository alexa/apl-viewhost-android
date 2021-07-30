/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies;

public interface IDataSourceErrorCallback {

    /**
     * Called when any error occurs in a Dynamic DataSource. The errors should be sent to Alexa as part
     * of a RuntimeError event.
     *
     * @param errors The errors APL object, convertible to JSON Array
     */
    void onDataSourceError(Object errors);
}
