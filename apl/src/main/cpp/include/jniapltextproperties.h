/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#ifndef APLVIEWHOSTANDROID_JNIAPLTEXTPROPERTIES_H
#define APLVIEWHOSTANDROID_JNIAPLTEXTPROPERTIES_H

#include <jni.h>
#include "apl/apl.h"

#ifdef __cplusplus
extern "C" {
#endif
/**
*  Initialize and cache java class and method handles for callback to the rendering layer.
*/
jboolean
apltextproperties_OnLoad(JavaVM *vm, void *reserved);

/**
 * Release the class and method cache.
 */
void
apltextproperties_OnUnload(JavaVM *vm, void *reserved);

namespace apl {
    namespace jni {
        class APLTextProperties {
        public:
            APLTextProperties() {}

            ~APLTextProperties() {
                release();
            }

            void release();

            void setInstance(jobject instance) {
                mInstance = instance;
            }

            jobject getInstance() {
                return mInstance;
            }

            apl::sg::TextProperties* getCoreTextProperties() {
                return mCoreTextProperties;
            }

            void setCoreLayer(apl::sg::TextProperties* coreTextProperties) {
                mCoreTextProperties = coreTextProperties;
            }

        private:
            jobject mInstance;
            apl::sg::TextProperties* mCoreTextProperties;
        };
    } // namespace jni
} // namespace apl

#ifdef __cplusplus
}
#endif
#endif //APLVIEWHOSTANDROID_JNIAPLTEXTPROPERTIES_H
