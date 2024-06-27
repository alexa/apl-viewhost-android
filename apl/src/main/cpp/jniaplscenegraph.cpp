//
// Created by Das, Sourabh on 2023-01-13.
//

#include "jniaplscenegraph.h"

#include <codecvt>
#include <jni.h>
#include <jniapllayer.h>
#include "jniutil.h"

#define ENV_CREATE() \
    JNIEnv *env; \
    if (JAVA_VM->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) { \
        LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed"; \
        return; \
    } \
    jobject localRef = env->NewLocalRef(mInstance); \
    if (!localRef) { \
        return; \
}

#define ENV_CLEAR() \
    env->DeleteLocalRef(localRef)

namespace apl {
    namespace jni {
#ifdef __cplusplus
        extern "C" {
#endif

        static JavaVM* APLSCENEGRAPH_VM_REFERENCE;

        /**
         * Create a class and method cache for calls to View Host.
         */
        jboolean
        aplscenegraph_OnLoad(JavaVM *vm, void *reserved) {

            LOG(apl::LogLevel::kDebug) << "Loading View Host Component JNI environment.";

            JNIEnv *env;
            if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return JNI_FALSE;
            }

            APLSCENEGRAPH_VM_REFERENCE = vm;
            return JNI_TRUE;
        }

        /**
         * Release the class and method cache.
         */
        void
        aplscenegraph_OnUnload(JavaVM *vm, void *reserved) {
            LOG(apl::LogLevel::kDebug) << "Unloading View Host Component JNI environment.";
            apl::LoggerFactory::instance().reset();

            JNIEnv *env;
            if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_scenegraph_APLScenegraph_nGetTop(JNIEnv *env,
                                                                     jobject instance,
                                                                     jlong rootContextHandle) {
            auto rootContext = get<RootContext>(rootContextHandle);
            auto scenegraph = rootContext->getSceneGraph();
            return reinterpret_cast<jlong>(scenegraph->getLayer().get());
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_scenegraph_APLScenegraph_nApplyUpdates(JNIEnv *env,
                                                                     jobject instance,
                                                                     jlong rootContextHandle) {
            auto rootContext = get<RootContext>(rootContextHandle);
            auto scenegraph = rootContext->getSceneGraph();
            if (scenegraph->updates().empty()) {
                return;
            }
            scenegraph->updates().mapChanged([&] (const apl::sg::LayerPtr& coreLayer) {
                auto *aplLayer = coreLayer->getUserData<APLLayer>();
                if (aplLayer) {
                    aplLayer->updateDirtyProperties(coreLayer->getAndClearFlags());
                } else {
                    LOG(apl::LogLevel::kError) << "Layer map changed when layer was not ensured";
                }
            });
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_scenegraph_APLScenegraph_nSerializeScenegraph(JNIEnv *env,
                                                                                  jclass clazz,
                                                                                  jlong rootContextHandle) {
            auto rc = get<RootContext>(rootContextHandle);
            rapidjson::Document document(rapidjson::kObjectType);
            auto context = rc->getSceneGraph()->serialize(document.GetAllocator());
            rapidjson::StringBuffer buffer;
            rapidjson::Writer<rapidjson::StringBuffer> writer(buffer);
            context.Accept(writer);
            std::u16string u16 = converter.from_bytes(buffer.GetString());
            return env->NewString(reinterpret_cast<const jchar *>(u16.c_str()), u16.length());
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_scenegraph_APLScenegraph_nGetDOM(JNIEnv *env,
                                                                     jclass clazz,
                                                                     jlong rootContextHandle) {
            auto rc = get<RootContext>(rootContextHandle);
            rapidjson::Document document(rapidjson::kObjectType);
            auto context = rc->serializeDOM(false, document.GetAllocator());
            rapidjson::StringBuffer buffer;
            rapidjson::Writer<rapidjson::StringBuffer> writer(buffer);
            context.Accept(writer);
            std::u16string u16 = converter.from_bytes(buffer.GetString());
            return env->NewString(reinterpret_cast<const jchar *>(u16.c_str()), u16.length());
        }

#ifdef __cplusplus
        }
#endif
    } //namespace jni
} //namespace apl
