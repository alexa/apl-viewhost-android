/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>

#include <string>

#include "alexaext/alexaext.h"
#include "jnidestinationfactory.h"
#include "jninativeowner.h"
#include "rapidjson/document.h"

namespace alexaext {
namespace jni {

#ifdef __cplusplus
extern "C" {
#endif

static JavaVM* METRICSEXTENSIONV2_VM_REFERENCE;

jboolean metricsextensionV2_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_FALSE;
    }

    METRICSEXTENSIONV2_VM_REFERENCE = vm;
    return JNI_TRUE;
}

void metricsextensionV2_OnUnload(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK) {
        // environment failure, can't proceed.
        return;
    }
}

#define CREATE_ACTIVITY_DESCRIPTOR()                                                  \
    const auto uri = env->GetStringUTFChars(uri_, nullptr);                           \
    const auto sessionId = env->GetStringUTFChars(sessionId_, nullptr);               \
    const auto activityId = env->GetStringUTFChars(activityId_, nullptr);             \
                                                                                      \
    auto session = std::make_shared<alexaext::SessionDescriptor>(sessionId);          \
    auto activityDescriptor = alexaext::ActivityDescriptor(uri, session, activityId); \
                                                                                      \
    env->ReleaseStringUTFChars(uri_, uri);                                            \
    env->ReleaseStringUTFChars(sessionId_, sessionId);                                \
    env->ReleaseStringUTFChars(activityId_, activityId);

class MetricsExtensionV2 : public metricsExtensionV2::AplMetricsExtensionV2 {
public:
    MetricsExtensionV2(
            std::shared_ptr<DestinationFactory> destinationFactory,
            const alexaext::ExecutorPtr& executor,
            jobject instance) :
            AplMetricsExtensionV2(destinationFactory, executor),
            mExecutor(executor) {
        JNIEnv* env;
        if (METRICSEXTENSIONV2_VM_REFERENCE->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
            return;
        }
        mWeakInstance = env->NewWeakGlobalRef(instance);
    }

    ~MetricsExtensionV2() {
        JNIEnv* env;
        if (METRICSEXTENSIONV2_VM_REFERENCE->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
            return;
        }
        env->DeleteWeakGlobalRef(mWeakInstance);
    }

private:
    jweak mWeakInstance;
    alexaext::ExecutorPtr mExecutor;
};

JNIEXPORT jlong JNICALL Java_com_amazon_alexaext_metricsextensionv2_MetricsExtensionV2_nCreate(
        JNIEnv* env,
        jobject instance,
        jlong destinationFactoryHandle_,
        jlong executorHandle_) {
    auto destinationFactory = apl::jni::get<DestinationFactory>(destinationFactoryHandle_);
    auto executor = apl::jni::get<alexaext::Executor>(executorHandle_);
    auto metricsExtensionV2 = std::make_shared<MetricsExtensionV2>(destinationFactory, executor, instance);
    return apl::jni::createHandle<MetricsExtensionV2>(metricsExtensionV2);
}

JNIEXPORT jstring JNICALL Java_com_amazon_alexaext_metricsextensionv2_MetricsExtensionV2_nCreateRegistration(
        JNIEnv* env,
        jobject instance,
        jlong handle,
        jstring uri_,
        jstring sessionId_,
        jstring activityId_,
        jstring registrationRequest_) {
    CREATE_ACTIVITY_DESCRIPTOR();
    auto aplMetricsExtensionV2 = apl::jni::get<MetricsExtensionV2>(handle);

    auto doc = rapidjson::Document();
    const char* registrationRequest = env->GetStringUTFChars(registrationRequest_, nullptr);
    doc.Parse(registrationRequest);
    if (doc.IsNull() || doc.HasParseError()) {
        env->ReleaseStringUTFChars(registrationRequest_, registrationRequest);
        return env->NewStringUTF("");
    }

    env->ReleaseStringUTFChars(registrationRequest_, registrationRequest);
    rapidjson::Value registrationRequestValue = rapidjson::Value(doc, doc.GetAllocator());

    auto createRegistrationResponse =
            aplMetricsExtensionV2->createRegistration(activityDescriptor, registrationRequestValue);
    return env->NewStringUTF(AsString(createRegistrationResponse).c_str());
}

JNIEXPORT jboolean JNICALL Java_com_amazon_alexaext_metricsextensionv2_MetricsExtensionV2_nInvokeCommand(
        JNIEnv* env,
        jobject instance,
        jlong handle,
        jstring uri_,
        jstring sessionId_,
        jstring activityId_,
        jstring command_) {
    CREATE_ACTIVITY_DESCRIPTOR();
    auto aplMetricsExtensionV2 = apl::jni::get<MetricsExtensionV2>(handle);

    auto doc = rapidjson::Document();
    const char* command = env->GetStringUTFChars(command_, nullptr);
    doc.Parse(command);
    if (doc.IsNull() || doc.HasParseError()) {
        env->ReleaseStringUTFChars(command_, command);
        return false;
    }

    env->ReleaseStringUTFChars(command_, command);
    rapidjson::Value commandValue = rapidjson::Value(doc, doc.GetAllocator());
    return aplMetricsExtensionV2->invokeCommand(activityDescriptor, commandValue);
}

JNIEXPORT void JNICALL Java_com_amazon_alexaext_metricsextensionv2_MetricsExtensionV2_nOnUnregistered(
        JNIEnv* env,
        jobject instance,
        jlong handle,
        jstring uri_,
        jstring sessionId_,
        jstring activityId_) {
    CREATE_ACTIVITY_DESCRIPTOR();
    auto aplMetricsExtensionV2 = apl::jni::get<MetricsExtensionV2>(handle);
    aplMetricsExtensionV2->onActivityUnregistered(activityDescriptor);
}

#ifdef __cplusplus
}
#endif

}  // namespace jni
}  // namespace alexaext