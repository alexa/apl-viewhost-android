
/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#ifndef ANDROID_JNIEXTENSIONPROXY_H
#define ANDROID_JNIEXTENSIONPROXY_H

#include <jni.h>
#include "alexaext/alexaext.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 *  Initialize and cache java class and method handles for callback to the rendering layer.
 */
jboolean
extensionproxy_OnLoad(JavaVM *vm, void *reserved);

/**
 * Release the class and method cache.
 */
void
extensionproxy_OnUnload(JavaVM *vm, void *reserved);

namespace alexaext {
namespace jni {

    class AndroidExtensionProxy : public alexaext::ExtensionProxy {
    public:
        AndroidExtensionProxy(jweak instance, const std::string &uri);

        ~AndroidExtensionProxy() override;

        std::set<std::string> getURIs() const override;
        bool initializeExtension(const std::string &uri) override;
        bool isInitialized(const std::string &uri) const override;
        bool getRegistration(
                const std::string &uri,
                const rapidjson::Value &registrationRequest,
                RegistrationSuccessCallback success,
                RegistrationFailureCallback error) override;
        bool invokeCommand(const std::string &uri, const rapidjson::Value &command,
                           CommandSuccessCallback success, CommandFailureCallback error) override;
        bool sendMessage(const std::string &uri, const rapidjson::Value &message) override;
        void registerEventCallback(alexaext::Extension::EventCallback callback) override;
        void registerLiveDataUpdateCallback(alexaext::Extension::LiveDataUpdateCallback callback) override;
        void onRegistered(const std::string &uri, const std::string &token) override;
        void onUnregistered(const std::string &uri, const std::string &token) override;

        void registrationResult(const std::string &uri, const std::string &registrationResult);

        void commandResult(const std::string &uri, const std::string &commandResult);

        void onResourceReady(const std::string &uri, const alexaext::ResourceHolderPtr &resourceHolder) override ;

        bool invokeExtensionEventHandler(const std::string& uri, const std::string& event);

        bool invokeLiveDataUpdate(const std::string& uri, const std::string& liveDataUpdate);

        bool attachSurfaceHolder(JNIEnv *env, jstring resourceId, jobject surfaceHolder);

    private:
        jweak mWeakInstance;
        std::set<std::string> mURIs;
        std::vector<alexaext::Extension::EventCallback> mEventCallbacks;
        std::vector<alexaext::Extension::LiveDataUpdateCallback> mLiveDataCallbacks;
        RegistrationSuccessCallback mRegistrationSuccessCallback;
        RegistrationFailureCallback mRegistrationErrorCallback;
        CommandSuccessCallback mCommandSuccessCallback;
        CommandFailureCallback mCommandErrorCallback;
        bool mInitialized;
    };
}
}

#ifdef __cplusplus
}
#endif
#endif //ANDROID_JNIEXTENSIONPROXY_H
