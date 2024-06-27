package com.amazon.apl.devtools.models.frameMetrics;

import android.util.Log;

import com.amazon.apl.android.utils.FrameStat;
import com.amazon.apl.devtools.enums.EventMethod;
import com.amazon.apl.devtools.models.common.Event;

import org.json.JSONException;
import org.json.JSONObject;

public class FrameIncidentReportedEvent extends Event {
    private static final String TAG = "IncidentReportedEvent";

    private final Params mParams;
    public FrameIncidentReportedEvent(String sessionId, int incidentId, FrameStat[] frameStats, double[] upsValues, Object detail) {
        super(EventMethod.FRAMEMETRICS_INCIDENT_REPORTED, sessionId);
        mParams = new Params(incidentId, frameStats, upsValues, detail);
    }

    private static class Params {
        private int mIncidentId;
        private FrameStat[] frameStats;
        private double[] upsValues;
        private Object detail;

        public Params(int mIncidentId, FrameStat[] frameStats, double[] upsValues, Object detail) {
            this.mIncidentId = mIncidentId;
            this.frameStats = frameStats;
            this.upsValues = upsValues;
            this.detail = detail;
        }

        public int getIncidentId() {
            return mIncidentId;
        }

        public FrameStat[] getFrameStats() {
            return frameStats;
        }

        public double[] getUpsValues() {
            return upsValues;
        }

        public Object getDetail() {
            return detail;
        }
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        Log.i(TAG, "Serializing " + EventMethod.FRAMEMETRICS_INCIDENT_REPORTED + " event object");
        return super.toJSONObject().put("params", new JSONObject()
                .put("incidentId", mParams.getIncidentId())
                .put("framestats", mParams.getFrameStats())
                .put("upsValues", mParams.getUpsValues())
                .put("detail", mParams.getDetail()));
    }
}
