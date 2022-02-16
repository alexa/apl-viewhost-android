/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters;

import android.content.Context;
import android.renderscript.RenderScript;

/**
 * Lazy loading RenderScript provider.
 */
public class RenderScriptProvider {

    private final Context mContext;
    private final RenderScriptFactory mFactory;
    private RenderScript mRenderScript;

    public RenderScriptProvider(RenderScriptFactory factory, Context context) {
        mFactory = factory;
        mContext = context;
    }

    public RenderScript get() {
        if (mRenderScript == null) {
            mRenderScript = mFactory.create(mContext);
        }
        return mRenderScript;
    }

    public void destroy() {
        if (mRenderScript != null) {
            mRenderScript.destroy();
        }
    }
}
