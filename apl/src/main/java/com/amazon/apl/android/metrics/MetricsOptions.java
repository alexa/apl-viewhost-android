/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.metrics;

import androidx.annotation.NonNull;

import com.amazon.apl.devtools.DevToolsProvider;
import com.amazon.apl.devtools.models.performance.DTMetricsSink;
import com.google.auto.value.AutoValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

//The class is supposed to be used by runtimes to provide metrics related configurations.
@AutoValue
public abstract class MetricsOptions {

    // The sinks to configure for this document.
    @NonNull
    public abstract List<IMetricsSink> getMetricsSinkList();

    // The initial metadata set to use for a document.
    @Nullable
    public abstract Map<String, String> getMetaData();

    public static Builder builder() {
        return new AutoValue_MetricsOptions.Builder();
    }

    public static class Builder {
        private Map<String, String> mMetaData;
        private List<IMetricsSink> mMetricsSinkList;

        Builder() {
            mMetricsSinkList = new ArrayList<>();
        }

        public Builder metaData(Map<String, String> metaData) {
            mMetaData = metaData;
            return this;
        }

        public Builder metricsSinkList(List<IMetricsSink> metricsSinkList) {
            mMetricsSinkList.addAll(metricsSinkList);
            return this;
        }

        public MetricsOptions build() {
            mMetricsSinkList.add(DevToolsProvider.createDevToolSink());
            return new AutoValue_MetricsOptions(mMetricsSinkList, mMetaData);
        }
    }
}
