/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.amazon.apl.android.image.filters;

import android.graphics.Bitmap;
import android.graphics.Color;
import androidx.core.graphics.ColorUtils;

import com.amazon.apl.enums.BlendMode;

/** https://gist.github.com/Den-Rimus/a64615b197f04869a5a6 */
public class Blender {

    private static final String TAG = "Blender";

    // TODO: Migrate all low level pixel operations to JNI layer for speed.
    // APL Spec: https://developer.amazon.com/en-US/docs/alexa/alexa-presentation-language/apl-filters.html#blend
    public static Bitmap performBlending(Bitmap source, Bitmap destination, Bitmap result, BlendMode mode) {
        int width = Math.min(source.getWidth(), destination.getWidth());
        int height = Math.min(source.getHeight(), destination.getHeight());
        final int[] srcPixels = new int[width * height];
        final int[] destPixels = new int[width * height];
        final int[] resultPixels = new int[width * height];

        final float[] sourceHsl = new float[3];
        final float[] destinationHsl = new float[3];
        final float[] resultHsl = new float[3];

        source.getPixels(srcPixels, 0, width, 0, 0, width, height);
        destination.getPixels(destPixels, 0, width, 0, 0, width, height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgbS = srcPixels[y * width + x];
                int aS = Color.alpha(rgbS);

                int rgbD = destPixels[y * width + x];
                int aD = Color.alpha(rgbD);

                int blendAlpha = aS + aD - Math.round((aS * aD) / 255f);
                int blendColor = blendPixels(blendAlpha, rgbS, rgbD, sourceHsl, destinationHsl, resultHsl, mode);

                resultPixels[y * width + x] = (blendAlpha << 24) | blendColor;
            }
        }

        result.setPixels(resultPixels, 0, result.getWidth(), 0, 0, result.getWidth(), result.getHeight());
        return result;
    }

    private static int blendPixels(int blendAlpha, int sourceColor, int destinationColor, float[] sourceHsl, float[] destinationHsl, float[] result, BlendMode mode) {
        switch (mode) {
            case kBlendModeColor:
                ColorUtils.RGBToHSL(Color.red(destinationColor), Color.green(destinationColor), Color.blue(destinationColor), destinationHsl);
                ColorUtils.RGBToHSL(Color.red(sourceColor), Color.green(sourceColor), Color.blue(sourceColor), sourceHsl);
                result[0] = sourceHsl[0];
                result[1] = sourceHsl[1];
                result[2] = destinationHsl[2];
                return ColorUtils.HSLToColor(result);
            case kBlendModeLuminosity:
                ColorUtils.RGBToHSL(Color.red(destinationColor), Color.green(destinationColor), Color.blue(destinationColor), destinationHsl);
                ColorUtils.RGBToHSL(Color.red(sourceColor), Color.green(sourceColor), Color.blue(sourceColor), sourceHsl);
                result[0] = destinationHsl[0];
                result[1] = destinationHsl[1];
                result[2] = sourceHsl[2];
                return ColorUtils.HSLToColor(result);
            case kBlendModeHue:
                ColorUtils.RGBToHSL(Color.red(destinationColor), Color.green(destinationColor), Color.blue(destinationColor), destinationHsl);
                ColorUtils.RGBToHSL(Color.red(sourceColor), Color.green(sourceColor), Color.blue(sourceColor), sourceHsl);
                result[0] = sourceHsl[0];
                result[1] = destinationHsl[1];
                result[2] = destinationHsl[2];
                return ColorUtils.HSLToColor(result);
            case kBlendModeSaturation:
                ColorUtils.RGBToHSL(Color.red(destinationColor), Color.green(destinationColor), Color.blue(destinationColor), destinationHsl);
                ColorUtils.RGBToHSL(Color.red(sourceColor), Color.green(sourceColor), Color.blue(sourceColor), sourceHsl);
                result[0] = destinationHsl[0];
                result[1] = sourceHsl[1];
                result[2] = destinationHsl[2];
                return ColorUtils.HSLToColor(result);
            default:
                int blendRed = blendByte(Color.red(sourceColor), Color.red(destinationColor), mode);
                int blendGreen = blendByte(Color.green(sourceColor), Color.green(destinationColor), mode);
                int blendBlue = blendByte(Color.blue(sourceColor), Color.blue(destinationColor), mode);
                return Color.argb(blendAlpha, blendRed, blendGreen, blendBlue);
        }
    }

    /**
     * Blend the color components.
     * Refer https://developer.mozilla.org/en-US/docs/Web/CSS/blend-mode
     * @param colChannelDest
     * @param colChannelSrc
     * @param mode
     * @return - Blended color value.
     */
    private static int blendByte(int colChannelSrc, int colChannelDest, BlendMode mode) {
        int result;
        switch (mode) {
            case kBlendModeMultiply:
                result = multiplyBlendByte(colChannelSrc, colChannelDest);
                break;
            case kBlendModeScreen:
                result = screenBlendByte(colChannelSrc, colChannelDest);
                break;
            case kBlendModeOverlay:
                result = overlayBlendByte(colChannelSrc, colChannelDest);
                break;
            case kBlendModeDarken:
                result = darkenBlendByte(colChannelSrc, colChannelDest);
                break;
            case kBlendModeLighten:
                result = lightenBlendByte(colChannelSrc, colChannelDest);
                break;
            case kBlendModeColorDodge:
                result = colorDodgeBlendByte(colChannelSrc, colChannelDest);
                break;
            case kBlendModeColorBurn:
                result = colorBurnBlendByte(colChannelSrc, colChannelDest);
                break;
            case kBlendModeHardLight:
                result = hardLightBlendByte(colChannelSrc, colChannelDest);
                break;
            case kBlendModeSoftLight:
                result = softLightBlendByte(colChannelSrc, colChannelDest);
                break;
            case kBlendModeDifference:
                result = differenceBlendByte(colChannelSrc, colChannelDest);
                break;
            case kBlendModeExclusion:
                result = exclusionBlendByte(colChannelSrc, colChannelDest);
                break;
            case kBlendModeNormal:
            default:
                result = normalBlendByte(colChannelSrc, colChannelDest);
        }
        return result;
    }

    /** http://stackoverflow.com/questions/5919663/how-does-photoshop-blend-two-images-together */
    private static int normalBlendByte(int A, int B) {
        return A;
    }

    private static int multiplyBlendByte(int A, int B) {
        return ((A * B) / 255);
    }

    private static int screenBlendByte(int A, int B) {
        return 255 - (((255 - A) * (255 - B)) >> 8);
    }

    private static int overlayBlendByte(int A, int B) {
        return B < 128 ? (2 * A * B / 255) : (255 - 2 * (255 - A) * (255 - B) / 255);
    }

    private static int darkenBlendByte(int A, int B) {
        return B > A ? A : B;
    }

    private static int lightenBlendByte(int A, int B) {
        return B > A ? B : A;
    }

    private static int colorDodgeBlendByte(int A, int B) {
        return A == 255 ? A : Math.min(255, ((B << 8 ) / (255 - A)));
    }

    private static int colorBurnBlendByte(int A, int B) {
        return A == 0 ? A : Math.max(0, (255 - ((255 - B) << 8 ) / A));
    }

    /** As per https://developer.mozilla.org/en-US/docs/Web/CSS/blend-mode, hard-light is
     *  equivalent to overlay but with the layers swapped.*/
    private static int hardLightBlendByte(int A, int B) {
        return overlayBlendByte(B, A);
    }

    private static int softLightBlendByte(int A, int B) {
        return ((int) ( (B < 128) ? (2*((A>>1)+64))*((float)B/255) : (255-(2*(255-((A>>1)+64))*(float)(255-B)/255))));
    }

    private static int differenceBlendByte(int A, int B) {
        return Math.abs(A - B);
    }

    private static int exclusionBlendByte(int A, int B) {
        return (A + B - 2 * A * B / 255);
    }
}
