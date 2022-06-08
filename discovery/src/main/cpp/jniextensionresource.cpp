/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include "alexaext/alexaext.h"
#include "jniextensionresource.h"
#include "jninativeowner.h"

namespace alexaext {
    namespace jni {

#ifdef __cplusplus

        extern "C" {
#endif
        using namespace alexaext;

        static jclass EXTENSIONRESOURCEPROVIDER_CLASS;
        static jmethodID EXTENSIONRESOURCEPROVIDER_CALLBACK;
        static JavaVM *RESOURCE_VM_REFERENCE;

        jboolean
        extensionresource_OnLoad(JavaVM *vm, void *reserved) {
            JNIEnv *env;
            if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                return JNI_FALSE;
            }

            RESOURCE_VM_REFERENCE = vm;

            // method signatures can be obtained with 'javap -s'
            EXTENSIONRESOURCEPROVIDER_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("com/amazon/alexaext/ExtensionResourceProvider")));

            EXTENSIONRESOURCEPROVIDER_CALLBACK = env->GetMethodID(
                    EXTENSIONRESOURCEPROVIDER_CLASS, "requestResource", "(Ljava/lang/String;)J");

            return JNI_TRUE;
        }

        void
        extensionresource_OnUnload(JavaVM *vm, void *reserved) {
            JNIEnv *env;
            if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                // environment failure, can't proceed.
                return;
            }

            env->DeleteGlobalRef(EXTENSIONRESOURCEPROVIDER_CLASS);
        }

        class AndroidExtensionResourceProvider : public ExtensionResourceProvider {
        public:
            AndroidExtensionResourceProvider(jweak weakInstance) :
                    ExtensionResourceProvider(),
                    mWeakInstance(weakInstance) {}

            ~AndroidExtensionResourceProvider() {
                JNIEnv *env;
                if (RESOURCE_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                    return;
                }
                env->DeleteWeakGlobalRef(mWeakInstance);
            }

            bool requestResource(const std::string &uri, const std::string &resourceId,
                                         ExtensionResourceSuccessCallback success,
                                         ExtensionResourceFailureCallback error) override {

                JNIEnv *env;
                if (RESOURCE_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                    return false;
                }

                jobject localRef = env->NewLocalRef(mWeakInstance);
                if (!localRef) {
                    return false;
                }

                // fetch the resource holder
                jstring jResourceId = env->NewStringUTF(resourceId.c_str());
                jlong jholder = env->CallLongMethod(localRef, EXTENSIONRESOURCEPROVIDER_CALLBACK, jResourceId);
                env->DeleteLocalRef(localRef);
                env->DeleteLocalRef(jResourceId);

                if (env->ExceptionCheck()) {
                    env->ExceptionDescribe();
                    env->ExceptionClear();
                    return false;
                }

                // respond with resource
                auto holder = apl::jni::get<ResourceHolder>(jholder);
                if (holder) {
                    success(uri, holder);
                } else {
                    error (uri, resourceId, 100, "resource missing");
                }

                return true;
            }

        private:
            jweak mWeakInstance;
        };


        JNIResourceHolder::JNIResourceHolder(std::string resourceId, jweak instance)
                : ResourceHolder(std::move(resourceId)), mWeakInstance(instance) {
        }

        JNIResourceHolder::~JNIResourceHolder() {
            JNIEnv *env;
            if (RESOURCE_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
                                              JNI_VERSION_1_6) != JNI_OK) {
                return;
            }
            env->DeleteWeakGlobalRef(mWeakInstance);
        }



        JNIEXPORT jlong JNICALL
        Java_com_amazon_alexaext_ExtensionResourceProvider_nCreate(JNIEnv *env, jobject instance) {
            auto provider = std::make_shared<AndroidExtensionResourceProvider>(
                    env->NewWeakGlobalRef(instance));
            return apl::jni::createHandle<AndroidExtensionResourceProvider>(provider);
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_alexaext_ResourceHolder_nCreate(JNIEnv *env, jobject instance,
                                                           jstring resourceId_) {
            const char* resourceId =  env->GetStringUTFChars(resourceId_, nullptr);
            auto holder = std::make_shared<JNIResourceHolder>(
                   resourceId,
                   env->NewWeakGlobalRef(instance));
            env->ReleaseStringUTFChars(resourceId_, resourceId);
            return apl::jni::createHandle<ResourceHolder>(holder);
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_alexaext_ResourceHolder_nResourceId(JNIEnv *env, jclass clazz,
                                                               jlong handler_) {
            auto holder = apl::jni::get<JNIResourceHolder>(handler_);
            return env->NewStringUTF(holder->resourceId().c_str());
        }

#ifdef __cplusplus
        }
#endif
    } //namespace jni
} //namespace alexaext