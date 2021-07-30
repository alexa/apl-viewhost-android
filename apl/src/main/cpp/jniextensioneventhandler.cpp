/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */
#include <jni.h>

#include "apl/apl.h"
#include <random>
#include "jniutil.h"
#include "loggingbridge.h"


namespace apl {
    namespace jni {

#ifdef __cplusplus
        extern "C" {
#endif


        /**
         * Get the 'URI' of an extension event handler.
         */
        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_ExtensionEventHandler_nGetURI(JNIEnv *env, jclass type,
                                                                  jlong nativeHandle) {

            auto eeh = get<ExtensionEventHandler>(nativeHandle);
            return env->NewStringUTF(eeh->getURI().c_str());
        }

        /**
         * Get the 'name' of an extension event handler
         */
        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_ExtensionEventHandler_nGetName(JNIEnv *env, jclass type,
                                                                   jlong nativeHandle) {

            auto eeh = get<ExtensionEventHandler>(nativeHandle);
            return env->NewStringUTF(eeh->getName().c_str());
        }


        /**
         * Create a extension event handler.
         */
        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_ExtensionEventHandler_nCreate(JNIEnv *env, jclass type,
                                                                  jstring uri_, jstring name_) {

            const char *uri = env->GetStringUTFChars(uri_, 0);
            const char *name = env->GetStringUTFChars(name_, 0);

            auto eeh = ExtensionEventHandler(uri, name);
            auto handle = createHandle<ExtensionEventHandler>(
                    std::make_shared<ExtensionEventHandler>(eeh));

            env->ReleaseStringUTFChars(uri_, uri);
            env->ReleaseStringUTFChars(name_, name);

            return handle;
        }


#ifdef __cplusplus
        }
#endif

    } //namespace jni
} //namespace apl