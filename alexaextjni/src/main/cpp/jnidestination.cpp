/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>

#include <string>

#include "alexaext/alexaext.h"
#include "jninativeowner.h"
#include "rapidjson/document.h"
#include "rapidjson/stringbuffer.h"
#include "rapidjson/writer.h"

namespace alexaext {
namespace jni {

#ifdef __cplusplus
extern "C" {
#endif

static jclass DESTINATION_CLASS;
static jmethodID DESTINATION_PUBLISH;
static jmethodID DESTINATION_PUBLISH_METRIC;
static jmethodID DESTINATION_PUBLISH_METRICLIST;
static jclass METRIC_CLASS;
static jmethodID METRIC_CONSTRUCTOR;
static jclass HASHMAP_CLASS;
static jmethodID HASHMAP_CONSTRUCTOR;
static jmethodID HASHMAP_PUT;
static jclass ARRAYLIST_CLASS;
static jmethodID ARRAYLIST_CONSTRUCTOR;
static jmethodID ARRAYLIST_ADD;
static JavaVM* DESTINATION_VM_REFERENCE;

jboolean destination_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_FALSE;
    }

    DESTINATION_VM_REFERENCE = vm;

    // method signatures can be obtained with 'javap -s'
    DESTINATION_CLASS = reinterpret_cast<jclass>(
            env->NewGlobalRef(env->FindClass("com/amazon/alexaext/metricsextensionv2/Destination")));
    if (DESTINATION_CLASS == nullptr) {
        return JNI_FALSE;
    }

    DESTINATION_PUBLISH_METRIC = env->GetMethodID(
            DESTINATION_CLASS,
            "publishInternal",
            "(Lcom/amazon/alexaext/metricsextensionv2/Metric;)V");
    if (DESTINATION_PUBLISH_METRIC == nullptr) {
        return JNI_FALSE;
    }

    DESTINATION_PUBLISH_METRICLIST = env->GetMethodID(DESTINATION_CLASS, "publishInternal", "(Ljava/util/List;)V");
    if (DESTINATION_PUBLISH_METRICLIST == nullptr) {
        return JNI_FALSE;
    }

    METRIC_CLASS = reinterpret_cast<jclass>(
            env->NewGlobalRef(env->FindClass("com/amazon/alexaext/metricsextensionv2/Metric")));
    if (METRIC_CLASS == nullptr) {
        return JNI_FALSE;
    }

    METRIC_CONSTRUCTOR = env->GetMethodID(METRIC_CLASS, "<init>", "(Ljava/lang/String;DLjava/util/HashMap;)V");
    if (METRIC_CONSTRUCTOR == nullptr) {
        return JNI_FALSE;
    }

    HASHMAP_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/util/HashMap")));
    if (HASHMAP_CLASS == nullptr) {
        return JNI_FALSE;
    }

    HASHMAP_CONSTRUCTOR = env->GetMethodID(HASHMAP_CLASS, "<init>", "()V");
    if (HASHMAP_CONSTRUCTOR == nullptr) {
        return JNI_FALSE;
    }

    HASHMAP_PUT = env->GetMethodID(HASHMAP_CLASS, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    if (HASHMAP_PUT == nullptr) {
        return JNI_FALSE;
    }

    ARRAYLIST_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/util/ArrayList")));
    if (ARRAYLIST_CLASS == nullptr) {
        return JNI_FALSE;
    }

    ARRAYLIST_CONSTRUCTOR = env->GetMethodID(ARRAYLIST_CLASS, "<init>", "()V");
    if (ARRAYLIST_CONSTRUCTOR == nullptr) {
        return JNI_FALSE;
    }

    ARRAYLIST_ADD = env->GetMethodID(ARRAYLIST_CLASS, "add", "(Ljava/lang/Object;)Z");
    if (ARRAYLIST_ADD == nullptr) {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

void destination_OnUnload(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv((void**) &env, JNI_VERSION_1_6) != JNI_OK) {
        return;
    }

    env->DeleteGlobalRef(DESTINATION_CLASS);
    env->DeleteGlobalRef(METRIC_CLASS);
    env->DeleteGlobalRef(HASHMAP_CLASS);
    env->DeleteGlobalRef(ARRAYLIST_CLASS);
}

#define ENV_CREATE(FAIL_RETURN)                                                                        \
    JNIEnv* env;                                                                                       \
    if (DESTINATION_VM_REFERENCE->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) { \
        return FAIL_RETURN;                                                                            \
    }                                                                                                  \
                                                                                                       \
    jobject localRef = env->NewLocalRef(mWeakInstance);                                                \
    if (!localRef) {                                                                                   \
        return FAIL_RETURN;                                                                            \
    }

#define ENV_CLEAR() env->DeleteLocalRef(localRef)

class Destination : public metricsExtensionV2::DestinationInterface {
public:
    Destination(jobject instance) {
        JNIEnv* env;
        if (DESTINATION_VM_REFERENCE->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
            return;
        }
        mWeakInstance = env->NewWeakGlobalRef(instance);
    }

    ~Destination() {
        JNIEnv* env;
        if (DESTINATION_VM_REFERENCE->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
            return;
        }
        env->DeleteWeakGlobalRef(mWeakInstance);
    }

    void publish(metricsExtensionV2::Metric metric) {
        ENV_CREATE();

        jstring name = env->NewStringUTF(metric.name.c_str());
        jdouble value = static_cast<jdouble>(metric.value);
        jobject dimensions = env->NewObject(HASHMAP_CLASS, HASHMAP_CONSTRUCTOR);
        for (const auto& pair : metric.dimensions) {
            jstring key = env->NewStringUTF(pair.first.c_str());
            jstring value = env->NewStringUTF(pair.second.c_str());
            env->CallObjectMethod(dimensions, HASHMAP_PUT, key, value);
            env->DeleteLocalRef(key);
            env->DeleteLocalRef(value);
        }

        auto metricObj = env->NewObject(METRIC_CLASS, METRIC_CONSTRUCTOR, name, value, dimensions);
        env->CallVoidMethod(localRef, DESTINATION_PUBLISH_METRIC, metricObj);
        env->DeleteLocalRef(name);
        env->DeleteLocalRef(dimensions);
        env->DeleteLocalRef(metricObj);
        ENV_CLEAR();
    }

    void publish(std::vector<metricsExtensionV2::Metric> metrics) {
        ENV_CREATE();

        jobject metricList = env->NewObject(ARRAYLIST_CLASS, ARRAYLIST_CONSTRUCTOR);
        for (const auto& metric : metrics) {
            jstring name = env->NewStringUTF(metric.name.c_str());
            jdouble value = static_cast<jdouble>(metric.value);
            jobject dimensions = env->NewObject(HASHMAP_CLASS, HASHMAP_CONSTRUCTOR);
            for (const auto& pair : metric.dimensions) {
                jstring key = env->NewStringUTF(pair.first.c_str());
                jstring value = env->NewStringUTF(pair.second.c_str());
                env->CallObjectMethod(dimensions, HASHMAP_PUT, key, value);
                env->DeleteLocalRef(key);
                env->DeleteLocalRef(value);
            }

            auto metricObj = env->NewObject(METRIC_CLASS, METRIC_CONSTRUCTOR, name, value, dimensions);
            env->CallBooleanMethod(metricList, ARRAYLIST_ADD, metricObj);

            env->DeleteLocalRef(name);
            env->DeleteLocalRef(dimensions);
            env->DeleteLocalRef(metricObj);
        }

        env->CallVoidMethod(localRef, DESTINATION_PUBLISH_METRICLIST, metricList);
        env->DeleteLocalRef(metricList);
        ENV_CLEAR();
    }

private:
    jweak mWeakInstance;
};

JNIEXPORT jlong JNICALL Java_com_amazon_alexaext_metricsextensionv2_Destination_nCreate(JNIEnv* env, jobject instance) {
    auto destination = std::make_shared<Destination>(instance);
    return apl::jni::createHandle<Destination>(destination);
}

#ifdef __cplusplus
}
#endif

}  // namespace jni
}  // namespace alexaext