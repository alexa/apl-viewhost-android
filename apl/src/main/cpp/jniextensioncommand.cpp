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
         * Get the value of a Extension Command Definition property.
         */
        JNIEXPORT jobject JNICALL
        Java_com_amazon_apl_android_ExtensionCommandDefinition_nGetPropertyValue(JNIEnv *env,
                                                                                 jclass type,
                                                                                 jlong nativeHandle,
                                                                                 jstring name_) {
            const char *name = env->GetStringUTFChars(name_, 0);

            auto ccd = get<ExtensionCommandDefinition>(nativeHandle);

            jobject obj = nullptr;

            auto properties = ccd->getPropertyMap();
            if (properties.count(name)) {
                auto prop = ccd->getPropertyMap().at(name);
                obj = getJObject(env, prop.defvalue);
            }

            env->ReleaseStringUTFChars(name_, name);

            return obj;
        }


        /**
         * Get the count of properties on the Extension Command Definition.
         */
        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_ExtensionCommandDefinition_nGetPropertyCount(JNIEnv *env,
                                                                                 jclass type,
                                                                                 jlong nativeHandle) {
            auto ccd = get<ExtensionCommandDefinition>(nativeHandle);
            return static_cast<int>(ccd->getPropertyMap().size());
        }


        /**
         * True if a property exists and is required, defaultValue otherwise.
         */
        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_ExtensionCommandDefinition_nIsPropertyRequired(JNIEnv *env, jclass type,
                                                                       jlong nativeHandle, jstring name_) {

            const char *name = env->GetStringUTFChars(name_, 0);

            auto ccd = get<ExtensionCommandDefinition>(nativeHandle);
            auto prop = ccd->getPropertyMap().at(name);
            env->ReleaseStringUTFChars(name_, name);

            return static_cast<jboolean>(prop.required);
        }


        /**
         * Get the 'requireResolution' value of a Extension Command Definition.
         */
        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_ExtensionCommandDefinition_nGetRequireResolution(JNIEnv *env, jclass type,
                                                                           jlong nativeHandle) {

            auto ccd = get<ExtensionCommandDefinition>(nativeHandle);
            return static_cast<jboolean>(ccd->getRequireResolution());

        }


        /**
         * Get the 'allowFastMode' value of a Extension Command Definition.
         */
        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_ExtensionCommandDefinition_nGetAllowFastMode(JNIEnv *env, jclass type,
                                                                       jlong nativeHandle) {

            auto ccd = get<ExtensionCommandDefinition>(nativeHandle);
            return static_cast<jboolean>(ccd->getAllowFastMode());
        }


        /**
         * Get the 'URI' of a Extension Extension Definition.
         */
        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_ExtensionCommandDefinition_nGetURI(JNIEnv *env, jclass type,
                                                                        jlong nativeHandle) {

            auto ccd = get<ExtensionCommandDefinition>(nativeHandle);
            return env->NewStringUTF(ccd->getURI().c_str());
        }

        /**
         * Get the 'name' of a Custom Extension Definition.
         */
        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_ExtensionCommandDefinition_nGetName(JNIEnv *env, jclass type, jlong nativeHandle) {

            auto ccd = get<ExtensionCommandDefinition>(nativeHandle);
            return env->NewStringUTF(ccd->getName().c_str());
        }


        /**
         * Set a property value for Extension Command Definition.
         */
        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_ExtensionCommandDefinition_nProperty(JNIEnv *env, jclass type, jlong nativeHandle,
                                                               jstring name_, jobject defvalue, jboolean required) {

            const char *name = env->GetStringUTFChars(name_, 0);

            auto ccd = get<ExtensionCommandDefinition>(nativeHandle);
            auto value = getAPLObject(env, defvalue);

            ccd->property(name, value, static_cast<bool>(required));

            env->ReleaseStringUTFChars(name_, name);
        }

        /**
         * Set a property value for Extension Command Definition.
        */
        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_ExtensionCommandDefinition_nArrayProperty(JNIEnv *env, jclass type,
                                                                         jlong nativeHandle,
                                                                         jstring name_,
                                                                         jboolean required) {

            const char *name = env->GetStringUTFChars(name_, 0);

            auto ccd = get<ExtensionCommandDefinition>(nativeHandle);

            ccd->arrayProperty(name, static_cast<bool>(required));

            env->ReleaseStringUTFChars(name_, name);
        }


        /**
         * Set Extension Command Definition 'requireResolution' value.
         */
        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_ExtensionCommandDefinition_nRequireResolution(JNIEnv *env, jclass type,
                                                                        jlong nativeHandle, jboolean requireResolution) {
            auto ccd = get<ExtensionCommandDefinition>(nativeHandle);
            ccd->requireResolution(requireResolution);
        }


        /**
         * Set Extension Command Definition 'allowFastMode' value.
         */
        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_ExtensionCommandDefinition_nAllowFastMode(JNIEnv *env, jclass type,
                                                                    jlong nativeHandle, jboolean allowFastMode) {

            auto ccd = get<ExtensionCommandDefinition>(nativeHandle);
            ccd->allowFastMode(allowFastMode);
        }


        /**
         * Create a Extension Command Definition.
         */
        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_ExtensionCommandDefinition_nCreate(JNIEnv *env, jclass type,
            jstring uri_, jstring name_) {

            const char *uri = env->GetStringUTFChars(uri_, 0);
            const char *name = env->GetStringUTFChars(name_, 0);

            auto ccd = ExtensionCommandDefinition(uri, name);
            auto handle = createHandle<ExtensionCommandDefinition>(
                    std::make_shared<ExtensionCommandDefinition>(ccd));

            env->ReleaseStringUTFChars(uri_, uri);
            env->ReleaseStringUTFChars(name_, name);

            return handle;
        }




#ifdef __cplusplus
        }
#endif

    } //namespace jni
} //namespace apl