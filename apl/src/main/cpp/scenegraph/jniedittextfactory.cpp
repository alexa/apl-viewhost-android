/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */


#include <jni.h>
#include "jniutil.h"
#include "jniedittext.h"
#include "jniedittextfactory.h"

#ifdef SCENEGRAPH
namespace apl {
    namespace jni {
#ifdef __cplusplus
        extern "C" {
#endif

        static jclass EDITTEXTFACTORY_CLASS;
        static jmethodID EDITTEXTFACTORY_CREATE_EDITTEXT;
        static JavaVM *EDITTEXTFACTORY_VM_REFERENCE;

        /**
         * Create a class and method cache for calls to View Host.
         */
        jboolean
        edittextfactory_OnLoad(JavaVM *vm, void *reserved) {

            LOG(apl::LogLevel::kDebug) << "Loading View Host jniedittextfactory JNI environment.";

            EDITTEXTFACTORY_VM_REFERENCE = vm;
            JNIEnv *env;
            if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return JNI_FALSE;
            }

            EDITTEXTFACTORY_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("com/amazon/apl/android/scenegraph/edittext/EditTextFactory")));

            EDITTEXTFACTORY_CREATE_EDITTEXT = env->GetMethodID(
                    EDITTEXTFACTORY_CLASS,
                    "createEditText",
                    "(J)Lcom/amazon/apl/android/scenegraph/edittext/EditText;");
            return JNI_TRUE;
        }

        /**
         * Release the class and method cache.
         */
        void
        edittextfactory_OnUnload(JavaVM *vm, void *reserved) {
            LOG(apl::LogLevel::kDebug) << "Unloading View Host jniedittextfactory JNI environment.";
            apl::LoggerFactory::instance().reset();

            JNIEnv *env;
            if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            env->DeleteGlobalRef(EDITTEXTFACTORY_CLASS);
        }

        class APLEditTextFactory : public apl::sg::EditTextFactory {
        public:
            explicit APLEditTextFactory(jweak weakInstance) : mWeakInstance(weakInstance) {}

            ~APLEditTextFactory() override {
                JNIEnv *env;
                if (EDITTEXTFACTORY_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
                                                         JNI_VERSION_1_6) != JNI_OK) {
                    LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                    return;
                }
                env->DeleteWeakGlobalRef(mWeakInstance);
            }

            sg::EditTextPtr createEditText(sg::EditTextSubmitCallback submitCallback,
                                           sg::EditTextChangedCallback changedCallback,
                                           sg::EditTextFocusCallback focusCallback) {
                JNIEnv *env;
                if (EDITTEXTFACTORY_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
                                                         JNI_VERSION_1_6) != JNI_OK) {
                    LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                    return nullptr;
                }

                jobject localRef = env->NewLocalRef(mWeakInstance);
                if (!localRef) {
                    return nullptr;
                }

                auto editText_ = std::make_shared<APLEditText>(std::move(submitCallback),
                                                               std::move(changedCallback),
                                                               std::move(focusCallback));
                auto editTextHandler = createHandle<sg::EditText>(editText_);
                if (editTextHandler == 0) return nullptr;

                auto instance = env->CallObjectMethod(localRef, EDITTEXTFACTORY_CREATE_EDITTEXT,
                                                      editTextHandler);
                editText_->setInstance(instance);
                auto editText = get<sg::EditText>(editTextHandler);

                return editText;
            }
        private:
            jweak mWeakInstance;
        };

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_scenegraph_edittext_EditTextFactory_nCreate(JNIEnv *env, jobject instance) {
            auto editTextFactory_ = std::make_shared<APLEditTextFactory>(
                    env->NewWeakGlobalRef(instance));
            return createHandle<APLEditTextFactory>(editTextFactory_);
        }
#endif
#ifdef __cplusplus
        }
#endif
    } //namespace jni
} //namespace apl