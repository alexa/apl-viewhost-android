/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include <locale>
#include <codecvt>
#include "apl/apl.h"
#include "alexaext/alexaext.h"
#include "jnidocumentsession.h"
#include "jniutil.h"
#include "jnicontent.h"

namespace apl {
    namespace jni {
#ifdef __cplusplus
        extern "C" {
#endif
        using namespace alexaext;

        static jclass EXTENSIONMEDIATOR_CLASS;
        static jmethodID EXTENSIONMEDIATOR_LOADED_CALLBACK;
        static jmethodID EXTENSIONMEDIATOR_IS_EXTENSION_GRANTED;
        static JavaVM *MEDIATOR_VM_REFERENCE;

        jboolean
        extensionmediator_OnLoad(JavaVM *vm, void *reserved) {
            JNIEnv *env;
            if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                return JNI_FALSE;
            }

            MEDIATOR_VM_REFERENCE = vm;

            // method signatures can be obtained with 'javap -s'
            EXTENSIONMEDIATOR_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("com/amazon/apl/android/ExtensionMediator")));

            EXTENSIONMEDIATOR_LOADED_CALLBACK = env->GetMethodID(
                    EXTENSIONMEDIATOR_CLASS,
                    "onExtensionsLoaded",
                    "()V");

            EXTENSIONMEDIATOR_IS_EXTENSION_GRANTED = env->GetMethodID(
                    EXTENSIONMEDIATOR_CLASS,
                    "isExtensionGranted",
                    "(Ljava/lang/String;)Z"
                    );

            if (nullptr == EXTENSIONMEDIATOR_LOADED_CALLBACK) {
                return JNI_FALSE;
            }

            return JNI_TRUE;
        }

        void
        extensionmediator_OnUnload(JavaVM *vm, void *reserved) {
            JNIEnv *env;
            if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                // environment failure, can't proceed.
                return;
            }

            env->DeleteGlobalRef(EXTENSIONMEDIATOR_CLASS);
        }

        class AndroidExtensionMediator : public ExtensionMediator {
        public:
            AndroidExtensionMediator(
                    const alexaext::ExtensionProviderPtr& provider,
                    const alexaext::ExtensionResourceProviderPtr& resourceProvider,
                    const alexaext::ExecutorPtr& messageExecutor,
                    const apl::ExtensionSessionPtr& session,
                    jweak weakInstance) :
                    ExtensionMediator(provider, resourceProvider, messageExecutor, session),
                    mWeakInstance(weakInstance) {}

            ~AndroidExtensionMediator() {
                JNIEnv *env;
                if (MEDIATOR_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                    return;
                }
                finish();
                env->DeleteWeakGlobalRef(mWeakInstance);
            }

            void onExtensionLoaded() {
                JNIEnv *env;
                if (MEDIATOR_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                    return;
                }

                jobject localRef = env->NewLocalRef(mWeakInstance);
                if (!localRef) {
                    return;
                }

                env->CallVoidMethod(localRef, EXTENSIONMEDIATOR_LOADED_CALLBACK);
                env->DeleteLocalRef(localRef);
            }

            bool isExtensionGranted(const std::string &uri) {
                JNIEnv *env;
                if (MEDIATOR_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                    return false;
                }

                jobject localRef = env->NewLocalRef(mWeakInstance);
                if (!localRef) {
                    return false;
                }

                jstring jUri = env->NewStringUTF(uri.c_str());
                auto result = env->CallBooleanMethod(localRef, EXTENSIONMEDIATOR_IS_EXTENSION_GRANTED, jUri);
                env->DeleteLocalRef(localRef);
                return result;

            }

        private:
            jweak mWeakInstance;
        };

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_ExtensionMediator_nCreate(JNIEnv *env, jobject instance,
                                                              jlong providerHandler_,
                                                              jlong resourceProviderHandler_,
                                                              jlong executorHandler_,
                                                              jlong sessionHandler_) {
            auto extensionProvider = get<alexaext::ExtensionProvider>(providerHandler_);
            auto resourceProvider = get<alexaext::ExtensionResourceProvider>(
                    resourceProviderHandler_);
            auto extensionExecutor = get<alexaext::Executor>(executorHandler_);
            auto session = get<AndroidDocumentSession>(sessionHandler_);
            auto extensionMediator_ = std::make_shared<AndroidExtensionMediator>(
                    extensionProvider,
                    resourceProvider,
                    extensionExecutor,
                    session->getExtensionSession(),
                    env->NewWeakGlobalRef(instance));
            return createHandle<ExtensionMediator>(extensionMediator_);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_ExtensionMediator_nInitializeExtensions__JJJ(JNIEnv *env,
                                                                      jclass clazz,
                                                                      jlong mediatorHandler_,
                                                                      jlong rootConfigHandler_,
                                                                      jlong contentHandler_) {
            auto mediator = get<AndroidExtensionMediator>(mediatorHandler_);
            auto rootConfig = get<RootConfig>(rootConfigHandler_);
            auto content = get<Content>(contentHandler_);
            auto weak_self = std::weak_ptr<AndroidExtensionMediator>(mediator);
            mediator->initializeExtensions(rootConfig, content,
                    [weak_self](const std::string& uri,
                            ExtensionMediator::ExtensionGrantResult grant,
                            ExtensionMediator::ExtensionGrantResult deny) {
                        if (auto self = weak_self.lock()) {
                            auto result = self->isExtensionGranted(uri);
                            if (result) grant(uri);
                            else deny(uri);
                        }
            });
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_ExtensionMediator_nInitializeExtensions__JLjava_lang_Object_2J(
                JNIEnv *env, jclass clazz, jlong mediatorHandler_, jobject flags_, jlong contentHandler_) {
            auto mediator = get<AndroidExtensionMediator>(mediatorHandler_);
            auto content = get<Content>(contentHandler_);
            auto weak_self = std::weak_ptr<AndroidExtensionMediator>(mediator);
            auto obj = getAPLObject(env, flags_);
            auto map = obj.isNull() ? *std::make_shared<ObjectMap>() : obj.getMap();
            mediator->initializeExtensions(map, content, [weak_self](const std::string& uri,
                                                                       ExtensionMediator::ExtensionGrantResult grant,
                                                                       ExtensionMediator::ExtensionGrantResult deny) {
                if (auto self = weak_self.lock()) {
                    auto result = self->isExtensionGranted(uri);
                    if (result) grant(uri);
                    else deny(uri);
                }
            });

        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_ExtensionMediator_nLoadExtensions__JJJ(JNIEnv *env, jclass clazz,
                                                                      jlong mediatorHandler_,
                                                                      jlong rootConfigHandler_,
                                                                      jlong contentHandler_) {
            auto mediator = get<AndroidExtensionMediator>(mediatorHandler_);
            auto rootConfig = get<RootConfig>(rootConfigHandler_);
            auto content = get<Content>(contentHandler_);
            auto weak_self = std::weak_ptr<AndroidExtensionMediator>(mediator);
            mediator->loadExtensions(rootConfig, content, [weak_self]() {
                if (auto self = weak_self.lock()) {
                    self->onExtensionLoaded();
                }
            });
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_ExtensionMediator_nLoadExtensions__JLjava_lang_Object_2J(JNIEnv *env,
                                                                                             jclass clazz,
                                                                                             jlong mediatorHandler_,
                                                                                             jobject flags_,
                                                                                             jlong contentHandler_) {
            auto mediator = get<AndroidExtensionMediator>(mediatorHandler_);
            auto content = get<Content>(contentHandler_);
            auto obj = getAPLObject(env, flags_);
            auto map = obj.isNull() ? *std::make_shared<ObjectMap>() : obj.getMap();
            auto weak_self = std::weak_ptr<AndroidExtensionMediator>(mediator);
            mediator->loadExtensions(map, content, [weak_self]() {
                if (auto self = weak_self.lock()) {
                    self->onExtensionLoaded();
                }
            });
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_ExtensionMediator_nEnable(JNIEnv *env, jclass clazz, jlong mediatorHandler_, jboolean enabled) {
            auto mediator = get<AndroidExtensionMediator>(mediatorHandler_);
            mediator->enable(enabled);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_ExtensionMediator_nOnSessionEnded(JNIEnv *env, jclass clazz,
                                                                      jlong mediatorHandler_) {
            auto mediator = get<AndroidExtensionMediator>(mediatorHandler_);
            mediator->onSessionEnded();
        }

#ifdef __cplusplus
        }
#endif
    } //namespace jni
} //namespace apl
