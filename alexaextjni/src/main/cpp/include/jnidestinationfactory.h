
/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#ifndef ANDROID_JNIDESTINATIONFACTORY_H
#define ANDROID_JNIDESTINATIONFACTORY_H

#include <jni.h>

#include <queue>

#include "alexaext/alexaext.h"
#include "jninativeowner.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Initialize and cache java class and method handles for callback to the rendering layer.
 */
jboolean destinationfactory_OnLoad(JavaVM* vm, void* reserved __attribute__((__unused__)));

/**
 * Release the class and method cache
 */
void destinationfactory_OnUnload(JavaVM* vm, void* reserved __attribute__((__unused__)));

namespace alexaext {
namespace jni {

class DestinationFactory : public metricsExtensionV2::DestinationFactoryInterface {
public:
    std::shared_ptr<metricsExtensionV2::DestinationInterface> createDestination(
            const rapidjson::Value& settings) override;

private:
    jweak mInstance = nullptr;
};
}  // namespace jni
}  // namespace alexaext

#ifdef __cplusplus
}
#endif

#endif  // ANDROID_JNIDESTINATIONFACTORY_H
