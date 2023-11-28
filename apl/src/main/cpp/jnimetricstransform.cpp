
/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */


#include "jnimetricstransform.h"
#include <jni.h>
#include <scaling.h>
#include <jniutil.h>

namespace apl {

    namespace jni {

#ifdef __cplusplus
        extern "C" {
#endif
        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_scaling_MetricsTransform_nCreate(JNIEnv *env, jclass clazz,
                                                             jint width, jint minWidth, jint maxWidth,
                                                             jint height, jint minHeight, jint maxHeight,
                                                             jint dpi,
                                                             jint shape,
                                                             jstring theme_,
                                                             jint mode,
                                                             jlong scalingHandle) {
            const char* theme = env->GetStringUTFChars(theme_, nullptr);
            auto metrics = Metrics();
            metrics.size(static_cast<int>(width), static_cast<int>(height))
                    .dpi(static_cast<int>(dpi))
                    .shape(static_cast<ScreenShape>(shape))
                    .theme(theme)
                    .mode(static_cast<ViewportMode>(mode));
            bool isAutoWidth = (minWidth != maxWidth);
            bool isAutoHeight = (minHeight != maxHeight);
            if (isAutoHeight) {
                metrics.minAndMaxHeight(minHeight, maxHeight);
            }
            if (isAutoWidth) {
                metrics.minAndMaxWidth(minWidth, maxWidth);
            }

            if (scalingHandle != 0) {
                auto scaling = get<apl::jni::Scaling>(scalingHandle);

                auto scalingOptions = ScalingOptions()
                        .biasConstant(scaling->biasConstant)
                        .shapeOverridesCost(true)
                        .allowedModes(scaling->allowModes);

                //add supported viewports only if auto size is disabled.
                if (!(metrics.getAutoWidth() || metrics.getAutoHeight())) {
                    scalingOptions.specifications(scaling->specifications);
                }

                auto metricsTransform = std::make_shared<MetricsTransform>(metrics, scalingOptions);
                env->ReleaseStringUTFChars(theme_, theme);
                return createHandle<MetricsTransform>(metricsTransform);
            } else {
                auto metricsTransform = std::make_shared<MetricsTransform>(metrics);
                env->ReleaseStringUTFChars(theme_, theme);
                return createHandle<MetricsTransform>(metricsTransform);
            }
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_scaling_MetricsTransform_nToViewhost(JNIEnv *env, jclass clazz, jlong handle, jfloat value) {
            auto transform = get<MetricsTransform>(handle);
            return static_cast<jfloat>(transform->toViewhost(static_cast<float>(value)));
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_scaling_MetricsTransform_nToCore(JNIEnv *env, jclass clazz, jlong handle, jfloat value) {
            auto transform = get<MetricsTransform>(handle);
            return static_cast<jfloat>(transform->toCore(static_cast<float>(value)));
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_scaling_MetricsTransform_nViewhostWidth(JNIEnv *env, jclass clazz, jlong handle) {
            auto transform = get<MetricsTransform>(handle);
            return static_cast<jfloat>(transform->getViewhostWidth());
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_scaling_MetricsTransform_nViewhostHeight(JNIEnv *env, jclass clazz, jlong handle) {
            auto transform = get<MetricsTransform>(handle);
            return static_cast<jfloat>(transform->getViewhostHeight());
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_scaling_MetricsTransform_nPixelWidth(JNIEnv *env, jclass clazz, jlong handle) {
            auto transform = get<MetricsTransform>(handle);
            // Convert dp to px
            auto pixelWidth = transform->getWidth() * transform->getDpi() / transform->CORE_DPI;
            return static_cast<jfloat>(pixelWidth);
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_scaling_MetricsTransform_nPixelHeight(JNIEnv *env, jclass clazz, jlong handle) {
            auto transform = get<MetricsTransform>(handle);
            // Convert dp to px
            auto pixelHeight = transform->getHeight() * transform->getDpi() / transform->CORE_DPI;
            return static_cast<jfloat>(pixelHeight);
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_scaling_MetricsTransform_nViewportMode(JNIEnv *env, jclass clazz, jlong handle) {
            auto transform = get<MetricsTransform>(handle);
            return static_cast<jint>(transform->getViewportMode());
        }

        }

#ifdef __cplusplus
    }
#endif
}