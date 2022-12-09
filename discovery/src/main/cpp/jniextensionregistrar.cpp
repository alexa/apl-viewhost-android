/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include <string>
#include <memory>
#include "alexaext/alexaext.h"
#include "jniextensionregistrar.h"
#include "jninativeowner.h"

namespace alexaext {
    namespace jni {

#ifdef __cplusplus

        extern "C" {
#endif
        using namespace alexaext;

        static jclass EXTENSIONREGISTRAR_CLASS;
        static jmethodID EXTENSIONREGISTRAR_CREATE_PROXY;
        static jmethodID EXTENSIONREGISTRAR_HAS_EXTENSION;

        static JavaVM *PROVIDER_VM_REFERENCE;

        jboolean
        extensionprovider_OnLoad(JavaVM *vm, void *reserved) {
            JNIEnv *env;
            if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                return JNI_FALSE;
            }

            PROVIDER_VM_REFERENCE = vm;

            // method signatures can be obtained with 'javap -s'
            EXTENSIONREGISTRAR_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("com/amazon/alexaext/ExtensionRegistrar")));

            EXTENSIONREGISTRAR_CREATE_PROXY = env->GetMethodID(
                    EXTENSIONREGISTRAR_CLASS,
                    "createProxy",
                    "(Ljava/lang/String;)J");

            EXTENSIONREGISTRAR_HAS_EXTENSION = env->GetMethodID(
                    EXTENSIONREGISTRAR_CLASS,
                    "hasExtension",
                    "(Ljava/lang/String;)Z");

            if (nullptr == EXTENSIONREGISTRAR_CREATE_PROXY ||
                nullptr == EXTENSIONREGISTRAR_HAS_EXTENSION) {
                return JNI_FALSE;
            }

            return JNI_TRUE;
        }

        void
        extensionprovider_OnUnload(JavaVM *vm, void *reserved) {
            JNIEnv *env;
            if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                // environment failure, can't proceed.
                return;
            }

            env->DeleteGlobalRef(EXTENSIONREGISTRAR_CLASS);
        }

        class AndroidExtensionProvider : public ExtensionProvider {
        public:
            AndroidExtensionProvider(jweak weakInstance) : mWeakInstance(weakInstance) {}

            ~AndroidExtensionProvider() override {
                JNIEnv *env;
                if (PROVIDER_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                    return;
                }
                env->DeleteWeakGlobalRef(mWeakInstance);
            }

            bool hasExtension(const std::string& uri) override {
                JNIEnv *env;
                if (PROVIDER_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                    return false;
                }

                jobject localRef = env->NewLocalRef(mWeakInstance);
                if (!localRef) {
                    return false;
                }

                bool result;
                jstring jUri = env->NewStringUTF(uri.c_str());
                result = env->CallBooleanMethod(localRef, EXTENSIONREGISTRAR_HAS_EXTENSION, jUri);

                env->DeleteLocalRef(localRef);
                env->DeleteLocalRef(jUri);

                if (env->ExceptionCheck()) {
                    env->ExceptionClear();
                    return false;
                }

                return result;
            }

            ExtensionProxyPtr getExtension(const std::string& uri) override {
                JNIEnv *env;
                if (PROVIDER_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                    return nullptr;
                }

                jobject localRef = env->NewLocalRef(mWeakInstance);
                if (!localRef) {
                    return nullptr;
                }

                jstring jUri = env->NewStringUTF(uri.c_str());
                ExtensionProxyPtr proxyPtr;
                auto proxyHandle = env->CallLongMethod(localRef, EXTENSIONREGISTRAR_CREATE_PROXY, jUri);
                if (proxyHandle > 0) proxyPtr = apl::jni::get<alexaext::ExtensionProxy>(proxyHandle);

                env->DeleteLocalRef(localRef);
                env->DeleteLocalRef(jUri);

                if (env->ExceptionCheck()) {
                    env->ExceptionClear();
                    return nullptr;
                }

                if (proxyPtr && !proxyPtr->isInitialized(uri)){
                    proxyPtr->initializeExtension(uri);
                }

                return proxyPtr;
            }

        private:
            jweak mWeakInstance;
        };

        JNIEXPORT jlong JNICALL
        Java_com_amazon_alexaext_ExtensionRegistrar_nCreate(JNIEnv *env, jobject thiz) {
            auto extensionProvider_ = std::make_shared<AndroidExtensionProvider>(env->NewWeakGlobalRef(thiz));
            return apl::jni::createHandle<AndroidExtensionProvider>(extensionProvider_);
        }

#ifdef __cplusplus
        }
#endif
    } //namespace jni
} //namespace alexaext