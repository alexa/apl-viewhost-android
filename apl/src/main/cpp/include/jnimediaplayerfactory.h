/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#ifndef APLVIEWHOSTANDROID_JNIMEDIAPLAYERFACTORY_H
#define APLVIEWHOSTANDROID_JNIMEDIAPLAYERFACTORY_H
#ifdef __cplusplus
extern "C" {
#endif
/**
  *  Initialize and cache java class and method handles for callback to the rendering layer.
  */
jboolean mediaplayerfactory_OnLoad(JavaVM *vm, void *reserved);

/**
 * Release the class and method cache.
 */
void mediaplayerfactory_OnUnload(JavaVM *vm, void *reserved);

#ifdef __cplusplus
}
#endif
#endif //APLVIEWHOSTANDROID_JNIMEDIAPLAYERFACTORY_H
