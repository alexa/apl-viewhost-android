/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */


#include <jni.h>
#include "jniutil.h"
#include "jniedittext.h"

namespace apl {
    namespace jni {
#ifdef __cplusplus
        extern "C" {
#endif

        static jclass EDITTEXT_CLASS;
        static JavaVM *EDITTEXT_VM_REFERENCE;

        /**
         * Create a class and method cache for calls to View Host.
         */
        jboolean
        edittext_OnLoad(JavaVM *vm, void *reserved) {

            LOG(apl::LogLevel::kDebug) << "Loading View Host jniedittext JNI environment.";

            EDITTEXT_VM_REFERENCE = vm;
            JNIEnv *env;
            if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return JNI_FALSE;
            }

            return JNI_TRUE;
        }

        /**
         * Release the class and method cache.
         */
        void
        edittext_OnUnload(JavaVM *vm, void *reserved) {
            LOG(apl::LogLevel::kDebug) << "Unloading View Host jniedittext JNI environment.";
            apl::LoggerFactory::instance().reset();

            JNIEnv *env;
            if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            env->DeleteGlobalRef(EDITTEXT_CLASS);
        }

        APLEditText::APLEditText(sg::EditTextSubmitCallback submitCallback,
                                 sg::EditTextChangedCallback changedCallback,
                                 sg::EditTextFocusCallback focusCallback) :
                sg::EditText(std::move(submitCallback),
                             std::move(changedCallback),
                             std::move(focusCallback)) {}

        void
        APLEditText::release() {
            JNIEnv *env;
            if (EDITTEXT_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
                                              JNI_VERSION_1_6) != JNI_OK) {
                return;
            }

            env->DeleteGlobalRef(mInstance);
        }

        void
        APLEditText::setInstance(jobject instance) {
            JNIEnv *env;
            if (EDITTEXT_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
                                              JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            // TODO consider refactoring this such that the Java instance is passed into the constructor
            mInstance = env->NewGlobalRef(instance);
        }

        void APLEditText::setFocus(bool hasFocus) {

        }

        void
        APLEditText::doSubmit() {
            if (mSubmitCallback) {
                mSubmitCallback();
            }
        }

        void
        APLEditText::doChanged(const std::string& text) {
            if (mChangedCallback) {
                mChangedCallback(text);
            }
        }

        void
        APLEditText::doFocused(bool focused) {
            if (mFocusCallback) {
                mFocusCallback(focused);
            }
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_scenegraph_edittext_EditText_nSubmit(JNIEnv *env,
                                                                         jclass clazz,
                                                                         jlong handle) {
            auto edittext = get<APLEditText>(handle);
            if (edittext) {
                edittext->doSubmit();
            }
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_scenegraph_edittext_EditText_nTextChanged(JNIEnv *env,
                                                                              jclass clazz,
                                                                              jlong handle,
                                                                              jstring text) {
            auto edittext = get<APLEditText>(handle);
            const char *newString = env->GetStringUTFChars(text, nullptr);
            if (edittext) {
                edittext->doChanged(newString);
            }
            env->ReleaseStringUTFChars(text, newString);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_scenegraph_edittext_EditText_nFocusChanged(JNIEnv *env,
                                                                               jclass clazz,
                                                                               jlong handle,
                                                                               jboolean focused) {
            auto edittext = get<APLEditText>(handle);
            edittext->doFocused(focused);
        }

#ifdef __cplusplus
        }
#endif
    } //namespace jni
} //namespace apl