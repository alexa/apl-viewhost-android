/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include <string>

#include "scaling.h"
#include "jniscaling.h"
#include "jniutil.h"
#include "apl/apl.h"

namespace apl {
    namespace jni {

#ifdef __cplusplus
        extern "C" {
#endif

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"

#pragma clang diagnostic push
#pragma ide diagnostic ignored "OCUnusedGlobalDeclarationInspection"
        const bool DEBUG_JNI = true;
#pragma clang diagnostic pop

        /**
         * Create a class and method cache for calls to View Host.
         */
        jboolean
        jniscaling_OnLoad(JavaVM *vm, void *reserved) {

            LOG(apl::LogLevel::kDebug) << "Loading View Host Scaling JNI environment.";

            JNIEnv *env;

            if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                return JNI_FALSE;
            }

            return JNI_TRUE;
        }

        /**
         * Release the class and method cache.
         */
        void
        jniscaling_OnUnload(JavaVM *vm, void *reserved) {
            LOG(apl::LogLevel::kDebug) << "Unloading View Host Scaling JNI environment.";
            apl::LoggerFactory::instance().reset();

            JNIEnv *env;
            if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                // environment failure, can't proceed.
                return;
            }
        }


        /**
        * Create a Metrics instance and attach it to the java peer.
        */
        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_scaling_Scaling_nScalingCreate(JNIEnv *env, jclass clazz,
                                                           jdouble biasConstant) {
            auto scaling = std::make_shared<apl::jni::Scaling>(static_cast<double>(biasConstant));
            return createHandle<apl::jni::Scaling>(scaling);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_scaling_Scaling_nAddViewportSpecification(JNIEnv *env, jclass clazz,
                                                                      jlong nativeHandle,
                                                                      jint wmin,
                                                                      jint wmax,
                                                                      jint hmin,
                                                                      jint hmax,
                                                                      jboolean isRound,
                                                                      jint mode) {
            auto scaling = get<apl::jni::Scaling>(nativeHandle);
            scaling->addViewportSpecification(
                    ViewportSpecification(static_cast<double>(wmin),
                                          static_cast<double>(wmax),
                                          static_cast<double>(hmin),
                                          static_cast<double>(hmax),
                                          static_cast<ViewportMode>(mode),
                                          static_cast<bool>(isRound)));
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_scaling_Scaling_nRemoveChosenViewportSpecification(JNIEnv *env, jclass clazz,
                                                                         jlong nativeHandle,
                                                                         jlong metricsTransformHandle) {
            auto metricsTransform = get<MetricsTransform>(metricsTransformHandle);
            auto scaling = get<apl::jni::Scaling>(nativeHandle);
            auto it = scaling->specifications.begin();
            for(; it != scaling->specifications.end(); ++it) {
                if(*it == metricsTransform->getChosenSpec()) {
                    scaling->specifications.erase(it);
                    return static_cast<jboolean>(true);
                }
            }
            return static_cast<jboolean>(false);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_scaling_Scaling_nAddAllowMode(JNIEnv *env, jclass clazz,
                                                                                       jlong nativeHandle,
                                                                                       jint mode) {
            auto scaling = get<apl::jni::Scaling>(nativeHandle);
            scaling->addAllowMode(static_cast<ViewportMode>(mode));
        }

#pragma clang diagnostic pop

#ifdef __cplusplus
        }
#endif

    } //namespace jni
} //namespace apl
