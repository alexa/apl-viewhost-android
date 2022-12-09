/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */
#include <jni.h>

#include "apl/apl.h"
#include <random>
#include "jniutil.h"

namespace apl {
    namespace jni {

#ifdef __cplusplus
        extern "C" {
#endif

        /**
         * Get the value of a Extension Filter Definition property.
         */
        JNIEXPORT jobject JNICALL
        Java_com_amazon_apl_android_ExtensionFilterDefinition_nGetPropertyValue(JNIEnv *env,
                                                                                jclass type,
                                                                                jlong nativeHandle,
                                                                                jstring name_) {
            const char *name = env->GetStringUTFChars(name_, 0);

            auto efd = get<ExtensionFilterDefinition>(nativeHandle);

            jobject obj = nullptr;

            auto properties = efd->getPropertyMap();
            if (properties.count(name)) {
                auto prop = efd->getPropertyMap().at(name);
                obj = getJObject(env, prop.defaultValue);
            }

            env->ReleaseStringUTFChars(name_, name);

            return obj;
        }


        /**
         * Get the count of properties on the Extension Filter Definition.
         */
        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_ExtensionFilterDefinition_nGetPropertyCount(JNIEnv *env,
                                                                                jclass type,
                                                                                jlong nativeHandle) {
            auto efd = get<ExtensionFilterDefinition>(nativeHandle);
            return static_cast<int>(efd->getPropertyMap().size());
        }

        /**
         * Get the 'URI' of a Extension Filter Definition.
         */
        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_ExtensionFilterDefinition_nGetURI(JNIEnv *env, jclass type,
                                                                      jlong nativeHandle) {

            auto efd = get<ExtensionFilterDefinition>(nativeHandle);
            return env->NewStringUTF(efd->getURI().c_str());
        }

        /**
         * Get the 'name' of a Custom Extension Definition.
         */
        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_ExtensionFilterDefinition_nGetName(JNIEnv *env, jclass type, jlong nativeHandle) {

            auto efd = get<ExtensionFilterDefinition>(nativeHandle);
            return env->NewStringUTF(efd->getName().c_str());
        }

        /**
         * Set a property value for Extension Filter Definition.
         */
        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_ExtensionFilterDefinition_nProperty(JNIEnv *env, jclass type, jlong nativeHandle,
                                                                        jstring name_, jobject defvalue) {

            const char *name = env->GetStringUTFChars(name_, 0);

            auto efd = get<ExtensionFilterDefinition>(nativeHandle);
            auto value = getAPLObject(env, defvalue);

            efd->property(name, value);

            env->ReleaseStringUTFChars(name_, name);
        }

        /**
         * Create a Extension Filter Definition.
         */
        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_ExtensionFilterDefinition_nCreate(JNIEnv *env, jclass type,
                                                                      jstring uri_, jstring name_,
                                                                      jint imageCount_) {

            const char *uri = env->GetStringUTFChars(uri_, 0);
            const char *name = env->GetStringUTFChars(name_, 0);

            auto efd = ExtensionFilterDefinition(uri, name, static_cast<ExtensionFilterDefinition::ImageCount>(imageCount_));
            auto handle = createHandle<ExtensionFilterDefinition>(
                    std::make_shared<ExtensionFilterDefinition>(efd));

            env->ReleaseStringUTFChars(uri_, uri);
            env->ReleaseStringUTFChars(name_, name);

            return handle;
        }

#ifdef __cplusplus
        }
#endif

    } //namespace jni
} //namespace apl