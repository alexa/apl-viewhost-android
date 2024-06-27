/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include "jnimediaobject.h"
#include "jnimediamanager.h"
#include "apl/apl.h"
#include "jninativeowner.h"

namespace apl {
    namespace jni {
        using CallbackID = int;
        std::shared_ptr<jniMediaObject>
        jniMediaObject::create(std::shared_ptr<jniMediaManager> mediaManager, std::string url, apl::EventMediaType type)
        {
            std::shared_ptr<jniMediaObject> self;
            self = std::make_shared<jniMediaObject>(mediaManager, std::move(url), type);
            return self;
        }

        jniMediaObject::jniMediaObject(std::weak_ptr<jniMediaManager> mediaManager, std::string url, apl::EventMediaType type)
                : mMediaManager(std::move(mediaManager)),
                  mURL(std::move(url)),
                  mType(type)
        {
        }

        jniMediaObject::~jniMediaObject()
        {
            LOG(apl::LogLevel::kInfo) << "Deconstructing object for " << mURL;

            auto manager = mMediaManager.lock();
            if (manager)
                manager->release(*this);
        }

        CallbackID
        jniMediaObject::addCallback(apl::MediaObjectCallback callback)
        {
            if (mState != kPending)
                return 0;

            mCallbackId += 1;
            mCallbacks.emplace(mCallbackId, std::move(callback));
            return mCallbackId;
        }

        void
        jniMediaObject::removeCallback(CallbackID callbackId)
        {
            mCallbacks.erase(callbackId);
        }

        void
        jniMediaObject::runCallbacks()
        {
            LOG(apl::LogLevel::kInfo) << "Running callbacks for " << mURL;
            auto self = shared_from_this();
            for (const auto& m : mCallbacks)
                m.second(self);

            mCallbacks.clear();
        }

#ifdef __cplusplus
        extern "C" { // JNI methods
#endif
        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_media_MediaObject_nBind(JNIEnv *env, jclass clazz, jlong nativeHandle, jobject instance) {
            auto mediaObject = reinterpret_cast<jniMediaObject *>(nativeHandle);
            if (mediaObject) {
                mediaObject->setJavaMediaObject(env->NewGlobalRef(instance));
            }
        }

        JNIEXPORT jobject JNICALL
        Java_com_amazon_apl_android_media_MediaObject_nGetJavaObject(JNIEnv *env, jclass clazz,
                                                                     jlong nativeHandle) {
            auto mediaObject = reinterpret_cast<jniMediaObject *>(nativeHandle);
            if (mediaObject) {
                return mediaObject->getJavaMediaObject();
            }
            return nullptr;
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_media_MediaObject_nOnLoad(JNIEnv *env, jobject clazz,
                                                                     jlong nativeHandle, jint width, jint height) {
            auto mediaObject = reinterpret_cast<jniMediaObject *>(nativeHandle);
            mediaObject->onLoad(width, height);
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_media_MediaObject_nOnError(JNIEnv *env, jobject thiz,
                                                               jlong nativeHandle, jint errorCode,
                                                               jbyteArray errorDescription) {
            auto mediaObject = reinterpret_cast<jniMediaObject *>(nativeHandle);
            mediaObject->onError(errorCode, std::string((char *)env->GetByteArrayElements(errorDescription,
                                                                                  nullptr)));
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_media_MediaObject_nGetUrl(JNIEnv *env, jclass clazz,
                                                              jlong nativeHandle) {
            auto mediaObject = reinterpret_cast<jniMediaObject *>(nativeHandle);
            return env->NewStringUTF(mediaObject->url().data());
        }
#ifdef __cplusplus
}
#endif

} // namespace jni
} // namespace apl
