/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include <queue>
#include <mutex>

#include "alexaext/alexaext.h"
#include "jniextensionexecutor.h"
#include "jninativeowner.h"

namespace alexaext {
    namespace jni {

#ifdef __cplusplus

        extern "C" {
#endif
        using namespace alexaext;

        static jclass EXTENSIONEXECUTOR_CLASS;
        static jmethodID EXTENSIONEXECUTOR_ON_TASK_ADDED;
        static JavaVM *EXECUTOR_VM_REFERENCE;

        jboolean
        extensionexecutor_OnLoad(JavaVM *vm, void *reserved) {
            JNIEnv *env;
            if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                return JNI_FALSE;
            }

            EXECUTOR_VM_REFERENCE = vm;

            // method signatures can be obtained with 'javap -s'
            EXTENSIONEXECUTOR_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("com/amazon/alexaext/ExtensionExecutor")));

            EXTENSIONEXECUTOR_ON_TASK_ADDED = env->GetMethodID(
                    EXTENSIONEXECUTOR_CLASS,
                    "onTaskAddedInternal",
                    "()V");

            if (nullptr == EXTENSIONEXECUTOR_ON_TASK_ADDED) {
                return JNI_FALSE;
            }

            return JNI_TRUE;
        }

        void
        extensionexecutor_OnUnload(JavaVM *vm, void *reserved) {
            JNIEnv *env;
            if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                // environment failure, can't proceed.
                return;
            }

            env->DeleteGlobalRef(EXTENSIONEXECUTOR_CLASS);
        }

        /**
         * Implements the extension executor contract, acting as a bridge between the C++ extension
         * framework and the Java ExtensionExecutor class. The Java ExtensionExecutor class ensures
         * that tasks are executed on the appropriate (core) thread.
         *
         * This class buffers tasks in in a local queue (mPendingTasks) and notifies the Java class
         * whenever a new task was added via onTaskAdded(). When the Java ExtensionExecutor instance
         * is ready to execute the tasks, it calls executePending() on the appropriate thread.
         */
        class ExtensionExecutor : public alexaext::Executor {
        public:
            ExtensionExecutor(jweak instance) : mWeakInstance(instance) {}

            ~ExtensionExecutor() {
                JNIEnv *env;
                if (EXECUTOR_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                    return;
                }
                env->DeleteWeakGlobalRef(mWeakInstance);
            }

            /**
             * Called by the extension framework when a new task needs to be executed. It adds the
             * task to the executor's queue, notifies Java ExtensionExecutor via onTaskAdded() and
             * returns immediately (does not block to actually perform the task).
             */
            bool enqueueTask(Task task) override {
                std::lock_guard<std::recursive_mutex> guard(mTasksMutex);
                mPendingTasks.push(std::move(task));
                onTaskAdded();
                return true;
            }

            /**
             * Called by Java ExtensionExecutor class to execute any queued tasks.
             */
            void executePending() {
                std::lock_guard<std::recursive_mutex> guard(mTasksMutex);
                while (!mPendingTasks.empty()) {
                    mPendingTasks.front()();
                    mPendingTasks.pop();
                }
            }

        private:
            void onTaskAdded() {
                JNIEnv *env;
                if (EXECUTOR_VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                    return;
                }

                jobject localRef = env->NewLocalRef(mWeakInstance);
                if (!localRef) {
                    return;
                }

                env->CallVoidMethod(localRef, EXTENSIONEXECUTOR_ON_TASK_ADDED);
                env->DeleteLocalRef(localRef);
            }

        private:
            jweak mWeakInstance;
            std::queue<Task> mPendingTasks;
            std::recursive_mutex mTasksMutex;
        };

        JNIEXPORT jlong JNICALL
        Java_com_amazon_alexaext_ExtensionExecutor_nCreate(JNIEnv *env, jobject thiz) {
            auto extensionExecutor_ = std::make_shared<ExtensionExecutor>(env->NewWeakGlobalRef(thiz));
            return apl::jni::createHandle<ExtensionExecutor>(extensionExecutor_);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_alexaext_ExtensionExecutor_nExecuteTasks(JNIEnv *env, jobject thiz, jlong _handler) {
            auto executor = apl::jni::get<ExtensionExecutor>(_handler);
            executor->executePending();
        }

#ifdef __cplusplus
        }
#endif
    } //namespace jni
} //namespace apl
