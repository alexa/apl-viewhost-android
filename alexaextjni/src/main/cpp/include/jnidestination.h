
/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#ifndef ANDROID_JNIDESTINATION_H
#define ANDROID_JNIDESTINATION_H

#include <jni.h>

#include "alexaext/alexaext.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Initialize and cache java class and method handles for callback to the rendering layer.
 */
jboolean destination_OnLoad(JavaVM* vm, void* reserved __attribute__((__unused__)));

/**
 * Release the class and method cache
 */
void destination_OnUnload(JavaVM* vm, void* reserved __attribute__((__unused__)));

namespace alexaext {
namespace jni {

class Destination : public metricsExtensionV2::DestinationInterface {
public:
    void publish(metricsExtensionV2::Metric metric) override;
    void publish(std::vector<metricsExtensionV2::Metric> metrics) override;

private:
    jweak mInstance = nullptr;
};
}  // namespace jni
}  // namespace alexaext

#ifdef __cplusplus
}
#endif

#endif  // ANDROID_JNIDESTINATION_H
