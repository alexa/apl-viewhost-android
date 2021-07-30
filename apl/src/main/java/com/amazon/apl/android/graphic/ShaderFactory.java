/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.graphic;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.Shader;
import androidx.annotation.NonNull;

import com.amazon.apl.android.APLVersionCodes;
import com.amazon.apl.android.RenderingContext;
import com.amazon.apl.android.primitive.Gradient;

/**
 * Factory class to get the {@link Shader} object with all the fields set.
 */
public class ShaderFactory {
    private static ShaderFactory sShaderFactory;

    private ShaderFactory() {
    }

    @NonNull
    static ShaderFactory getInstance() {
        if(sShaderFactory == null) {
            sShaderFactory = new ShaderFactory();
        }
        return sShaderFactory;
    }

    /**
     * Get the {@link Shader} object.
     * @param graphicGradient
     * @param graphicBounds
     * @param transform
     * @return - {@link android.graphics.LinearGradient} or {@link android.graphics.RadialGradient}
     */
    Shader getShader(final Gradient graphicGradient,
                     final Rect graphicBounds,
                     final Matrix transform,
                     final RenderingContext renderingContext) {
        Shader shader = null;
        switch(graphicGradient.getType()) {
            case LINEAR:
                shader = new LinearShaderWrapper(graphicGradient, graphicBounds, transform).getShader();
                break;
            case RADIAL:
                if (renderingContext.getDocVersion() <= APLVersionCodes.APL_1_6) {
                    shader = new RadialShaderWrapper(graphicGradient, graphicBounds, transform).getShader();
                } else {
                    shader = new RadialShaderWrapperV2(graphicGradient, graphicBounds, transform).getShader();
                }
                break;
        }
        return shader;
    }
}
