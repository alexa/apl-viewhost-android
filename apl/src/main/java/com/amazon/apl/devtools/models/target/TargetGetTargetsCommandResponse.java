/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.target;

import android.util.Log;

import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.models.Target;
import com.amazon.apl.devtools.models.common.TargetDomainCommandResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;

public final class TargetGetTargetsCommandResponse extends TargetDomainCommandResponse {
    private static final String TAG = TargetGetTargetsCommandResponse.class.getSimpleName();
    private final Result mResult;

    public TargetGetTargetsCommandResponse(int id, Collection<Target> targets) {
        super(id);
        mResult = new Result(targets);
    }

    public Result getResult() {
        return mResult;
    }

    private static class Result {
        private final Collection<TargetInfo> mTargetInfos;

        public Result(Collection<Target> targets) {
            mTargetInfos = parseTargetInfos(targets);
        }

        public Collection<TargetInfo> getTargetInfos() {
            return mTargetInfos;
        }

        /**
         * parseTargetInfos parses a collection of Target into a collection of TargetInfo
         */
        private Collection<TargetInfo> parseTargetInfos(Collection<Target> targets) {
            Collection<TargetInfo> targetInfos = new ArrayList<>();
            for (Target target : targets) {
                targetInfos.add(parseTargetInfo(target));
            }
            return targetInfos;
        }

        /**
         * parseTargetInfo parses a single Target into a TargetInfo
         */
        private TargetInfo parseTargetInfo(Target target) {
            return new TargetInfo(target.getTargetId(), target.getType().toString(),
                    target.getName());
        }

        private static class TargetInfo {
            private final String mTargetId;
            private final String mType;
            private final String mName;

            public TargetInfo(String targetId, String type, String name) {
                mTargetId = targetId;
                mType = type;
                mName = name;
            }

            public String getTargetId() {
                return mTargetId;
            }

            public String getType() {
                return mType;
            }

            public String getName() {
                return mName;
            }
        }
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        Log.i(TAG, "Serializing " + CommandMethod.TARGET_GET_TARGETS + " response object");
        // Parse collection of TargetInfo into a JSONArray
        JSONArray targetInfoJSONArray = new JSONArray();
        for (Result.TargetInfo targetInfo : getResult().getTargetInfos()) {
            targetInfoJSONArray.put(new JSONObject().put("targetId", targetInfo.getTargetId())
                    .put("type", targetInfo.getType())
                    .put("name", targetInfo.getName()));
        }
        return super.toJSONObject().put("result", new JSONObject()
                .put("targetInfos", targetInfoJSONArray));
    }
}
