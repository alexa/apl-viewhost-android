/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include "jnipackagemanager.h"
#include "jninativeowner.h"
#include "jnicomplexproperty.h"

#ifdef __cplusplus
extern "C" {
#endif
namespace apl {
    namespace jni {

        JNIEXPORT jlong
        JNICALL
        Java_com_amazon_apl_android_PackageManager_nCreate(JNIEnv *env, jobject instance) {
            auto packageManager = jniPackageManager::create(env->NewWeakGlobalRef(instance));
            return createHandle<apl::PackageManager>(packageManager);
        }

        JNIEXPORT void
        JNICALL
        Java_com_amazon_apl_android_PackageManager_nSuccess(JNIEnv *env, jobject instance, jlong requestHandle, jlong jsonHandle) {
            auto request = get<PackageManager::PackageRequest>(requestHandle);
            auto aplJSON = get<SharedJsonData>(jsonHandle);

            request->succeed(*aplJSON);
        }

        JNIEXPORT void
        JNICALL
        Java_com_amazon_apl_android_PackageManager_nFailure(JNIEnv *env, jobject instance, jlong packageRequestHandle, jint errorCode, jstring errorMessage) {

            auto request = get<PackageManager::PackageRequest>(packageRequestHandle);
            const char* errorMessageCharA = errorMessage != nullptr?env->GetStringUTFChars(errorMessage, nullptr) : nullptr;
            std::string errorMessageCPP( errorMessageCharA != nullptr? errorMessageCharA : "");

            request->fail(errorMessageCPP, errorCode);

            if(errorMessageCharA != nullptr)
                env->ReleaseStringUTFChars(errorMessage, errorMessageCharA);

        }


        static jclass JAVA_PACKAGEMANAGER_CLASS;
        static jmethodID JAVA_PACKAGEMANAGER_REQUEST;
        static JavaVM *VM_REFERENCE;

        jboolean packagemanager_OnLoad(JavaVM *vm, void *reserved) {

            LOG(apl::LogLevel::kDebug) << "Loading View Host PackageManager JNI environment.";

            JNIEnv *env;
            if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return JNI_FALSE;
            }

            VM_REFERENCE = vm;

            JAVA_PACKAGEMANAGER_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("com/amazon/apl/android/PackageManager")));

            JAVA_PACKAGEMANAGER_REQUEST = env->GetMethodID(
                    JAVA_PACKAGEMANAGER_CLASS,
                    "coreRequestPackage",
                    "(JJLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");

            if (nullptr == JAVA_PACKAGEMANAGER_REQUEST) {
                return JNI_FALSE;
            }

            return JNI_TRUE;
        }

        void packagemanager_OnUnload(JavaVM *vm, void *reserved) {
            LOG(apl::LogLevel::kDebug) << "Unloading View Host PackageManager JNI environment.";
            apl::LoggerFactory::instance().reset();

            JNIEnv *env;
            if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            env->DeleteGlobalRef(JAVA_PACKAGEMANAGER_CLASS);
        }


        apl::PackageManagerPtr
        jniPackageManager::create(jweak packageManagerInstance) {
            auto jpm = std::make_shared<jniPackageManager>();
            jpm->mJavaPackageManagerInstance = packageManagerInstance;
            return jpm;
        }

        jniPackageManager::~jniPackageManager() {

            JNIEnv *env;
            if (VM_REFERENCE==nullptr || VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
                                     JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            if(mJavaPackageManagerInstance != nullptr)
                env->DeleteWeakGlobalRef(mJavaPackageManagerInstance);
        }


        void jniPackageManager::loadPackage(const PackageRequestPtr &packageRequest) {

            JNIEnv *env;
            if (VM_REFERENCE->GetEnv(reinterpret_cast<void **>(&env),
                                     JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            auto packageRequestHandle = createHandle<PackageRequest>(packageRequest);
            auto importRequestPtr = std::make_shared<apl::ImportRequest>(packageRequest->request());
            auto importRequestHandle = createHandle<ImportRequest>(importRequestPtr);

            auto source = env->NewStringUTF(packageRequest->request().source().c_str());
            auto name = env->NewStringUTF(packageRequest->request().reference().name().c_str());
            auto version = env->NewStringUTF(
                    packageRequest->request().reference().version().c_str());
            auto domain = env->NewStringUTF(
                    packageRequest->request().reference().domain().c_str());

            jobject javaPackageManager = env->NewLocalRef(mJavaPackageManagerInstance);

            if (javaPackageManager) {

                env->CallVoidMethod(mJavaPackageManagerInstance, JAVA_PACKAGEMANAGER_REQUEST,
                                    packageRequestHandle, importRequestHandle,
                                    source, name,
                                    version, domain);
            }

            env->DeleteLocalRef(javaPackageManager);
            env->DeleteLocalRef(source);
            env->DeleteLocalRef(name);
            env->DeleteLocalRef(version);
            env->DeleteLocalRef(domain);
        }
    }
}

#ifdef __cplusplus
}
#endif

