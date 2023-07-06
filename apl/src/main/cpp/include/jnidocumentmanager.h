/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#ifndef APLVIEWHOSTANDROID_JNIDOCUMENTMANAGER_H
#define APLVIEWHOSTANDROID_JNIDOCUMENTMANAGER_H
#include "apl/apl.h"

#ifdef __cplusplus
extern "C" {
#endif
/**
 * Initialize and cache java class and method handles for callback to the rendering layer.
 */
jboolean documentmanager_OnLoad(JavaVM *vm, void *reserved __attribute__((__unused__)));

/**
 * Release the class and method cache.
 */
void documentmanager_OnUnload(JavaVM *vm, void *reserved __attribute__((__unused__)));

namespace apl {
    namespace jni {
        class AndroidDocumentManager : public DocumentManager {
        public:
            AndroidDocumentManager() {}

            virtual ~AndroidDocumentManager();
            void setInstance(jobject instance);

            void request(const std::weak_ptr<EmbedRequest>& request,
                         EmbedRequestSuccessCallback success,
                         EmbedRequestFailureCallback error) override;
        private:
            jweak mInstance;
        };
    } // namespace jni
} // namespace apl

#ifdef __cplusplus
}
#endif
#endif //APLVIEWHOSTANDROID_JNIDOCUMENTMANAGER_H
