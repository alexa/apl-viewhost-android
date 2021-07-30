/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.text.StaticLayout;
import android.text.TextPaint;

import com.amazon.apl.android.text.TextMeasuringInput;

public class NoOpTextMeasurementCache implements ITextMeasurementCache {
    private static NoOpTextMeasurementCache INSTANCE;

    private NoOpTextMeasurementCache() {}

    public static NoOpTextMeasurementCache getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NoOpTextMeasurementCache();
        }
        return INSTANCE;
    }

    @Override
    public void put(String componentId, TextMeasuringInput measuringInput, StaticLayout textLayout) {

    }

    @Override
    public TextMeasuringInput getMeasuringInput(String componentId) {
        return null;
    }

    @Override
    public StaticLayout getStaticLayout(String componentId) {
        return null;
    }

    @Override
    public TextPaint getTextPaint(TextMeasuringInput measuringInput) {
        return null;
    }

    @Override
    public void clear() {

    }
}
