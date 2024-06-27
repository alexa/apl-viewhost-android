/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

#include <jni.h>

#include "jniextensionexecutor.h"
#include "jniextensionproxy.h"
#include "jniextensionregistrar.h"
#include "jniextensionresource.h"
#include "jnimetricsextensionv2.h"
#include "jnidestinationfactory.h"
#include "jnidestination.h"

#ifdef __cplusplus
extern "C" {
#endif

namespace alexaext {
namespace jni {

/**
 * Initialize and cache java class and method handles for callback to the rendering layer.
 * The VM calls JNI_OnLoad when the native library is loaded (for example, through System.loadLibrary).
 */
JNIEXPORT jint
JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return -1;  //JNI_ERR
    }

    jboolean extensionExecutorLoaded = extensionexecutor_OnLoad(vm, reserved);
    jboolean extensionProxyLoaded = extensionproxy_OnLoad(vm, reserved);
    jboolean extensionProviderLoaded = extensionprovider_OnLoad(vm, reserved);
    jboolean extensionResourceProviderLoaded = extensionresource_OnLoad(vm, reserved);
    jboolean metricsextensionV2Loaded = metricsextensionV2_OnLoad(vm, reserved);
    jboolean destinationfactoryLoaded = destinationfactory_OnLoad(vm, reserved);
    jboolean destinationLoaded = destination_OnLoad(vm, reserved);

    if (!extensionProxyLoaded || !extensionProviderLoaded || !extensionResourceProviderLoaded
        || !extensionExecutorLoaded || !metricsextensionV2Loaded
        || !destinationfactoryLoaded || !destinationLoaded) {
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}

/**
 * Release the class and method cache.
 * The VM calls JNI_OnUnload when the class loader containing the native library is garbage collected.
 */
JNIEXPORT void
JNI_OnUnload(JavaVM *vm, void *reserved) {
    extensionexecutor_OnUnload(vm, reserved);
    extensionproxy_OnUnload(vm, reserved);
    extensionprovider_OnUnload(vm, reserved);
    extensionresource_OnUnload(vm, reserved);
    metricsextensionV2_OnUnload(vm, reserved);
    destinationfactory_OnUnload(vm, reserved);
    destination_OnUnload(vm, reserved);
}

} //namespace jni
} //namespace alexaext

#ifdef __cplusplus
}
#endif