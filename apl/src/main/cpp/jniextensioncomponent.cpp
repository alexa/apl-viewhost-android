/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include <string>
#include "apl/apl.h"
#include <jniutil.h>

namespace apl {
    namespace jni {

#ifdef __cplusplus
        extern "C" {
#endif
        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_ExtensionComponent_nUpdateExtensionResourceState(JNIEnv *env,
                                                                                     jclass clazz,
                                                                                     jlong handle,
                                                                                     jint state) {
            auto c = get<Component>(handle);
            c->updateResourceState(static_cast<ExtensionComponentResourceState>(state));
        }

#ifdef __cplusplus
        }
#endif
    } // namespace jni
} // namespace apl

