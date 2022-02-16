/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.primitive;

import com.amazon.common.BoundObject;

import com.amazon.apl.android.utils.HttpUtils;
import com.amazon.apl.enums.APLEnum;

import java.util.ArrayList;

class UrlRequestGetter extends ArrayGetter<UrlRequests, UrlRequests.UrlRequest> {
    UrlRequestGetter(BoundObject boundObject, APLEnum propertyKey) {
        super(boundObject, propertyKey);
    }

    @Override
    UrlRequests builder() {
        return new AutoValue_UrlRequests(new ArrayList<>());
    }

    @Override
    public UrlRequests.UrlRequest get(int index) {
        return UrlRequests.UrlRequest.builder()
                .url(nGetUrlRequestSourceAt(getNativeHandle(), getIndex(), index))
                .headers(HttpUtils.listToHeadersMap(nGetUrlRequestHeadersAt(getNativeHandle(), getIndex(), index)))
                .build();
    }

    /**
     * UrlRequests Sources can be either an array, or a single value.
     * This size method returns 1 for single value.
     * @return
     */
    @Override
    public int size() {
        return nSize(getNativeHandle(), getIndex());
    }

    private native int nSize(long componentHandle, int propertyKey);

    private static native String nGetUrlRequestSourceAt(long componentHandle, int propertyKey, int index);

    private static native String[] nGetUrlRequestHeadersAt(long componentHandle, int propertyKey, int index);
}
