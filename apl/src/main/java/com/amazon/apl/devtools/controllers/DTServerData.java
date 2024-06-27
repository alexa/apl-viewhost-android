
/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.controllers;


/**
 * This class is to track the necessary data of the DTServer that we don't want to expose to any runtimes,
 * like retry count and port number.
 */
public class DTServerData {
    private static final int DEFAULT_PORT = 9092;

    /**
     * The maximum of times to retry to start the server.
     */
    public static final int MAX_RETRIES = 15;
    private static DTServerData sInstance;
    private int mPortNumer = DEFAULT_PORT;
    private int mRetryCount = 0;

    private DTServerData() {}

    public static DTServerData getInstance() {
        if (sInstance == null) {
            sInstance = new DTServerData();
        }
        return sInstance;
    }

    public void setPortNumber(int portNumber) {
        mPortNumer = portNumber;
    }

    public void incrementRetryCount() {
        mRetryCount++;
    }

    public int getPortNumber() {
        return mPortNumer;
    }

    public int getRetryCount() {
        return mRetryCount;
    }

    public void reset() {
        mRetryCount = 0;
        mPortNumer = DEFAULT_PORT;
    }
}