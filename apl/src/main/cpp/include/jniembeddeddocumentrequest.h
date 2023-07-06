/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#ifndef APLVIEWHOSTANDROID_JNIEMBEDDEDDOCUMENTREQUEST_H
#define APLVIEWHOSTANDROID_JNIEMBEDDEDDOCUMENTREQUEST_H
#include "apl/apl.h"

#ifdef __cplusplus
extern "C" {
#endif

namespace apl {
    namespace jni {
        class AndroidEmbeddedDocumentRequest {
        public:
            std::shared_ptr<EmbedRequest> mEmbedRequest;
            EmbedRequestSuccessCallback mSuccessCallback;
            EmbedRequestFailureCallback mFailureCallback;
        };
    } // namespace jni
} // namespace apl

#ifdef __cplusplus
}
#endif
#endif //APLVIEWHOSTANDROID_JNIEMBEDDEDDOCUMENTREQUEST_H
