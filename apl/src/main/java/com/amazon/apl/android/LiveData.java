/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import com.amazon.common.BoundObject;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

abstract public class LiveData extends BoundObject {
    @AllArgsConstructor
    @Getter
    public static class Update {
        private String type;
        private int index;
        private String key;
        private Object value;
    }

    public abstract boolean applyUpdates(List<LiveData.Update> operations);
}
