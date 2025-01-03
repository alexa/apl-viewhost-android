/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.common;

import com.amazon.apl.devtools.enums.TargetType;
import com.amazon.apl.devtools.util.IdGenerator;

public abstract class TargetModel {
    private final static String SEPARATOR = ".";
    private final static IdGenerator mSerializeIdGenerator = new IdGenerator(1);
    private final String mTargetId;
    private final TargetType mType;
    private final String mName;

    protected TargetModel(String targetId, TargetType type, String name) {
        mTargetId = targetId;
        mType = type;
        mName = name + SEPARATOR + mSerializeIdGenerator.generateId();
    }

    public String getTargetId() {
        return mTargetId;
    }

    public TargetType getType() {
        return mType;
    }

    public String getName() {
        return mName;
    }
}
