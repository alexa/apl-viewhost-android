/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>

#include "apl/apl.h"

#include "jniutil.h"
#include "jniaction.h"

namespace apl {
    namespace jni {

#ifdef __cplusplus
        extern "C" {
#endif

#pragma clang diagnostic push
#pragma ide diagnostic ignored "OCUnusedGlobalDeclarationInspection"
        const bool DEBUG_JNI = true;
#pragma clang diagnostic pop

        // Access the View Host Action class.
        static jclass ACTION_CLASS;
        static jmethodID ON_TERMINATE_CALLBACK;
        static jmethodID ON_THEN;
        static JavaVM* JAVA_VM;




        /**
         * Create a class and method cache for calls to View Host.
         */
        jboolean
        action_OnLoad(JavaVM *vm, void *reserved) {

            LOG(apl::LogLevel::kDebug) << "Loading View Host Action JNI environment.";

            JNIEnv *env;
            if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                return JNI_FALSE;
            }
            JAVA_VM = vm;

            // method signatures can be obtained with 'javap -s'
            ACTION_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("com/amazon/apl/android/Action")));
            ON_TERMINATE_CALLBACK = env->GetMethodID(ACTION_CLASS, "onTerminate", "()V");
            ON_THEN = env->GetMethodID(ACTION_CLASS, "onThen", "()V");

            Action::setUserDataReleaseCallback([](void* ptr) {
                auto* data = reinterpret_cast<rapidjson::Document*>(ptr);
                if(data) {
                    delete data;
                }
            });

            if (nullptr == ACTION_CLASS
                || nullptr == ON_TERMINATE_CALLBACK
                || nullptr == ON_THEN) {

                LOG(apl::LogLevel::kError)
                        << "Could not load methods for class com.amazon.apl.android.Action";
                return JNI_FALSE;
            }

            return JNI_TRUE;
        }


        /**
         * Release the class and method cache.
         */
        void
        action_OnUnload(JavaVM *vm, void *reserved) {
            LOG(apl::LogLevel::kDebug) << "Unloading View Host Action JNI environment.";
            apl::LoggerFactory::instance().reset();

            JNIEnv *env;
            if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                // environment failure, can't proceed.
                return;
            }

            env->DeleteGlobalRef(ACTION_CLASS);
        }

        /**
         * Create a Content instance and attach it to the view host peer.
         */
        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_Action_nInit(JNIEnv *env, jobject instance, jlong nativeHandle) {
            auto action = get<Action>(nativeHandle);
            if(!action) {
                LOG(apl::LogLevel::kWarn) << "Could not find action from handle " << nativeHandle;
                return;
            }

            auto weak = env->NewWeakGlobalRef(instance);
            action->addTerminateCallback([weak](const std::shared_ptr<Timers>&) {
                // May be called from a different thread than nInit necessitating a call to get this thread's JNIEnv
                JNIEnv* jniEnv;
                JAVA_VM->GetEnv(reinterpret_cast<void **>(&jniEnv), JNI_VERSION_1_6);
                if (!jniEnv) {
                    return;
                }

                auto local = jniEnv->NewLocalRef(weak);
                if(!local) {
                    return;
                }
                jniEnv->CallVoidMethod(local, ON_TERMINATE_CALLBACK);
                jniEnv->DeleteWeakGlobalRef(weak);
                jniEnv->DeleteLocalRef(local);
            });

            action->then([weak](const ActionPtr& action) {
                // May be called from a different thread than nInit necessitating a call to get this thread's JNIEnv
                JNIEnv* jniEnv;
                JAVA_VM->GetEnv(reinterpret_cast<void **>(&jniEnv), JNI_VERSION_1_6);
                if (!jniEnv) {
                    return;
                }

                auto local = jniEnv->NewLocalRef(weak);
                if(!local) {
                    return;
                }
                jniEnv->CallVoidMethod(local, ON_THEN);
                jniEnv->DeleteWeakGlobalRef(weak);
                jniEnv->DeleteLocalRef(local);
            });
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_Action_nIsPending(JNIEnv *env, jclass clazz, jlong nativeHandle) {
            auto action = get<Action>(nativeHandle);
            if(!action) {
                LOG(apl::LogLevel::kWarn) << "Could not find action from handle " << nativeHandle;
                return static_cast<jboolean>(false);
            }
            return static_cast<jboolean>(action->isPending());
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_Action_nIsTerminated(JNIEnv *env, jclass clazz, jlong nativeHandle) {
            auto action = get<Action>(nativeHandle);
            if(!action) {
                LOG(apl::LogLevel::kWarn) << "Could not find action from handle " << nativeHandle;
                return static_cast<jboolean>(false);
            }
            return static_cast<jboolean>(action->isTerminated());
        }

        JNIEXPORT jboolean JNICALL
        Java_com_amazon_apl_android_Action_nIsResolved(JNIEnv *env, jclass clazz, jlong nativeHandle) {
            auto action = get<Action>(nativeHandle);
            if(!action) {
                LOG(apl::LogLevel::kWarn) << "Could not find action from handle " << nativeHandle;
                return static_cast<jboolean>(false);
            }
            return static_cast<jboolean>(action->isResolved());
        }



#ifdef __cplusplus
        }
#endif

    } //namespace jni
} //namespace apl
