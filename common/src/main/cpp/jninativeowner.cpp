/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include <string>

#include "jninativeowner.h"

namespace apl {
namespace jni {

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Unbind a native object.
 */
JNIEXPORT void JNICALL
Java_com_amazon_common_NativeBinding_nUnbind(JNIEnv *env, jclass clazz, jlong handle) {
    NativeOwner<>::unbind(handle);
}


/**
 * Test if a native object exists.
 */
JNIEXPORT jboolean JNICALL
Java_com_amazon_common_NativeBinding_nTestNativePeer(JNIEnv *env, jclass clazz,
jlong handle) {
    auto owner = NativeOwner<>::getNativeOwner(handle);
    auto hasPeer = owner->getBoundObject() != nullptr;
    return static_cast<jboolean >(hasPeer);
}

/**
 * Test if a native object exists.
 */
JNIEXPORT jint JNICALL
Java_com_amazon_common_NativeBinding_nTestPointerCount(JNIEnv *env, jclass clazz,
jlong handle) {
    auto owner = NativeOwner<>::getNativeOwner(handle);
    return static_cast<jint>(owner->getPointerCount());
}

class TestBoundObject {};

JNIEXPORT jlong JNICALL
Java_com_amazon_common_BindingTest_00024TestBoundObject_nTestBoundObjectCreate(JNIEnv *env, jobject thiz) {
    return createHandle<TestBoundObject>(std::make_shared<TestBoundObject>());
}

#ifdef __cplusplus
}
#endif

} //namespace jni
} //namespace apl
