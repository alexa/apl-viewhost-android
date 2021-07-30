/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>

#ifndef ANDROID_JNICOMPLEXPROPERTY_H
#define ANDROID_JNICOMPLEXPROPERTY_H

#ifdef __cplusplus
extern "C" {
#endif

/**
*  Initialize and cache java class and method handles for callback to the rendering layer.
*/
jboolean
complexproperty_OnLoad(JavaVM *vm, void __unused *reserved);

/**
* Release the class and method cache.
*/
void
complexproperty_OnUnload(JavaVM *vm, void __unused *reserved);


#ifdef __cplusplus
}
#endif

#endif //ANDROID_JNICOMPLEXPROPERTY_H
