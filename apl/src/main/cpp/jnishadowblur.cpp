/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */
#include <jni.h>
#include <algorithm>
#include <cmath>
#include <cstdint>
#include <stdint.h>

#ifdef __ANDROID__
#include <android/bitmap.h>
#endif

#ifdef __cplusplus
extern "C" {
#endif

    void boxBlur(uint32_t *src, uint32_t *dst, int width, int height, int blurRadius);

#ifdef __ANDROID__
JNIEXPORT void JNICALL
    Java_com_amazon_apl_android_shadow_ShadowBoxBlur_nativeBoxBlur(
            JNIEnv *env,
            jclass type,
            jobject bitmap,
            jint blurRadius) {
        AndroidBitmapInfo bitmapInfo;
        AndroidBitmap_getInfo(env, bitmap, &bitmapInfo);
        int height = bitmapInfo.height;
        int width = bitmapInfo.width;

        void* pixels;
        if (AndroidBitmap_lockPixels(env, bitmap, &pixels) == ANDROID_BITMAP_RESULT_SUCCESS) {
            uint32_t *src = (uint32_t *) pixels;
            uint32_t *dst = new uint32_t[bitmapInfo.height * bitmapInfo.width];

            // perform three passes to get close approximation to gaussian blur
            for (int i = 0; i < 3; i++ ) {
                // horizontal box blur
                boxBlur(src, dst, width, height, blurRadius );
                // vertical box blur
                boxBlur(dst, src, height, width, blurRadius );
            }

            AndroidBitmap_unlockPixels(env, bitmap);
            delete[] dst;
        }
    }
#endif

    /**
    * Box-blur the given src pixels into the given dst. This is done by creating a 1-D filter matrix
    * whose size is a function of the blur radius and moving it over the src pixels, row by row.
    * Each new pixel is computed as the average of all the pixels currently "overlaid" by the filter matrix,
    * with the center of the filter overlaid over the pixel we are computing.
    *
    * This algorithm can be further optimized:
    * a) only apply along the "edges" where the blur radius actually takes effect, since for shadows
    *    the center is just a solid colour
    * b) also due to the shadow being a solid we can just compute one row and copy the result to
    *    all other rows (excluding corners since those can have varying corner-radii)
    *
    * TODO: consider optimizing performance using RenderScript
    */
    void boxBlur(uint32_t *src, uint32_t *dst, int width, int height, int blurRadius) {
        float sigma = ((float) blurRadius) / 2;
        int boxWidth = (int) floor(sigma * 3 * sqrt(2*M_PI)/4 + 0.5); // w3 blur spec https://www.w3.org/TR/SVG11/filters.html#feGaussianBlurElement
        if(boxWidth % 2 == 0) boxWidth += 1; // keep it odd so we have one center
        int boxRadius = boxWidth/2;
        int rowSrc = 0;
        for (int y = 0; y < height; y++) {
            int sumAlpha = 0, sumRed = 0, sumGreen = 0, sumBlue = 0;
            // compute the box filter average once at start of each row, then we can use a "moving-window" average for rest of row
            for (int i = -boxRadius; i <= boxRadius; i++ ) {
                int pixel = src[rowSrc + std::max(0, std::min(i, width-1))]; // if we overflow just use the edge pixel
                sumAlpha += (pixel >> 24) & 0xFF;
                sumRed += (pixel >> 16) & 0xFF;
                sumGreen += (pixel >> 8) & 0xFF;
                sumBlue += pixel & 0xFF;
            }

            int avgAlpha, avgRed, avgGreen, avgBlue;
            int enteringSrcColumn, exitingSrcColumn;
            for (int x = 0; x < width; x++) {

                avgAlpha = sumAlpha / boxWidth;
                avgRed = sumRed / boxWidth;
                avgGreen = sumGreen / boxWidth;
                avgBlue = sumBlue / boxWidth;

                // set the new pixel to the current average
                dst[x*height + y] = static_cast<uint32_t>(avgAlpha << 24 | avgRed << 16 | avgGreen << 8 | avgBlue);

                // Add the next pixel to the moving-window average for next iteration
                enteringSrcColumn = std::min(x + boxRadius + 1, width - 1);
                int enteringColour = src[rowSrc + enteringSrcColumn];
                sumAlpha += (enteringColour >> 24) & 0xFF;
                sumRed += (enteringColour >> 16) & 0xFF;
                sumGreen += (enteringColour >> 8) & 0xFF;
                sumBlue += enteringColour & 0xFF;

                // Subtract the pixel being removed from the moving average
                exitingSrcColumn = std::max(0, x - boxRadius);
                int exitingColour = src[rowSrc + exitingSrcColumn];
                sumAlpha -= (exitingColour >> 24) & 0xFF;
                sumRed -= (exitingColour >> 16) & 0xFF;
                sumGreen -= (exitingColour >> 8) & 0xFF;
                sumBlue -= exitingColour & 0xFF;
            }
            rowSrc += width;
        }
    }
#ifdef __cplusplus
}
#endif
