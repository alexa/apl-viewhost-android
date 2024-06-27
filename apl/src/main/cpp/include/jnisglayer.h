/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>

#ifndef ANDROID_JNISGLAYER_H
#define ANDROID_JNISGLAYER_H

#ifdef __cplusplus
extern "C" {
#endif

/**
*  Initialize and cache java class and method handles for callback to the rendering layer.
*/
jboolean
sglayer_OnLoad(JavaVM *vm, void *reserved);

/**
* Release the class and method cache.
*/
void
sglayer_OnUnload(JavaVM *vm, void *reserved);

#ifdef __cplusplus
}
#endif

#endif //ANDROID_JNISGLAYER_H
