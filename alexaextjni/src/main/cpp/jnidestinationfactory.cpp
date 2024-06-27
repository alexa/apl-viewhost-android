/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>

#include <string>

#include "alexaext/alexaext.h"
#include "jninativeowner.h"
#include "rapidjson/document.h"

namespace alexaext {
namespace jni {

#ifdef __cplusplus
extern "C" {
#endif

static jclass DESTINATIONFACTORY_CLASS;
static jmethodID DESTINATIONFACTORY_CREATE_DESTINATION;
static JavaVM* DESTINATIONFACTORY_VM_REFERENCE;

jboolean destinationfactory_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_FALSE;
    }

    DESTINATIONFACTORY_VM_REFERENCE = vm;

    // method signatures can be obtained with 'javap -s'
    DESTINATIONFACTORY_CLASS = reinterpret_cast<jclass>(
            env->NewGlobalRef(env->FindClass("com/amazon/alexaext/metricsextensionv2/DestinationFactory")));
    if (DESTINATIONFACTORY_CLASS == nullptr) {
        return JNI_FALSE;
    }

    DESTINATIONFACTORY_CREATE_DESTINATION =
            env->GetMethodID(DESTINATIONFACTORY_CLASS, "createDestinationInternal", "(Ljava/lang/String;)J");
    if (DESTINATIONFACTORY_CREATE_DESTINATION == nullptr) {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

void destinationfactory_OnUnload(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK) {
        // environment failure, can't proceed.
        return;
    }

    env->DeleteGlobalRef(DESTINATIONFACTORY_CLASS);
}

#define ENV_CREATE(FAIL_RETURN)                                                                               \
    JNIEnv* env;                                                                                              \
    if (DESTINATIONFACTORY_VM_REFERENCE->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) { \
        return FAIL_RETURN;                                                                                   \
    }                                                                                                         \
                                                                                                              \
    jobject localRef = env->NewLocalRef(mWeakInstance);                                                       \
    if (!localRef) {                                                                                          \
        return FAIL_RETURN;                                                                                   \
    }

#define ENV_CLEAR() env->DeleteLocalRef(localRef)

class DestinationFactory : public metricsExtensionV2::DestinationFactoryInterface {
public:
    DestinationFactory(jobject instance) {
        JNIEnv* env;
        if (DESTINATIONFACTORY_VM_REFERENCE->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
            return;
        }
        mWeakInstance = env->NewWeakGlobalRef(instance);
    }

    ~DestinationFactory() {
        JNIEnv* env;
        if (DESTINATIONFACTORY_VM_REFERENCE->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
            return;
        }
        env->DeleteWeakGlobalRef(mWeakInstance);
    }

    std::shared_ptr<metricsExtensionV2::DestinationInterface> createDestination(const rapidjson::Value& settings) {
        ENV_CREATE(nullptr);

        jstring settingsString = env->NewStringUTF(AsString(settings).c_str());

        jlong destinationHandle = env->CallLongMethod(localRef, DESTINATIONFACTORY_CREATE_DESTINATION, settingsString);
        env->DeleteLocalRef(settingsString);
        if (destinationHandle == -1) {
            ENV_CLEAR();
            return nullptr;
        }
        ENV_CLEAR();
        return apl::jni::get<metricsExtensionV2::DestinationInterface>(destinationHandle);
    }

private:
    jweak mWeakInstance;
};

JNIEXPORT jlong JNICALL
Java_com_amazon_alexaext_metricsextensionv2_DestinationFactory_nCreate(JNIEnv* env, jobject instance) {
    auto destinationFactory = std::make_shared<DestinationFactory>(instance);
    return apl::jni::createHandle<DestinationFactory>(destinationFactory);
}

#ifdef __cplusplus
}
#endif

}  // namespace jni
}  // namespace alexaext