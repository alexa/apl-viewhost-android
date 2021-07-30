/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies.impl;

import com.amazon.apl.android.providers.IDataRetriever;

public class NoOpDataRetriever implements IDataRetriever {
    @Override
    public void fetch(String source, Callback callback) {

    }

    @Override
    public void cancelAll() {

    }
}
