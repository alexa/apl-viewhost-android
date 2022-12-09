/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include <locale>
#include <codecvt>
#include "apl/apl.h"
#include "jniutil.h"
#include "jnicontent.h"

namespace apl {
    namespace jni {

#ifdef __cplusplus

        extern "C" {
#endif
        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_ExtensionClient_nCreate(JNIEnv *env, jclass clazz,
                                                            jlong rootConfig_, jstring uri_) {
            auto rootConfig = get<RootConfig>(rootConfig_);
            const char *uri = env->GetStringUTFChars(uri_, nullptr);
            auto extensionClient_ = ExtensionClient::create(rootConfig, uri);
            env->ReleaseStringUTFChars(uri_, uri);

            return createHandle<ExtensionClient>(extensionClient_);
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_ExtensionClient_nCreateRegistrationRequestFromContent(JNIEnv *env, jclass clazz,
                                                                                          jlong handle, jlong content_) {
            auto client = get<ExtensionClient>(handle);
            auto content = get<Content>(content_);
            rapidjson::Document document(rapidjson::kObjectType);
            auto registerRequestJson = client->createRegistrationRequest(document.GetAllocator(),
                                                                         *content);
            rapidjson::StringBuffer buffer;
            rapidjson::Writer<rapidjson::StringBuffer> writer(buffer);
            registerRequestJson.Accept(writer);
            return env->NewStringUTF(buffer.GetString());
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_ExtensionClient_nCreateRegistrationRequestFromMap(JNIEnv *env, jclass clazz,
                                                                                      jstring uri_, jobject settings_,
                                                                                      jobject flags_){
            std::string uri = getStdString(env, uri_);
            rapidjson::Document document(rapidjson::kObjectType);
            auto registerRequestJson = ExtensionClient::createRegistrationRequest(document.GetAllocator(),
                                                                                  std::string(uri), getAPLObject(env, settings_),
                                                                                  getAPLObject(env, flags_));

            rapidjson::StringBuffer buffer;
            rapidjson::Writer<rapidjson::StringBuffer> writer(buffer);
            registerRequestJson.Accept(writer);
            return env->NewStringUTF(buffer.GetString());
        }


        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_ExtensionClient_nRegistrationMessageProcessed(JNIEnv *env,
                                                                                  jclass clazz,
                                                                                  jlong handle) {
            auto client = get<ExtensionClient>(handle);
            return static_cast<jboolean>(client->registrationMessageProcessed());
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_ExtensionClient_nRegistered(JNIEnv *env, jclass clazz,
                                                                jlong handle) {
            auto client = get<ExtensionClient>(handle);
            return static_cast<jboolean>(client->registered());
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_ExtensionClient_nProcessMessage(JNIEnv *env, jclass clazz,
                                                                    jlong handle,
                                                                    jlong rootContext_,
                                                                    jstring message_) {
            auto client = get<ExtensionClient>(handle);
            std::shared_ptr<RootContext> rootContext = nullptr;
            if (rootContext_ != 0) {
                rootContext = get<RootContext>(rootContext_);
            }
            const char *message = env->GetStringUTFChars(message_, nullptr);
            bool result = client->processMessage(rootContext, std::string(message));
            env->ReleaseStringUTFChars(message_, message);
            return static_cast<jboolean>(result);
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_ExtensionClient_nProcessCommand(JNIEnv *env, jclass clazz,
                                                                    jlong handle, jlong event_) {
            auto client = get<ExtensionClient>(handle);
            auto event = get<Event>(event_);
            rapidjson::Document document(rapidjson::kObjectType);
            auto commandJson = client->processCommand(document.GetAllocator(), *event);
            rapidjson::StringBuffer buffer;
            rapidjson::Writer<rapidjson::StringBuffer> writer(buffer);
            commandJson.Accept(writer);
            return env->NewStringUTF(buffer.GetString());
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_ExtensionClient_nProcessComponentUpdate(JNIEnv *env, jclass clazz,
                                                                            jlong handle,
                                                                            jlong component_) {
            auto client = get<ExtensionClient>(handle);
            auto extensionComponent = get<ExtensionComponent>(component_);
            rapidjson::Document document(rapidjson::kObjectType);
            auto commandJson = client->createComponentChange(document.GetAllocator(), *extensionComponent);
            rapidjson::StringBuffer buffer;
            rapidjson::Writer<rapidjson::StringBuffer> writer(buffer);
            commandJson.Accept(writer);
            return env->NewStringUTF(buffer.GetString());
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_ExtensionClient_nHandleDisconnection(JNIEnv *env, jclass clazz,
                                                                         jlong handle,
                                                                         jlong rootContext_,
                                                                         jint errorCode,
                                                                         jstring message_) {
            auto client = get<ExtensionClient>(handle);
            std::shared_ptr<RootContext> rootContext = nullptr;
            if (rootContext_ != 0) {
                rootContext = get<RootContext>(rootContext_);
            }
            const char *message = env->GetStringUTFChars(message_, nullptr);
            bool result = client->handleDisconnection(rootContext, errorCode, std::string(message));
            env->ReleaseStringUTFChars(message_, message);
            return result;
        }

#ifdef __cplusplus
        }



#endif

    } //namespace jni
} //namespace apl