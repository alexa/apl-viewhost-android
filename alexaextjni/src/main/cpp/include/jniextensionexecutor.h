
/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#ifndef ANDROID_JNIEXTENSIONEXECUTOR_H
#define ANDROID_JNIEXTENSIONEXECUTOR_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 *  Initialize and cache java class and method handles for callback to the rendering layer.
 */
jboolean
extensionexecutor_OnLoad(JavaVM *vm, void *reserved);

/**
 * Release the class and method cache.
 */
void
extensionexecutor_OnUnload(JavaVM *vm, void *reserved);

#ifdef __cplusplus
}
#endif
#endif //ANDROID_JNIEXTENSIONEXECUTOR_H
