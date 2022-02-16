/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters;

import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.ScriptIntrinsic;
import android.renderscript.ScriptIntrinsicBlend;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.ScriptIntrinsicColorMatrix;

import androidx.annotation.Nullable;

/**
 * Class for testing RenderScript dependency.
 */
public class RenderScriptWrapper {
    private final RenderScriptProvider mRenderScriptProvider;

    public RenderScriptWrapper(RenderScriptProvider renderScriptProvider) {
        mRenderScriptProvider = renderScriptProvider;
    }

    public Allocation createFromBitmap(Bitmap source, Allocation.MipmapControl mipmapControl, int usage) {
        return Allocation.createFromBitmap(mRenderScriptProvider.get(), source, mipmapControl, usage);
    }

    @Nullable
    public <T extends ScriptIntrinsic> T createScript(Element element, Class<T> clazz) {
        ScriptIntrinsic scriptIntrinsic = null;
        if (clazz == ScriptIntrinsicBlur.class) {
            scriptIntrinsic = ScriptIntrinsicBlur.create(mRenderScriptProvider.get(), element);
        } else if (clazz == ScriptIntrinsicBlend.class) {
            scriptIntrinsic = ScriptIntrinsicBlend.create(mRenderScriptProvider.get(), element);
        } else if (clazz == ScriptIntrinsicColorMatrix.class) {
            scriptIntrinsic = ScriptIntrinsicColorMatrix.create(mRenderScriptProvider.get());
        }

        return clazz.cast(scriptIntrinsic);
    }

    /**
     * Destroy this Renderscript freeing up resources on API < 23.
     */
    public void destroy() {
        mRenderScriptProvider.destroy();
    }
}
