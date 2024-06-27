/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>

#include <jnisgcontent.h>
#include <jnitextlayout.h>
#include "jniutil.h"

#ifdef SCENEGRAPH
namespace apl {
    namespace jni {

#ifdef __cplusplus

        jfloatArray convertPointToJFloatArray(JNIEnv *pEnv, Point point);

        extern "C" {
#endif

#pragma clang diagnostic push
#pragma ide diagnostic ignored "OCUnusedGlobalDeclarationInspection"
        const bool DEBUG_JNI = true;
#pragma clang diagnostic pop

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"

        /**
         * Create a class and method cache for calls to View Host.
         */
        jboolean
        paint_OnLoad(JavaVM *vm, void *reserved) {
        }

        /**
         * Release the class and method cache.
         */
        void
        paint_OnUnload(JavaVM *vm, void *reserved) {
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_sgcontent_Paint_nGetType(JNIEnv *env,
                                                             jclass clazz,
                                                             jlong address) {
            apl::sg::Paint * paint = reinterpret_cast<apl::sg::Paint *>(address);
            char* paintType = "Unknown";
            switch (paint->type()) {
                case sg::Paint::Type::kColor:
                    paintType = "Color";
                    break;
                case sg::Paint::Type::kPattern:
                    paintType = "Pattern";
                    break;
                case sg::Paint::Type::kLinearGradient:
                    paintType = "LinearGradient";
                    break;
                case sg::Paint::Type::kRadialGradient:
                    paintType = "RadialGradient";
                    break;
            }
            return env->NewStringUTF(paintType);
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_sgcontent_Paint_nGetTransform(JNIEnv *env,
                                                                  jclass clazz,
                                                                  jlong coreLayerHandle) {
            auto paint = reinterpret_cast<apl::sg::Paint *>(coreLayerHandle);
            auto transform = paint->getTransform().get();
            jfloatArray jTransform = env->NewFloatArray(6);
            env->SetFloatArrayRegion(jTransform, 0, 6, transform.data());
            return jTransform;
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_sgcontent_Paint_nGetColor(JNIEnv *env,
                                                              jclass clazz,
                                                              jlong address) {
            apl::sg::ColorPaint * paint = reinterpret_cast<apl::sg::ColorPaint *>(address);
            return static_cast<jint>(paint->getColor().get());
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_sgcontent_Paint_nGetOpacity(JNIEnv *env,
                                                              jclass clazz,
                                                              jlong address) {
            apl::sg::Paint * paint = reinterpret_cast<apl::sg::Paint *>(address);
            return static_cast<jfloat>(paint->getOpacity());
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_sgcontent_Paint_nPatternGetSize(JNIEnv *env,
                                                                jclass clazz,
                                                                jlong address) {
            apl::sg::PatternPaint * paint = reinterpret_cast<apl::sg::PatternPaint *>(address);
            float size[] = {paint->getSize().getWidth(), paint->getSize().getHeight()};
            jfloatArray result = env->NewFloatArray(2);
            env->SetFloatArrayRegion(result,0,2, size);
            return result;
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_sgcontent_Paint_nPatternGetNode(JNIEnv *env,
                                                                    jclass clazz,
                                                                    jlong address) {
            apl::sg::PatternPaint * paint = reinterpret_cast<apl::sg::PatternPaint *>(address);
            return reinterpret_cast<jlong>(paint->getNode().get());
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_sgcontent_Paint_nGradientGetPoints(JNIEnv *env,
                                                                    jclass clazz,
                                                                    jlong address) {
            apl::sg::GradientPaint * paint = reinterpret_cast<apl::sg::GradientPaint *>(address);
            auto points = paint->getPoints();
            int pointsSize = points.size();
            std::vector<jfloat> floatPoints(points.begin(), points.end());
            jfloatArray result = env->NewFloatArray(pointsSize);
            env->SetFloatArrayRegion(result, 0, pointsSize, floatPoints.data());
            return result;
        }

        JNIEXPORT jintArray JNICALL
        Java_com_amazon_apl_android_sgcontent_Paint_nGradientGetColors(JNIEnv *env,
                                                                       jclass clazz,
                                                                       jlong address) {
            apl::sg::GradientPaint * paint = reinterpret_cast<apl::sg::GradientPaint *>(address);
            auto colors = paint->getColors();
            int colorsSize = colors.size();
            jint colorsArray[colorsSize];
            for (int i = 0; i < colorsSize; i++) {
                colorsArray[i] = static_cast<jint>(colors[i].get());
            }
            jintArray result = env->NewIntArray(colorsSize);
            env->SetIntArrayRegion(result, 0, colorsSize, colorsArray);
            return result;
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_sgcontent_Paint_nGradientGetSpreadMethod(JNIEnv *env,
                                                                       jclass clazz,
                                                                       jlong address) {
            apl::sg::GradientPaint * paint = reinterpret_cast<apl::sg::GradientPaint *>(address);
            return paint->getSpreadMethod();
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_sgcontent_Paint_nGradientGetUseBoundingBox(JNIEnv *env,
                                                                             jclass clazz,
                                                                             jlong address) {
            apl::sg::GradientPaint * paint = reinterpret_cast<apl::sg::GradientPaint *>(address);
            return paint->getUseBoundingBox();
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_sgcontent_Paint_nLinearGradientGetStart(JNIEnv *env,
                                                                               jclass clazz,
                                                                               jlong address) {
            apl::sg::LinearGradientPaint * paint = reinterpret_cast<apl::sg::LinearGradientPaint *>(address);
            return convertPointToJFloatArray(env, paint->getStart());
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_sgcontent_Paint_nLinearGradientGetEnd(JNIEnv *env,
                                                                            jclass clazz,
                                                                            jlong address) {
            apl::sg::LinearGradientPaint * paint = reinterpret_cast<apl::sg::LinearGradientPaint *>(address);
            return convertPointToJFloatArray(env, paint->getEnd());
        }

        JNIEXPORT jfloatArray JNICALL
        Java_com_amazon_apl_android_sgcontent_Paint_nRadialGradientGetCenter(JNIEnv *env,
                                                                          jclass clazz,
                                                                          jlong address) {
            apl::sg::RadialGradientPaint * paint = reinterpret_cast<apl::sg::RadialGradientPaint *>(address);
            return convertPointToJFloatArray(env, paint->getCenter());
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_sgcontent_Paint_nRadialGradientGetRadius(JNIEnv *env,
                                                                             jclass clazz,
                                                                             jlong address) {
            apl::sg::RadialGradientPaint * paint = reinterpret_cast<apl::sg::RadialGradientPaint *>(address);
            return paint->getRadius();
        }

        int hashPoint(int h, Point point) {
            unsigned x, y;
            auto xf = point.getX();
            auto yf = point.getY();
            memcpy(&x, &(xf), 4);
            memcpy(&y, &(yf), 4);
            h ^= x;
            h *= 1000003;
            h ^ y;
            h * 1000003;
            return h;
        }

        int hashSize(int h, Size size) {
            unsigned w, he;
            auto wf = size.getWidth();
            auto hf = size.getHeight();
            memcpy(&w, &(wf), 4);
            memcpy(&he, &(hf), 4);
            h ^= w;
            h *= 1000003;
            h ^ he;
            h * 1000003;
            return h;
        }

        int hashGradientProperties(int h, apl::sg::GradientPaint * gradientPaint) {
            h ^= gradientPaint->getSpreadMethod();
            h *= 1000003;
            h ^= gradientPaint->getUseBoundingBox() ? 1 : 0;
            h *= 1000003;
            for (auto iter = gradientPaint->getColors().begin(); iter != gradientPaint->getColors().end(); iter++) {
                h ^= iter->get();
                h *= 1000003;
            }
            for (auto iter = gradientPaint->getPoints().begin(); iter != gradientPaint->getPoints().end(); iter++) {
                h ^= *((long long*)(&(*iter)));
                h *= 1000003;
            }
            return h;
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_sgcontent_Paint_nGetHashCode(JNIEnv *env,
                                                                             jclass clazz,
                                                                             jlong address) {
            apl::sg::Paint * paint = reinterpret_cast<apl::sg::Paint *>(address);
            int h = 1000003;
            unsigned o;
            auto of = paint->getOpacity();
            memcpy(&o, &(of), 4);
            h ^= 0;
            h *= 1000003;

            auto transform = paint->getTransform().get();
            for (int i =0; i < 6; i++) {
                unsigned t;
                auto tf = transform[i];
                memcpy(&t, &(tf), 4);
                h ^= t;
                h *= 1000003;
            }

            switch(paint->type()) {
                case sg::ColorPaint::kColor: {
                    apl::sg::ColorPaint * colorPaint = reinterpret_cast<apl::sg::ColorPaint *>(address);
                    h ^= colorPaint->getColor().get();
                    h *= 1000003;
                    break;
                }
                case sg::GradientPaint::kLinearGradient: {
                    apl::sg::LinearGradientPaint * gradientPaint = reinterpret_cast<apl::sg::LinearGradientPaint *>(address);
                    hashGradientProperties(h, gradientPaint);
                    h = hashPoint(h, gradientPaint->getStart());
                    h = hashPoint(h, gradientPaint->getEnd());
                    break;
                }
                case sg::GradientPaint::kRadialGradient: {
                    apl::sg::RadialGradientPaint * gradientPaint = reinterpret_cast<apl::sg::RadialGradientPaint *>(address);
                    hashGradientProperties(h, gradientPaint);
                    h = hashPoint(h, gradientPaint->getCenter());
                    unsigned r;
                    auto rf = gradientPaint->getRadius();
                    memcpy(&r, &(rf), 4);
                    h ^= r;
                    h *= 1000003;
                    break;
                }
                case sg::PatternPaint::kPattern: {
                    apl::sg::PatternPaint * patternPaint = reinterpret_cast<apl::sg::PatternPaint *>(address);
                    h = hashSize(h, patternPaint->getSize());
                    // TODO: how to handle nodes?
                    h ^= reinterpret_cast<long>(patternPaint->getNode().get());
                    h *= 1000003;
                }
            }
            return h;
        }

#pragma clang diagnostic pop

#ifdef __cplusplus
}

        jfloatArray convertPointToJFloatArray(JNIEnv *env, Point point) {
            float pointArray[] = {point.getX(), point.getY()};
            jfloatArray result = env->NewFloatArray(2);
            env->SetFloatArrayRegion(result,0,2, pointArray);
            return result;
        }

#endif

} //namespace jni
} //namespace apl
#endif