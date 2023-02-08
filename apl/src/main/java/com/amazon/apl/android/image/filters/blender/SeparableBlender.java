/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters.blender;

import android.graphics.Color;

import com.amazon.apl.enums.BlendMode;

import java.security.InvalidParameterException;

// A subset of the blend types can be calculated on each on the color channels independently.
// These are called "separable" blend modes.
// https://www.w3.org/TR/compositing-1/#blendingseparable
public class SeparableBlender extends Blender {

    public SeparableBlender(BlendMode blendMode) {
        super(blendMode);
    }

    // Equations used from https://github.com/google/skia/blob/main/src/opts/SkRasterPipeline_opts.h.
    @Override
    int blendPixels(int sourceColor, int destinationColor) {
        // The blend calculations work on a value range from 0.0f to 1.0f. So we need to convert the 255 color to that.
        float alphaSrc = Color.alpha(sourceColor) / 255f;
        float alphaDst = Color.alpha(destinationColor) / 255f;

        // The screen blending mode uses a different calculation for alphas.
        float blendAlpha = mBlendMode == BlendMode.kBlendModeScreen ?
                blendColor(alphaSrc, alphaDst, alphaSrc, alphaDst)
                : mad(alphaDst, inverse(alphaSrc), alphaSrc); //(alphaSrc + alphaDst) - (alphaSrc * alphaDst);

        float blendRed = blendColor(Color.red(sourceColor) / 255f, Color.red(destinationColor) / 255f, alphaSrc, alphaDst);
        float blendGreen = blendColor(Color.green(sourceColor) / 255f, Color.green(destinationColor) / 255f, alphaSrc, alphaDst);
        float blendBlue = blendColor(Color.blue(sourceColor) / 255f, Color.blue(destinationColor) / 255f, alphaSrc, alphaDst);

        // Times by 255 to move the value back to the 255 range.
        return Color.argb(
                Math.round(blendAlpha * 255),
                Math.round(blendRed * 255),
                Math.round(blendGreen * 255),
                Math.round(blendBlue * 255));
    }

    private float blendColor(float colorSrc, float colorDst, float alphaSrc, float alphaDst) {
        switch (mBlendMode) {
            case kBlendModeScreen:
                return blendScreen(colorSrc, colorDst, alphaSrc, alphaDst);
            case kBlendModeOverlay:
                return blendOverlay(colorSrc, colorDst, alphaSrc, alphaDst);
            case kBlendModeDarken:
                return blendDarken(colorSrc, colorDst, alphaSrc, alphaDst);
            case kBlendModeLighten:
                return blendLighten(colorSrc, colorDst, alphaSrc, alphaDst);
            case kBlendModeColorDodge:
                return blendColorDodge(colorSrc, colorDst, alphaSrc, alphaDst);
            case kBlendModeColorBurn:
                return blendColorBurn(colorSrc, colorDst, alphaSrc, alphaDst);
            case kBlendModeHardLight:
                return blendHardLight(colorSrc, colorDst, alphaSrc, alphaDst);
            case kBlendModeSoftLight:
                return blendSoftLight(colorSrc, colorDst, alphaSrc, alphaDst);
            case kBlendModeDifference:
                return blendDifference(colorSrc, colorDst, alphaSrc, alphaDst);
            case kBlendModeExclusion:
                return blendExclusion(colorSrc, colorDst, alphaSrc, alphaDst);
            default:
                throw new InvalidParameterException();
        }
    }

    private float blendScreen(float colorSrc, float colorDst, float alphaSrc, float alphaDst) {
        return colorSrc + colorDst - colorSrc * colorDst;
    }

    private float blendOverlay(float colorSrc, float colorDst, float alphaSrc, float alphaDst) {
        if (two(colorDst) <= alphaDst) {
            return colorSrc * inverse(alphaDst) + colorDst * inverse(alphaSrc)
                    + two(colorSrc * colorDst);
        } else {
            return colorSrc * inverse(alphaDst) + colorDst * inverse(alphaSrc)
                    + alphaSrc * alphaDst - two((alphaDst - colorDst) * (alphaSrc - colorSrc));
        }
    }

    private float blendDarken(float colorSrc, float colorDst, float alphaSrc, float alphaDst) {
        return colorSrc + colorDst - Math.max(colorSrc * alphaDst, colorDst * alphaSrc);
    }

    private float blendLighten(float colorSrc, float colorDst, float alphaSrc, float alphaDst) {
        return colorSrc + colorDst - Math.min(colorSrc * alphaDst, colorDst * alphaSrc);
    }

    private float blendColorDodge(float colorSrc, float colorDst, float alphaSrc, float alphaDst) {
        if (colorDst == 0) {
            return colorSrc * inverse(alphaDst);
        } else if (colorSrc == alphaSrc) {
            return colorSrc + colorDst * inverse(alphaSrc);
        } else {
            return alphaSrc * Math.min(alphaDst, (colorDst * alphaSrc) * rcp(alphaSrc - colorSrc))
                    + colorSrc * inverse(alphaDst)
                    + colorDst * inverse(alphaSrc);
        }
    }

    private float blendColorBurn(float colorSrc, float colorDst, float alphaSrc, float alphaDst) {
        if (colorDst == alphaDst) {
            return colorDst + colorSrc * inverse(alphaDst);
        } else if (colorSrc == 0) {
            return colorDst * inverse(alphaSrc);
        } else {
            return alphaSrc * (alphaDst - Math.min(alphaDst, (alphaDst - colorDst) * alphaSrc * rcp(colorSrc)))
                    + colorSrc * inverse(alphaDst)
                    + colorDst * inverse(alphaSrc);
        }
    }

    private float blendHardLight(float colorSrc, float colorDst, float alphaSrc, float alphaDst) {
        if (two(colorSrc) <= alphaSrc) {
            return colorSrc * inverse(alphaDst) + colorDst * inverse(alphaSrc)
                    + two(colorSrc * colorDst);
        } else {
            return colorSrc * inverse(alphaDst) + colorDst * inverse(alphaSrc)
                    + alphaSrc * alphaDst - two((alphaDst - colorDst) * (alphaSrc - colorSrc));
        }
    }

    private float blendSoftLight(float colorSrc, float colorDst, float alphaSrc, float alphaDst) {
        float m = alphaDst > 0 ? colorDst / alphaDst : 0;
        float s2 = two(colorSrc);
        float m4 = two(two(m));

        if (s2 <= alphaSrc) {
            return colorSrc * inverse(alphaDst) + colorDst * inverse(alphaSrc)
                    + colorDst * (alphaSrc + (s2 - alphaSrc) * (1.0f - m));
        } else if (two(two(colorDst)) <= alphaDst) {
            return colorSrc * inverse(alphaDst) + colorDst * inverse(alphaSrc)
                    + colorDst * alphaSrc + alphaDst * (s2 - alphaSrc) * ((m4 * m4 + m4) * (m - 1.0f) + 7.0f * m);
        } else {
            return colorSrc * inverse(alphaDst) + colorDst * inverse(alphaSrc)
                    + colorDst * alphaSrc + alphaDst * (s2 - alphaSrc) * ((float)Math.sqrt(m) - m);
        }
    }

    private float blendDifference(float colorSrc, float colorDst, float alphaSrc, float alphaDst) {
        return colorSrc + colorDst - two(Math.min(colorSrc * alphaDst, colorDst * alphaSrc));
    }

    private float blendExclusion(float colorSrc, float colorDst, float alphaSrc, float alphaDst) {
        return colorSrc + colorDst - two(colorSrc * colorDst);
    }

    private float rcp(float A) {
        return 1.0f / A;
    }

    private float inverse(float A) {
        return 1.0f - A;
    }

    private float two(float A) {
        return A + A;
    }

    private float mad(float f, float m, float a) {
        return f * m + a;
    }
}
