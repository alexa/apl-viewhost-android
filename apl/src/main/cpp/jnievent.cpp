/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include "apl/apl.h"
#include "jnievent.h"
#include "jniutil.h"
#include "jnimetricstransform.h"
#include "jnidocumentcontext.h"

namespace apl {
    namespace jni {

#ifdef __cplusplus
        extern "C" {
#endif
#pragma clang diagnostic push
#pragma ide diagnostic ignored "OCUnusedGlobalDeclarationInspection"
        const bool DEBUG_JNI = true;
#pragma clang diagnostic pop

        static jclass EVENT_CLASS;
        static jmethodID ACTION_ON_TERMINATE;
        static JavaVM* JAVA_VM;

        jboolean
        event_OnLoad(JavaVM *vm, void *reserved) {
            LOG(apl::LogLevel::kDebug) << "Loading View Host Event JNI environment.";

            JAVA_VM = vm;
            JNIEnv *env;
            if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                return JNI_ERR;
            }

            // method signatures can be obtained with 'javap -s'
            EVENT_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("com/amazon/apl/android/Event")));
            ACTION_ON_TERMINATE = env->GetMethodID(EVENT_CLASS, "onTerminate", "()V");

            if (nullptr == EVENT_CLASS || nullptr == ACTION_ON_TERMINATE) {
                LOG(apl::LogLevel::kError)
                        << "Could not load methods for class com.amazon.apl.android.Event";
                return JNI_FALSE;
            }

            // Clean up the JNI references
            Event::setUserDataReleaseCallback([](void* userData) {
                JNIEnv* jniEnv;
                JAVA_VM->GetEnv(reinterpret_cast<void **>(&jniEnv), JNI_VERSION_1_6);
                if (!jniEnv) {
                    return;
                }
                if (userData) {
                    jniEnv->DeleteWeakGlobalRef((jweak) userData);
                }
            });

            return JNI_TRUE;
        }

        void
        event_OnUnload(JavaVM *vm, void *reserved) {
            LOG(apl::LogLevel::kDebug) << "Unloading View Host Event JNI environment.";
            apl::LoggerFactory::instance().reset();
            JNIEnv *env;
            if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                // environment failure, can't proceed.
                return;
            }

        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_Event_nInit(JNIEnv *env, jobject instance,
                                                jlong eventHandle) {
            auto event = get<Event>(eventHandle);
            //sometimes there is no action associated with an event.
            if(event->getActionRef().isEmpty()) {
                return;
            }

            jweak weakRef = env->NewWeakGlobalRef(instance);
            // Pass in the weakRef as UserData for clean-up later
            event->setUserData(weakRef);

            // ActionRef might outlive the Event, so sent a weak_ref in the callback
            auto weakEvent = std::weak_ptr<Event>(event);
            event->getActionRef().addTerminateCallback([weakEvent](const std::shared_ptr<Timers>&) {
                // Call the terminate callback only if the Event is still in memory
                if (auto event = weakEvent.lock()) {
                    // May be called from a different thread than nInit necessitating a call to get this thread's JNIEnv
                    JNIEnv* jniEnv;
                    JAVA_VM->GetEnv(reinterpret_cast<void **>(&jniEnv), JNI_VERSION_1_6);
                    if (!jniEnv) {
                        return;
                    }

                    auto weakRef = (jweak) event->getUserData();

                    auto local = jniEnv->NewLocalRef(weakRef);
                    if (!local) {
                        return;
                    }
                    jniEnv->CallVoidMethod(local, ACTION_ON_TERMINATE);
                    jniEnv->DeleteLocalRef(local);
                }
            });
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_Event_nGetType(JNIEnv *env, jclass clazz,
                                                   jlong eventHandle) {
            auto event = get<Event>(eventHandle);
            return static_cast<jint>(event->getType());
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_Event_nGetDocumentContextId(JNIEnv *env,
                                                               jclass clazz,
                                                               jlong eventHandle) {
            auto event = get<Event>(eventHandle);
            return getDocumentContextId(event->getDocument());
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_Event_nGetComponentId(JNIEnv *env, jclass clazz,
                                                          jlong eventHandle) {
            auto event = get<Event>(eventHandle);
            if(!event->getComponent()) {
                return env->NewStringUTF("");
            }
            return env->NewStringUTF(event->getComponent()->getUniqueId().c_str());
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_Event_nResolve(JNIEnv *env, jclass clazz,
                                                   jlong eventHandle) {
            auto event = get<Event>(eventHandle);
            if (!event->getActionRef().isEmpty()) {
                event->getActionRef().resolve();
            }
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_Event_nResolveArg(JNIEnv *env, jclass clazz,
                                                   jlong eventHandle, jint arg) {

            auto event = get<Event>(eventHandle);
            if (!event->getActionRef().isEmpty()) {
                event->getActionRef().resolve(static_cast<int>(arg));
            }
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_Event_nResolveRect(JNIEnv *env, jclass clazz,
                                                      jlong eventHandle,
                                                      jint x, jint y, jint w, jint h) {
            auto event = get<Event>(eventHandle);
            event->getActionRef().resolve(Rect(
                    static_cast<int>(x),
                    static_cast<int>(y),
                    static_cast<int>(w),
                    static_cast<int>(h)));
        }

#ifdef __cplusplus
        }
#endif

    }
}
