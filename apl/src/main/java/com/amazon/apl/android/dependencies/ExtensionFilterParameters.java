/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;

import java.util.Map;


@AutoValue
public abstract class ExtensionFilterParameters {


    public static ExtensionFilterParameters create(final String uri, final String name,
                                                   Integer source, Integer destination,
                                                   Map<String, Object> filterParams) {
        return new AutoValue_ExtensionFilterParameters(uri, name,
                source, destination, filterParams);
    }

    @NonNull
    public abstract String getURI();
    @NonNull
    public abstract String getName();
    @Nullable
    public abstract Integer getSource();
    @Nullable
    public abstract Integer getDestination();
    @Nullable
    public abstract Map<String, Object> getFilterParams();
}
