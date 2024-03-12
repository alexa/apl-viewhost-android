/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.network;

import android.util.Log;
import com.amazon.apl.devtools.enums.DTNetworkRequestType;
import com.amazon.apl.devtools.models.ViewTypeTarget;

public class DTNetworkRequestHandler implements IDTNetworkRequestHandler {
    private final static String TAG = DTNetworkRequestHandler.class.getSimpleName();
    private final ViewTypeTarget mTarget;

    public DTNetworkRequestHandler(ViewTypeTarget target) {
        mTarget = target;
    }

    @Override
    public void requestWillBeSent(int requestId, double timestamp, String url, DTNetworkRequestType type) {
        Log.d(TAG, "Event requestWillBeSent to " + mTarget.getName() + ". with request Id: " + requestId);
        mTarget.post(() -> mTarget.onNetworkRequestWillBeSent(requestId, timestamp, url, type.toString()));
    }

    @Override
    public void loadingFailed(int requestId, double timestamp) {
        Log.d(TAG, "Event loadingFailed to " + mTarget.getName() + ". with request Id: " + requestId);
        mTarget.post(() -> mTarget.onNetworkLoadingFailed(requestId, timestamp));
    }

    @Override
    public void loadingFinished(int requestId, double timestamp, int encodedDataLength) {
        Log.d(TAG, "Event loadingFinished to " + mTarget.getName() + ". with request Id: " + requestId);
        mTarget.post(() -> mTarget.onNetworkLoadingFinished(requestId, timestamp, encodedDataLength));
    }
}
