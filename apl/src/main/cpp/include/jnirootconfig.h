/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>

#ifndef ANDROID_JNIROOTCONFIG_H
#define ANDROID_JNIROOTCONFIG_H

#ifdef __cplusplus
extern "C" {
#endif


/**
* Initialize and cache java class and method handles for callback to the rendering layer.
*/
jboolean
rootconfig_OnLoad(JavaVM *vm, void *reserved __attribute__((__unused__)));

/**
* Release the class and method cache
*/
void
rootconfig_OnUnload(JavaVM *vm, void *reserved __attribute__((__unused__)));


#ifdef __cplusplus
}
#endif

#endif //ANDROID_JNIROOTCONFIG_H
