/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#ifndef APLVIEWHOSTANDROID_JNIMEDIAMANAGER_H
#define APLVIEWHOSTANDROID_JNIMEDIAMANAGER_H

#include <jni.h>
#include "apl/apl.h"
#include "jnimediaobject.h"

#ifdef __cplusplus
extern "C" {
#endif

namespace apl {
    namespace jni {
        class jniMediaObject;

        class jniMediaManager
                : public apl::MediaManager, public std::enable_shared_from_this<jniMediaManager> {
        public:
            static apl::MediaManagerPtr create(jweak javaMediaManagerInstance);

            jniMediaManager() = default;

            virtual ~jniMediaManager() = default;

            apl::MediaObjectPtr request(const std::string &url, apl::EventMediaType type) override;

            apl::MediaObjectPtr request(const std::string &url, apl::EventMediaType type,
                                        const apl::HeaderArray &headers) override;

            void processMediaRequests(const apl::ContextPtr &context) override {};

            void mediaLoadComplete(const std::string &source, bool isReady, int errorCode,
                                   const std::string &errorReason) override {};

            void release(jniMediaObject &mediaObject);

        private:
            bool invokeMediaManager(jmethodID method, ...);

            std::map<std::string, std::weak_ptr<jniMediaObject>> mObjectMap;
            jweak mJavaMediaManagerInstance;
        };

        /**
        *  Initialize and cache java class and method handles for callback to the rendering layer.
        */
        jboolean mediamanager_OnLoad(JavaVM *vm, void *reserved __attribute__((__unused__)));

        /**
         * Release the class and methmediaLoadCompleteod cache.
         */
        void mediamanager_OnUnload(JavaVM *vm, void *reserved __attribute__((__unused__)));

    } // namespace jni
} // namespace apl

#ifdef __cplusplus
}
#endif

#endif //APLVIEWHOSTANDROID_JNIMEDIAMANAGER_H
