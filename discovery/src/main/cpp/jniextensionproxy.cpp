/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include <string>
#include <memory>
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

        static jclass SESSION_DESCRIPTOR_CLASS;
        static jmethodID SESSION_DESCRIPTOR_CONSTRUCTOR;

        static jclass ACTIVITY_DESCRIPTOR_CLASS;
        static jmethodID ACTIVITY_DESCRIPTOR_CONSTRUCTOR;

        static jclass EXTENSIONPROXY_CLASS;
        static jmethodID EXTENSIONPROXY_INITIALIZE;
        static jmethodID EXTENSIONPROXY_INVOKE_COMMAND;
        static jmethodID EXTENSIONPROXY_SEND_MESSAGE;
        static jmethodID EXTENSIONPROXY_CREATE_REGISTRATION;
        static jmethodID EXTENSIONPROXY_ON_REGISTERED;
        static jmethodID EXTENSIONPROXY_ON_UNREGISTERED;
        static jmethodID EXTENSIONPROXY_ON_RESOURCE_READY;
        static jmethodID EXTENSIONPROXY_ON_SESSION_STARTED;
        static jmethodID EXTENSIONPROXY_ON_SESSION_ENDED;
        static jmethodID EXTENSIONPROXY_ON_FOREGROUND;
        static jmethodID EXTENSIONPROXY_ON_BACKGROUND;
        static jmethodID EXTENSIONPROXY_ON_HIDDEN;
        static JavaVM *PROXY_VM_REFERENCE;

        jboolean
        extensionproxy_OnLoad(JavaVM *vm, void *reserved) {
            JNIEnv *env;
            if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                return JNI_FALSE;
            }

            PROXY_VM_REFERENCE = vm;

            SESSION_DESCRIPTOR_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("com/amazon/alexaext/SessionDescriptor")));
            SESSION_DESCRIPTOR_CONSTRUCTOR = env->GetMethodID(
                    SESSION_DESCRIPTOR_CLASS,
                    "<init>",
                    "(Ljava/lang/String;)V");

            ACTIVITY_DESCRIPTOR_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("com/amazon/alexaext/ActivityDescriptor")));
            ACTIVITY_DESCRIPTOR_CONSTRUCTOR = env->GetMethodID(
                    ACTIVITY_DESCRIPTOR_CLASS,
                    "<init>",
                    "(Ljava/lang/String;Lcom/amazon/alexaext/SessionDescriptor;Ljava/lang/String;)V");

            EXTENSIONPROXY_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("com/amazon/alexaext/ExtensionProxy")));

            EXTENSIONPROXY_INITIALIZE = env->GetMethodID(
                    EXTENSIONPROXY_CLASS,
                    "initializeNative",
                    "(Ljava/lang/String;)Z");

            EXTENSIONPROXY_INVOKE_COMMAND = env->GetMethodID(
                    EXTENSIONPROXY_CLASS,
                    "invokeCommandNative",
                    "(Lcom/amazon/alexaext/ActivityDescriptor;Ljava/lang/String;)Z");

            EXTENSIONPROXY_SEND_MESSAGE = env->GetMethodID(
                    EXTENSIONPROXY_CLASS,
                    "sendMessageNative",
                    "(Lcom/amazon/alexaext/ActivityDescriptor;Ljava/lang/String;)Z");

            EXTENSIONPROXY_CREATE_REGISTRATION = env->GetMethodID(
                    EXTENSIONPROXY_CLASS,
                    "requestRegistrationNative",
                    "(Lcom/amazon/alexaext/ActivityDescriptor;Ljava/lang/String;)Z");

            EXTENSIONPROXY_ON_REGISTERED = env->GetMethodID(
                    EXTENSIONPROXY_CLASS,
                    "onRegisteredNative",
                    "(Lcom/amazon/alexaext/ActivityDescriptor;)V");

            EXTENSIONPROXY_ON_UNREGISTERED = env->GetMethodID(
                    EXTENSIONPROXY_CLASS,
                    "onUnregisteredNative",
                    "(Lcom/amazon/alexaext/ActivityDescriptor;)V");

            EXTENSIONPROXY_ON_RESOURCE_READY = env->GetMethodID(
                    EXTENSIONPROXY_CLASS,
                    "onResourceReadyNative",
                    "(Lcom/amazon/alexaext/ActivityDescriptor;Lcom/amazon/alexaext/ResourceHolder;)V");

            EXTENSIONPROXY_ON_SESSION_STARTED = env->GetMethodID(
                    EXTENSIONPROXY_CLASS,
                    "onSessionStarted",
                    "(Lcom/amazon/alexaext/SessionDescriptor;)V");

            EXTENSIONPROXY_ON_SESSION_ENDED = env->GetMethodID(
                    EXTENSIONPROXY_CLASS,
                    "onSessionEnded",
                    "(Lcom/amazon/alexaext/SessionDescriptor;)V");

            EXTENSIONPROXY_ON_FOREGROUND = env->GetMethodID(
                    EXTENSIONPROXY_CLASS,
                    "onForeground",
                    "(Lcom/amazon/alexaext/ActivityDescriptor;)V");

            EXTENSIONPROXY_ON_BACKGROUND = env->GetMethodID(
                    EXTENSIONPROXY_CLASS,
                    "onBackground",
                    "(Lcom/amazon/alexaext/ActivityDescriptor;)V");

            EXTENSIONPROXY_ON_HIDDEN = env->GetMethodID(
                    EXTENSIONPROXY_CLASS,
                    "onHidden",
                    "(Lcom/amazon/alexaext/ActivityDescriptor;)V");

            if (nullptr == SESSION_DESCRIPTOR_CONSTRUCTOR ||
                nullptr == ACTIVITY_DESCRIPTOR_CONSTRUCTOR ||
                nullptr == EXTENSIONPROXY_INITIALIZE ||
                nullptr == EXTENSIONPROXY_INVOKE_COMMAND ||
                nullptr == EXTENSIONPROXY_CREATE_REGISTRATION ||
                nullptr == EXTENSIONPROXY_ON_REGISTERED ||
                nullptr == EXTENSIONPROXY_ON_RESOURCE_READY ||
                nullptr == EXTENSIONPROXY_ON_SESSION_STARTED ||
                nullptr == EXTENSIONPROXY_ON_SESSION_ENDED ||
                nullptr == EXTENSIONPROXY_ON_FOREGROUND ||
                nullptr == EXTENSIONPROXY_ON_BACKGROUND ||
                nullptr == EXTENSIONPROXY_ON_HIDDEN) {
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
            env->DeleteGlobalRef(ACTIVITY_DESCRIPTOR_CLASS);
            env->DeleteGlobalRef(SESSION_DESCRIPTOR_CLASS);
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

// Kinda dislike macros in C++. But useful here.
#define ENV_CREATE(FAIL_RETURN) \
    JNIEnv *env; \
    if (PROXY_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) { \
        return FAIL_RETURN; \
    } \
    \
    jobject localRef = env->NewLocalRef(mWeakInstance); \
    if (!localRef) { \
        return FAIL_RETURN; \
    }

#define ENV_CLEAR() \
    env->DeleteLocalRef(localRef)

#define SESSION_CREATE(SESSION) \
    jstring jSessionId = env->NewStringUTF((SESSION).getId().c_str()); \
    auto jSessionDescriptor = env->NewObject(SESSION_DESCRIPTOR_CLASS, SESSION_DESCRIPTOR_CONSTRUCTOR, jSessionId)

#define SESSION_CLEAR() \
    env->DeleteLocalRef(jSessionDescriptor); \
    env->DeleteLocalRef(jSessionId)

#define ACTIVITY_CREATE(ACTIVITY) \
    jstring jUri = env->NewStringUTF((ACTIVITY).getURI().c_str()); \
    jstring jActivityId = env->NewStringUTF((ACTIVITY).getId().c_str()); \
    auto jActivityDescriptor = env->NewObject(ACTIVITY_DESCRIPTOR_CLASS, ACTIVITY_DESCRIPTOR_CONSTRUCTOR, jUri, jSessionDescriptor, jActivityId)

#define ACTIVITY_CLEAR() \
    env->DeleteLocalRef(jActivityDescriptor); \
    env->DeleteLocalRef(jActivityId); \
    env->DeleteLocalRef(jUri)

        bool
        AndroidExtensionProxy::getRegistration(
                const ActivityDescriptor& activity,
                const rapidjson::Value& registrationRequest,
                RegistrationSuccessActivityCallback&& success,
                RegistrationFailureActivityCallback&& error)
        {
            int errorCode = kErrorNone;
            std::string errorMsg;

            // check the URI is supported
            if (mURIs.find(activity.getURI()) == mURIs.end()) {
                if (error) {
                    rapidjson::Document fail = RegistrationFailure("1.0").uri(activity.getURI())
                            .errorCode(kErrorUnknownURI).errorMessage(sErrorMessage[kErrorUnknownURI] + activity.getURI());
                    error(activity, fail);
                }
                return false;
            }

            ENV_CREATE(false);

            mRegistrationSuccessCallback = std::move(success);
            mRegistrationErrorCallback = std::move(error);

            auto result = false;

            // request the schema from the extension
            SESSION_CREATE(*activity.getSession());
            ACTIVITY_CREATE(activity);

            jstring jRequest = env->NewStringUTF(AsString(registrationRequest).c_str());

            result = env->CallBooleanMethod(localRef, EXTENSIONPROXY_CREATE_REGISTRATION, jActivityDescriptor, jRequest);

            ACTIVITY_CLEAR();
            SESSION_CLEAR();
            env->DeleteLocalRef(jRequest);
            ENV_CLEAR();

            if (!result) {
                errorCode = kErrorException;
                errorMsg = sErrorMessage[kErrorException];
            }

            if (errorCode != kErrorNone) {
                rapidjson::Document fail = RegistrationFailure("1.0")
                        .errorCode(errorCode).errorMessage(errorMsg).uri(activity.getURI());
                mRegistrationErrorCallback(activity, fail);
                return false;
            }

            return true;
        }

        void
        AndroidExtensionProxy::registrationResult(const ActivityDescriptor& activity, const std::string &registrationResult)
        {
            rapidjson::Document registrationDoc;
            registrationDoc.Parse(registrationResult.c_str());

            // failed schema creation notify failure callback
            if (registrationDoc.IsNull() || registrationDoc.HasParseError()) {
                if (mRegistrationErrorCallback) {
                    rapidjson::Document fail =
                            RegistrationFailure("1.0")
                            .errorCode(kErrorInvalidExtensionSchema)
                            .errorMessage(sErrorMessage[kErrorInvalidExtensionSchema] + activity.getURI())
                            .uri(activity.getURI());
                    mRegistrationErrorCallback(activity, fail);
                }
                return; // registration message failed execution and was not handled by the extension
            }

            // non-success messages are assumed to be failure and forwarded to the error handler
            auto method = RegistrationSuccess::METHOD().Get(registrationDoc);
            if (method && *method != "RegisterSuccess") {
                if (mRegistrationErrorCallback) {
                    mRegistrationErrorCallback(activity, registrationDoc);
                }
            } else if (mRegistrationSuccessCallback) {
                mRegistrationSuccessCallback(activity, registrationDoc);
            }
        }

        bool
        AndroidExtensionProxy::invokeCommand(
                const ActivityDescriptor& activity,
                const rapidjson::Value& command,
                CommandSuccessActivityCallback&& success,
                CommandFailureActivityCallback&& error)
        {// verify the command has an ID
            const rapidjson::Value* commandValue = alexaext::Command::ID().Get(command);
            if (!commandValue || !commandValue->IsNumber()) {
                if (error) {
                    rapidjson::Document fail = CommandFailure("1.0")
                            .uri(activity.getURI())
                            .errorCode(kErrorInvalidMessage)
                            .errorMessage(sErrorMessage[kErrorInvalidMessage]);
                    error(activity, fail);
                }
                return false;
            }
            int commandID = (int) commandValue->GetDouble();

            // check the URI is supported and the command has ID
            if (mURIs.find(activity.getURI()) == mURIs.end()) {
                if (error) {
                    rapidjson::Document fail = CommandFailure("1.0")
                            .uri(activity.getURI())
                            .id(commandID)
                            .errorCode(kErrorUnknownURI)
                            .errorMessage(sErrorMessage[kErrorUnknownURI] + activity.getURI());
                    error(activity, fail);
                }
                return false;
            }

            ENV_CREATE(false);

            // invoke the extension command
            int errorCode = kErrorNone;
            std::string errorMsg;
            bool result = false;
            SESSION_CREATE(*activity.getSession());
            ACTIVITY_CREATE(activity);
            jstring jCommand = env->NewStringUTF(AsString(command).c_str());

            result = env->CallBooleanMethod(localRef, EXTENSIONPROXY_INVOKE_COMMAND, jActivityDescriptor, jCommand);

            ACTIVITY_CLEAR();
            SESSION_CLEAR();
            ENV_CLEAR();
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
                            .uri(activity.getURI())
                            .id(commandID)
                            .errorCode(errorCode)
                            .errorMessage(errorMsg);
                    error(activity, fail);
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
        AndroidExtensionProxy::sendComponentMessage(const ActivityDescriptor& activity, const rapidjson::Value& message)
        {
            ENV_CREATE(false);

            bool result;
            SESSION_CREATE(*activity.getSession());
            ACTIVITY_CREATE(activity);
            jstring jMessage = env->NewStringUTF(AsString(message).c_str());
            result = env->CallBooleanMethod(localRef, EXTENSIONPROXY_SEND_MESSAGE, jActivityDescriptor, jMessage);
            ACTIVITY_CLEAR();
            SESSION_CLEAR();
            ENV_CLEAR();
            env->DeleteLocalRef(jMessage);

            return result;
        }

        void
        AndroidExtensionProxy::commandResult(const ActivityDescriptor& activity, const std::string &commandResult)
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
                                    .errorMessage(sErrorMessage[kErrorFailedCommand] + activity.getURI())
                                    .uri(activity.getURI());
                    mCommandErrorCallback(activity, fail);
                }
                return; // registration message failed execution and was not handled by the extension
            }

            // non-success messages are assumed to be failure and forwarded to the error handler
            auto method = RegistrationSuccess::METHOD().Get(resultDoc);
            if (method &&  *method != "CommandSuccess") {
                if (mCommandErrorCallback) {
                    mCommandErrorCallback(activity, resultDoc);
                }
            } else if (mCommandSuccessCallback) {
                mCommandSuccessCallback(activity, resultDoc);
            }
        }

        void
        AndroidExtensionProxy::registerEventCallback(
                const ActivityDescriptor& activity,
                Extension::EventActivityCallback&& callback)
        {
            if (callback) mEventCallbacks.emplace_back(std::move(callback));
        }

        void
        AndroidExtensionProxy::registerLiveDataUpdateCallback(
                const ActivityDescriptor& activity,
                Extension::LiveDataUpdateActivityCallback&& callback)
        {
            if (callback) mLiveDataCallbacks.emplace_back(std::move(callback));
        }

        void
        AndroidExtensionProxy::onRegistered(const ActivityDescriptor& activity)
        {
            ENV_CREATE();
            SESSION_CREATE(*activity.getSession());
            ACTIVITY_CREATE(activity);

            env->CallVoidMethod(localRef, EXTENSIONPROXY_ON_REGISTERED, jActivityDescriptor);
            if (env->ExceptionCheck()) {
                env->ExceptionDescribe();
                env->ExceptionClear();
            }

            ACTIVITY_CLEAR();
            SESSION_CLEAR();
            ENV_CLEAR();
        }

        void
        AndroidExtensionProxy::onUnregistered(const ActivityDescriptor& activity)
        {
            ENV_CREATE();
            SESSION_CREATE(*activity.getSession());
            ACTIVITY_CREATE(activity);

            env->CallVoidMethod(localRef, EXTENSIONPROXY_ON_UNREGISTERED, jActivityDescriptor);
            if (env->ExceptionCheck()) {
                env->ExceptionDescribe();
                env->ExceptionClear();
            }

            ACTIVITY_CLEAR();
            SESSION_CLEAR();
            ENV_CLEAR();
        }

        void
        AndroidExtensionProxy::onResourceReady(
                const ActivityDescriptor& activity,
                const ResourceHolderPtr &resourceHolder) {

            ENV_CREATE();
            auto holder = std::static_pointer_cast<JNIResourceHolder>(resourceHolder);
            SESSION_CREATE(*activity.getSession());
            ACTIVITY_CREATE(activity);

            env->CallVoidMethod(localRef, EXTENSIONPROXY_ON_RESOURCE_READY, jActivityDescriptor, holder->mWeakInstance);

            ACTIVITY_CLEAR();
            SESSION_CLEAR();
            ENV_CLEAR();
            if (env->ExceptionCheck()) {
                env->ExceptionDescribe();
                env->ExceptionClear();
            }
        }

        void
        AndroidExtensionProxy::onSessionStarted(const SessionDescriptor& session) {
            ENV_CREATE();
            SESSION_CREATE(session);

            env->CallVoidMethod(localRef, EXTENSIONPROXY_ON_SESSION_STARTED, jSessionDescriptor);

            SESSION_CLEAR();
            ENV_CLEAR();
        }

        void
        AndroidExtensionProxy::onSessionEnded(const SessionDescriptor& session) {
            ENV_CREATE();
            SESSION_CREATE(session);

            env->CallVoidMethod(localRef, EXTENSIONPROXY_ON_SESSION_ENDED, jSessionDescriptor);

            SESSION_CLEAR();
            ENV_CLEAR();
        }

        void
        AndroidExtensionProxy::onForeground(const ActivityDescriptor& activity) {
            ENV_CREATE();
            SESSION_CREATE(*activity.getSession());
            ACTIVITY_CREATE(activity);

            env->CallVoidMethod(localRef, EXTENSIONPROXY_ON_FOREGROUND, jActivityDescriptor);

            ACTIVITY_CLEAR();
            SESSION_CLEAR();
            ENV_CLEAR();
        }

        void
        AndroidExtensionProxy::onBackground(const ActivityDescriptor& activity) {
            ENV_CREATE();
            SESSION_CREATE(*activity.getSession());
            ACTIVITY_CREATE(activity);

            env->CallVoidMethod(localRef, EXTENSIONPROXY_ON_BACKGROUND, jActivityDescriptor);

            ACTIVITY_CLEAR();
            SESSION_CLEAR();
            ENV_CLEAR();
        }

        void
        AndroidExtensionProxy::onHidden(const ActivityDescriptor& activity) {
            ENV_CREATE();
            SESSION_CREATE(*activity.getSession());
            ACTIVITY_CREATE(activity);

            env->CallVoidMethod(localRef, EXTENSIONPROXY_ON_HIDDEN, jActivityDescriptor);

            ACTIVITY_CLEAR();
            SESSION_CLEAR();
            ENV_CLEAR();
        }

        bool
        AndroidExtensionProxy::invokeExtensionEventHandler(const ActivityDescriptor& activity, const std::string& event)
        {
            if (mEventCallbacks.empty()) return false;

            rapidjson::Document eventDoc;
            eventDoc.Parse(event.c_str());

            if (eventDoc.IsNull() || eventDoc.HasParseError()) {
                return false;
            }

            for (auto& callback : mEventCallbacks) {
                callback(activity, eventDoc);
            }
            return true;
        }

        bool
        AndroidExtensionProxy::invokeLiveDataUpdate(const ActivityDescriptor& activity, const std::string& liveDataUpdate)
        {
            if (mEventCallbacks.empty()) return false;

            rapidjson::Document liveDataUpdateDoc;
            liveDataUpdateDoc.Parse(liveDataUpdate.c_str());

            if (liveDataUpdateDoc.IsNull() || liveDataUpdateDoc.HasParseError()) {
                return false;
            }

            for (auto& callback : mLiveDataCallbacks) {
                callback(activity, liveDataUpdateDoc);
            }
            return true;
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_alexaext_ExtensionProxy_nCreate(JNIEnv *env, jobject instance, jstring uri_) {
            const auto uri = env->GetStringUTFChars(uri_, nullptr);
            auto extensionProxy_ = std::make_shared<AndroidExtensionProxy>(env->NewWeakGlobalRef(instance), uri);
            env->ReleaseStringUTFChars(uri_, uri);
            return apl::jni::createHandle<AndroidExtensionProxy>(extensionProxy_);
        }

#define CREATE_ACTIVITY_DESCRIPTOR() \
        const auto uri = env->GetStringUTFChars(uri_, nullptr); \
        const auto sessionId = env->GetStringUTFChars(sessionId_, nullptr); \
        const auto activityId = env->GetStringUTFChars(activityId_, nullptr); \
        \
        auto session = std::make_shared<SessionDescriptor>(sessionId); \
        auto activityDescriptor = ActivityDescriptor(uri, session, activityId); \
        \
        env->ReleaseStringUTFChars(uri_, uri); \
        env->ReleaseStringUTFChars(sessionId_, sessionId); \
        env->ReleaseStringUTFChars(activityId_, activityId);

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_alexaext_ExtensionProxy_nInvokeExtensionEventHandler(JNIEnv *env, jclass clazz,
                                                                             jlong handler_,
                                                                             jstring uri_,
                                                                             jstring sessionId_,
                                                                             jstring activityId_,
                                                                             jstring event_) {
            CREATE_ACTIVITY_DESCRIPTOR();

            auto proxy = apl::jni::get<AndroidExtensionProxy>(handler_);
            const auto event = env->GetStringUTFChars(event_, nullptr);
            jboolean result = static_cast<jboolean>(proxy->invokeExtensionEventHandler(activityDescriptor, event));
            env->ReleaseStringUTFChars(event_, event);
            return result;
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_alexaext_ExtensionProxy_nInvokeLiveDataUpdate(JNIEnv *env, jclass clazz,
                                                                         jlong handler_,
                                                                         jstring uri_,
                                                                         jstring sessionId_,
                                                                         jstring activityId_,
                                                                         jstring liveDataUpdate_) {
            CREATE_ACTIVITY_DESCRIPTOR();

            auto proxy = apl::jni::get<AndroidExtensionProxy>(handler_);
            const auto liveDataUpdate = env->GetStringUTFChars(liveDataUpdate_, nullptr);
            jboolean result = static_cast<jboolean>(proxy->invokeLiveDataUpdate(activityDescriptor, liveDataUpdate));
            env->ReleaseStringUTFChars(liveDataUpdate_, liveDataUpdate);
            return result;
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_alexaext_ExtensionProxy_nRegistrationResult(JNIEnv *env, jclass clazz,
                                                                       jlong handler_,
                                                                       jstring uri_,
                                                                       jstring sessionId_,
                                                                       jstring activityId_,
                                                                       jstring registrationResult_) {
            CREATE_ACTIVITY_DESCRIPTOR();
            auto proxy = apl::jni::get<AndroidExtensionProxy>(handler_);
            const auto registrationResult = env->GetStringUTFChars(registrationResult_, nullptr);
            proxy->registrationResult(activityDescriptor, registrationResult);
            env->ReleaseStringUTFChars(registrationResult_, registrationResult);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_alexaext_ExtensionProxy_nCommandResult(JNIEnv *env, jclass clazz,
                                                                  jlong handler_,
                                                                  jstring uri_,
                                                                  jstring sessionId_,
                                                                  jstring activityId_,
                                                                  jstring commandResult_) {
            CREATE_ACTIVITY_DESCRIPTOR();
            auto proxy = apl::jni::get<AndroidExtensionProxy>(handler_);
            const auto commandResult = env->GetStringUTFChars(commandResult_, nullptr);
            proxy->commandResult(activityDescriptor, commandResult);
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