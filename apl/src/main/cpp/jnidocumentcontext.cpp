/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include <locale>
#include <codecvt>
#include "jnidocumentcontext.h"
#include "jniutil.h"

namespace apl {
    namespace jni {
#ifdef __cplusplus
        extern "C" {
#endif
        static std::wstring_convert<std::codecvt_utf8_utf16<char16_t>, char16_t> converter;

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_viewhost_internal_DocumentContext_nExecuteCommands(JNIEnv *env, jclass clazz,
                                                                               jlong handle,
                                                                               jstring commands_) {
            auto documentContext = get<DocumentContext>(handle);
            auto doc = rapidjson::Document();
            const char* commands = env->GetStringUTFChars(commands_, nullptr);
            doc.Parse(commands);
            env->ReleaseStringUTFChars(commands_, commands);
            apl::Object obj = apl::Object(std::move(doc));
            auto action = documentContext->executeCommands(obj, false);
            if (action == nullptr) {
                return 0;
            }
            return createHandle<Action>(action);
        }

        jlong getDocumentContextId(const DocumentContextPtr documentContext) {
            // This is just the memory address of the underlying core DocumentContext instance
            return documentContext ? reinterpret_cast<jlong>(documentContext.get()) : 0;
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_viewhost_internal_DocumentContext_nGetId(JNIEnv *env,
                                                                     jobject instance,
                                                                     jlong nativeHandle) {
            auto documentContext = get<DocumentContext>(nativeHandle);
            return getDocumentContextId(documentContext);
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_viewhost_internal_DocumentContext_nIsVisualContextDirty(JNIEnv *env,
                                                                                    jobject instance,
                                                                                    jlong handle) {
            auto documentContext = get<DocumentContext>(handle);
            return static_cast<jboolean>(documentContext->isVisualContextDirty());
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_viewhost_internal_DocumentContext_nClearVisualContextDirty(JNIEnv *env,
                                                                                       jobject instance,
                                                                                       jlong handle) {
            auto documentContext = get<DocumentContext>(handle);
            documentContext->clearVisualContextDirty();
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_viewhost_internal_DocumentContext_nSerializeVisualContext(JNIEnv *env,
                                                                                      jobject instance,
                                                                                      jlong handle) {
            auto documentContext = get<DocumentContext>(handle);
            rapidjson::Document document(rapidjson::kObjectType);
            auto context = documentContext->serializeVisualContext(document.GetAllocator());
            rapidjson::StringBuffer buffer;
            rapidjson::Writer<rapidjson::StringBuffer> writer(buffer);
            context.Accept(writer);
            std::u16string u16 = converter.from_bytes(buffer.GetString());
            return env->NewString(reinterpret_cast<const jchar *>(u16.c_str()), u16.length());
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_viewhost_internal_DocumentContext_nIsDataSourceContextDirty(JNIEnv *env,
                                                                                        jobject instance,
                                                                                        jlong handle) {
            auto documentContext = get<DocumentContext>(handle);
            return static_cast<jboolean>(documentContext->isDataSourceContextDirty());
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_viewhost_internal_DocumentContext_nClearDataSourceContextDirty(JNIEnv *env,
                                                                                           jobject instance,
                                                                                           jlong handle) {
            auto documentContext = get<DocumentContext>(handle);
            documentContext->clearDataSourceContextDirty();
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_viewhost_internal_DocumentContext_nSerializeDataSourceContext(JNIEnv *env,
                                                                                          jobject instance,
                                                                                          jlong handle) {
            auto documentContext = get<DocumentContext>(handle);
            rapidjson::Document document(rapidjson::kObjectType);
            auto context = documentContext->serializeDataSourceContext(document.GetAllocator());
            rapidjson::StringBuffer buffer;
            rapidjson::Writer<rapidjson::StringBuffer> writer(buffer);
            context.Accept(writer);
            std::u16string u16 = converter.from_bytes(buffer.GetString());
            return env->NewString(reinterpret_cast<const jchar *>(u16.c_str()), u16.length());
        }

#ifdef __cplusplus
        }
#endif
    } //namespace jni
} //namespace apl
