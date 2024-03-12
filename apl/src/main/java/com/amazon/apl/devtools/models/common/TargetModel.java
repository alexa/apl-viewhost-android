/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.devtools.models.common;

import com.amazon.apl.devtools.enums.TargetType;
import com.amazon.apl.devtools.util.DependencyContainer;
import com.amazon.apl.devtools.util.TargetCatalog;

public abstract class TargetModel {
    private final static String SEPERATOR = ".";
    private final String mTargetId;
    private final TargetType mType;
    private final String mName;

    protected TargetModel(String targetId, TargetType type, String name) {
        TargetCatalog catalog = DependencyContainer.getInstance().getTargetCatalog();
        mTargetId = targetId;
        mType = type;
        mName = name + SEPERATOR + catalog.getAll().size();
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
