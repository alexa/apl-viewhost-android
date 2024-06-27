/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools;

import android.util.Log;
import com.amazon.apl.android.BuildConfig;
import com.amazon.apl.android.dependencies.IAPLSessionListener;
import com.amazon.apl.android.metrics.IMetricsSink;
import com.amazon.apl.devtools.models.ViewTypeTarget;
import com.amazon.apl.devtools.models.network.DTNetworkRequestHandler;
import com.amazon.apl.devtools.models.network.IDTNetworkRequestHandler;
import com.amazon.apl.devtools.models.performance.DTMetricsSink;
import com.amazon.apl.devtools.models.performance.IMetricsService;
import com.amazon.apl.devtools.models.performance.NoOpDTMetricSink;
import java.util.List;

/**
 * This class is meant to provide any Dev Tools related class implementation.
 */
public class DevToolsProvider {
    private final static String TAG = DevToolsProvider.class.getSimpleName();
    private final ViewTypeTarget mDTView;
    private final IDTNetworkRequestHandler mNetworkRequestHandler;
    public DevToolsProvider() {
        mDTView = new ViewTypeTarget();
        mNetworkRequestHandler = new DTNetworkRequestHandler(mDTView);
    }

    /**
     * Creates a new instance of {@link DTMetricsSink}.
     *
     * @return the real implementation if the build config if debuggable, else a no op implementation.
     */
    public static IMetricsSink createDevToolSink() {
        // This is to avoid any unnecessary tracking of metrics on release builds.
        if (BuildConfig.DEBUG) {
            return new DTMetricsSink();
        }
        return new NoOpDTMetricSink();
    }

    public void registerSink(final List<IMetricsSink> metricsSinks) {
        if (!BuildConfig.DEBUG) {
            return;
        }
        for (IMetricsSink sink: metricsSinks) {
            if (sink instanceof IMetricsService) {
                mDTView.setMetricsRetriever((IMetricsService) sink);
                return;
            }
        }
        Log.w(TAG, "No devtools sink were found to register.");
    }

    public IAPLSessionListener getAPLSessionListener() {
        return mDTView;
    }

    public ViewTypeTarget getDTView() {
        return mDTView;
    }

    public IDTNetworkRequestHandler getNetworkRequestHandler() {
        return mNetworkRequestHandler;
    }
}
