/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies;

import org.json.JSONArray;

/**
 * Defines API for listening to DataSource Context updates.
 */
public interface IDataSourceContextListener {

    /**
     * Called when the DataSourceContext is updated.
     *
     * @param DataSourceContext the updated DataSourceContext context.
     */
    void onDataSourceContextUpdate(JSONArray DataSourceContext);

}
