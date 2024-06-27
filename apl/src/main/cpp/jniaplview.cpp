//
// Created by Das, Sourabh on 2022-09-30.
//

#include <jni.h>
#include "jniaplview.h"
#include "jniutil.h"

namespace apl {
    namespace jni {
#ifdef __cplusplus
extern "C" {
#endif

    static JavaVM* APLVIEW_VM_REFERENCE;
    static jclass APLVIEW_CLASS;

    /**
     * Create a class and method cache for calls to View Host.
     */
    jboolean
    aplview_OnLoad(JavaVM *vm, void *reserved) {

        LOG(apl::LogLevel::kDebug) << "Loading View Host Component JNI environment.";

        JNIEnv *env;
        if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
            LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
            return JNI_FALSE;
        }

        APLVIEW_VM_REFERENCE = vm;

        APLVIEW_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                env->FindClass("com/amazon/apl/android/views/APLView")));
        return JNI_TRUE;
    }

    /**
     * Release the class and method cache.
     */
    void
    aplview_OnUnload(JavaVM *vm, void *reserved) {
        LOG(apl::LogLevel::kDebug) << "Unloading View Host Component JNI environment.";
        apl::LoggerFactory::instance().reset();

        JNIEnv *env;
        if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
            LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
            return;
        }

        env->DeleteGlobalRef(APLVIEW_CLASS);
    }

    void JNIAPLView::release() {
        JNIEnv *env;
        if (APLVIEW_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
                                             JNI_VERSION_1_6) != JNI_OK) {
            LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
            return;
        }

        LOG(apl::LogLevel::kError) << "Deleting global reference";
        env->DeleteGlobalRef(mInstance);
    }
#ifdef __cplusplus
        }
#endif
    } //namespace jni
} //namespace apl
