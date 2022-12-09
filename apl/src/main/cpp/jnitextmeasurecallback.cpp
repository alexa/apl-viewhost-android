/*
 * Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include <string>
#include <cmath>
#include "apl/apl.h"

#include "jniutil.h"
#include "jnitextmeasurecallback.h"

namespace apl {
    namespace jni {

#ifdef __cplusplus
        extern "C" {
#endif
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"

        // Access APL view host TextMeasure class.
        static jclass TEXTMEASURECALLBACK_CLASS;
        static jmethodID TEXTMEASURECALLBACK_MEASURE;
        static JavaVM* JAVA_VM;

        const static float FLOAT_MAX = std::numeric_limits<float>::max();

        /**
         * Initialize and cache java class and method handles for callback to the rendering layer.
         * Called from JNI_OnLoad when the native library is loaded (for example, through System.loadLibrary).
         */
        jboolean
        textmeasurecallback_OnLoad(JavaVM *vm, void *reserved) {


            LOG(apl::LogLevel::kDebug) << "Loading View Host textmeasure JNI environment.";

            JAVA_VM = vm;
            JNIEnv *env;
            if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                return JNI_FALSE;
            }

            // method signatures can be obtained with 'javap -s'
            TEXTMEASURECALLBACK_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("com/amazon/apl/android/TextMeasureCallback")));
            TEXTMEASURECALLBACK_MEASURE = env->GetMethodID(TEXTMEASURECALLBACK_CLASS, "callbackMeasure",
                                                           "(Ljava/lang/String;IFIFI)[F");

            if (nullptr == TEXTMEASURECALLBACK_MEASURE) {
                LOG(apl::LogLevel::kError)
                        << "Could not load methods for class com.amazon.apl.android.TextMeasure";
                return JNI_FALSE;
            }

            return JNI_TRUE;
        }


        /**
         * Release the class and method cache.
         * Called from JNI_OnUnload when the class loader containing the native library is garbage collected.
         */
        void
        textmeasurecallback_OnUnload(JavaVM *vm, void *reserved) {

            JNIEnv *env;
            if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                // environment failure, can't proceed.
                return;
            }

            JAVA_VM = nullptr;
            env->DeleteGlobalRef(TEXTMEASURECALLBACK_CLASS);
        }

        void JniTextMeasurement::setInstance(jobject instance) {
            JNIEnv *env;
            if (JAVA_VM->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                // environment failure, can't proceed.
                return;
            }

            if (mInstance != nullptr && !env->IsSameObject(mInstance, nullptr)) {
                env->DeleteWeakGlobalRef(mInstance);
            }
            mInstance = env->NewWeakGlobalRef(instance);
        }

        /**
         * Create a TextMeasure and attach it to the view host peer.
         */
        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_TextMeasureCallback_nCreate(JNIEnv *env,
                jobject instance) {
            auto measure = std::make_shared<JniTextMeasurement>();

            // Set up TextMeasure for JNI use as a native object
            // the environment calls back to 'instance'
            // the measurement values are then fetched with property lookup
            measure->setInstance(instance);
            auto owner = new NativeOwner<JniTextMeasurement>(measure);
            owner->lookup = measure;

            return owner->instance();
        }

        /**
         * Create a binding to a previously configured TextMeasure and attach it to the view host peer.
         */
        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_TextMeasureCallback_nCreateHandle(JNIEnv *env, jobject instance,
                                                                jlong rootConfigHandle) {

            auto config = get<RootConfig>(rootConfigHandle);
            auto measure = std::static_pointer_cast<JniTextMeasurement>(config->getMeasure());

            // Set up TextMeasure for JNI use as a native object
            // the environment calls back to 'instance'
            // the measurement values are then fetched with property lookup
            measure->setInstance(instance);
            auto owner = new NativeOwner<JniTextMeasurement>(measure);
            owner->lookup = measure;

            return owner->instance();
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_TextMeasureCallback_nGetNativeAddress(JNIEnv *env, jclass clazz,
                                                                       jlong nativeHandle) {

            auto measure = get<JniTextMeasurement>(nativeHandle);
            return reinterpret_cast<jlong>(measure.get());
        }

        JniTextMeasurement::~JniTextMeasurement() {
            mCurrentComponent = nullptr;
            JNIEnv *env;
            if (JAVA_VM->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                // environment failure, can't proceed.
                return;
            }
            env->DeleteWeakGlobalRef(mInstance);
        }

        LayoutSize JniTextMeasurement::measure(Component *textPtr,
                           float width,
                           MeasureMode widthMode,
                           float height,
                           MeasureMode heightMode) {
            JNIEnv *env;
            if (JAVA_VM->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                // environment failure, can't proceed.
                LOG(apl::LogLevel::kError)
                        << "Attempt to measure with no env.";
                return {.width = 0, .height = 0};
            }

            if (env->IsSameObject(mInstance, NULL)) {
                LOG(apl::LogLevel::kError)
                        << "Attempt to measure after Textmeasure is finished.";
                return {.width = 0, .height = 0};
            }
            mCurrentComponent = textPtr;

            auto visualHash = mCurrentComponent->getCalculated(kPropertyVisualHash).asString();
            auto jVisualHash = env->NewStringUTF(visualHash.c_str());
            auto measureResult = (jfloatArray) env->CallObjectMethod(mInstance,
                                                                      TEXTMEASURECALLBACK_MEASURE,
                                                                      jVisualHash,
                                                                      static_cast<jint>(mCurrentComponent->getType()),
                                                                      static_cast<jfloat>(std::isnan(
                                                                              width)
                                                                                          ? FLOAT_MAX
                                                                                          :
                                                                                          width),
                                                                      static_cast<jint>(widthMode),
                                                                      static_cast<jfloat>(std::isnan(
                                                                              height)
                                                                                          ? FLOAT_MAX
                                                                                          :
                                                                                          height),
                                                                      static_cast<jint>(heightMode));
            mCurrentComponent = nullptr;
            env->DeleteLocalRef(jVisualHash);
            auto array = env->GetFloatArrayElements(measureResult, 0);
            auto measureWidth = static_cast<float>(array[0]);
            auto measureHeight = static_cast<float>(array[1]);
            env->ReleaseFloatArrayElements(measureResult, array, 0);
            env->DeleteLocalRef(measureResult);
            return { measureWidth, measureHeight };
        }

        float JniTextMeasurement::baseline(Component *component,
                               float width,
                               float height) {
            return height * 0.5;
        }

        apl::Object JniTextMeasurement::getObject(int propertyId, jlong handle)  {
            // handle references "this", the bound target is the current component
            auto prop = static_cast<apl::PropertyKey>(propertyId);
            return mCurrentComponent->getCalculated().get(prop);
        }




#pragma clang diagnostic pop

#ifdef __cplusplus
        }
#endif

    } //namespace jni
} //namespace apl


