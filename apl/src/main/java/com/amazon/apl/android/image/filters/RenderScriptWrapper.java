/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsic;
import android.renderscript.ScriptIntrinsicBlend;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.ScriptIntrinsicColorMatrix;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.amazon.apl.android.IDocumentLifecycleListener;
import com.amazon.apl.android.RootContext;
import com.amazon.apl.android.thread.Threading;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Class for testing RenderScript dependency.
 */
public class RenderScriptWrapper {
    private final RenderScript mRenderScript;

    public RenderScriptWrapper(Context context) {
        mRenderScript = RenderScript.create(context.getApplicationContext());
    }

    public Allocation createFromBitmap(Bitmap source, Allocation.MipmapControl mipmapControl, int usage) {
        return Allocation.createFromBitmap(mRenderScript, source, mipmapControl, usage);
    }

    @Nullable
    public <T extends ScriptIntrinsic> T createScript(Element element, Class<T> clazz) {
        ScriptIntrinsic scriptIntrinsic = null;
        if (clazz == ScriptIntrinsicBlur.class) {
            scriptIntrinsic = ScriptIntrinsicBlur.create(mRenderScript, element);
        } else if (clazz == ScriptIntrinsicBlend.class) {
            scriptIntrinsic = ScriptIntrinsicBlend.create(mRenderScript, element);
        } else if (clazz == ScriptIntrinsicColorMatrix.class) {
            scriptIntrinsic = ScriptIntrinsicColorMatrix.create(mRenderScript);
        }

        return clazz.cast(scriptIntrinsic);
    }

    /**
     * Destroy this Renderscript freeing up resources on API < 23.
     */
    public void destroy() {
        mRenderScript.destroy();
    }
}
