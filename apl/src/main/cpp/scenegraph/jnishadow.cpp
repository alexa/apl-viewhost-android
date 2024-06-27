/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>

#include <jnisgcontent.h>
#include <jnitextlayout.h>
#include "jniutil.h"

#ifdef SCENEGRAPH
namespace apl {
    namespace jni {

#ifdef __cplusplus

        extern "C" {
#endif

#pragma clang diagnostic push
#pragma ide diagnostic ignored "OCUnusedGlobalDeclarationInspection"
        const bool DEBUG_JNI = true;
#pragma clang diagnostic pop

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"

                JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_sgcontent_Shadow_nGetColor(JNIEnv *env,
                                                                             jclass clazz,
                                                                             jlong nativeHandle) {
            apl::sg::Shadow * shadow = reinterpret_cast<apl::sg::Shadow *>(nativeHandle);
            return shadow->getColor().get();
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_sgcontent_Shadow_nGetOffset(JNIEnv *env,
                                                               jclass clazz,
                                                               jlong nativeHandle) {
            apl::sg::Shadow * shadow = reinterpret_cast<apl::sg::Shadow *>(nativeHandle);
            auto point =  shadow->getOffset();
            float pointArray[] = {point.getX(), point.getY()};
            jfloatArray result = env->NewFloatArray(2);
            env->SetFloatArrayRegion(result,0,2, pointArray);
            return result;
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_sgcontent_Shadow_nGetRadius(JNIEnv *env,
                                                               jclass clazz,
                                                               jlong nativeHandle) {
            apl::sg::Shadow * shadow = reinterpret_cast<apl::sg::Shadow *>(nativeHandle);
            return shadow->getRadius();
        }

#pragma clang diagnostic pop

#ifdef __cplusplus
}

#endif

} //namespace jni
} //namespace apl
#endif