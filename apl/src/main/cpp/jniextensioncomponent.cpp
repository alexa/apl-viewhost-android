/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include <string>
#include "apl/apl.h"
#include <jniutil.h>
#include "apl/extension/extensioncomponent.h"

namespace apl {
    namespace jni {

#ifdef __cplusplus
        extern "C" {
#endif
        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_ExtensionComponent_nGetUri(JNIEnv *env, jclass clazz,
                                                               jlong handle) {
            auto c = get<ExtensionComponent>(handle);
            return env->NewStringUTF(c->getUri().c_str());
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_ExtensionComponent_nUpdateExtensionResourceState(JNIEnv *env,
                                                                                     jclass clazz,
                                                                                     jlong handle,
                                                                                     jint state) {
            auto c = get<ExtensionComponent>(handle);
            c->updateResourceState(static_cast<ExtensionComponentResourceState>(state));
        }

#ifdef __cplusplus
        }
#endif
    } // namespace jni
} // namespace apl

