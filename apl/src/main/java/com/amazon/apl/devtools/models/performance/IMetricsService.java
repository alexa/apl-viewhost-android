 /*
  * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
  * SPDX-License-Identifier: Apache-2.0
  */

package com.amazon.apl.devtools.models.performance;

import com.amazon.apl.android.utils.MetricInfo;
import java.util.List;

 /**
  * This interface is to give access to devtools to retrieve the collected metrics.
  * Also, to inform the metric provider when to clear the metrics.
  */
 public interface IMetricsService {
    /**
     * @return a list of the collected metrics.
     */
    List<MetricInfo> retrieveMetrics();

    /**
     * This is call to inform the Metrics provider that they can clear the metrics as they aren't needed anymore for devtools.
     */
    void clearMetrics();
}