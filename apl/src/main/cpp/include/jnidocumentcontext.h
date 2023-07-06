/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#ifndef APLVIEWHOSTANDROID_JNIDOCUMENTCONTEXT_H
#define APLVIEWHOSTANDROID_JNIDOCUMENTCONTEXT_H
#include "apl/apl.h"

#ifdef __cplusplus
extern "C" {
#endif

namespace apl {
    namespace jni {
        /**
         * Given a core DocumentContext, return a unique identifier for the underlying object.
         * @param document The DocumentContext
         * @return an opaque identifier or 0 if the provided document context is null
         */
        jlong getDocumentContextId(const DocumentContextPtr document);

    } // namespace jni
} // namespace apl

#ifdef __cplusplus
}
#endif
#endif //APLVIEWHOSTANDROID_JNIDOCUMENTCONTEXT_H
