/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.helper;

import android.graphics.Shader;

import java.lang.reflect.Field;

public class ShaderWrapper {
    final Shader mWrappedShader;

    ShaderWrapper(Shader shader) {
        mWrappedShader = shader;
    }

    protected <T> T getWithReflection(String fieldName) {
        try {
            Field field = mWrappedShader.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(mWrappedShader);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return null;
        }
    }
}
