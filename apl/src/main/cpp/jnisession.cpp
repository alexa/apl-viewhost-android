/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include <string>

#include "apl/apl.h"

#include "jniutil.h"

namespace apl {
    namespace jni {

#ifdef __cplusplus
        extern "C" {
#endif
         JNIEXPORT jlong JNICALL
         Java_com_amazon_apl_android_Session_nCreate(JNIEnv *env, jclass clazz) {
             auto session = makeDefaultSession();
             return createHandle<Session>(session);
         }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_Session_nGetLogId(JNIEnv *env, jclass clazz, jlong _handle) {
            auto session = get<Session>(_handle);
            return env->NewStringUTF(session->getLogId().c_str());
        }

#ifdef __cplusplus
        }
#endif
    }
}