/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include "alexaext/alexaext.h"
#include "jniextensionproxy.h"
#include "jniextensionresource.h"
#include "jninativeowner.h"

namespace alexaext {
    namespace jni {

#ifdef __cplusplus

        extern "C" {
#endif
        using namespace alexaext;

        static jclass EXTENSIONPROXY_CLASS;
        static jmethodID EXTENSIONPROXY_INITIALIZE;
        static jmethodID EXTENSIONPROXY_INVOKE_COMMAND;
        static jmethodID EXTENSIONPROXY_SEND_MESSAGE;
        static jmethodID EXTENSIONPROXY_CREATE_REGISTRATION;
        static jmethodID EXTENSIONPROXY_ON_REGISTERED;
        static jmethodID EXTENSIONPROXY_ON_UNREGISTERED;
        static jmethodID EXTENSIONPROXY_ON_RESOURCE_READY;
        static JavaVM *PROXY_VM_REFERENCE;

        jboolean
        extensionproxy_OnLoad(JavaVM *vm, void *reserved) {
            JNIEnv *env;
            if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                return JNI_FALSE;
            }

            PROXY_VM_REFERENCE = vm;

            // method signatures can be obtained with 'javap -s'
            EXTENSIONPROXY_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("com/amazon/alexaext/ExtensionProxy")));

            EXTENSIONPROXY_INITIALIZE = env->GetMethodID(
                    EXTENSIONPROXY_CLASS,
                    "initialize",
                    "(Ljava/lang/String;)Z");

            EXTENSIONPROXY_INVOKE_COMMAND = env->GetMethodID(
                    EXTENSIONPROXY_CLASS,
                    "invokeCommand",
                    "(Ljava/lang/String;Ljava/lang/String;)Z");

            EXTENSIONPROXY_SEND_MESSAGE = env->GetMethodID(
                    EXTENSIONPROXY_CLASS,
                    "sendMessage",
                    "(Ljava/lang/String;Ljava/lang/String;)Z");

            EXTENSIONPROXY_CREATE_REGISTRATION = env->GetMethodID(
                    EXTENSIONPROXY_CLASS,
                    "requestRegistration",
                    "(Ljava/lang/String;Ljava/lang/String;)Z");

            EXTENSIONPROXY_ON_REGISTERED = env->GetMethodID(
                    EXTENSIONPROXY_CLASS,
                    "onRegistered",
                    "(Ljava/lang/String;Ljava/lang/String;)V");

            EXTENSIONPROXY_ON_UNREGISTERED = env->GetMethodID(
                    EXTENSIONPROXY_CLASS,
                    "onUnregistered",
                    "(Ljava/lang/String;Ljava/lang/String;)V");

            EXTENSIONPROXY_ON_RESOURCE_READY = env->GetMethodID(
                    EXTENSIONPROXY_CLASS,
                    "onResourceReady",
                    "(Ljava/lang/String;Lcom/amazon/alexaext/ResourceHolder;)V");

            if (nullptr == EXTENSIONPROXY_INITIALIZE ||
                nullptr == EXTENSIONPROXY_INVOKE_COMMAND ||
                nullptr == EXTENSIONPROXY_CREATE_REGISTRATION ||
                nullptr == EXTENSIONPROXY_ON_REGISTERED ||
                nullptr == EXTENSIONPROXY_ON_RESOURCE_READY) {
                return JNI_FALSE;
            }

            return JNI_TRUE;
        }

        void
        extensionproxy_OnUnload(JavaVM *vm, void *reserved) {
            JNIEnv *env;
            if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                // environment failure, can't proceed.
                return;
            }

            env->DeleteGlobalRef(EXTENSIONPROXY_CLASS);
        }


        AndroidExtensionProxy::AndroidExtensionProxy(jweak instance, const std::string &uri)
        : mWeakInstance(instance)
        {
            mInitialized = false;
            mURIs.emplace(uri);
        }

        AndroidExtensionProxy::~AndroidExtensionProxy()
        {
            JNIEnv *env;
            if (PROXY_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                return;
            }

            env->DeleteWeakGlobalRef(mWeakInstance);
        }

        std::set<std::string>
        AndroidExtensionProxy::getURIs() const
        {
            return mURIs;
        }


        bool
        AndroidExtensionProxy::initializeExtension(const std::string &uri)
        {
            if (!mURIs.count(uri)) return false;
            if (mInitialized) return true;

            JNIEnv *env;
            if (PROXY_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                return JNI_FALSE;
            }

            jobject localRef = env->NewLocalRef(mWeakInstance);
            if (!localRef) {
                return JNI_FALSE;
            }

            jstring jUri = env->NewStringUTF(uri.c_str());
            auto result = env->CallBooleanMethod(localRef, EXTENSIONPROXY_INITIALIZE, jUri);
            env->DeleteLocalRef(jUri);
            env->DeleteLocalRef(localRef);

            mInitialized = result;

            return result;
        }

        bool
        AndroidExtensionProxy::isInitialized(const std::string &uri) const
        {
            return mInitialized;
        }

        bool
        AndroidExtensionProxy::getRegistration(
                const std::string &uri,
                const rapidjson::Value &registrationRequest,
                RegistrationSuccessCallback success,
                RegistrationFailureCallback error)
        {
            int errorCode = kErrorNone;
            std::string errorMsg;

            // check the URI is supported
            if (mURIs.find(uri) == mURIs.end()) {
                if (error) {
                    rapidjson::Document fail = RegistrationFailure("1.0").uri(uri)
                            .errorCode(kErrorUnknownURI).errorMessage(sErrorMessage[kErrorUnknownURI] + uri);
                    error(uri, fail);
                }
                return false;
            }

            JNIEnv *env;
            if (PROXY_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                return false;
            }

            jobject localRef = env->NewLocalRef(mWeakInstance);
            if (!localRef) {
                return false;
            }

            mRegistrationSuccessCallback = std::move(success);
            mRegistrationErrorCallback = std::move(error);

            auto result = false;

            // request the schema from the extension
            jstring jUri = env->NewStringUTF(uri.c_str());
            auto requestString = AsString(registrationRequest);
            jstring jRequest = env->NewStringUTF(requestString.c_str());

            try {
                result = env->CallBooleanMethod(localRef, EXTENSIONPROXY_CREATE_REGISTRATION, jUri, jRequest);
            }
            catch (const std::exception& e) {
                errorCode = kErrorExtensionException;
                errorMsg = e.what();
            }
            catch (...) {
                errorCode = kErrorException;
                errorMsg = sErrorMessage[kErrorException];
            }

            env->DeleteLocalRef(localRef);
            env->DeleteLocalRef(jUri);
            env->DeleteLocalRef(jRequest);

            if (!result) {
                errorCode = kErrorException;
                errorMsg = sErrorMessage[kErrorException];
            }

            if (errorCode != kErrorNone) {
                rapidjson::Document fail = RegistrationFailure("1.0")
                        .errorCode(errorCode).errorMessage(errorMsg).uri(uri);
                mRegistrationErrorCallback(uri, fail);
                return false;
            }

            return true;
        }

        void
        AndroidExtensionProxy::registrationResult(const std::string &uri, const std::string &registrationResult)
        {
            rapidjson::Document registrationDoc;
            registrationDoc.Parse(registrationResult.c_str());

            // failed schema creation notify failure callback
            if (registrationDoc.IsNull() || registrationDoc.HasParseError()) {
                if (mRegistrationErrorCallback) {
                    rapidjson::Document fail =
                            RegistrationFailure("1.0")
                            .errorCode(kErrorInvalidExtensionSchema)
                            .errorMessage(sErrorMessage[kErrorInvalidExtensionSchema] + uri)
                            .uri(uri);
                    mRegistrationErrorCallback(uri, fail);
                }
                return; // registration message failed execution and was not handled by the extension
            }

            // non-success messages are assumed to be failure and forwarded to the error handler
            auto method = RegistrationSuccess::METHOD().Get(registrationDoc);
            if (method && *method != "RegisterSuccess") {
                if (mRegistrationErrorCallback) {
                    mRegistrationErrorCallback(uri, registrationDoc);
                }
            } else if (mRegistrationSuccessCallback) {
                mRegistrationSuccessCallback(uri, registrationDoc);
            }
        }

        bool
        AndroidExtensionProxy::invokeCommand(
                const std::string &uri,
                const rapidjson::Value &command,
                CommandSuccessCallback success,
                CommandFailureCallback error)
        {// verify the command has an ID
            const rapidjson::Value* commandValue = alexaext::Command::ID().Get(command);
            if (!commandValue || !commandValue->IsNumber()) {
                if (error) {
                    rapidjson::Document fail = CommandFailure("1.0")
                            .uri(uri)
                            .errorCode(kErrorInvalidMessage)
                            .errorMessage(sErrorMessage[kErrorInvalidMessage]);
                    error(uri, fail);
                }
                return false;
            }
            int commandID = (int) commandValue->GetDouble();

            // check the URI is supported and the command has ID
            if (mURIs.find(uri) == mURIs.end()) {
                if (error) {
                    rapidjson::Document fail = CommandFailure("1.0")
                            .uri(uri)
                            .id(commandID)
                            .errorCode(kErrorUnknownURI)
                            .errorMessage(sErrorMessage[kErrorUnknownURI] + uri);
                    error(uri, fail);
                }
                return false;
            }

            JNIEnv *env;
            if (PROXY_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                return false;
            }

            jobject localRef = env->NewLocalRef(mWeakInstance);
            if (!localRef) {
                return false;
            }

            // invoke the extension command
            int errorCode = kErrorNone;
            std::string errorMsg;
            bool result = false;
            jstring jUri = env->NewStringUTF(uri.c_str());
            jstring jCommand = env->NewStringUTF(AsString(command).c_str());
            try {
                result = env->CallBooleanMethod(localRef, EXTENSIONPROXY_INVOKE_COMMAND, jUri, jCommand);
            }
            catch (const std::exception& e) {
                errorCode = kErrorExtensionException;
                errorMsg = e.what();
            }
            catch (...) {
                errorCode = kErrorException;
                errorMsg = sErrorMessage[kErrorException];
            }
            env->DeleteLocalRef(localRef);
            env->DeleteLocalRef(jUri);
            env->DeleteLocalRef(jCommand);

            // failed command invocation
            if (!result) {
                if (error) {
                    if (errorCode == kErrorNone) {
                        //  the call was attempted but the extension failed without exception
                        errorCode = kErrorFailedCommand;
                        errorMsg = sErrorMessage[kErrorFailedCommand] + std::to_string(commandID);
                    }
                    rapidjson::Document fail = CommandFailure("1.0")
                            .uri(uri)
                            .id(commandID)
                            .errorCode(errorCode)
                            .errorMessage(errorMsg);
                    error(uri, fail);
                }
                return false;
            }

            // Success needs to come from the proxy if response required. Error may come too.
            // Only reason why we can stash this functions is that ExtensionMediator is singleton.
            // This callbacks is always same implementations and use only weak references.
            mCommandSuccessCallback = std::move(success);
            mCommandErrorCallback = std::move(error);

            return true;
        }

        bool
        AndroidExtensionProxy::sendMessage(const std::string &uri, const rapidjson::Value &message)
        {
            JNIEnv *env;
            if (PROXY_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                return false;
            }

            jobject localRef = env->NewLocalRef(mWeakInstance);
            if (!localRef) {
                return false;
            }

            bool result;
            jstring jUri = env->NewStringUTF(uri.c_str());
            jstring jMessage = env->NewStringUTF(AsString(message).c_str());
            try {
                result = env->CallBooleanMethod(localRef, EXTENSIONPROXY_SEND_MESSAGE, jUri, jMessage);
            }
            catch (...) {
                env->DeleteLocalRef(localRef);
                env->DeleteLocalRef(jUri);
                env->DeleteLocalRef(jMessage);
                return false;
            }
            env->DeleteLocalRef(localRef);
            env->DeleteLocalRef(jUri);
            env->DeleteLocalRef(jMessage);

            return result;
        }

        void
        AndroidExtensionProxy::commandResult(const std::string &uri, const std::string &commandResult)
        {
            rapidjson::Document resultDoc;
            resultDoc.Parse(commandResult.c_str());

            // failed schema creation notify failure callback
            if (resultDoc.IsNull() || resultDoc.HasParseError()) {
                if (mCommandErrorCallback) {
                    // TODO: It's not possible to get the command id if parsing failed.
                    rapidjson::Document fail =
                            CommandFailure("1.0")
                                    .errorCode(kErrorFailedCommand)
                                    .errorMessage(sErrorMessage[kErrorFailedCommand] + uri)
                                    .uri(uri);
                    mCommandErrorCallback(uri, fail);
                }
                return; // registration message failed execution and was not handled by the extension
            }

            // non-success messages are assumed to be failure and forwarded to the error handler
            auto method = RegistrationSuccess::METHOD().Get(resultDoc);
            if (method &&  *method != "CommandSuccess") {
                if (mCommandErrorCallback) {
                    mCommandErrorCallback(uri, resultDoc);
                }
            } else if (mCommandSuccessCallback) {
                mCommandSuccessCallback(uri, resultDoc);
            }
        }

        void
        AndroidExtensionProxy::registerEventCallback(Extension::EventCallback callback)
        {
            if (callback) mEventCallbacks.emplace_back(std::move(callback));
        }

        void
        AndroidExtensionProxy::registerLiveDataUpdateCallback(Extension::LiveDataUpdateCallback callback)
        {
            if (callback) mLiveDataCallbacks.emplace_back(std::move(callback));
        }

        void
        AndroidExtensionProxy::onRegistered(const std::string &uri, const std::string &token)
        {
            JNIEnv *env;
            if (PROXY_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                return;
            }

            jobject localRef = env->NewLocalRef(mWeakInstance);
            if (!localRef) {
                return;
            }

            jstring jUri = env->NewStringUTF(uri.c_str());
            jstring jToken = env->NewStringUTF(token.c_str());

            env->CallVoidMethod(localRef, EXTENSIONPROXY_ON_REGISTERED, jUri, jToken);
            env->DeleteLocalRef(localRef);
            if (env->ExceptionCheck()) {
                env->ExceptionDescribe();
                env->ExceptionClear();
            }

            env->DeleteLocalRef(jUri);
            env->DeleteLocalRef(jToken);
        }

        void
        AndroidExtensionProxy::onUnregistered(const std::string &uri, const std::string &token)
        {
            JNIEnv *env;
            if (PROXY_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                return;
            }
            jobject localRef = env->NewLocalRef(mWeakInstance);
            if (!localRef) {
                return;
            }

            jstring jUri = env->NewStringUTF(uri.c_str());
            jstring jToken = env->NewStringUTF(token.c_str());

            env->CallVoidMethod(localRef, EXTENSIONPROXY_ON_UNREGISTERED, jUri, jToken);
            env->DeleteLocalRef(localRef);
            if (env->ExceptionCheck()) {
                env->ExceptionDescribe();
                env->ExceptionClear();
            }

            env->DeleteLocalRef(jUri);
            env->DeleteLocalRef(jToken);
        }

        bool
        AndroidExtensionProxy::invokeExtensionEventHandler(const std::string& uri, const std::string& event)
        {
            if (mEventCallbacks.empty()) return false;

            rapidjson::Document eventDoc;
            eventDoc.Parse(event.c_str());

            if (eventDoc.IsNull() || eventDoc.HasParseError()) {
                return false;
            }

            for (auto& callback : mEventCallbacks) {
                callback(uri, eventDoc);
            }
            return true;
        }

        bool
        AndroidExtensionProxy::invokeLiveDataUpdate(const std::string& uri, const std::string& liveDataUpdate)
        {
            if (mEventCallbacks.empty()) return false;

            rapidjson::Document liveDataUpdateDoc;
            liveDataUpdateDoc.Parse(liveDataUpdate.c_str());

            if (liveDataUpdateDoc.IsNull() || liveDataUpdateDoc.HasParseError()) {
                return false;
            }

            for (auto& callback : mLiveDataCallbacks) {
                callback(uri, liveDataUpdateDoc);
            }
            return true;
        }

        void
        AndroidExtensionProxy::onResourceReady(const std::string &uri,
                                     const alexaext::ResourceHolderPtr &resourceHolder) {

            JNIEnv *env;
            if (PROXY_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) !=
                JNI_OK) {
                return;
            }

            jobject localRef = env->NewLocalRef(mWeakInstance);
            if (!localRef) {
                return;
            }

            auto holder = std::dynamic_pointer_cast<JNIResourceHolder>(resourceHolder);
            jstring jUri = env->NewStringUTF(uri.c_str());

            env->CallVoidMethod(localRef, EXTENSIONPROXY_ON_RESOURCE_READY, jUri, holder->mWeakInstance);
            env->DeleteLocalRef(localRef);
            env->DeleteLocalRef(jUri);
            if (env->ExceptionCheck()) {
                env->ExceptionDescribe();
                env->ExceptionClear();
            }
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_alexaext_ExtensionProxy_nCreate(JNIEnv *env, jobject instance, jstring uri_) {
            const auto uri = env->GetStringUTFChars(uri_, nullptr);
            auto extensionProxy_ = std::make_shared<AndroidExtensionProxy>(env->NewWeakGlobalRef(instance), uri);
            env->ReleaseStringUTFChars(uri_, uri);
            return apl::jni::createHandle<AndroidExtensionProxy>(extensionProxy_);
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_alexaext_ExtensionProxy_nInvokeExtensionEventHandler(JNIEnv *env, jclass clazz,
                                                                                jlong handler_,
                                                                                jstring uri_,
                                                                                jstring event_) {
            auto proxy = apl::jni::get<AndroidExtensionProxy>(handler_);
            const auto uri = env->GetStringUTFChars(uri_, nullptr);
            const auto event = env->GetStringUTFChars(event_, nullptr);
            jboolean result = static_cast<jboolean>(proxy->invokeExtensionEventHandler(uri, event));
            env->ReleaseStringUTFChars(uri_, uri);
            env->ReleaseStringUTFChars(event_, event);
            return result;
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_alexaext_ExtensionProxy_nInvokeLiveDataUpdate(JNIEnv *env, jclass clazz,
                                                                         jlong handler_, jstring uri_,
                                                                         jstring liveDataUpdate_) {
            auto proxy = apl::jni::get<AndroidExtensionProxy>(handler_);
            const auto uri = env->GetStringUTFChars(uri_, nullptr);
            const auto liveDataUpdate = env->GetStringUTFChars(liveDataUpdate_, nullptr);
            jboolean result = static_cast<jboolean>(proxy->invokeLiveDataUpdate(uri, liveDataUpdate));
            env->ReleaseStringUTFChars(uri_, uri);
            env->ReleaseStringUTFChars(liveDataUpdate_, liveDataUpdate);
            return result;
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_alexaext_ExtensionProxy_nRegistrationResult(JNIEnv *env, jclass clazz,
                                                                       jlong handler_, jstring uri_,
                                                                       jstring registrationResult_) {
            auto proxy = apl::jni::get<AndroidExtensionProxy>(handler_);
            const auto uri = env->GetStringUTFChars(uri_, nullptr);
            const auto registrationResult = env->GetStringUTFChars(registrationResult_, nullptr);
            proxy->registrationResult(uri, registrationResult);
            env->ReleaseStringUTFChars(uri_, uri);
            env->ReleaseStringUTFChars(registrationResult_, registrationResult);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_alexaext_ExtensionProxy_nCommandResult(JNIEnv *env, jclass clazz,
                                                                  jlong handler_, jstring uri_,
                                                                  jstring commandResult_) {
            auto proxy = apl::jni::get<AndroidExtensionProxy>(handler_);
            const auto uri = env->GetStringUTFChars(uri_, nullptr);
            const auto commandResult = env->GetStringUTFChars(commandResult_, nullptr);
            proxy->commandResult(uri, commandResult);
            env->ReleaseStringUTFChars(uri_, uri);
            env->ReleaseStringUTFChars(commandResult_, commandResult);
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_alexaext_ExtensionProxy_nGetUri(JNIEnv *env, jclass clazz, jlong handler_) {
            auto proxy = apl::jni::get<AndroidExtensionProxy>(handler_);
            return env->NewStringUTF(proxy->getURIs().begin()->c_str());
        }

#ifdef __cplusplus
        }
#endif
    } //namespace jni
} //namespace alexaext