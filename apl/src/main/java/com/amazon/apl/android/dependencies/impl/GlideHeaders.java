package com.amazon.apl.android.dependencies.impl;

import com.bumptech.glide.load.model.Headers;

import java.util.Map;

/**
 * A implementation of {@link Headers} that does equality checks on the Headers themselves.
 * This generates deterministic hash keys based on the headers included.
 */
class GlideStaticHeaders implements Headers {

    private Map<String, String> headers;

    public GlideStaticHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    @Override
    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public boolean equals(Object otherObject) {
        if (otherObject instanceof GlideStaticHeaders) {
            GlideStaticHeaders otherHeaders = (GlideStaticHeaders) otherObject;
            return headers.equals(otherHeaders.headers);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return headers.hashCode();
    }
}