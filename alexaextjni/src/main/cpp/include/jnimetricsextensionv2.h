
/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#ifndef ANDROID_JNIMETRICSEXTENSIONV2_H
#define ANDROID_JNIMETRICSEXTENSIONV2_H

#include <jni.h>

#include "alexaext/alexaext.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Initialize and cache java class and method handles for callback to the rendering layer.
 */
jboolean metricsextensionV2_OnLoad(JavaVM* vm, void* reserved __attribute__((__unused__)));

/**
 * Release the class and method cache
 */
void metricsextensionV2_OnUnload(JavaVM* vm, void* reserved __attribute__((__unused__)));

#ifdef __cplusplus
}
#endif

#endif  // ANDROID_JNIMETRICSEXTENSIONV2_H
