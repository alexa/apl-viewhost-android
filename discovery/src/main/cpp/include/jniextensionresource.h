
/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#ifndef JNI_EXTENSIONRESOURCE_H
#define JNI_EXTENSIONRESOURCE_H

#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>
#include "alexaext/alexaext.h"

/**
 *  Initialize and cache java class and method handles for callback to the rendering layer.
 */
jboolean
extensionresource_OnLoad(JavaVM *vm, void *reserved);

/**
 * Release the class and method cache.
 */
void
extensionresource_OnUnload(JavaVM *vm, void *reserved);

namespace alexaext {
    namespace jni {


        class JNIResourceHolder : public alexaext::ResourceHolder {
        public:
            JNIResourceHolder(std::string resourceId, jobject instance);

            ~JNIResourceHolder() override;

            jweak mWeakInstance;
        };


#ifdef __cplusplus
}
#endif

    } //namespace jni
} //namespace apl

#endif //JNI_EXTENSIONRESOURCE_H
