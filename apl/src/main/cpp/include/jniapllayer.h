//
// Created by Das, Sourabh on 2023-01-12.
//

#ifndef APLVIEWHOSTANDROID_JNIAPLLAYER_H
#define APLVIEWHOSTANDROID_JNIAPLLAYER_H
#include "apl/apl.h"

#ifdef __cplusplus
extern "C" {
#endif
/**
*  Initialize and cache java class and method handles for callback to the rendering layer.
*/
jboolean
apllayer_OnLoad(JavaVM *vm, void *reserved);

/**
 * Release the class and method cache.
 */
void
apllayer_OnUnload(JavaVM *vm, void *reserved);

#ifdef SCENEGRAPH
namespace apl {
    namespace jni {
        class APLLayer {
        public:
            APLLayer() {}

            ~APLLayer() {
                release();
            }

            void updateDirtyProperties(int flags);

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
#endif

#ifdef __cplusplus
}
#endif
#endif //APLVIEWHOSTANDROID_JNIAPLLAYER_H
