
/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>


#ifndef ANDROID_JNIGRAPHIC_H
#define ANDROID_JNIGRAPHIC_H
#ifdef __cplusplus
extern "C" {
#endif

/**
*  Initialize and cache java class and method handles for callback to the rendering layer.
*/
jboolean
graphic_OnLoad(JavaVM *vm, void __unused *reserved);

/**
* Release the class and method cache.
*/
void
graphic_OnUnload(JavaVM *vm, void __unused *reserved);


#ifdef __cplusplus
}
#endif

#endif //ANDROID_JNIGRAPHIC_H
