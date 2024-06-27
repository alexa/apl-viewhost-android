//
// Created by Das, Sourabh on 2022-09-30.
//

#ifndef APLVIEWHOSTANDROID_JNIAPLVIEW_H
#define APLVIEWHOSTANDROID_JNIAPLVIEW_H
#include "apl/apl.h"

#ifdef __cplusplus
extern "C" {
#endif
/**
*  Initialize and cache java class and method handles for callback to the rendering layer.
*/
jboolean
aplview_OnLoad(JavaVM *vm, void *reserved);

/**
 * Release the class and method cache.
 */
void
aplview_OnUnload(JavaVM *vm, void *reserved);

namespace apl {
    namespace jni {
        class JNIAPLView {
        public:
            JNIAPLView() {}

            ~JNIAPLView() {}

            void release();

            void setInstance(jobject instance) {
                mInstance = instance;
            }

            jobject getInstance() {
                return mInstance;
            }

        private:
            jobject mInstance;
        };
    } // namespace jni
} // namespace apl

#ifdef __cplusplus
}
#endif
#endif //APLVIEWHOSTANDROID_JNIAPLVIEW_H
