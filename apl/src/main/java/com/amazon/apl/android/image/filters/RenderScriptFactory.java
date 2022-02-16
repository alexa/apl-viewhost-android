/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters;

import android.content.Context;
import android.renderscript.RenderScript;

public interface RenderScriptFactory {
    RenderScript create(Context context);
}
