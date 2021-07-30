/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dynamicdatasource;

import com.amazon.apl.android.APLController;
import com.amazon.apl.android.dependencies.IDataSourceFetchCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;

import java.util.Map;

/**
 * Test implementation of token-based {@link IDataSourceFetchCallback}.
 * Used with implementations of {@link AbstractDynamicDataSourceComponentViewTest}.
 */
public abstract class TestTokenBasedDataSourceFetchCallback implements IDataSourceFetchCallback {

    @Override
    public void onDataSourceFetchRequest(String type, Map<String, Object> eventPayload) {
        try {
            final JSONObject response = new JSONObject();
            final String correlationToken = (String) eventPayload.get("correlationToken");
            final String listId = (String) eventPayload.get("listId");
            final String pageToken = (String) eventPayload.get("pageToken");

            // Get the requested items from the provider:
            final JSONArray items = getItems(pageToken);
            // Get the next page token from the provider:
            final String nextPageToken = getNextPageToken(pageToken);

            response.put("correlationToken", correlationToken);
            response.put("listId", listId);
            response.put("pageToken", pageToken);
            if (nextPageToken != null) {
                response.put("nextPageToken", nextPageToken);
            }
            response.put("items", items);

            // Update the data source in core
            if (!getAplController().updateDataSource(type, response.toString())) {
                Assert.fail("TestDataSourceFetchCallback: updateDataSource was unsuccessful");
            }
        } catch (JSONException e) {
            Assert.fail("JSON exception " + e);
        }
    }

    abstract JSONArray getItems(String pageToken);

    abstract String getNextPageToken(String pageToken);

    abstract APLController getAplController();
}