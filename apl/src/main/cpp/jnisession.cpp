/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include <string>
#include <atomic>
#include "apl/apl.h"

#include "jniutil.h"

namespace apl {
    namespace jni {

#ifdef __cplusplus
        extern "C" {
#endif

        std::atomic_bool isDebuggingEnabled(false);

        class AndroidSession : public Session {
        public:
            void write(const char *filename, const char *func, const char *value) override {
                LoggerFactory::instance().getLogger(LogLevel::kWarn, filename, func).session(
                        *this).log("%s", value);
            }

            void write(LogCommandMessage &&message) override {
                if (!isDebuggingEnabled.load()) {
                    return;
                }

                rapidjson::StringBuffer buffer;
                rapidjson::Writer<rapidjson::StringBuffer> writer(buffer);
                rapidjson::Document serializer;

                writer.StartObject();

                // Add arguments array if it's not empty
                if (!message.arguments.empty()) {
                    writer.Key("arguments");
                    rapidjson::Value serializedArg = message.arguments.serialize(serializer.GetAllocator());
                    serializedArg.Accept(writer); // Serialize into the writer directly
                }

                // Add origin object if it's not empty
                if (!message.origin.empty()) {
                    writer.Key("origin");
                    rapidjson::Value originSerialized = message.origin.serialize(serializer.GetAllocator());
                    originSerialized.Accept(writer);
                }
                writer.EndObject();

                std::string result = buffer.GetString();
                LoggerFactory::instance().getLogger(message.level, "Log", "Command").session(*this).log("%s %s",
                                                                                                        message.text.c_str(),
                                                                                                        result.c_str());
            }
        };



        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_Session_nSetDebuggingEnabled(JNIEnv *env, jclass clazz,
                                                                 jboolean enabled) {
            isDebuggingEnabled.store(enabled);
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_Session_nCreate(JNIEnv *env, jclass clazz) {
            auto session = std::make_shared<AndroidSession>();
            return createHandle<Session>(session);
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_Session_nGetLogId(JNIEnv *env, jclass clazz, jlong _handle) {
            auto session = get<Session>(_handle);
            return env->NewStringUTF(session->getLogId().c_str());
        }

#ifdef __cplusplus
        }
#endif
    }
}