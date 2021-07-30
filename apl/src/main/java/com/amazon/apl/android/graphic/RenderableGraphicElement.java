/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.amazon.apl.android.graphic;

import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;

import com.amazon.apl.android.PropertyMap;
import com.amazon.apl.enums.ObjectType;

import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyFill;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyFillOpacity;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyFillTransform;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyStroke;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyStrokeOpacity;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyStrokeTransform;
import static com.amazon.apl.enums.GraphicPropertyKey.kGraphicPropertyStrokeWidth;

/**
 * Common interface for graphic elements which can be stroked/filled.
 */
interface RenderableGraphicElement {

    /**
     * The properties for the RenderableGraphicElement.
     */
    PropertyMap getProperties();

    /**
     * The fill pattern. Only applicable when the fill type is Pattern.
     */
    GraphicPattern getFillGraphicPattern();

    /**
     * The stroke pattern. Only applicable when the stroke type is Pattern.
     */
    GraphicPattern getStrokeGraphicPattern();

    /**
     * The type of the fill. Supported types are Color, Gradient, and Pattern.
     */
    default ObjectType getFillType() {
        return getProperties().getType(kGraphicPropertyFill);
    }

    /**
     * The opacity of the element's fill.
     */
    default float getFillOpacity() {
        return getProperties().getFloat(kGraphicPropertyFillOpacity);
    }

    /**
     * Transformation applied against the fill. Only applicable if fill type is Gradient or Pattern.
     */
    default Matrix getFillTransform() {
        return getProperties().getTransform(kGraphicPropertyFillTransform);
    }

    /**
     * The color of the element's fill. Only applicable when the fill type is Color.
     */
    default int getFillColor() {
        return getProperties().getColor(kGraphicPropertyFill);
    }

    /**
     * The type of the type. Supported types are Color, Gradient, and Pattern.
     */
    default ObjectType getStrokeType() {
        return getProperties().getType(kGraphicPropertyStroke);
    }

    /**
     * The opacity of the element's stroke.
     */
    default float getStrokeOpacity() {
        return getProperties().getFloat(kGraphicPropertyStrokeOpacity);
    }

    /**
     * Transformation applied against the stroke. Only applicable if stroke type is Gradient or
     * Pattern.
     */
    default Matrix getStrokeTransform() {
        return getProperties().getTransform(kGraphicPropertyStrokeTransform);
    }

    /**
     * The color of the element's stroke. Only applicable when the stroke type is Color.
     */
    default int getStrokeColor() {
        return getProperties().getColor(kGraphicPropertyStroke);
    }

    /**
     * The width of the stroke.
     */
    default float getStrokeWidth() {
        return getProperties().getFloat(kGraphicPropertyStrokeWidth);
    }


    default Paint getFillPaint() {
        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);
        final ObjectType type = getFillType();
        if (ObjectType.kGraphicPatternType.equals(type)) {
            fillPaint.setShader(PathRenderer.createPattern(getFillTransform(),
                    getFillGraphicPattern()));
            fillPaint.setAlpha((int)(255 * getFillOpacity()));
        } else if (ObjectType.kColorType.equals(type)) {
            fillPaint.setColor(applyAlpha(getFillColor(),
                    getFillOpacity()));
        } else {
            // gradient shader is applied during draw call.
            fillPaint.setAlpha((int)(255 * getFillOpacity()));
        }
        return fillPaint;
    }

    default Paint getStrokePaint() {
        Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(getStrokeWidth());
        final ObjectType type = getStrokeType();
        if (ObjectType.kGraphicPatternType.equals(type)) {
            strokePaint.setShader(PathRenderer.createPattern(getStrokeTransform(),
                    getStrokeGraphicPattern()));
            strokePaint.setAlpha((int)(255 * getStrokeOpacity()));
        } else if (ObjectType.kColorType.equals(type)) {
            strokePaint.setColor(applyAlpha(getStrokeColor(),
                    getStrokeOpacity()));
        } else {
            // gradient shader is applied during draw call.
            strokePaint.setAlpha((int)(255 * getStrokeOpacity()));
        }
        return strokePaint;
    }

    static int applyAlpha(final int color, final float alpha) {
        int alphaBytes = Color.alpha(color);
        int colorWithAlphaApplied = color & 0x00FFFFFF;
        colorWithAlphaApplied |= ((int) (alphaBytes * alpha)) << 24;
        return colorWithAlphaApplied;
    }
}
