
/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#ifndef ANDROID_JNIEXTENSIONPROVIDER_H
#define ANDROID_JNIEXTENSIONPROVIDER_H
#ifdef __cplusplus
extern "C" {
#endif

/**
 *  Initialize and cache java class and method handles for callback to the rendering layer.
 */
jboolean
extensionprovider_OnLoad(JavaVM *vm, void *reserved);

/**
 * Release the class and method cache.
 */
void
extensionprovider_OnUnload(JavaVM *vm, void *reserved);

#ifdef __cplusplus
}
#endif
#endif //ANDROID_JNIEXTENSIONPROVIDER_H
