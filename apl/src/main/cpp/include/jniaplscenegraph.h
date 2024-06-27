//
// Created by Das, Sourabh on 2023-01-13.
//

#ifndef APLVIEWHOSTANDROID_JNIAPLSCENEGRAPH_H
#define APLVIEWHOSTANDROID_JNIAPLSCENEGRAPH_H

#include <jni.h>
#include <jnimetricstransform.h>
#include "apl/apl.h"
#include <utility>

#ifdef __cplusplus
extern "C" {
#endif

/**
*  Initialize and cache java class and method handles for callback to the rendering layer.
*/
jboolean
aplscenegraph_OnLoad(JavaVM *vm, void * reserved ) ;

/**
* Release the class and method cache.
*/
void
aplscenegraph_OnUnload(JavaVM *vm, void * reserved ) ;
#ifdef __cplusplus
}
#endif
#endif //APLVIEWHOSTANDROID_JNIAPLSCENEGRAPH_H
