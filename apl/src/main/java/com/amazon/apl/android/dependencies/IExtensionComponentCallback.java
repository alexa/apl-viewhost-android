/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.dependencies;

import android.graphics.Rect;
import android.view.Surface;

import com.amazon.apl.android.Component;

public interface IExtensionComponentCallback {
    public void onExtensionComponentCreated(Component component, String resourceId,
                                            Surface resource, Rect rect);

    public void onExtensionComponentUpdated(Component component, String resourceId,
                                            Surface resource);

    public void onExtensionComponentDeleted(Component component, String resourceId,
                                            Surface resource);
}
