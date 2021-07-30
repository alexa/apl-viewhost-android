/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.events;

import com.amazon.apl.android.Event;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.dependencies.IDataSourceFetchCallback;
import com.amazon.apl.enums.EventProperty;

import java.util.Map;

/**
 * Event emitted by Core to request more items from a Dynamic DataSource.
 * Corresponds to kEventTypeDataSourceFetchRequest EventType.
 *
 * See https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-interface.html#loadindexlistdata-request
 */
public class DataSourceFetchEvent extends Event {

    private static final String TAG = "DataSourceFetchEvent";

    private final IDataSourceFetchCallback mDataSourceFetchCallback;

    private DataSourceFetchEvent(
            final long nativeHandle,
            final RootContext rootContext,
            final IDataSourceFetchCallback dataSourceFetchCallback) {
        super(nativeHandle, rootContext);
        mDataSourceFetchCallback = dataSourceFetchCallback;
    }

    public static DataSourceFetchEvent create(
            final long nativeHandle,
            final RootContext rootContext,
            final IDataSourceFetchCallback dataSourceFetchCallback) {
        return new DataSourceFetchEvent(nativeHandle, rootContext, dataSourceFetchCallback);
    }

    @Override
    public void execute() {
        final String type = mProperties.getString(EventProperty.kEventPropertyName);
        final Map<String, Object> payload = mProperties.get(EventProperty.kEventPropertyValue);
        resolve();
        mDataSourceFetchCallback.onDataSourceFetchRequest(type, payload);
    }

    @Override
    public void terminate() {
        // Do nothing.
    }
}
