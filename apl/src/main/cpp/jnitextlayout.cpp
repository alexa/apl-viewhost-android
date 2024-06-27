/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <jni.h>
#include <string>
#include "apl/apl.h"

#include "jnitextlayout.h"
#include "jniutil.h"

namespace apl {
    namespace jni {

#ifdef __cplusplus
        extern "C" {
#endif
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunused-parameter"

        static JavaVM* JAVA_VM;
        static jclass APLTEXTLAYOUT_CLASS;
        static jmethodID APLTEXTLAYOUT_GETLINECOUNT_METHOD;
        static jmethodID APLTEXTLAYOUT_GETBASELINE_METHOD;
        static jmethodID APLTEXTLAYOUT_GETSIZE_METHOD;
        static jmethodID APLTEXTLAYOUT_GETBYTELENGTH_METHOD;
        static jmethodID APLTEXTLAYOUT_GET_BOUNDINGBOX_FOR_LINERANGE_METHOD;
        static jmethodID APLTEXTLAYOUT_GET_LINERANGE_FROM_BYTERANGE_METHOD;
        static jmethodID APLTEXTLAYOUT_GET_LAID_OUT_TEXT_METHOD;
        static jmethodID APLTEXTLAYOUT_IS_TRUNCATED_METHOD;

        jboolean
        textlayout_OnLoad(JavaVM *vm, void * reserved ) {
            LOG(apl::LogLevel::kDebug) << "Loading View Host textmeasure JNI environment.";

            JAVA_VM = vm;
            JNIEnv *env;
            if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                return JNI_FALSE;
            }

            APLTEXTLAYOUT_CLASS = reinterpret_cast<jclass>(env->NewGlobalRef(
                    env->FindClass("com/amazon/apl/android/scenegraph/text/APLTextLayout")));

            APLTEXTLAYOUT_GETLINECOUNT_METHOD = env->GetMethodID(APLTEXTLAYOUT_CLASS, "getLineCount", "()I");
            APLTEXTLAYOUT_GETSIZE_METHOD = env->GetMethodID(APLTEXTLAYOUT_CLASS, "getSize", "()[F");
            APLTEXTLAYOUT_GETBASELINE_METHOD = env->GetMethodID(APLTEXTLAYOUT_CLASS, "getBaseLine","()D");
            APLTEXTLAYOUT_GETBYTELENGTH_METHOD = env->GetMethodID(APLTEXTLAYOUT_CLASS, "getByteLength",
                                                                  "()I");
            APLTEXTLAYOUT_GET_BOUNDINGBOX_FOR_LINERANGE_METHOD = env->GetMethodID(APLTEXTLAYOUT_CLASS, "getBoundingBoxForLineRange",
                                                                                  "(II)[I");
            APLTEXTLAYOUT_GET_LINERANGE_FROM_BYTERANGE_METHOD = env->GetMethodID(APLTEXTLAYOUT_CLASS, "getLineRangeFromByteRange",
                                                                                 "(II)[I");
            APLTEXTLAYOUT_GET_LAID_OUT_TEXT_METHOD = env->GetMethodID(APLTEXTLAYOUT_CLASS, "getLaidOutText",
                                                                      "()Ljava/lang/String;");
            APLTEXTLAYOUT_IS_TRUNCATED_METHOD = env->GetMethodID(APLTEXTLAYOUT_CLASS, "isTruncated",
                                                                  "()Z");
            return JNI_TRUE;
        }

        void
        textlayout_OnUnload(JavaVM *vm, void * reserved ) {
            JNIEnv *env;
            if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                // environment failure, can't proceed.
                return;
            }

            env->DeleteGlobalRef(APLTEXTLAYOUT_CLASS);
        }

        void APLTextLayout::release() const {
            JNIEnv *env;
            if (JAVA_VM->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                // environment failure, can't proceed.
                return;
            }
            env->DeleteGlobalRef(mTextLayout);
        }

        int APLTextLayout::getLineCount() const {
            JNIEnv *env;
            if (JAVA_VM->GetEnv(reinterpret_cast<void **>(&env),
                                                 JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return 0;
            }

            return env->CallIntMethod(mTextLayout, APLTEXTLAYOUT_GETLINECOUNT_METHOD);
        }

        float APLTextLayout::getBaseline() const {
            JNIEnv *env;
            if (JAVA_VM->GetEnv(reinterpret_cast<void **>(&env),
                                JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return 0;
            }

            return env->CallDoubleMethod(mTextLayout, APLTEXTLAYOUT_GETBASELINE_METHOD);
        }

        Size APLTextLayout::getSize() const {
            JNIEnv *env;
            if (JAVA_VM->GetEnv(reinterpret_cast<void **>(&env),
                                JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return {0, 0};
            }

            jfloatArray size = (jfloatArray) env->CallObjectMethod(mTextLayout, APLTEXTLAYOUT_GETSIZE_METHOD);
            auto array = env->GetFloatArrayElements(size, 0);
            auto measureWidth = static_cast<float>(array[0]);
            auto measureHeight = static_cast<float>(array[1]);
            env->ReleaseFloatArrayElements(size, array, 0);
            env->DeleteLocalRef(size);
            return { measureWidth, measureHeight };
        }

        unsigned int APLTextLayout::getByteLength() const {
            JNIEnv *env;
            if (JAVA_VM->GetEnv(reinterpret_cast<void **>(&env),
                                JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return 0;
            }

            return (unsigned int) env->CallIntMethod(mTextLayout, APLTEXTLAYOUT_GETBYTELENGTH_METHOD);
        }

        apl::Rect APLTextLayout::getBoundingBoxForLines(Range lineRange) const {
            JNIEnv *env;
            if (JAVA_VM->GetEnv(reinterpret_cast<void **>(&env),
                                JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return {0,0,0,0};
            }

            jintArray bounds = (jintArray) env->CallObjectMethod(mTextLayout,
                                                                 APLTEXTLAYOUT_GET_BOUNDINGBOX_FOR_LINERANGE_METHOD,
                                                                 lineRange.lowerBound(),
                                                                 lineRange.upperBound());
            auto array = env->GetIntArrayElements(bounds, 0);
            auto left = static_cast<float>(array[0]);
            auto top = static_cast<float>(array[1]);
            auto width = static_cast<float>(array[2]);
            auto height = static_cast<float>(array[3]);
            env->ReleaseIntArrayElements(bounds, array, 0);
            env->DeleteLocalRef(bounds);
            return Rect { left, top, width, height };
        }

        Range APLTextLayout::getLineRangeFromByteRange(Range byteRange) const {
            JNIEnv *env;
            if (JAVA_VM->GetEnv(reinterpret_cast<void **>(&env),
                                JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return {0,0};
            }

            jintArray bounds = (jintArray) env->CallObjectMethod(mTextLayout,
                                                                 APLTEXTLAYOUT_GET_LINERANGE_FROM_BYTERANGE_METHOD,
                                                                 byteRange.lowerBound(),
                                                                 byteRange.upperBound());
            auto array = env->GetIntArrayElements(bounds, 0);
            auto start = static_cast<int>(array[0]);
            auto end = static_cast<int>(array[1]);
            env->ReleaseIntArrayElements(bounds, array, 0);
            env->DeleteLocalRef(bounds);
            return Range { start, end };
        }

        std::string APLTextLayout::getLaidOutText() const {
            JNIEnv *env;
            if (JAVA_VM->GetEnv(reinterpret_cast<void **>(&env),
                                JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return "";
            }

            auto jtext = (jstring) env->CallObjectMethod(mTextLayout, APLTEXTLAYOUT_GET_LAID_OUT_TEXT_METHOD);

            auto text = env->GetStringUTFChars(jtext, 0);
            env->ReleaseStringUTFChars(jtext, text);

            return text;
        }

        bool APLTextLayout::isTruncated() const {
            JNIEnv *env;
            if (JAVA_VM->GetEnv(reinterpret_cast<void **>(&env),
                                JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return false;
            }

            return env->CallBooleanMethod(mTextLayout,
                                          APLTEXTLAYOUT_IS_TRUNCATED_METHOD);
        }

        void
        APLTextLayout::setTextLayout(jobject textLayout) {
            JNIEnv *env;
            if (JAVA_VM->GetEnv(reinterpret_cast<void **>(&env),
                                                 JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }

            mTextLayout = env->NewGlobalRef(textLayout);
        }

        rapidjson::Value APLTextLayout::serialize(rapidjson::Document::AllocatorType &allocator) const {
            auto out = rapidjson::Value(rapidjson::kObjectType);
            out.AddMember("size", getSize().serialize(allocator), allocator);
            out.AddMember("baseline", getBaseline(), allocator);
            out.AddMember("lineCount", getLineCount(), allocator);
            out.AddMember("byteLength", getByteLength(), allocator);
            //out.AddMember("text", rapidjson::Value(mLayout.debugString.UTF8String, allocator), allocator);

            auto props = mTextProperties.lock();
            if (props)
                out.AddMember("textProperties", props->serialize(allocator), allocator);
            auto chunk = mTextChunk.lock();
            if (chunk)
                out.AddMember("raw", rapidjson::Value(chunk->styledText().getRawText().c_str(), allocator), allocator);

            return out;
        }

#pragma clang diagnostic pop

#ifdef __cplusplus
        }
#endif

    } //namespace jni
} //namespace apl
