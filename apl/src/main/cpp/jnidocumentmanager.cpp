/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include <locale>
#include <codecvt>
#include "jnidocumentmanager.h"
#include "jniembeddeddocumentrequest.h"
#include "jniutil.h"

namespace apl {
    namespace jni {
#ifdef __cplusplus
        extern "C" {
#endif

        static JavaVM *JAVA_VM;
        jclass DOCUMENTMANAGER_CLASS;
        jclass EMBEDEDDOCUMENTREQUESTPROXY_CLASS;
        jmethodID DOCUMENTMANAGER_REQUEST_EMBEDDED_DOCUMENT;
        jmethodID EMBEDEDDOCUMENTREQUESTPROXY_CONSTRUCTOR;
        /**
         * Create a class and method cache for calls to View Host.
        */
        jboolean documentmanager_OnLoad(JavaVM *vm, void *reserved) {
            LOG(apl::LogLevel::kDebug) << "Loading View Host Document manager JNI environment.";

            JNIEnv *env;
            if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return JNI_FALSE;
            }
            JAVA_VM = vm;
            DOCUMENTMANAGER_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("com/amazon/apl/viewhost/internal/DocumentManager")));
            EMBEDEDDOCUMENTREQUESTPROXY_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("com/amazon/apl/viewhost/internal/EmbeddedDocumentRequestProxy")));
            DOCUMENTMANAGER_REQUEST_EMBEDDED_DOCUMENT = env->GetMethodID(DOCUMENTMANAGER_CLASS,
                                                                         "requestEmbeddedDocument",
                                                                         "(Lcom/amazon/apl/viewhost/internal/EmbeddedDocumentRequestProxy;)V");
            EMBEDEDDOCUMENTREQUESTPROXY_CONSTRUCTOR = env->GetMethodID(EMBEDEDDOCUMENTREQUESTPROXY_CLASS, "<init>",
                                                                       "(J)V");
            if (nullptr == DOCUMENTMANAGER_REQUEST_EMBEDDED_DOCUMENT) {
                return JNI_FALSE;
            }

            return JNI_TRUE;
        }

        /**
         * Release the class and method cache.
         */
        void documentmanager_OnUnload(JavaVM *vm, void *reserved) {
            LOG(apl::LogLevel::kDebug) << "Unloading View Host Document manager JNI environment.";
            apl::LoggerFactory::instance().reset();

            JNIEnv *env;
            if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }
            env->DeleteGlobalRef(DOCUMENTMANAGER_CLASS);
            env->DeleteGlobalRef(EMBEDEDDOCUMENTREQUESTPROXY_CLASS);
        }

        void
        AndroidDocumentManager::request(const std::weak_ptr<EmbedRequest>& request,
                     EmbedRequestSuccessCallback success,
                     EmbedRequestFailureCallback error) {
            LOG(apl::LogLevel::kDebug) << "Host Component Request in View Host Document manager JNI.";

            JNIEnv *env;
            if (JAVA_VM->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            if (auto strongRequest = request.lock()) {
                auto androidEmbeddedRequest = std::make_shared<AndroidEmbeddedDocumentRequest>();
                androidEmbeddedRequest->mEmbedRequest = strongRequest;
                androidEmbeddedRequest->mSuccessCallback = std::move(success);
                androidEmbeddedRequest->mFailureCallback = std::move(error);
                auto requestHandler = createHandle<AndroidEmbeddedDocumentRequest>(androidEmbeddedRequest);

                auto requestProxyObject = env->NewObject(EMBEDEDDOCUMENTREQUESTPROXY_CLASS, EMBEDEDDOCUMENTREQUESTPROXY_CONSTRUCTOR,
                                                      requestHandler);

                std::string url = strongRequest.get()->getUrlRequest().getUrl();
                LOG(apl::LogLevel::kInfo) << "Fulfilling embed request: " + url;
                env->CallVoidMethod(mInstance, DOCUMENTMANAGER_REQUEST_EMBEDDED_DOCUMENT,
                                    requestProxyObject);
                env->DeleteLocalRef(requestProxyObject);
            }
        }

        void
        AndroidDocumentManager::setInstance(jobject instance) {
            JNIEnv *env;
            if (JAVA_VM->GetEnv(reinterpret_cast<void **>(&env),
                                JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }
            mInstance = env->NewWeakGlobalRef(instance);
        }

        AndroidDocumentManager::~AndroidDocumentManager() {
            JNIEnv *env;
            if (JAVA_VM->GetEnv(reinterpret_cast<void **>(&env),
                                JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }
            env->DeleteWeakGlobalRef(mInstance);
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_viewhost_internal_DocumentManager_nCreate(JNIEnv *env, jobject instance) {
            auto documentManager_ = std::make_shared<AndroidDocumentManager>();
            documentManager_->setInstance(instance);
            return createHandle<AndroidDocumentManager>(documentManager_);
        }

#ifdef __cplusplus
        }
#endif
    } //namespace jni
} //namespace apl
