/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android;

import android.text.StaticLayout;
import android.text.TextPaint;

import com.amazon.apl.android.text.TextMeasuringInput;

public interface ITextMeasurementCache {
    void put(String componentId, TextMeasuringInput measuringInput, StaticLayout textLayout);
    TextMeasuringInput getMeasuringInput(String componentId);
    StaticLayout getStaticLayout(String componentId);
    TextPaint getTextPaint(TextMeasuringInput measuringInput);
    void clear();
}
