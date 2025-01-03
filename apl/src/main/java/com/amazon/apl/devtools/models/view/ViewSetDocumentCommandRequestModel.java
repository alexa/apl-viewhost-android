/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.view;

import com.amazon.apl.devtools.controllers.impl.DTConnection;
import com.amazon.apl.devtools.enums.CommandMethod;
import com.amazon.apl.devtools.enums.DTError;
import com.amazon.apl.devtools.models.common.SessionCommandRequest;
import com.amazon.apl.devtools.models.error.DTException;
import com.amazon.apl.devtools.util.CommandRequestValidator;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;

public abstract class ViewSetDocumentCommandRequestModel
        extends SessionCommandRequest<ViewSetDocumentCommandResponse> {
    private final Params mParams;

    public static class Configuration {
        public final Map<String, List<Object>> mLiveArrays = new HashMap<>();
        public final Map<String, Map<String, Object>> mLiveMaps = new HashMap<>();

        public static Configuration create(JSONObject json) throws JSONException {
            Configuration result = new Configuration();
            if (json.has("liveData")) {
                JSONObject liveData = json.getJSONObject("liveData");
                for (Iterator<String> it = liveData.keys(); it.hasNext(); ) {
                    String key = it.next();

                    if (liveData.optJSONArray(key) != null) {
                        List<Object> l = new ArrayList<>();
                        for (int i = 0; i < liveData.getJSONArray(key).length(); i++) {
                            l.add(liveData.getJSONArray(key).get(i));
                        }
                        result.mLiveArrays.put(key, l);
                    } else {
                        Map<String, Object> m = new HashMap<>();
                        for (Iterator<String> kiter = liveData.getJSONObject(key).keys(); kiter.hasNext(); ) {
                            String k = kiter.next();
                            m.put(k, liveData.getJSONObject(key).get(k));
                        }
                        result.mLiveMaps.put(key, m);
                    }
                }
            }
            return result;
        }
    }

    protected ViewSetDocumentCommandRequestModel(JSONObject obj, CommandRequestValidator commandRequestValidator, DTConnection connection) throws JSONException, DTException {
        super(CommandMethod.VIEW_SET_DOCUMENT, obj, commandRequestValidator, connection);
        try {
            JSONObject params = obj.getJSONObject("params");
            JSONObject document = params.getJSONObject("document");
            if (document.has("name") && document.getString("name").equals("RenderDocument")) {
                JSONObject payload = document.getJSONObject("payload");
                mParams = new Params(payload.getJSONObject("document").toString(),
                        payload.getJSONObject("datasources").toString());
            } else {
                mParams = new Params(document.toString());

                JSONObject datasources = params.optJSONObject("data");
                if (datasources != null) mParams.data(datasources.toString());
            }

            if (params.has("configuration")) {
                mParams.configuration(Configuration.create(params.getJSONObject("configuration")));
            }
        } catch (JSONException e) {
            throw new DTException(getId(), DTError.INVALID_DOCUMENT.getErrorCode(),
                    "Invalid APL document", e);
        }
    }

    public Params getParams() {
        return mParams;
    }

    public static class Params {
        private final String mDocument;
        private String mData;
        private Configuration mConfiguration;

        private Params(String document) {
            mDocument = document;
            mData = null;
        }

        private Params(String document, String data) {
            mDocument = document;
            mData = data;
        }

        private void configuration(Configuration configuration) {
            mConfiguration = configuration;
        }

        private void data(String data) {
            mData = data;
        }

        public String getDocument() {
            return mDocument;
        }

        public String getData() {
            return mData;
        }

        public Configuration getConfiguration() { return mConfiguration; }
    }
}
