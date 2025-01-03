package com.amazon.apl.devtools.models.frameMetrics;

import android.util.Log;

import com.amazon.apl.android.utils.FrameStat;
import com.amazon.apl.devtools.enums.EventMethod;
import com.amazon.apl.devtools.models.common.Event;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.stream.Collectors;

public class FrameIncidentReportedEvent extends Event {
    private static final String TAG = "IncidentReportedEvent";

    private final Params mParams;
    public FrameIncidentReportedEvent(String sessionId, int incidentId, FrameStat[] frameStats, Double[] upsValues, JSONObject detail) {
        super(EventMethod.FRAMEMETRICS_INCIDENT_REPORTED, sessionId);
        mParams = new Params(incidentId, frameStats, upsValues, detail);
    }

    private static class Params {
        private final int mIncidentId;
        private final FrameStat[] frameStats;
        private final Double[] upsValues;
        private final JSONObject detail;

        public Params(int mIncidentId, FrameStat[] frameStats, Double[] upsValues, JSONObject detail) {
            this.mIncidentId = mIncidentId;
            this.frameStats = frameStats;
            this.upsValues = upsValues;
            this.detail = detail;
        }

        public int getIncidentId() {
            return mIncidentId;
        }

        public JSONArray getFrameStats() {
            // Convert the array to a JSONArray
            JSONArray jsonArray = new JSONArray();
            try {
                for (FrameStat frameStat : frameStats) {
                    jsonArray.put(frameStat.toJSON());
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error serializing frameStats ", e);
            }
            return jsonArray;
        }

        public Double[] getUpsValues() {
            return upsValues;
        }

        public JSONObject getDetail() {
            return detail;
        }
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        Log.i(TAG, "Serializing " + EventMethod.FRAMEMETRICS_INCIDENT_REPORTED + " event object");
        return super.toJSONObject().put("params", new JSONObject()
                .put("incidentId", mParams.getIncidentId())
                .put("framestats", mParams.getFrameStats())
                .put("upsValues", new JSONArray(mParams.getUpsValues()))
                .put("detail", mParams.getDetail()));
    }
}
