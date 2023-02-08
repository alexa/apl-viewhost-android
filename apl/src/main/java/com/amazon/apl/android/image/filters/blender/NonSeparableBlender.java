/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters.blender;

import android.graphics.Color;

import com.amazon.apl.enums.BlendMode;

// A subset of the blend modes need to blend using all the color channels at once. They cannot be
// calculated individually. These are the "Non Separable" blend modes.
// https://www.w3.org/TR/compositing-1/#blendingnonseparable
public class NonSeparableBlender extends Blender {

    public NonSeparableBlender(BlendMode blendMode) {
        super(blendMode);
    }

    @Override
     int blendPixels(int sourceColor, int destinationColor) {
        // All calculations work on the 0.0 to 1.0f color range.
        float srcR = Color.red(sourceColor) / 255f;
        float srcG = Color.green(sourceColor) / 255f;
        float srcB = Color.blue(sourceColor) / 255f;
        float srcA = Color.alpha(sourceColor) / 255f;

        float dstR = Color.red(destinationColor) / 255f;
        float dstG = Color.green(destinationColor) / 255f;
        float dstB = Color.blue(destinationColor) / 255f;
        float dstA = Color.alpha(destinationColor) / 255f;

        float[] rgb = new float[3];

        switch (mBlendMode) {
            case kBlendModeHue:
                rgb[0] = srcR * srcA;
                rgb[1] = srcG * srcA;
                rgb[2] = srcB * srcA;

                setSat(rgb, sat(dstR, dstG, dstB) * srcA);
                setLum(rgb, lum(dstR, dstG, dstB) * srcA);
                break;

            case kBlendModeSaturation:
                rgb[0] = dstR * srcA;
                rgb[1] = dstG * srcA;
                rgb[2] = dstB * srcA;

                setSat(rgb, sat(srcR, srcG, srcB) * dstA);
                setLum(rgb, lum(dstR, dstG, dstB) * srcA);
                break;

            case kBlendModeColor:
                rgb[0] = srcR * dstA;
                rgb[1] = srcG * dstA;
                rgb[2] = srcB * dstA;

                setLum(rgb, lum(dstR, dstG, dstB) * srcA);
                break;

            case kBlendModeLuminosity:
                rgb[0] = dstR * srcA;
                rgb[1] = dstG * srcA;
                rgb[2] = dstB * srcA;

                setLum(rgb, lum(srcR, srcG, srcB) * dstA);
                break;

            default:
                throw new IllegalStateException("Unexpected value: " + mBlendMode);
        }

        clipColor(rgb, srcA * dstA);

        float r = srcR * inverse(dstA) + dstR * inverse(srcA) + rgb[0];
        float g = srcG * inverse(dstA) + dstG * inverse(srcA) + rgb[1];
        float b = srcB * inverse(dstA) + dstB * inverse(srcA) + rgb[2];
        float a = srcA + dstA - srcA * dstA;

        // Times by 255 to move the value back to the 255 range.
        return Color.argb(
                Math.round(a * 255),
                Math.round(r * 255),
                Math.round(g * 255),
                Math.round(b * 255));
    }

    private float scale(float c, float s, float min, float sat) {
        return sat == 0 ? 0 : (c - min) * s / sat;
    }

    private void setSat(float[] rgb, float s) {
        float r = rgb[0];
        float g = rgb[1];
        float b = rgb[2];
        float min = Math.min(r, Math.min(g, b));
        float max = Math.max(r, Math.max(g, b));
        float sat = max - min;

        rgb[0] = scale(r, s, min, sat);
        rgb[1] = scale(g, s, min, sat);
        rgb[2] = scale(b, s, min, sat);
    }

    private void setLum(float[] rgb, float l) {
        float diff = l - lum(rgb[0], rgb[1], rgb[2]);

        rgb[0] += diff;
        rgb[1] += diff;
        rgb[2] += diff;
    }

    private void clipColor(float[] rgb, float a) {
        float r = rgb[0];
        float g = rgb[1];
        float b = rgb[2];
        float min = Math.min(r, Math.min(g, b));
        float max = Math.max(r, Math.max(g, b));
        float l = lum(r, g, b);

        rgb[0] = clip(r, min, max, l, a);
        rgb[1] = clip(g, min, max, l, a);
        rgb[2] = clip(b, min, max, l, a);
    }

    private float clip(float c, float min, float max, float l, float a) {
        c = min < 0 && l != min ? l + (c - l) * (    l) / (l - min) : c;
        c = max > a && l != max ? l + (c - l) * (a - l) / (max - l) : c;
        c = Math.max(c, 0);
        return c;
    }

    private float sat(float r, float g, float b) {
        return Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b));
    }

    private float lum(float r, float g, float b) {
        return r * 0.30f + g * 0.59f + b * 0.11f;
    }

    private float inverse(float A) {
        return 1.0f - A;
    }
}
