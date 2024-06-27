/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include "jnimediamanager.h"
#include "jninativeowner.h"
#include "jnicomplexproperty.h"

#ifdef __cplusplus
extern "C" {
#endif
namespace apl {
    namespace jni {

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_media_MediaManager_nCreate(JNIEnv *env, jobject instance) {
            auto mediaManager = jniMediaManager::create(env->NewWeakGlobalRef(instance));
            return createHandle<apl::MediaManager>(mediaManager);
        }


        static jclass JAVA_MEDIAMANAGER_CLASS;
        static jmethodID JAVA_MEDIAMANAGER_REQUEST;
        static jmethodID JAVA_MEDIAMANAGER_RELEASE;
        static JavaVM * VM_REFERENCE;

        jboolean mediamanager_OnLoad(JavaVM *vm, void *reserved) {

            LOG(apl::LogLevel::kDebug) << "Loading View Host MediaManager JNI environment.";

            JNIEnv *env;
            if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return JNI_FALSE;
            }

            VM_REFERENCE = vm;

            JAVA_MEDIAMANAGER_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("com/amazon/apl/android/media/MediaManager")));

            JAVA_MEDIAMANAGER_REQUEST = env->GetMethodID(
                    JAVA_MEDIAMANAGER_CLASS,
                    "request",
                    "(Ljava/lang/String;I[Ljava/lang/String;J)V");

            if (nullptr == JAVA_MEDIAMANAGER_REQUEST) {
                return JNI_FALSE;
            }

            JAVA_MEDIAMANAGER_RELEASE = env->GetMethodID(
                    JAVA_MEDIAMANAGER_CLASS,
                    "release",
                    "(Ljava/lang/String;)V");

            if (nullptr == JAVA_MEDIAMANAGER_RELEASE) {
                return JNI_FALSE;
            }

            return JNI_TRUE;
        }

        void mediamanager_OnUnload(JavaVM *vm, void *reserved) {
            LOG(apl::LogLevel::kDebug) << "Unloading View Host MediaManager JNI environment.";
            apl::LoggerFactory::instance().reset();

            JNIEnv *env;
            if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            env->DeleteGlobalRef(JAVA_MEDIAMANAGER_CLASS);
        }

        apl::MediaManagerPtr
        jniMediaManager::create(jweak mediaManagerInstance) {
            auto jmm = std::make_shared<jniMediaManager>();
            jmm->mJavaMediaManagerInstance = mediaManagerInstance;
            return jmm;
        }

        apl::MediaObjectPtr
        jniMediaManager::request(const std::string &url, apl::EventMediaType type) {
            return request(url, type, {});
        }

        apl::MediaObjectPtr
        jniMediaManager::request(const std::string &url, apl::EventMediaType type,
                                 const apl::HeaderArray &headers) {
            auto it = mObjectMap.find(url);
            if (it != mObjectMap.end()) {
                auto ptr = it->second.lock();
                if (ptr)
                    return ptr;
            }

            JNIEnv *env;
            if (VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
                                     JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return nullptr;
            }

            jobject javaMediaManager = env->NewLocalRef(mJavaMediaManagerInstance);
            if (!javaMediaManager) {
                return nullptr;
            }

            auto ptr = jniMediaObject::create(shared_from_this(), url, type);
            mObjectMap.emplace(url, ptr);

            auto urlString = env->NewStringUTF(url.data());
            jobjectArray headersArray = getStringArray(env, headers);
            env->CallVoidMethod(javaMediaManager, JAVA_MEDIAMANAGER_REQUEST,
                                urlString, static_cast<int>(type), headersArray, reinterpret_cast<jlong>(ptr.get()));

            return ptr;
        }

        void
        jniMediaManager::release(jniMediaObject& jniMediaObject) {
            auto url = jniMediaObject.url();
            mObjectMap.erase(url);

            JNIEnv *env;
            if (VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
                                     JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            // Delete global ref to allow java side MediaObject to be GC'd
            auto javaMediaObject = jniMediaObject.getJavaMediaObject();
            if (javaMediaObject) {
                env->DeleteGlobalRef(javaMediaObject);
            }

            jobject javaMediaManager = env->NewLocalRef(mJavaMediaManagerInstance);
            if (!javaMediaManager) {
                return;
            }

            auto urlString = env->NewStringUTF(url.data());
            env->CallVoidMethod(javaMediaManager, JAVA_MEDIAMANAGER_RELEASE, urlString);
        }
    } // jni
} // apl

#ifdef __cplusplus
}
#endif