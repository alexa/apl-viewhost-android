/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 */

#include <locale>
#include <codecvt>
#include <jni.h>

#include "apl/apl.h"

#include "jniutil.h"

namespace apl {
    namespace jni {
#ifdef __cplusplus
        extern "C" {
#endif

            // TODO: The following methods can be moved to their own JNI file for handling apl::sg::TextChunk
            JNIEXPORT jstring JNICALL
            Java_com_amazon_apl_android_primitive_StyledText_nGetTextFromChunk(JNIEnv *env, jclass clazz,
                                                                            jlong handle) {
                auto textChunk = reinterpret_cast<apl::sg::TextChunk *>(handle);
                const auto &styledText = textChunk->styledText();

                // for emoji characters, rapidjson stores in 4-byte format
                // eg. U+1F33D -> \xF0\x9F\x8C\xBD
                // but NewStringUTF expects modified UTF-8 and \xF0 is invalid
                // so converts the rapidjson value to UTF-16
                // and uses NewString to create Java string instead
                // not using NewStringUTF
                std::u16string u16str = converter.from_bytes(styledText.getText().data());
                return env->NewString((const jchar*)u16str.data(), u16str.length());
            }

            JNIEXPORT jint JNICALL
            Java_com_amazon_apl_android_primitive_StyledText_nGetSpanCountFromChunk(JNIEnv *env, jclass clazz,
                                                                                 jlong handle) {
                auto textChunk = reinterpret_cast<apl::sg::TextChunk *>(handle);
                const auto &styledText = textChunk->styledText();
                auto spans = styledText.getSpans();
                return static_cast<jint>(spans.size());
            }

            JNIEXPORT jint JNICALL
            Java_com_amazon_apl_android_primitive_StyledText_nGetSpanTypeAtFromChunk(JNIEnv *env, jclass clazz,
                                                                                  jlong handle,
                                                                                  jint index) {
                auto textChunk = reinterpret_cast<apl::sg::TextChunk *>(handle);
                const auto &styledText = textChunk->styledText();
                auto spans = styledText.getSpans();
                auto span = spans.at(static_cast<unsigned int>(index));
                return static_cast<jint>(span.type);
            }

            JNIEXPORT jint JNICALL
            Java_com_amazon_apl_android_primitive_StyledText_nGetSpanStartAtFromChunk(JNIEnv *env, jclass clazz,
                                                                                   jlong handle,
                                                                                   jint index) {
                auto textChunk = reinterpret_cast<apl::sg::TextChunk *>(handle);
                const auto &styledText = textChunk->styledText();
                auto spans = styledText.getSpans();
                auto span = spans.at(static_cast<unsigned int>(index));
                return static_cast<jint>(span.start);
            }

            JNIEXPORT jint JNICALL
            Java_com_amazon_apl_android_primitive_StyledText_nGetSpanEndAtFromChunk(JNIEnv *env, jclass clazz,
                                                                                 jlong handle,
                                                                                 jint index) {
                auto textChunk = reinterpret_cast<apl::sg::TextChunk *>(handle);
                const auto &styledText = textChunk->styledText();
                auto spans = styledText.getSpans();
                auto span = spans.at(static_cast<unsigned int>(index));
                return static_cast<jint>(span.end);
            }

            JNIEXPORT jlong JNICALL
            Java_com_amazon_apl_android_primitive_StyledText_nCreateStyledTextIteratorFromChunk(
                    JNIEnv *env, jclass clazz,
                    jlong handle) {
                auto textChunk = reinterpret_cast<apl::sg::TextChunk *>(handle);
                const auto &styledText = textChunk->styledText();
                auto spans = styledText.getSpans();
                return (jlong) new StyledText::Iterator(styledText);
            }

            JNIEXPORT jstring JNICALL
            Java_com_amazon_apl_android_primitive_StyledText_nGetText(JNIEnv *env, jclass clazz,
                                                                      jlong handle, jint propertyId) {
                auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
                const auto &styledText = value.get<StyledText>();

                // for emoji characters, rapidjson stores in 4-byte format
                // eg. U+1F33D -> \xF0\x9F\x8C\xBD
                // but NewStringUTF expects modified UTF-8 and \xF0 is invalid
                // so converts the rapidjson value to UTF-16
                // and uses NewString to create Java string instead
                // not using NewStringUTF
                std::u16string u16str = converter.from_bytes(styledText.getText().data());
                return env->NewString((const jchar*)u16str.data(), u16str.length());
            }

            JNIEXPORT jint JNICALL
            Java_com_amazon_apl_android_primitive_StyledText_nGetSpanCount(JNIEnv *env, jclass clazz,
                                                                           jlong handle,
                                                                           jint propertyId) {
                auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
                const auto &styledText = value.get<StyledText>();
                const auto& spans = styledText.getSpans();
                return static_cast<jint>(spans.size());
            }

            JNIEXPORT jint JNICALL
            Java_com_amazon_apl_android_primitive_StyledText_nGetSpanTypeAt(JNIEnv *env, jclass clazz,
                                                                            jlong handle,
                                                                            jint propertyId,
                                                                            jint index) {
                auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
                const auto &styledText = value.get<StyledText>();
                auto spans = styledText.getSpans();
                auto span = spans.at(static_cast<unsigned int>(index));
                return static_cast<jint>(span.type);
            }

            JNIEXPORT jint JNICALL
            Java_com_amazon_apl_android_primitive_StyledText_nGetSpanStartAt(JNIEnv *env, jclass clazz,
                                                                             jlong handle,
                                                                             jint propertyId,
                                                                             jint index) {
                auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
                const auto &styledText = value.get<StyledText>();
                auto spans = styledText.getSpans();
                auto span = spans.at(static_cast<unsigned int>(index));
                return static_cast<jint>(span.start);
            }

            JNIEXPORT jint JNICALL
            Java_com_amazon_apl_android_primitive_StyledText_nGetSpanEndAt(JNIEnv *env, jclass clazz,
                                                                           jlong handle,
                                                                           jint propertyId,
                                                                           jint index) {
                auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
                const auto &styledText = value.get<StyledText>();
                auto spans = styledText.getSpans();
                auto span = spans.at(static_cast<unsigned int>(index));
                return static_cast<jint>(span.end);
            }

            JNIEXPORT jlong JNICALL
            Java_com_amazon_apl_android_primitive_StyledText_nCreateStyledTextIterator(
                    JNIEnv *env, jclass clazz,
                    jlong handle,
                    jint propertyId) {
                auto value = getLookup<PropertyLookup>(handle)->getObject(static_cast<int>(propertyId), handle);
                const auto &styledText = value.get<StyledText>();
                return (jlong) new StyledText::Iterator(styledText);
            }

            JNIEXPORT jint JNICALL
            Java_com_amazon_apl_android_primitive_StyledText_nStyledTextIteratorNext(JNIEnv *env, jclass clazz, jlong nativePtr) {
                auto it = (StyledText::Iterator *) nativePtr;
                return static_cast<jint>(it->next());
            }

            JNIEXPORT jint JNICALL
            Java_com_amazon_apl_android_primitive_StyledText_nStyledTextIteratorGetSpanType(JNIEnv *env, jclass clazz, jlong nativePtr) {
                auto it = (StyledText::Iterator *) nativePtr;
                return static_cast<jint>(it->getSpanType());
            }

            JNIEXPORT jintArray JNICALL
            Java_com_amazon_apl_android_primitive_StyledText_nStyledTextIteratorGetSpanAttributesNames(JNIEnv *env, jclass clazz, jlong nativePtr) {
                auto it = (StyledText::Iterator *) nativePtr;
                auto spanAttributes = it->getSpanAttributes();

                auto count = spanAttributes.size();
                jint names[count];
                for (size_t i = 0; i < count; i++) {
                    names[i] = static_cast<int>(spanAttributes[i].name);
                }
                jintArray result = env->NewIntArray(count);
                env->SetIntArrayRegion(result, 0, count, names);

                return result;
            }

            JNIEXPORT jlong JNICALL
            Java_com_amazon_apl_android_primitive_StyledText_nStyledTextIteratorGetSpanAttributeGetColor(JNIEnv *env, jclass clazz, jlong nativePtr, jint attributeKey) {
                auto it = (StyledText::Iterator *) nativePtr;
                auto spanAttributes = it->getSpanAttributes();

                for (const auto& attr : spanAttributes) {
                    if (attr.name == attributeKey) {
                        return static_cast<jlong>(attr.value.asColor().get());
                    }
                }

                return 0;
            }

            JNIEXPORT jdouble JNICALL
            Java_com_amazon_apl_android_primitive_StyledText_nStyledTextIteratorGetSpanAttributeGetDimension(JNIEnv *env, jclass clazz, jlong nativePtr, jint attributeKey) {
                auto it = (StyledText::Iterator *) nativePtr;
                auto spanAttributes = it->getSpanAttributes();

                for (const auto& attr : spanAttributes) {
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
            Java_com_amazon_apl_android_primitive_StyledText_nStyledTextIteratorGetString(JNIEnv *env, jclass clazz, jlong nativePtr) {
                auto it = (StyledText::Iterator *) nativePtr;
                auto len = it->getString().length();
                jbyteArray bytes = env->NewByteArray(len);
                env->SetByteArrayRegion(bytes, 0, len, (jbyte*) it->getString().c_str());
                return bytes;
            }

            JNIEXPORT void JNICALL
            Java_com_amazon_apl_android_primitive_StyledText_nDestroyStyledTextIterator(JNIEnv *env, jclass clazz, jlong nativePtr) {
                delete (StyledText::Iterator *) nativePtr;
            }

            JNIEXPORT jlong JNICALL
            Java_com_amazon_apl_android_primitive_StyledText_nGetHash(JNIEnv *env, jclass clazz,
                                                                      jlong nativePtr) {
                auto textChunk = reinterpret_cast<apl::sg::TextChunk *>(nativePtr);
                return static_cast<jlong>(textChunk->hash());
            }


    #ifdef __cplusplus
        }
    #endif
    }
}
