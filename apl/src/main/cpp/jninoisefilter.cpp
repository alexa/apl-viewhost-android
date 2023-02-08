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
    static const float SQRT_3 = 1.73205080757f;   // sqrt(3.0f);
    static const int DECIMAL_MASK = 100000000;    // mask for softRandom
    static const int CHANNEL_MAX = 255;
    static const int CHANNEL_MIN = 0;

    static int initialSeed = 42;            // initial seed value for softRandom

    // Volatile is needed here to prevent compiler optimizations that cause a Heisenbug.
    // The compiler optimizations causes the MASK in softRandom to not be applied.
    static volatile int currentSeed = initialSeed;   // current seed value
    static int generate = 0;                         // flag for gaussianRand
    static float z1 = 0.0f;                          // variable for gaussianRand

    void reset();
    void noiseFilter(uint32_t *src, int width, int height, int sigma, bool useColor, bool isUniform);
    static float softRandom();
    static float uniformRand();
    static float gaussianRand();
    static int validateChannel(int input);
    static int generateNoise(float (*noise_ptr)(void), int sigma);

    JNIEXPORT void JNICALL
    Java_com_amazon_apl_android_image_filters_NoiseFilterOperation_nativeSetNoiseSeed(
            JNIEnv *env,
            jclass type,
            jint seed) {
        initialSeed = seed;
    }

#ifdef __ANDROID__
JNIEXPORT void JNICALL
    Java_com_amazon_apl_android_image_filters_NoiseFilterOperation_nativeNoiseFilter(
            JNIEnv *env,
            jclass type,
            jobject bitmap,
            jint sigma,
            jboolean useColor,
            jboolean isUniform) {
        AndroidBitmapInfo bitmapInfo;
        AndroidBitmap_getInfo(env, bitmap, &bitmapInfo);
        int height = bitmapInfo.height;
        int width = bitmapInfo.width;

        void* pixels;
        if (AndroidBitmap_lockPixels(env, bitmap, &pixels) == ANDROID_BITMAP_RESULT_SUCCESS) {
            uint32_t *src = (uint32_t *) pixels;
            reset();
            noiseFilter(src, width, height, sigma, useColor, isUniform);
            AndroidBitmap_unlockPixels(env, bitmap);
        }
    }
#endif

    // reset states
    void reset() {
        currentSeed = initialSeed;
        generate = 0;
        z1 = 0.0f;
    }

    void noiseFilter(uint32_t *src, int width, int height, int sigma, bool useColor, bool isUniform) {
        float (*noise_ptr)(void) = (isUniform ? &uniformRand : &gaussianRand);
        const int channelSigma = validateChannel(sigma);
        for (int y = 0, index = 0, noise = 0; y < height; y++) {
            for (int x = 0; x < width; x++, index++) {
                const int pixel = src[index];
                const int alpha = (pixel >> 24) & 0xFF;
                int red = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue = pixel & 0xFF;

                noise = generateNoise(noise_ptr, channelSigma);
                red = validateChannel(red + noise);
                if (useColor) { noise = generateNoise(noise_ptr, channelSigma); }
                green = validateChannel(green + noise);
                if (useColor) { noise = generateNoise(noise_ptr, channelSigma); }
                blue = validateChannel(blue + noise);

                src[index] = static_cast<uint32_t>(alpha << 24 | red << 16 | green << 8 | blue);
            }
        }
    }

    static float softRandom() {
        int val = (( currentSeed * 1103515245) + 12345) & 0x7fffffff;
        currentSeed = val;
        return (float)(val % DECIMAL_MASK) / (float)DECIMAL_MASK;
    }

    // Uniform distribution, mean=0, sigma=1
    static float uniformRand() {
        return SQRT_3 * (softRandom() * 2 - 1.0f);
    }

    // A method described by Abramowitz and Stegun
    // Box-Muller gaussian distribution, standard algorithm, mean=0, std.dev=1
    static float gaussianRand() {
        if (generate == 0) {
            float u1 = 0.0f;
            float u2 = 0.0f;
            do {
                u1 = softRandom();
                u2 = softRandom();
            } while (u1 == 0.0f);
            float z0 = sqrt(-2.0f * log(u1)) * cos(M_PI * 2.0f * u2);
            z1 = sqrt(-2.0f * log(u1)) * sin(M_PI * 2.0f * u2);
            generate = 1;
            return z0;
        }
        generate = 0;
        return z1;
    }

    // input can be out of channel range [CHANNEL_MIN, CHANNEL_MAX], which is [0, 255]
    // output is guaranteed in channel range
    static int validateChannel(int input) {
        return fmax(fmin(input, CHANNEL_MAX), CHANNEL_MIN);
    }

    static int generateNoise(float (*noise_ptr)(void), int sigma) {
        return round(sigma * (*noise_ptr)());
    }

#ifdef __cplusplus
}
#endif