/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dynamicdatasource;

import com.amazon.apl.android.APLController;
import com.amazon.apl.android.dependencies.IDataSourceFetchCallback;
import com.amazon.common.test.LeakRulesBaseClass;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;

import java.util.Map;

/**
 * Test implementation of index-based {@link IDataSourceFetchCallback}.
 * Used with implementations of {@link AbstractDynamicDataSourceComponentViewTest}.
 */
public abstract class TestIndexBasedDataSourceFetchCallback extends LeakRulesBaseClass implements IDataSourceFetchCallback {

    @Override
    public void onDataSourceFetchRequest(String type, Map<String, Object> eventPayload) {
        try {
            final String correlationToken = (String) eventPayload.get("correlationToken");
            final String listId = (String) eventPayload.get("listId");
            final int count = (Integer) eventPayload.get("count");
            final int startIndex = (Integer) eventPayload.get("startIndex");

            // Get the requested items from the provider:
            final JSONArray items = new JSONArray();
            for (int i = startIndex; i < startIndex + count; i++) {
                items.put(new JSONObject(getItem(i)));
            }
            // Response payload structure comes from
            // https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-interface.html#sendindexlistdata-directive
            final JSONObject response = new JSONObject();
            response.put("correlationToken", correlationToken);
            response.put("listId", listId);
            response.put("startIndex", startIndex);
            response.put("items", items);
            response.put("minimumInclusiveIndex", 0);
            response.put("maximumExclusiveIndex", getItemCount());

            // Update the data source in core
            if(!getAplController().updateDataSource(type, response.toString())) {
                Assert.fail("TestDataSourceFetchCallback: updateDataSource was unsuccessful");
            }
        } catch (JSONException e) {
            Assert.fail("JSON exception " + e);
        }
    }

    abstract Map<String, Object> getItem(int index);

    abstract int getItemCount();

    abstract APLController getAplController();
}
