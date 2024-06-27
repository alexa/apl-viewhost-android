/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>

#include <jnisgcontent.h>
#include <jnitextlayout.h>
#include "jniutil.h"
#include "jnimediaobject.h"

#ifdef SCENEGRAPH
namespace apl {
    namespace jni {

#ifdef __cplusplus
        extern "C" {
#endif
        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_sgcontent_filters_Filter_nGetType(JNIEnv *env,
                                                                             jclass filterClass,
                                                                             jlong nativeHandle) {
            apl::sg::Filter * filter = reinterpret_cast<apl::sg::Filter *>(nativeHandle);
            return filter->type;
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_sgcontent_filters_Filter_nGetSize(JNIEnv *env,
                                                              jclass filterClass,
                                                              jlong nativeHandle) {
            apl::sg::Filter * filter = reinterpret_cast<apl::sg::Filter *>(nativeHandle);
            auto size = filter->size();
            float sizeArray[] = {size.getWidth(), size.getHeight()};
            jfloatArray result = env->NewFloatArray(2);
            env->SetFloatArrayRegion(result,0,2, sizeArray);
            return result;
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_sgcontent_filters_Filter_nMediaObjectGetMediaObject(JNIEnv *env,
                                                                                jclass filterClass,
                                                              jlong nativeHandle) {
            apl::sg::MediaObjectFilter * filter = reinterpret_cast<apl::sg::MediaObjectFilter *>(nativeHandle);
            return reinterpret_cast<jlong>(filter->mediaObject.get());
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_sgcontent_filters_Filter_nBlurGetRadius(JNIEnv *env,
                                                                                jclass filterClass,
                                                                                jlong nativeHandle) {
            apl::sg::BlurFilter * filter = reinterpret_cast<apl::sg::BlurFilter *>(nativeHandle);
            return filter->radius;
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_sgcontent_filters_Filter_nBlurGetFilter(JNIEnv *env,
                                                                    jclass filterClass,
                                                                    jlong nativeHandle) {
            apl::sg::BlurFilter * filter = reinterpret_cast<apl::sg::BlurFilter *>(nativeHandle);
            return reinterpret_cast<jlong>(filter->filter.get());
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_sgcontent_filters_Filter_nGrayscaleGetFilter(JNIEnv *env,
                                                                    jclass filterClass,
                                                                    jlong nativeHandle) {
            apl::sg::GrayscaleFilter * filter = reinterpret_cast<apl::sg::GrayscaleFilter *>(nativeHandle);
            return reinterpret_cast<jlong>(filter->filter.get());
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_sgcontent_filters_Filter_nGrayscaleGetAmount(JNIEnv *env,
                                                                         jclass filterClass,
                                                                         jlong nativeHandle) {
            apl::sg::GrayscaleFilter * filter = reinterpret_cast<apl::sg::GrayscaleFilter *>(nativeHandle);
            return filter->amount;
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_sgcontent_filters_Filter_nNoiseGetFilter(JNIEnv *env,
                                                                         jclass filterClass,
                                                                         jlong nativeHandle) {
            apl::sg::NoiseFilter * filter = reinterpret_cast<apl::sg::NoiseFilter *>(nativeHandle);
            return reinterpret_cast<jlong>(filter->filter.get());
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_sgcontent_filters_Filter_nNoiseGetSigma(JNIEnv *env,
                                                                     jclass filterClass,
                                                                     jlong nativeHandle) {
            apl::sg::NoiseFilter * filter = reinterpret_cast<apl::sg::NoiseFilter *>(nativeHandle);
            return filter->sigma;
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_sgcontent_filters_Filter_nNoiseGetKind(JNIEnv *env,
                                                               jclass filterClass,
                                                               jlong nativeHandle) {
            apl::sg::NoiseFilter * filter = reinterpret_cast<apl::sg::NoiseFilter *>(nativeHandle);
            return filter->kind;
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_sgcontent_filters_Filter_nNoiseUseColor(JNIEnv *env,
                                                               jclass filterClass,
                                                               jlong nativeHandle) {
            apl::sg::NoiseFilter * filter = reinterpret_cast<apl::sg::NoiseFilter *>(nativeHandle);
            return filter->useColor;
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_sgcontent_filters_Filter_nSaturateGetFilter(JNIEnv *env,
                                                                         jclass filterClass,
                                                                         jlong nativeHandle) {
            apl::sg::SaturateFilter * filter = reinterpret_cast<apl::sg::SaturateFilter *>(nativeHandle);
            return reinterpret_cast<jlong>(filter->filter.get());
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_sgcontent_filters_Filter_nSaturateGetAmount(JNIEnv *env,
                                                                         jclass filterClass,
                                                                         jlong nativeHandle) {
            apl::sg::SaturateFilter * filter = reinterpret_cast<apl::sg::SaturateFilter *>(nativeHandle);
            return filter->amount;
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_sgcontent_filters_Filter_nSolidGetPaint(JNIEnv *env,
                                                                    jclass filterClass,
                                                                    jlong nativeHandle) {
            apl::sg::SolidFilter * filter = reinterpret_cast<apl::sg::SolidFilter *>(nativeHandle);
            return reinterpret_cast<jlong>(filter->paint.get());
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_sgcontent_filters_Filter_nBlendGetFront(JNIEnv *env, jclass filterClass, jlong nativeHandle) {
            apl::sg::BlendFilter * filter = reinterpret_cast<apl::sg::BlendFilter *>(nativeHandle);
            return reinterpret_cast<jlong>(filter->front.get());
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_sgcontent_filters_Filter_nBlendGetBack(JNIEnv *env, jclass filterClass, jlong nativeHandle) {
            apl::sg::BlendFilter * filter = reinterpret_cast<apl::sg::BlendFilter *>(nativeHandle);
            return reinterpret_cast<jlong>(filter->back.get());
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_sgcontent_filters_Filter_nBlendGetBlendMode(JNIEnv *env, jclass filterClass, jlong nativeHandle) {
            apl::sg::BlendFilter * filter = reinterpret_cast<apl::sg::BlendFilter *>(nativeHandle);
            return filter->blendMode;
        }
#ifdef __cplusplus
}
#endif

} //namespace jni
} //namespace apl
#endif