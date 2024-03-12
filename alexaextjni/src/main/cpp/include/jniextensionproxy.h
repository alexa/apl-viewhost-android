
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

        /// alexaext::ExtensionProxy overrides
        std::set<std::string> getURIs() const override;
        bool initializeExtension(const std::string &uri) override;
        bool isInitialized(const std::string &uri) const override;
        bool getRegistration(
                const ActivityDescriptor& activity,
                const rapidjson::Value& registrationRequest,
                RegistrationSuccessActivityCallback&& success,
                RegistrationFailureActivityCallback&& error) override;
        bool invokeCommand(
                const ActivityDescriptor& activity,
                const rapidjson::Value& command,
                CommandSuccessActivityCallback&& success,
                CommandFailureActivityCallback&& error) override;
        bool sendComponentMessage(
                const ActivityDescriptor& activity,
                const rapidjson::Value& message) override;
        void registerEventCallback(
                const ActivityDescriptor& activity,
                Extension::EventActivityCallback&& callback) override;
        void registerLiveDataUpdateCallback(
                const ActivityDescriptor& activity,
                Extension::LiveDataUpdateActivityCallback&& callback) override;
        void onRegistered(const ActivityDescriptor& activity) override;
        void onUnregistered(const ActivityDescriptor& activity) override;

        void onResourceReady(
                const ActivityDescriptor& activity,
                const ResourceHolderPtr &resourceHolder) override;
        void onSessionStarted(const SessionDescriptor& session) override;
        void onSessionEnded(const SessionDescriptor& session) override;
        void onForeground(const ActivityDescriptor& activity) override;
        void onBackground(const ActivityDescriptor& activity) override;
        void onHidden(const ActivityDescriptor& activity) override;


        void registrationResult(const ActivityDescriptor& activity, const std::string &registrationResult);
        void commandResult(const ActivityDescriptor& activity, const std::string &commandResult);
        bool invokeExtensionEventHandler(const ActivityDescriptor& activity, const std::string& event);
        bool invokeLiveDataUpdate(const ActivityDescriptor& activity, const std::string& liveDataUpdate);
        bool attachSurfaceHolder(JNIEnv *env, jstring resourceId, jobject surfaceHolder);

    private:
        jweak mWeakInstance;
        std::set<std::string> mURIs;
        std::vector<alexaext::Extension::EventActivityCallback> mEventCallbacks;
        std::vector<alexaext::Extension::LiveDataUpdateActivityCallback> mLiveDataCallbacks;
        RegistrationSuccessActivityCallback mRegistrationSuccessCallback;
        RegistrationFailureActivityCallback mRegistrationErrorCallback;
        CommandSuccessActivityCallback mCommandSuccessCallback;
        CommandFailureActivityCallback mCommandErrorCallback;
        bool mInitialized;
    };
}
}

#ifdef __cplusplus
}
#endif
#endif //ANDROID_JNIEXTENSIONPROXY_H
