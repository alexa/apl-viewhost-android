/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include <string>
#include <atomic>
#include "apl/apl.h"

#include "jniutil.h"
#include "jnisession.h"

namespace apl {
    namespace jni {

#ifdef __cplusplus
        extern "C" {
#endif
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"
        static jclass SESSION_CLASS;
        static jmethodID SESSION_WRITE;
        static JavaVM *JAVA_VM;

        jboolean jnisession_OnLoad(JavaVM *vm, void *reserved) {
            LOG(apl::LogLevel::kDebug) << "Loading View Host session JNI environment.";

            JAVA_VM = vm;
            JNIEnv *env;
            if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                return JNI_FALSE;
            }
            SESSION_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("com/amazon/apl/android/Session")));
            SESSION_WRITE = env->GetMethodID(SESSION_CLASS, "write",
                                             "(Lcom/amazon/apl/android/Session$LogEntryLevel;Lcom/amazon/apl/android/Session$LogEntrySource;Ljava/lang/String;[Ljava/lang/Object;)V");
            return JNI_TRUE;
        }

        void jnisession_OnUnload(JavaVM *vm, void *reserved) {
            LOG(apl::LogLevel::kDebug) << "Unloading View Host Session JNI environment.";
            apl::LoggerFactory::instance().reset();

            JNIEnv *env;
            if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            env->DeleteGlobalRef(SESSION_CLASS);
        }

        std::atomic_bool isDebuggingEnabled(false);

        class AndroidSession : public Session {
        private:
            jobject mInstance;

        public:
            AndroidSession() : mInstance(nullptr) {}

            void setInstance(jobject instance) {
                JNIEnv *env;
                if (JAVA_VM->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                    // environment failure, can't proceed.
                    return;
                }

                if (mInstance != nullptr && !env->IsSameObject(mInstance, nullptr)) {
                    env->DeleteWeakGlobalRef(mInstance);
                }
                mInstance = env->NewWeakGlobalRef(instance);
            }

            ~AndroidSession() override {
                JNIEnv *env;
                if (JAVA_VM->GetEnv((void **)&env, JNI_VERSION_1_6) != JNI_OK) {
                    return;
                }

                if (mInstance != nullptr) {
                    env->DeleteWeakGlobalRef(mInstance);
                    mInstance = nullptr;
                }
            }

            void write(const char *filename, const char *func, const char *value) override {
                LoggerFactory::instance().getLogger(LogLevel::kWarn, filename, func).session(
                        *this).log("%s", value);

                if (!isDebuggingEnabled.load()) {
                    return;
                }

                JNIEnv *env;
                if (JAVA_VM->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                    // environment failure, can't proceed.
                    LOG(apl::LogLevel::kError)
                            << "Attempted to write a session log with no env.";
                    return;
                }
                std::string message = std::string("[") + std::string(filename) + std::string(":") + std::string(func) + std::string("] ") + std::string(value);
                jobject javaLevel = convertToJavaLogLevel(env, LogLevel::kInfo);
                jobject javaSource = convertToJavaLogSource(env, "session");
                jstring javaMessage = env->NewStringUTF(message.c_str());

                env->CallVoidMethod(mInstance, SESSION_WRITE, javaLevel, javaSource, javaMessage, nullptr);

                env->DeleteLocalRef(javaLevel);
                env->DeleteLocalRef(javaSource);
                env->DeleteLocalRef(javaMessage);
            }

            //Report a log message resulting from execution of the Log command.
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
                    rapidjson::Value serializedArg = message.arguments.serialize(
                            serializer.GetAllocator());
                    serializedArg.Accept(writer); // Serialize into the writer directly
                }

                // Add origin object if it's not empty
                if (!message.origin.empty()) {
                    writer.Key("origin");
                    rapidjson::Value originSerialized = message.origin.serialize(
                            serializer.GetAllocator());
                    originSerialized.Accept(writer);
                }
                writer.EndObject();

                std::string result = buffer.GetString();
                LoggerFactory::instance().getLogger(message.level, "Log", "Command").session(
                        *this).log("%s %s",
                                   message.text.c_str(),
                                   result.c_str());

                JNIEnv *env;
                if (JAVA_VM->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                    // environment failure, can't proceed.
                    LOG(apl::LogLevel::kError)
                            << "Attempted to execute a log command with no env.";
                    return;
                }

                jobject javaLevel = convertToJavaLogLevel(env, message.level);
                jobject javaSource = convertToJavaLogSource(env, "command");
                jstring javaMessage = env->NewStringUTF(message.text.c_str());
                Object arguments = message.arguments;

                size_t size = arguments.size();
                jobjectArray javaArray = env->NewObjectArray(
                        static_cast<jsize>(size),
                        env->FindClass("java/lang/Object"),
                        nullptr);

                for (size_t i = 0; i < size; ++i) {
                    apl::Object element = arguments.at(static_cast<std::uint64_t>(i));
                    jobject javaObject = getJObject(env, element);
                    env->SetObjectArrayElement(javaArray, static_cast<jsize>(i), javaObject);
                    env->DeleteLocalRef(javaObject);
                }

                env->CallVoidMethod(mInstance, SESSION_WRITE, javaLevel, javaSource, javaMessage, javaArray);

                env->DeleteLocalRef(javaLevel);
                env->DeleteLocalRef(javaSource);
                env->DeleteLocalRef(javaMessage);

            }

            // Utility function to convert C++ LogEntryLevel to Java LogEntryLevel
            jobject convertToJavaLogLevel(JNIEnv *env, LogLevel level) {
                jclass enumClass = env->FindClass("com/amazon/apl/android/Session$LogEntryLevel");
                jfieldID fieldID = nullptr;

                switch (level) {
                    case LogLevel::kNone:
                        fieldID = env->GetStaticFieldID(enumClass, "NONE", "Lcom/amazon/apl/android/Session$LogEntryLevel;");
                        break;
                    case LogLevel::kTrace:
                        fieldID = env->GetStaticFieldID(enumClass, "TRACE", "Lcom/amazon/apl/android/Session$LogEntryLevel;");
                        break;
                    case LogLevel::kDebug:
                        fieldID = env->GetStaticFieldID(enumClass, "DEBUG", "Lcom/amazon/apl/android/Session$LogEntryLevel;");
                        break;
                    case LogLevel::kInfo:
                        fieldID = env->GetStaticFieldID(enumClass, "INFO", "Lcom/amazon/apl/android/Session$LogEntryLevel;");
                        break;
                    case LogLevel::kWarn:
                        fieldID = env->GetStaticFieldID(enumClass, "WARN", "Lcom/amazon/apl/android/Session$LogEntryLevel;");
                        break;
                    case LogLevel::kError:
                        fieldID = env->GetStaticFieldID(enumClass, "ERROR", "Lcom/amazon/apl/android/Session$LogEntryLevel;");
                        break;
                    case LogLevel::kCritical:
                        fieldID = env->GetStaticFieldID(enumClass, "CRITICAL", "Lcom/amazon/apl/android/Session$LogEntryLevel;");
                        break;
                    default:
                        break;
                }

                return env->GetStaticObjectField(enumClass, fieldID);
            }

            // Utility function to convert C++ LogEntrySource to Java LogEntrySource
            jobject convertToJavaLogSource(JNIEnv *env, const std::string &sourceString) {
                jclass enumClass = env->FindClass("com/amazon/apl/android/Session$LogEntrySource");
                jfieldID fieldID = nullptr;

                // Convert the source string to uppercase for case-insensitive comparison
                std::string upperSourceString = sourceString;
                std::transform(upperSourceString.begin(), upperSourceString.end(), upperSourceString.begin(), ::toupper);

                if (upperSourceString == "SESSION") {
                    fieldID = env->GetStaticFieldID(enumClass, "SESSION", "Lcom/amazon/apl/android/Session$LogEntrySource;");
                } else if (upperSourceString == "VIEW") {
                    fieldID = env->GetStaticFieldID(enumClass, "VIEW", "Lcom/amazon/apl/android/Session$LogEntrySource;");
                } else if (upperSourceString == "COMMAND") {
                    fieldID = env->GetStaticFieldID(enumClass, "COMMAND", "Lcom/amazon/apl/android/Session$LogEntrySource;");
                }

                return env->GetStaticObjectField(enumClass, fieldID);
            }
        };

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_Session_nSetDebuggingEnabled(JNIEnv *env, jclass clazz,
                                                                 jboolean enabled) {
            isDebuggingEnabled.store(enabled);
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_Session_nCreate(JNIEnv *env, jobject instance) {
            auto session = std::make_shared<AndroidSession>();
            session->setInstance(instance);
            return createHandle<Session>(session);
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_Session_nGetLogId(JNIEnv *env, jclass clazz, jlong _handle) {
            auto session = get<Session>(_handle);
            return env->NewStringUTF(session->getLogId().c_str());
        }

#pragma clang diagnostic pop

#ifdef __cplusplus
        }
#endif
    }
}