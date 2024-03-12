/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.util;

public final class IdGenerator {
    private int mNextId = 100;

    public IdGenerator() {
    }

    public IdGenerator(int startId) {
        mNextId = startId;
    }

    public int generateId() {
        int id = mNextId;
        mNextId++;
        return id;
    }

    public String generateId(String prefix) {
        return prefix + generateId();
    }
}
