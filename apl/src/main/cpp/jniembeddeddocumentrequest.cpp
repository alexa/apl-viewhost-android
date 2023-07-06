/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include <locale>
#include "apl/dynamicdata.h"
#include "jniembeddeddocumentrequest.h"
#include "jniutil.h"
#include "jninativeowner.h"

namespace apl {
    namespace jni {

        DataSourceProviderPtr findDataSourceProviderByType(DocumentConfigPtr documentConfig, std::string type) {
            for (const auto& provider : documentConfig->getDataSourceProviders()) {
                if (provider->getType() == type) {
                    return provider;
                }
            }
            return nullptr;
        }

#ifdef __cplusplus
        extern "C" {
#endif

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_viewhost_internal_DocumentHandleImpl_nUpdateDataSource(JNIEnv *env, jclass clazz,
                                                                                   jstring type_, jstring payload_,
                                                                                   jlong documentConfigHandle_) {
            auto dc = get<DocumentConfig>(documentConfigHandle_);
            if (nullptr == dc) {
                LOG(apl::LogLevel::kError) << "Invalid document config handle, ignoring data source update.";
                return JNI_FALSE;
            }

            if (nullptr == type_) {
                LOG(apl::LogLevel::kError) << "Invalid data provider type, ignoring data source update.";
                return JNI_FALSE;
            }

            const char *type = env->GetStringUTFChars(type_, nullptr);
            std::string typeString = std::string(type);
            DataSourceProviderPtr provider = findDataSourceProviderByType(dc, typeString);
            if (nullptr == provider) {
                LOG(apl::LogLevel::kError) << "Could not find data source provider, ignoring data source update for type: " << typeString;
                env->ReleaseStringUTFChars(type_, type);
                return JNI_FALSE;
            }

            LOG(apl::LogLevel::kInfo) << "Processing data source update for type: " << typeString;
            const char *payload = env->GetStringUTFChars(payload_, nullptr);
            const bool processed = provider->processUpdate(payload);
            if (!processed) {
                LOG(apl::LogLevel::kError) << "Data source update failed for type: " << typeString;
            }

            env->ReleaseStringUTFChars(payload_, payload);
            env->ReleaseStringUTFChars(type_, type);
            return static_cast <jboolean>(processed);
        }

        JNIEXPORT jobject JNICALL
        Java_com_amazon_apl_viewhost_internal_ViewhostImpl_nGetDataSourceErrors(JNIEnv *env, jclass clazz,
                                                                                jlong handle) {
            auto dc = get<DocumentConfig>(handle);

            auto knownDataSources = {
                    apl::DynamicIndexListConstants::DEFAULT_TYPE_NAME,
                    apl::DynamicTokenListConstants::DEFAULT_TYPE_NAME,
            };

            std::vector<apl::Object> errorArray;

            for (auto& type : knownDataSources) {
                DataSourceProviderPtr provider = findDataSourceProviderByType(dc, std::string(type));
                if (nullptr != provider) {
                    auto pendingErrors = provider->getPendingErrors();
                    if (!pendingErrors.empty() && pendingErrors.isArray()) {
                        errorArray.insert(errorArray.end(), pendingErrors.getArray().begin(), pendingErrors.getArray().end());
                    }
                }
            }
            auto errors = apl::Object(std::make_shared<apl::ObjectArray>(errorArray));

            if(!errors.isArray() || errors.empty()) {
                return nullptr;
            }
            return getJObject(env, errors);
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_viewhost_internal_EmbeddedDocumentRequestImpl_nCreateDocumentConfig(JNIEnv *env,
                                                                                                jclass clazz,
                                                                                                jlong mediatorHandle_) {
            auto documentConfig = apl::DocumentConfig::create();
            documentConfig->dataSourceProvider(std::make_shared<DynamicIndexListDataSourceProvider>());
            documentConfig->dataSourceProvider(std::make_shared<DynamicTokenListDataSourceProvider>());

            //set extension mediator
            if (mediatorHandle_ != 0) {
                auto mediator = get<ExtensionMediator>(mediatorHandle_);
                documentConfig->extensionMediator(mediator);
            }

            return createHandle<DocumentConfig>(documentConfig);
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_viewhost_internal_EmbeddedDocumentRequestProxy_nSuccess(JNIEnv *env, jobject instance,
                                                                                    jlong nativeHandle_, jlong contentHandle, jboolean isVisualContextConnected,
                                                                                    jlong documentConfigHandle_) {
            // Get the content from the NativeOwner
            auto embeddedDocumentRequest = get<AndroidEmbeddedDocumentRequest>(nativeHandle_);
            auto content = get<Content>(contentHandle);
            LOG(apl::LogLevel::kInfo) << "Calling success callback";
            auto documentConfig = get<DocumentConfig>(documentConfigHandle_);
            EmbeddedRequestSuccessResponse response{embeddedDocumentRequest->mEmbedRequest, content, (bool)(isVisualContextConnected == JNI_TRUE), documentConfig};
            auto documentContext = embeddedDocumentRequest->mSuccessCallback(std::move(response));
            assert(documentContext);

            return createHandle<DocumentContext>(documentContext);
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_viewhost_internal_EmbeddedDocumentRequestProxy_nGetRequestUrl(JNIEnv *env,
                                                                                          jobject instance,
                                                                                          jlong native_handle) {
            // Get the content from the NativeOwner
            auto embeddedDocumentRequest = get<AndroidEmbeddedDocumentRequest>(native_handle);
            std::string url = embeddedDocumentRequest->mEmbedRequest->getUrlRequest().getUrl();
            LOG(apl::LogLevel::kInfo) << "embed request url: " + url;
            return env->NewStringUTF(url.c_str());
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_viewhost_internal_EmbeddedDocumentRequestProxy_nFailure(JNIEnv *env, jobject instance, jlong native_handle, jstring failureMessage) {
            // Get the content from the NativeOwner
            auto embeddedDocumentRequest = get<AndroidEmbeddedDocumentRequest>(native_handle);
            const char* chars = env->GetStringUTFChars(failureMessage, nullptr);
            std::string message = std::string(chars);
            env->ReleaseStringUTFChars(failureMessage, chars);
            LOG(apl::LogLevel::kInfo) << "Error occurred with message: " + message;
            embeddedDocumentRequest->mFailureCallback(EmbeddedRequestFailureResponse{embeddedDocumentRequest->mEmbedRequest, message});
        }

#ifdef __cplusplus
        }
#endif
    } //namespace jni
} //namespace apl