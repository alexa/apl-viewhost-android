/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */


#include <jni.h>
#include "jniutil.h"

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
        Java_com_amazon_apl_android_scenegraph_accessibility_Accessibility_nGetRole(JNIEnv *env,
                                                                                    jclass clazz,
                                                                                    jlong address) {
            auto accessibility = reinterpret_cast<apl::sg::Accessibility *>(address);
            if (accessibility) {
                return static_cast<jint>(accessibility->getRole());
            }
            return static_cast<jint>(kRoleNone);
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_scenegraph_accessibility_Accessibility_nGetAccessibilityLabel(JNIEnv *env,
                                                                                                  jclass clazz,
                                                                                                  jlong address) {
            auto accessibility = reinterpret_cast<apl::sg::Accessibility *>(address);
            if (accessibility) {
                return env->NewStringUTF(accessibility->getLabel().c_str());
            }
            return NULL;
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_scenegraph_accessibility_Accessibility_nGetActionsSize(JNIEnv *env,
                                                                                           jclass clazz,
                                                                                           jlong address) {
            auto accessibility = reinterpret_cast<apl::sg::Accessibility *>(address);
            if (accessibility) {
                return static_cast<jint>(accessibility->actions().size());
            }
            return 0;
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_scenegraph_accessibility_Accessibility_nGetActionLabelAt(JNIEnv *env,
                                                                                             jclass clazz,
                                                                                             jlong address,
                                                                                             jint index) {
            auto accessibility = reinterpret_cast<apl::sg::Accessibility *>(address);
            auto action = accessibility->actions().at(index);
            return env->NewStringUTF(action.label.c_str());
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_scenegraph_accessibility_Accessibility_nGetActionNameAt(JNIEnv *env,
                                                                                            jclass clazz,
                                                                                            jlong address,
                                                                                            jint index) {
            auto accessibility = reinterpret_cast<apl::sg::Accessibility *>(address);
            auto action = accessibility->actions().at(index);
            return env->NewStringUTF(action.name.c_str());
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_scenegraph_accessibility_Accessibility_nExecuteCallback(JNIEnv *env,
                                                                                            jclass clazz,
                                                                                            jlong address,
                                                                                            jbyteArray value) {
            auto accessibility = reinterpret_cast<apl::sg::Accessibility *>(address);
            jbyte * expression = env->GetByteArrayElements(value, nullptr);
            accessibility->executeCallback(reinterpret_cast<char *>(expression));
            env->ReleaseByteArrayElements(value, expression, 0);
        }

#pragma clang diagnostic pop
#ifdef __cplusplus
        }
#endif
    } //namespace jni
} //namespace apl
