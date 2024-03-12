package com.amazon.apl.devtools.models.frameMetrics;

import com.amazon.apl.devtools.models.common.FrameMetricsDomainCommandResponse;

public class FrameMetricsRecordCommandResponse extends FrameMetricsDomainCommandResponse {
    private static final String TAG = FrameMetricsRecordCommandResponse.class.getSimpleName();

    public FrameMetricsRecordCommandResponse(int id, String sessionId) {
        super(id, sessionId);
    }

}
