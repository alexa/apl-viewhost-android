/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>

#ifndef ANDROID_JNISGCONTENT_H
#define ANDROID_JNISGCONTENT_H

#ifdef __cplusplus
extern "C" {
#endif

/**
*  Initialize and cache java class and method handles for callback to the rendering layer.
*/
jboolean
sgcontent_OnLoad(JavaVM *vm, void *reserved);

/**
* Release the class and method cache.
*/
void
sgcontent_OnUnload(JavaVM *vm, void *reserved);

#ifdef __cplusplus
}
#endif

#endif //ANDROID_JNISGCONTENT_H
