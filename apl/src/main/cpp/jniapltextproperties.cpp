/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include "jniapltextproperties.h"

#include <locale>
#include <codecvt>
#include <jni.h>
#include "jniutil.h"

namespace apl {
namespace jni {
#ifdef __cplusplus
    extern "C" {
#endif
        /**
         * Create a class and method cache for calls to View Host.
         */
        jboolean
        apltextproperties_OnLoad(JavaVM *vm, void *reserved) {

            LOG(apl::LogLevel::kDebug) << "Loading View Host Component JNI environment.";

            JNIEnv *env;
            if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return JNI_FALSE;
            }

            return JNI_TRUE;
        }

        /**
         * Release the class and method cache.
         */
        void
        apltextproperties_OnUnload(JavaVM *vm, void *reserved) {
            LOG(apl::LogLevel::kDebug) << "Unloading View Host Component JNI environment.";
            apl::LoggerFactory::instance().reset();

            JNIEnv *env;
            if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
                LOG(apl::LogLevel::kError) << "Environment failure, cannot proceed";
                return;
            }
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_scenegraph_text_APLTextProperties_nGetTextAlign(JNIEnv *env, jclass clazz,
                                                                                    jlong handle) {
            auto coreTextProperties = reinterpret_cast<apl::sg::TextProperties *>(handle);
            return static_cast<jint>(coreTextProperties->textAlign());
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_scenegraph_text_APLTextProperties_nGetFontFamily(JNIEnv *env, jclass clazz,
                                                                                     jlong handle) {
            auto coreTextProperties = reinterpret_cast<apl::sg::TextProperties *>(handle);
            auto value = coreTextProperties->fontFamily().begin();
            std::u16string u16 = converter.from_bytes(value->c_str());
            return env->NewString(reinterpret_cast<const jchar *>(u16.c_str()), u16.length());
        }

        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_scenegraph_text_APLTextProperties_nGetFontLanguage(JNIEnv *env, jclass clazz,
                                                                                     jlong handle) {
            auto coreTextProperties = reinterpret_cast<apl::sg::TextProperties *>(handle);
            auto value = coreTextProperties->language();
            std::u16string u16 = converter.from_bytes(value.c_str());
            return env->NewString(reinterpret_cast<const jchar *>(u16.c_str()), u16.length());
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_scenegraph_text_APLTextProperties_nGetFontWeight(JNIEnv *env, jclass clazz,
                                                                                     jlong handle) {
            auto coreTextProperties = reinterpret_cast<apl::sg::TextProperties *>(handle);
            return static_cast<jint>(coreTextProperties->fontWeight());
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_scenegraph_text_APLTextProperties_nGetFontStyle(JNIEnv *env, jclass clazz,
                                                                                    jlong handle) {
            auto coreTextProperties = reinterpret_cast<apl::sg::TextProperties *>(handle);
            return static_cast<jint>(coreTextProperties->fontStyle());
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_scenegraph_text_APLTextProperties_nGetFontSize(JNIEnv *env, jclass clazz,
                                                                                   jlong handle) {
            auto coreTextProperties = reinterpret_cast<apl::sg::TextProperties *>(handle);
            return static_cast<jfloat>(coreTextProperties->fontSize());
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_scenegraph_text_APLTextProperties_nGetLetterSpacing(JNIEnv *env, jclass clazz,
                                                                                        jlong handle) {
            auto coreTextProperties = reinterpret_cast<apl::sg::TextProperties *>(handle);
            return static_cast<jfloat>(coreTextProperties->letterSpacing());
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_scenegraph_text_APLTextProperties_nGetMaxLines(JNIEnv *env, jclass clazz,
                                                                                   jlong handle) {
            auto coreTextProperties = reinterpret_cast<apl::sg::TextProperties *>(handle);
            return static_cast<jint>(coreTextProperties->maxLines());
        }

        JNIEXPORT jfloat JNICALL
        Java_com_amazon_apl_android_scenegraph_text_APLTextProperties_nGetLineHeight(JNIEnv *env, jclass clazz,
                                                                                     jlong handle) {
            auto coreTextProperties = reinterpret_cast<apl::sg::TextProperties *>(handle);
            return static_cast<jfloat>(coreTextProperties->lineHeight());
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_scenegraph_text_APLTextProperties_nGetHash(JNIEnv *env,
                                                                               jclass clazz,
                                                                               jlong handle) {
            auto coreTextProperties = reinterpret_cast<apl::sg::TextProperties *>(handle);
            return static_cast<jlong>(coreTextProperties->hash());
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_scenegraph_text_APLTextProperties_nGetTextAlignVertical(JNIEnv *env, jclass clazz,
                                                                                            jlong handle) {
            auto coreTextProperties = reinterpret_cast<apl::sg::TextProperties *>(handle);
            return static_cast<jint>(coreTextProperties->textAlignVertical());
        }

        // TODO: The following methods can be moved to their own JNI file for handling apl::sg::TextChunk
        JNIEXPORT jstring JNICALL
        Java_com_amazon_apl_android_scenegraph_text_StyledText_nGetText(JNIEnv *env, jclass clazz,
                                                                        jlong handle) {
            auto textChunk = reinterpret_cast<apl::sg::TextChunk *>(handle);
            const auto &styledText = textChunk->styledText();

            // for emoji characters, rapidjson stores in 4-byte format
            // eg. U+1F33D -> \xF0\x9F\x8C\xBD
            // but NewStringUTF expects modified UTF-8 and \xF0 is invalid
            // so converts the rapidjson value to UTF-16
            // and uses NewString to create Java string instead
            // not using NewStringUTF
            std::wstring_convert<std::codecvt_utf8_utf16<char16_t>, char16_t> converter;
            std::u16string u16str = converter.from_bytes(styledText.getText().data());
            return env->NewString((const jchar*)u16str.data(), u16str.length());
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_scenegraph_text_StyledText_nGetSpanCount(JNIEnv *env, jclass clazz,
                                                                             jlong handle) {
            auto textChunk = reinterpret_cast<apl::sg::TextChunk *>(handle);
            const auto &styledText = textChunk->styledText();
            auto spans = styledText.getSpans();
            return static_cast<jint>(spans.size());
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_scenegraph_text_StyledText_nGetSpanTypeAt(JNIEnv *env, jclass clazz,
                                                                              jlong handle,
                                                                              jint index) {
            auto textChunk = reinterpret_cast<apl::sg::TextChunk *>(handle);
            const auto &styledText = textChunk->styledText();
            auto spans = styledText.getSpans();
            auto span = spans.at(static_cast<unsigned int>(index));
            return static_cast<jint>(span.type);
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_scenegraph_text_StyledText_nGetSpanStartAt(JNIEnv *env, jclass clazz,
                                                                               jlong handle,
                                                                               jint index) {
            auto textChunk = reinterpret_cast<apl::sg::TextChunk *>(handle);
            const auto &styledText = textChunk->styledText();
            auto spans = styledText.getSpans();
            auto span = spans.at(static_cast<unsigned int>(index));
            return static_cast<jint>(span.start);
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_scenegraph_text_StyledText_nGetSpanEndAt(JNIEnv *env, jclass clazz,
                                                                             jlong handle,
                                                                             jint index) {
            auto textChunk = reinterpret_cast<apl::sg::TextChunk *>(handle);
            const auto &styledText = textChunk->styledText();
            auto spans = styledText.getSpans();
            auto span = spans.at(static_cast<unsigned int>(index));
            return static_cast<jint>(span.end);
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_scenegraph_text_StyledText_nCreateStyledTextIterator(
                JNIEnv *env, jclass clazz,
                jlong handle) {
            auto textChunk = reinterpret_cast<apl::sg::TextChunk *>(handle);
            const auto &styledText = textChunk->styledText();
            auto spans = styledText.getSpans();
            return (jlong) new StyledText::Iterator(styledText);
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_scenegraph_text_StyledText_nStyledTextIteratorNext(JNIEnv *env, jclass clazz, jlong nativePtr) {
            auto it = (StyledText::Iterator *) nativePtr;
            return static_cast<jint>(it->next());
        }

        JNIEXPORT jint JNICALL
        Java_com_amazon_apl_android_scenegraph_text_StyledText_nStyledTextIteratorGetSpanType(JNIEnv *env, jclass clazz, jlong nativePtr) {
            auto it = (StyledText::Iterator *) nativePtr;
            return static_cast<jint>(it->getSpanType());
        }

        JNIEXPORT jintArray JNICALL
        Java_com_amazon_apl_android_scenegraph_text_StyledText_nStyledTextIteratorGetSpanAttributesNames(JNIEnv *env, jclass clazz, jlong nativePtr) {
            auto it = (StyledText::Iterator *) nativePtr;
            auto spanAttributes = it->getSpanAttributes();

            int count = spanAttributes.size();
            jint names[count];
            for (int i = 0; i < count; i++) {
                names[i] = static_cast<int>(spanAttributes[i].name);
            }
            jintArray result = env->NewIntArray(count);
            env->SetIntArrayRegion(result, 0, count, names);

            return result;
        }

        JNIEXPORT jlong JNICALL
        Java_com_amazon_apl_android_scenegraph_text_StyledText_nStyledTextIteratorGetSpanAttributeGetColor(JNIEnv *env, jclass clazz, jlong nativePtr, jint attributeKey) {
            auto it = (StyledText::Iterator *) nativePtr;
            auto spanAttributes = it->getSpanAttributes();

            for (auto attr : spanAttributes) {
                if (attr.name == attributeKey) {
                    return static_cast<jlong>(attr.value.asColor().get());
                }
            }

            return 0;
        }

        JNIEXPORT jdouble JNICALL
        Java_com_amazon_apl_android_scenegraph_text_StyledText_nStyledTextIteratorGetSpanAttributeGetDimension(JNIEnv *env, jclass clazz, jlong nativePtr, jint attributeKey) {
            auto it = (StyledText::Iterator *) nativePtr;
            auto spanAttributes = it->getSpanAttributes();

            for (auto attr : spanAttributes) {
                if (attr.name == attributeKey) {
                    if (attr.value.isAbsoluteDimension()) {
                        auto dimension = attr.value;
                        return static_cast<jdouble>(dimension.getAbsoluteDimension());
                    }
                }
            }

            return 0;
        }

        JNIEXPORT jbyteArray JNICALL
        Java_com_amazon_apl_android_scenegraph_text_StyledText_nStyledTextIteratorGetString(JNIEnv *env, jclass clazz, jlong nativePtr) {
            auto it = (StyledText::Iterator *) nativePtr;
            auto len = it->getString().length();
            jbyteArray bytes = env->NewByteArray(len);
            env->SetByteArrayRegion(bytes, 0, len, (jbyte*) it->getString().c_str());
            return bytes;
        }

        JNIEXPORT void JNICALL
        Java_com_amazon_apl_android_scenegraph_text_StyledText_nDestroyStyledTextIterator(JNIEnv *env, jclass clazz, jlong nativePtr) {
            delete (StyledText::Iterator *) nativePtr;
        }
#ifdef __cplusplus
        }
#endif
    }
}
