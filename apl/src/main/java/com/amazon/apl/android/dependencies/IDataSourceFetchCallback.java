/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies;

import org.json.JSONException;

import java.util.Map;

public interface IDataSourceFetchCallback {

    /**
     * Called when a {@link com.amazon.apl.android.events.DataSourceFetchEvent} is executed as a
     * result of a DataSource fetch request from Core.
     *
     * @param type The type of DataSource that APL is requesting a fetch from.
     * @param payload The event payload.
     */
    void onDataSourceFetchRequest(String type, Map<String, Object> payload);
}
