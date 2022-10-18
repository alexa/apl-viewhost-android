/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */
#include <jni.h>

#include "apl/apl.h"
#include "documentsession.h"
#include <random>
#include "jniutil.h"
#include "jnidocumentsession.h"
#include "loggingbridge.h"


namespace apl {
namespace jni {

aplviewhost::DocumentSessionPtr
AndroidDocumentSession::create() {
    return std::make_shared<AndroidDocumentSession>();
}

AndroidDocumentSession::AndroidDocumentSession()
        : mExtensionSession(apl::ExtensionSession::create())
{}

std::string
AndroidDocumentSession::getId() const {
    std::lock_guard<std::mutex> lock(mMutex);
    return mExtensionSession->getSessionDescriptor()->getId();
}

bool
AndroidDocumentSession::hasEnded() const {
    std::lock_guard<std::mutex> lock(mMutex);
    return mExtensionSession->hasEnded();
}

void
AndroidDocumentSession::end() {
    std::lock_guard<std::mutex> lock(mMutex);
    if (mExtensionSession->hasEnded()) return;

    mExtensionSession->end();
}

#ifdef __cplusplus
extern "C" {
#endif

    JNIEXPORT jlong JNICALL
    Java_com_amazon_apl_android_DocumentSession_nCreate(JNIEnv *env, jobject thiz) {
        auto session_ = AndroidDocumentSession::create();
        return createHandle<aplviewhost::DocumentSession>(session_);
    }

    JNIEXPORT void JNICALL
    Java_com_amazon_apl_android_DocumentSession_nEnd(JNIEnv *env, jobject thiz,
                                                     jlong handler_) {
        auto session = get<aplviewhost::DocumentSession>(handler_);
        session->end();
    }

    JNIEXPORT jstring JNICALL
    Java_com_amazon_apl_android_DocumentSession_nGetId(JNIEnv *env, jobject thiz,
                                                       jlong handler_) {
        auto session = get<aplviewhost::DocumentSession>(handler_);
        return env->NewStringUTF(session->getId().c_str());
    }

    JNIEXPORT jboolean JNICALL
    Java_com_amazon_apl_android_DocumentSession_nHasEnded(JNIEnv *env, jobject thiz,
                                                          jlong handler_) {
        auto session = get<aplviewhost::DocumentSession>(handler_);
        return session->hasEnded();
    }

#ifdef __cplusplus
    }
#endif

} //namespace jni
} //namespace apl
